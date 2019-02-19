package com.furongsoft.ide.debugger.java;

import com.alibaba.fastjson.JSONObject;
import com.furongsoft.core.misc.StringUtils;
import com.furongsoft.core.misc.Tracker;
import com.furongsoft.ide.debugger.core.Debugger;
import com.furongsoft.ide.debugger.entities.Stack;
import com.furongsoft.ide.debugger.entities.*;
import com.sun.jdi.Location;
import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java调试器
 *
 * @author Alex
 */
@Component
public class JavaDebugger extends Debugger implements Runnable {
    // refs: https://blog.csdn.net/ksqqxq/article/details/7419758
    // refs: https://www.cnblogs.com/wade-luffy/p/5991785.html#_label4
    // javac -g -source 1.8 -target 1.8 *.java
    // java -Xdebug -Xrunjdwp:transport=dt_socket,suspend=y,server=y,address=8000 Test
    private static final String HOST = "127.0.0.1";
    private static final String PORT = "5000";
    private static final String DT_SOCKET = "dt_socket";
    private static final int MAX_LINES = 1000;

    /**
     * 输出队列
     */
    private final List<String> output = new LinkedList<>();

    /**
     * 虚拟机
     */
    private VirtualMachine vm;

    /**
     * 断点列表
     */
    private ConcurrentHashMap<String, BreakpointRequest> breakpointRequests = new ConcurrentHashMap<>();

    /**
     * 虚拟机事件处理线程
     */
    private Thread thread;

    /**
     * 运行标志
     */
    private boolean runFlag;

    /**
     * 目标进程
     */
    private ProcessExecutor targetProcess;

    /**
     * 当前调试线程
     */
    private ThreadReference threadReference;

    /**
     * 调试请求列表
     */
    private ConcurrentHashMap<ThreadReference, StepRequest> stepRequestMap = new ConcurrentHashMap<>();

    /**
     * 源代码分析器
     */
    private Analyzer analyzer = new Analyzer();

    @Override
    public void dispose() {
        stop();
    }

    @Override
    public void analyze() {
        analyzer.analyze("demos/demo2");
    }

    @Override
    public Symbol getSymbol(String sourcePath, int lineNumber, int columnNumber) {
        return analyzer.getSymbol(sourcePath, lineNumber, columnNumber);
    }

    @Override
    public synchronized boolean addBreakpoint(Breakpoint breakpoint) {
        if (!super.addBreakpoint(breakpoint)) {
            return false;
        }

        return (vm == null) || setBreakpoint(breakpoint);
    }

    @Override
    public synchronized boolean deleteBreakpoint(Breakpoint breakpoint) {
        if (!super.deleteBreakpoint(breakpoint)) {
            return false;
        }

        if (!breakpointRequests.containsKey(breakpoint.key())) {
            return true;
        }

        return clearBreakpoint(breakpoint);
    }

    @Override
    public synchronized boolean setBreakpointEnabled(Breakpoint breakpoint, boolean enabled) {
        if (!breakpointRequests.containsKey(breakpoint.key())) {
            return false;
        }

        if (enabled) {
            breakpointRequests.get(breakpoint.key()).enable();
        } else {
            breakpointRequests.get(breakpoint.key()).disable();
        }

        return true;
    }

    @Override
    public synchronized String getCode(String sourcePath) {
        File file = new File(sourcePath);
        if (!file.exists()) {
            return null;
        }

        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();

        try {
            reader = new BufferedReader(new FileReader(file, Charset.forName("UTF-8")));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }

            return sb.toString();
        } catch (IOException e) {
            Tracker.error(e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Tracker.error(e);
                }
            }
        }
    }

    @Override
    public Collection<String> getConsole() {
        synchronized (output) {
            Collection<String> ret = new ArrayList<>(output);
            output.clear();
            return ret;
        }
    }

    @Override
    public boolean start(String script, String arguments) {
        stop();

        synchronized (this) {
            if (thread != null) {
                return false;
            }

            // 启动脚本
            Map<String, String> map = JSONObject.parseObject(arguments, Map.class);
            StringBuilder sb = new StringBuilder();
            if (arguments != null) {
                map.forEach((key, value) -> sb.append(key).append(" ").append(value));
            }

            String command = String.format("java %s -Xdebug -Xrunjdwp:transport=dt_socket,suspend=y,server=y,address=%s %s", sb.toString(), PORT, script);
            targetProcess = new ProcessExecutor().start(command, output, MAX_LINES);
            if (targetProcess == null) {
                return false;
            }

            // TODO: wait for target proceess
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                targetProcess.stop();
                targetProcess = null;
                return false;
            }

            for (int i = 0; i < 10; ++i) {
                vm = getRemoteJvm(HOST, PORT);
                if (vm != null) {
                    break;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    targetProcess.stop();
                    targetProcess = null;
                    return false;
                }
            }

            if (vm == null) {
                targetProcess.stop();
                targetProcess = null;
                return false;
            }

            registerEvents(null);

            debuggerState = DebuggerState.Running;
            runFlag = true;
            thread = new Thread(this);
            thread.start();
        }

        return true;
    }

    @Override
    public boolean stop() {
        Thread thread;
        synchronized (this) {
            if (targetProcess != null) {
                targetProcess.stop();
                targetProcess = null;
            }

            runFlag = false;
            thread = this.thread;
        }

        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Tracker.error(e);
                return false;
            }
        }

        synchronized (this) {
            this.thread = null;

            if (vm != null) {
                try {
                    vm.exit(0);
                    vm.dispose();
                } catch (Exception e) {
                    Tracker.error(e);
                }
                vm = null;
            }

            debuggerState = DebuggerState.Idle;
            threadReference = null;
            stepRequestMap.clear();
            breakpointRequests.clear();
            breakpoints.values().forEach(breakpoint -> breakpoint.setActive(false));
            location = null;
            if (stack != null) {
                stack.clear();
                stack = null;
            }
            if (variables != null) {
                variables.clear();
                variables = null;
            }
        }

        return true;
    }

    @Override
    public boolean suspend() {
        return false;
    }

    @Override
    public synchronized boolean resume() {
        if (debuggerState != DebuggerState.Breaking) {
            return false;
        }

        threadReference = null;
        stepRequestMap.clear();
        vm.resume();

        return true;
    }

    @Override
    public synchronized boolean stepInto() {
        if (debuggerState != DebuggerState.Breaking) {
            return false;
        }

        if (stepRequestMap.containsKey(threadReference)) {
            EventRequestManager eventRequestManager = vm.eventRequestManager();
            eventRequestManager.deleteEventRequest(stepRequestMap.get(threadReference));
        }

        EventRequestManager eventRequestManager = vm.eventRequestManager();
        StepRequest request = eventRequestManager.createStepRequest(threadReference, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
        request.addCountFilter(1);
        request.enable();
        stepRequestMap.put(threadReference, request);
        vm.resume();

        return true;
    }

    @Override
    public synchronized boolean stepOut() {
        if (debuggerState != DebuggerState.Breaking) {
            return false;
        }

        if (stepRequestMap.containsKey(threadReference)) {
            EventRequestManager eventRequestManager = vm.eventRequestManager();
            eventRequestManager.deleteEventRequest(stepRequestMap.get(threadReference));
        }

        EventRequestManager eventRequestManager = vm.eventRequestManager();
        StepRequest request = eventRequestManager.createStepRequest(this.threadReference, StepRequest.STEP_LINE, StepRequest.STEP_OUT);
        request.addCountFilter(1);
        request.enable();
        stepRequestMap.put(threadReference, request);
        vm.resume();

        return true;
    }

    @Override
    public synchronized boolean stepOver() {
        if (debuggerState != DebuggerState.Breaking) {
            return false;
        }

        if (stepRequestMap.containsKey(threadReference)) {
            EventRequestManager eventRequestManager = vm.eventRequestManager();
            eventRequestManager.deleteEventRequest(stepRequestMap.get(threadReference));
        }

        EventRequestManager eventRequestManager = vm.eventRequestManager();
        StepRequest request = eventRequestManager.createStepRequest(this.threadReference, StepRequest.STEP_LINE, StepRequest.STEP_OVER);
        request.addCountFilter(1);
        request.enable();
        stepRequestMap.put(threadReference, request);
        vm.resume();

        return true;
    }

    @Override
    public void run() {
        EventQueue eventQueue = vm.eventQueue();
        while (runFlag) {
            try {
                EventSet eventSet = eventQueue.remove();
                EventIterator eventIterator = eventSet.eventIterator();
                boolean resume = true;
                while (eventIterator.hasNext()) {
                    Event event = eventIterator.next();
                    try {
                        resume = execute(event) && resume;
                    } catch (Exception e) {
                        Tracker.error(e);
                    }
                }

                if (resume) {
                    eventSet.resume();
                }
            } catch (Exception e) {
                Tracker.error(e);
                break;
            }
        }

        synchronized (this) {
            runFlag = false;
            this.thread = null;

            if (vm != null) {
                try {
                    vm.exit(0);
                    vm.dispose();
                } catch (Exception e) {
                    Tracker.error(e);
                }
                vm = null;
            }

            if (targetProcess != null) {
                targetProcess.stop();
                targetProcess = null;
            }

            debuggerState = DebuggerState.Idle;
            threadReference = null;
            stepRequestMap.clear();
            breakpointRequests.clear();
            breakpoints.values().forEach(breakpoint -> breakpoint.setActive(false));
            location = null;
            if (stack != null) {
                stack.clear();
                stack = null;
            }
            if (variables != null) {
                variables.clear();
                variables = null;
            }
        }
    }

    /**
     * 获取连接器
     *
     * @return 连接器
     */
    private synchronized AttachingConnector getAttachingConnector() {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        List<AttachingConnector> connectors = vmm.attachingConnectors();
        AttachingConnector socketAttachingConnector = null;

        for (Connector connector : connectors) {
            Transport transport = connector.transport();
            if (DT_SOCKET.equals(transport.name())) {
                socketAttachingConnector = (AttachingConnector) connector;
                break;
            }
        }

        return socketAttachingConnector;
    }

    /**
     * 获取虚拟机
     *
     * @param host 主机
     * @param port 端口
     * @return 虚拟机
     */
    private synchronized VirtualMachine getRemoteJvm(String host, String port) {
        AttachingConnector connector = getAttachingConnector();
        if (connector == null) {
            return null;
        }

        Map<String, Connector.Argument> arguments = connector.defaultArguments();
        arguments.get("hostname").setValue(host);
        arguments.get("port").setValue(port);

        try {
            return connector.attach(arguments);
        } catch (IOException | IllegalConnectorArgumentsException e) {
            return null;
        }
    }

    /**
     * 注册事件
     */
    private synchronized void registerEvents(String className) {
        EventRequestManager eventRequestManager = vm.eventRequestManager();

        ClassPrepareRequest classPrepareRequest = eventRequestManager.createClassPrepareRequest();
        classPrepareRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        if (!StringUtils.isNullOrEmpty(className)) {
            classPrepareRequest.addClassFilter(className);
        }
        classPrepareRequest.enable();
    }

    /**
     * 设置断点
     *
     * @param breakpoint 断点
     * @return 是否成功
     */
    private synchronized boolean setBreakpoint(Breakpoint breakpoint) {
        if (vm == null) {
            return false;
        }

        if (breakpointRequests.containsKey(breakpoint.key())) {
            return true;
        }

        EventRequestManager eventRequestManager = vm.eventRequestManager();
        ClassType clazz = (ClassType) vm.classesByName(breakpoint.getClassName()).get(0);

        try {
            Location location = clazz.locationsOfLine(breakpoint.getLineNumber()).get(0);
            BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location);
            breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            breakpointRequest.enable();
            breakpoint.setActive(true);
            breakpointRequests.put(breakpoint.key(), breakpointRequest);

            return true;
        } catch (AbsentInformationException e) {
            super.deleteBreakpoint(breakpoint);
            Tracker.error(e);
            return false;
        }
    }

    /**
     * 清空断点
     *
     * @param breakpoint 断点
     * @return 是否成功
     */
    private synchronized boolean clearBreakpoint(Breakpoint breakpoint) {
        if (vm == null) {
            return false;
        }

        if (!breakpointRequests.containsKey(breakpoint.key())) {
            return false;
        }

        EventRequestManager eventRequestManager = vm.eventRequestManager();
        eventRequestManager.deleteEventRequest(breakpointRequests.get(breakpoint.key()));
        breakpointRequests.remove(breakpoint.key());

        return true;
    }

    /**
     * 处理事件
     *
     * @param event 事件
     * @throws Exception 异常
     */
    private boolean execute(Event event) throws Exception {
        if (event instanceof VMStartEvent) {
            Tracker.info("VMStartEvent");
            return true;
        } else if (event instanceof VMDisconnectEvent) {
            Tracker.info("VMDisconnectEvent");
            return true;
        } else if (event instanceof ClassPrepareEvent) {
            String className = ((ClassPrepareEvent) event).referenceType().name();
            Tracker.info("=========== ClassPrepareEvent -> " + className);
            breakpoints.values().forEach(breakpoint -> {
                if (breakpoint.getClassName().equals(className)) {
                    setBreakpoint(breakpoint);
                }
            });
            return true;
        } else if (event instanceof MethodEntryEvent) {
            Method method = ((MethodEntryEvent) event).method();
            Tracker.info("MethodEntryEvent: " + method.name());
            return true;
        } else if (event instanceof MethodExitEvent) {
            Method method = ((MethodExitEvent) event).method();
            Tracker.info("MethodExitEvent: " + method.name());
            return true;
        } else if (event instanceof BreakpointEvent) {
            List<com.furongsoft.ide.debugger.entities.Location> locations = new ArrayList<>();
            List<Variable> variables = new ArrayList<>();

            BreakpointEvent breakpointEvent = (BreakpointEvent) event;
            ThreadReference threadReference = breakpointEvent.thread();
            List<StackFrame> frames = threadReference.frames();
            for (StackFrame frame : frames) {
                Location location = frame.location();
                Method method = location.method();
                Tracker.info(String.format("=========== frame -> %s (%s:%s)", method.name(), location.sourcePath(), location.lineNumber()));
                locations.add(new com.furongsoft.ide.debugger.entities.Location(location.sourcePath(), location.lineNumber(), method.name()));
            }

            StackFrame stackFrame = threadReference.frame(0);
            List<LocalVariable> localVariables = stackFrame.visibleVariables();
            for (LocalVariable localVariable : localVariables) {
                Value value = stackFrame.getValue(localVariable);
                Tracker.info(String.format("=========== local -> %s %s = %s", value.type(), localVariable.name(), value));
                variables.add(new Variable(VariableType.local, value.type().name(), localVariable.name(), value.toString()));
            }

            Tracker.info(stackFrame.thisObject().type().name());
            List<Field> fields = stackFrame.thisObject().referenceType().allFields();
            Map<Field, Value> map = stackFrame.thisObject().getValues(fields);
            for (Map.Entry<Field, Value> entry : map.entrySet()) {
                Tracker.info(String.format("=========== member -> %s %s = %s", entry.getValue().type(), entry.getKey().name(), entry.getValue()));
                variables.add(new Variable(VariableType.member, entry.getValue().type().name(), entry.getKey().name(), entry.getValue().toString()));
            }

            synchronized (this) {
                this.debuggerState = DebuggerState.Breaking;
                this.location = locations.get(0);
                this.stack = new Stack(locations);
                this.variables = variables;
                this.threadReference = threadReference;
            }

            return false;
        } else if (event instanceof StepEvent) {
            List<com.furongsoft.ide.debugger.entities.Location> locations = new ArrayList<>();
            List<Variable> variables = new ArrayList<>();

            StepEvent stepEvent = (StepEvent) event;
            ThreadReference threadReference = stepEvent.thread();
            List<StackFrame> frames = threadReference.frames();
            for (StackFrame frame : frames) {
                Location location = frame.location();
                Method method = location.method();
                Tracker.info(String.format("=========== frame -> %s (%s:%s)", method.name(), location.sourcePath(), location.lineNumber()));
                locations.add(new com.furongsoft.ide.debugger.entities.Location(location.sourcePath(), location.lineNumber(), method.name()));
            }

            StackFrame stackFrame = threadReference.frame(0);
            List<LocalVariable> localVariables = stackFrame.visibleVariables();
            for (LocalVariable localVariable : localVariables) {
                Value value = stackFrame.getValue(localVariable);
                Tracker.info(String.format("=========== local -> %s %s = %s", value.type(), localVariable.name(), value));
                variables.add(new Variable(VariableType.local, value.type().name(), localVariable.name(), value.toString()));
            }

            Tracker.info(stackFrame.thisObject().type().name());
            List<Field> fields = stackFrame.thisObject().referenceType().allFields();
            Map<Field, Value> map = stackFrame.thisObject().getValues(fields);
            for (Map.Entry<Field, Value> entry : map.entrySet()) {
                Tracker.info(String.format("=========== member -> %s %s = %s", entry.getValue().type(), entry.getKey().name(), entry.getValue()));
                variables.add(new Variable(VariableType.member, entry.getValue().type().name(), entry.getKey().name(), entry.getValue().toString()));
            }

            synchronized (this) {
                this.debuggerState = DebuggerState.Breaking;
                this.location = locations.get(0);
                this.stack = new Stack(locations);
                this.variables = variables;
                this.threadReference = threadReference;
            }

            return false;
        }

        return true;
    }
}

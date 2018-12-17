package com.furongsoft.ide.debugger.java;

import com.alibaba.fastjson.JSONObject;
import com.furongsoft.core.misc.StringUtils;
import com.furongsoft.core.misc.Tracker;
import com.furongsoft.ide.debugger.core.Debugger;
import com.furongsoft.ide.debugger.entities.Breakpoint;
import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java调试器
 *
 * @author Alex
 */
public class JavaDebugger extends Debugger implements Runnable {
    // refs: https://blog.csdn.net/ksqqxq/article/details/7419758
    // refs: https://www.cnblogs.com/wade-luffy/p/5991785.html#_label4
    // javac -g -source 1.8 -target 1.8 Test.java
    // java -Xdebug -Xrunjdwp:transport=dt_socket,suspend=y,server=y,address=8000 Test
    private static final String HOST = "127.0.0.1";
    private static final String PORT = "5000";
    private static final String DT_SOCKET = "dt_socket";
    private static final int MAX_LINES = 1000;

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

    @Override
    public void dispose() {
        stop();
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

        if (!breakpointRequests.contains(breakpoint.key())) {
            return false;
        }

        if (vm != null) {
            EventRequestManager eventRequestManager = vm.eventRequestManager();
            eventRequestManager.deleteEventRequest(breakpointRequests.get(breakpoint.key()));
            breakpointRequests.remove(breakpoint.key());
        }

        return true;
    }

    @Override
    public synchronized boolean setBreakpointEnabled(Breakpoint breakpoint, boolean enabled) {
        if (!breakpointRequests.contains(breakpoint.key())) {
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

            targetProcess = new ProcessExecutor().start(command, MAX_LINES);
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
                vm.exit(0);
                vm.dispose();
                vm = null;
            }

            if (targetProcess != null) {
                targetProcess.stop();
                targetProcess = null;
            }
        }

        return true;
    }

    @Override
    public boolean suspend() {
        return false;
    }

    @Override
    public boolean resume() {
        return false;
    }

    @Override
    public boolean stepIn() {
        return false;
    }

    @Override
    public boolean stepOut() {
        return false;
    }

    @Override
    public boolean stepOver() {
        return false;
    }

    @Override
    public void run() {
        EventQueue eventQueue = vm.eventQueue();
        while (runFlag) {
            try {
                EventSet eventSet = eventQueue.remove();
                EventIterator eventIterator = eventSet.eventIterator();
                while (eventIterator.hasNext()) {
                    Event event = eventIterator.next();
                    try {
                        execute(event);
                    } catch (Exception e) {
                        Tracker.error(e);
                    }
                }

                eventSet.resume();
            } catch (Exception e) {
                Tracker.error(e);
                break;
            }
        }

        synchronized (this) {
            runFlag = false;
            thread = null;

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

        EventRequestManager eventRequestManager = vm.eventRequestManager();
        ClassType clazz = (ClassType) vm.classesByName(breakpoint.getClassName()).get(0);

        try {
            Location location = clazz.locationsOfLine(breakpoint.getLine()).get(0);
            BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location);
            breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            breakpointRequest.enable();
            breakpointRequests.put(breakpoint.key(), breakpointRequest);

            return true;
        } catch (AbsentInformationException e) {
            super.deleteBreakpoint(breakpoint);
            Tracker.error(e);
            return false;
        }
    }

    /**
     * 处理事件
     *
     * @param event 事件
     * @throws Exception 异常
     */
    private void execute(Event event) throws Exception {
        if (event instanceof VMStartEvent) {
            Tracker.info("VMStartEvent");
        } else if (event instanceof VMDisconnectEvent) {
            Tracker.info("VMDisconnectEvent");
        } else if (event instanceof ClassPrepareEvent) {
            String className = ((ClassPrepareEvent) event).referenceType().name();
            Tracker.info("=========== ClassPrepareEvent -> " + className);
            breakpoints.values().forEach(breakpoint -> {
                if (breakpoint.getClassName().equals(className)) {
                    setBreakpoint(breakpoint);
                }
            });
        } else if (event instanceof MethodEntryEvent) {
            Method method = ((MethodEntryEvent) event).method();
            Tracker.info("MethodEntryEvent: " + method.name());
        } else if (event instanceof MethodExitEvent) {
            Method method = ((MethodExitEvent) event).method();
            Tracker.info("MethodExitEvent: " + method.name());
        } else if (event instanceof BreakpointEvent) {
            BreakpointEvent breakpointEvent = (BreakpointEvent) event;
            ThreadReference threadReference = breakpointEvent.thread();
            List<StackFrame> frames = threadReference.frames();
            for (StackFrame frame : frames) {
                Location location = frame.location();
                Method method = location.method();
                Tracker.info(String.format("=========== frame -> %s (%s:%s)", method.name(), location.sourcePath(), location.lineNumber()));
            }

            StackFrame stackFrame = threadReference.frame(0);
            List<LocalVariable> localVariables = stackFrame.visibleVariables();
            // Method method = stackFrame.location().method();
            // List<LocalVariable> localVariables = method.variables();

            for (LocalVariable localVariable : localVariables) {
                Value value = stackFrame.getValue(localVariable);
                Tracker.info(String.format("=========== local -> %s %s = %s", value.type(), localVariable.name(), value));
            }

            Tracker.info(stackFrame.thisObject().type().name());
            List<Field> fields = stackFrame.thisObject().referenceType().allFields();
            Map<Field, Value> map = stackFrame.thisObject().getValues(fields);
            for (Map.Entry<Field, Value> entry : map.entrySet()) {
                Tracker.info(String.format("=========== member -> %s %s = %s", entry.getValue().type(), entry.getKey().name(), entry.getValue()));
            }
        }
    }
}

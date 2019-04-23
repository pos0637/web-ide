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
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String ROOT_PATH = "demos/demo3";

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
    public synchronized boolean compile(String rootPath, String classPath) {
        String command = String.format("javac -J-Duser.language=en -g -source 1.8 -target 1.8 -classpath %s -sourcepath %s %s/*.java", classPath, rootPath, rootPath);
        ProcessExecutor targetProcess = new ProcessExecutor().start(command, output, MAX_LINES);
        if (targetProcess == null) {
            return false;
        }

        targetProcess.join();
        targetProcess.stop();

        return true;
    }

    @Override
    public synchronized boolean analyze(String rootPath) {
        return analyzer.analyze(rootPath);
    }

    @Override
    public synchronized Symbol getDeclarationSymbol(String sourcePath, int lineNumber, int columnNumber) {
        return analyzer.getDeclarationSymbol(sourcePath, lineNumber, columnNumber);
    }

    @Override
    public synchronized String getSymbolValue(String sourcePath, int lineNumber, int columnNumber) {
        if (variables == null) {
            return null;
        }

        Symbol symbol = analyzer.getDeclarationSymbol(sourcePath, lineNumber, columnNumber);
        if (symbol == null) {
            return null;
        }

        Optional<Variable> result = variables.stream().filter(variable -> symbol.getKey().startsWith(variable.getKey())).findFirst();
        if (!result.isPresent()) {
            return null;
        }

        return result.get().getValue();
    }

    /**
     * 执行表达式
     *
     * @param expression 表达式
     * @return 结果
     */
    @Override
    public synchronized Object evaluation(String expression) {
        if (debuggerState != DebuggerState.Breaking) {
            return false;
        }

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        Object result = null;

        try {
            engine.put("__context__", new Invoker());
            engine.eval("" +
                    "load('nashorn:mozilla_compat.js');\n" +
                    "importPackage(com.furongsoft.ide.debugger.java);\n" +
                    String.format("__result__ = %s;", prepareExpression(expression)));
            result = engine.get("__result__");
        } catch (Exception e) {
            Tracker.error(e);
        }

        return result;
    }

    @Override
    public synchronized boolean addBreakpoint(Breakpoint breakpoint) {
        setBreakpointClassName(breakpoint);
        if (!super.addBreakpoint(breakpoint)) {
            return false;
        }

        return (vm == null) || setBreakpoint(breakpoint);
    }

    @Override
    public synchronized boolean deleteBreakpoint(Breakpoint breakpoint) {
        setBreakpointClassName(breakpoint);
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
        File file = new File(ROOT_PATH + '/' + sourcePath);
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
    public synchronized boolean saveCode(String sourcePath, String code) {
        File file = new File(ROOT_PATH + '/' + sourcePath);
        if (!file.exists() || !file.isFile()) {
            return false;
        }

        FileWriter writer = null;

        try {
            writer = new FileWriter(file, Charset.forName("UTF-8"));
            writer.write(code);

            return true;
        } catch (IOException e) {
            Tracker.error(e);
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
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
            if (!map.containsKey("-classpath")) {
                return false;
            }

            if (!compile(map.get("-sourcepath"), map.get("-classpath"))) {
                return false;
            }

            if (!analyze(map.get("-sourcepath"))) {
                return false;
            }

            String command = String.format("java -classpath %s;%s -Djava.library.path=C:/tools/opencv4/build/java/x64 -Xdebug -Xrunjdwp:transport=dt_socket,suspend=y,server=y,address=%s %s", map.get("-sourcepath"), map.get("-classpath"), PORT, script);
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

        debuggerState = DebuggerState.Running;
        threadReference = null;
        location = null;
        if (stack != null) {
            stack.clear();
            stack = null;
        }
        if (variables != null) {
            variables.clear();
            variables = null;
        }

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
     * 设置断点类型名称
     *
     * @param breakpoint 断点
     */
    private void setBreakpointClassName(Breakpoint breakpoint) {
        String sourcePath = breakpoint.getSourcePath();
        String className = sourcePath.substring(0, sourcePath.length() - ".java".length());
        className = className.replace('/', '.');
        breakpoint.setClassName(className);
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
        } else if ((event instanceof BreakpointEvent) || (event instanceof StepEvent)) {
            List<com.furongsoft.ide.debugger.entities.Location> locations = new ArrayList<>();
            List<Variable> variables = new ArrayList<>();

            LocatableEvent locatableEvent = (LocatableEvent) event;
            ThreadReference threadReference = locatableEvent.thread();
            List<StackFrame> frames = threadReference.frames();
            for (StackFrame frame : frames) {
                Location location = frame.location();
                Method method = location.method();
                Tracker.info(String.format("=========== frame -> %s (%s:%s)", method.name(), location.sourcePath(), location.lineNumber()));
                locations.add(new com.furongsoft.ide.debugger.entities.Location(location.sourcePath().replace('\\', '/'), location.lineNumber(), method.name()));
            }

            StackFrame stackFrame = threadReference.frame(0);
            ReferenceType classType = stackFrame.location().declaringType();
            List<LocalVariable> localVariables = stackFrame.visibleVariables();
            Location location = stackFrame.location();
            Method method = location.method();
            for (LocalVariable localVariable : method.arguments()) {
                Value value = stackFrame.getValue(localVariable);
                Tracker.info(String.format("=========== arguments -> %s %s = %s", localVariable.typeName(), localVariable.name(), value));
                variables.add(new Variable(VariableType.local, localVariable.typeName(), localVariable.name(), value == null ? "null" : value.toString(), getSymbolKey(classType, method, localVariable)));
            }

            for (LocalVariable localVariable : localVariables) {
                Value value = stackFrame.getValue(localVariable);
                Tracker.info(String.format("=========== local -> %s %s = %s", localVariable.typeName(), localVariable.name(), value));
                variables.add(new Variable(VariableType.local, localVariable.typeName(), localVariable.name(), value == null ? "null" : value.toString(), getSymbolKey(classType, method, localVariable)));
            }

            if (stackFrame.thisObject() != null) {
                Tracker.info(stackFrame.thisObject().type().name());
                classType = stackFrame.thisObject().referenceType();
                List<Field> fields = classType.allFields();
                Map<Field, Value> map = stackFrame.thisObject().getValues(fields);
                for (Map.Entry<Field, Value> entry : map.entrySet()) {
                    Field field = entry.getKey();
                    Value value = entry.getValue();
                    Tracker.info(String.format("=========== member -> %s %s = %s", field.typeName(), field.name(), value));
                    variables.add(new Variable(VariableType.member, field.typeName(), field.name(), value == null ? "null" : value.toString(), getSymbolKey(classType, field)));
                }
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

    /**
     * 获取符号类型缩写
     *
     * @param classType     类型
     * @param method        方法
     * @param localVariable 变量
     * @return 符号类型缩写
     */
    private String getSymbolKey(ReferenceType classType, Method method, LocalVariable localVariable) {
        // local: [class signature].[method name][method signature]#[name]
        return String.format("%s.%s%s#%s", classType.signature(), method.name(), method.signature(), localVariable.name());
    }

    /**
     * 获取符号类型缩写
     *
     * @param classType 类型
     * @param field     变量
     * @return 符号类型缩写
     */
    private String getSymbolKey(ReferenceType classType, Field field) {
        // member: [class signature].[name])[signature]
        return String.format("%s.%s)%s", classType.signature(), field.name(), field.signature());
    }

    /**
     * 预处理表达式
     *
     * @param expression 表达式
     * @return 表达式
     */
    private String prepareExpression(String expression) {
        Pattern pattern1 = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*[\\s]*\\(");
        Pattern pattern2 = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");
        Pattern pattern3 = Pattern.compile("__invoke\\(");
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();
        StringBuffer sb3 = new StringBuffer();

        Matcher matcher1 = pattern1.matcher(expression);
        while (matcher1.find()) {
            String match = matcher1.group();
            System.out.println(match);
            match = match.substring(0, match.length() - 1);
            matcher1.appendReplacement(sb1, String.format("__invoke(\"%s\", ", match));
        }
        matcher1.appendTail(sb1);

        expression = sb1.toString();
        Matcher matcher2 = pattern2.matcher(expression);
        while (matcher2.find()) {
            String match = matcher2.group();
            System.out.println(match);
            if (match.equals("__invoke")) {
                continue;
            }

            if (matcher2.start() > 0) {
                if (expression.charAt(matcher2.start() - 1) == '\"') {
                    continue;
                }
            }

            matcher2.appendReplacement(sb2, String.format("__invoke(\"%s\")", match));
        }
        matcher2.appendTail(sb2);

        String result = sb2.toString();
        result = result.replaceAll(",[\\s]*\\)", ")");

        Matcher matcher3 = pattern3.matcher(result);
        while (matcher3.find()) {
            if (isLastInvocation(result, matcher3.end())) {
                matcher3.appendReplacement(sb3, "__invoke2(");
            }
        }
        matcher3.appendTail(sb3);

        result = sb3.toString();
        result = result.replaceAll("\\.__invoke", ".invoke");
        result = result.replaceAll("__invoke", "__context__.invoke");
        Tracker.info(result);

        return result;
    }

    /**
     * 是否为最后一次调用函数
     *
     * @param expression 表达式
     * @param openTagPos 开始标签位置
     * @return 是否为最后一次调用函数
     */
    private boolean isLastInvocation(String expression, int openTagPos) {
        int pos = findCloseTag(expression, openTagPos, '(', ')');
        if (pos == -1) {
            return true;
        }

        if (pos >= expression.length() - 1) {
            return true;
        }

        return expression.charAt(pos + 1) != '.';
    }

    /**
     * 寻找闭合标签
     *
     * @param expression 表达式
     * @param openTagPos 开始标签位置
     * @param openTag    开始标签字符
     * @param closeTag   闭合标签字符
     * @return 闭合标签位置
     */
    private int findCloseTag(String expression, int openTagPos, char openTag, char closeTag) {
        for (int i = openTagPos + 1, count = 1; i < expression.length(); ++i) {
            if (expression.charAt(i) == openTag) {
                count++;
            } else if (expression.charAt(i) == closeTag) {
                count--;
            }

            if (count == 0) {
                return i;
            }
        }

        return -1;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public class Invoker {
        private ObjectReference objectReference;

        public Object invoke(String name, Object... arguments) throws Exception {
            if (objectReference == null) {
                Value value = getObjectReference(name);
                if (!(value instanceof ObjectReference)) {
                    throw new Exception("variable not found!");
                }

                objectReference = (ObjectReference) value;
            } else {
                List<Field> fields = objectReference.referenceType().allFields();
                Optional<Field> field = fields.stream().filter(f -> f.name().equals(name)).findFirst();
                if (field.isPresent()) {
                    Value value = objectReference.getValue(field.get());
                    return new Invoker((ObjectReference) value);
                }

                List<Method> methods = objectReference.referenceType().methods();
                Optional<Method> method = methods.stream().filter(m -> m.name().equals(name)).findFirst();
                if (method.isPresent()) {
                    List<Value> values = getArguments(method.get(), arguments);
                    Value value = objectReference.invokeMethod(JavaDebugger.this.threadReference, method.get(), values, ObjectReference.INVOKE_SINGLE_THREADED);
                    if (!(value instanceof ObjectReference)) {
                        throw new Exception("return value invalid!");
                    }

                    return new Invoker((ObjectReference) value);
                }

                throw new Exception("method not found!");
            }

            return this;
        }

        public Object invoke2(String name, Object... arguments) throws Exception {
            if (objectReference == null) {
                Value value = getObjectReference(name);
                if (value == null) {
                    throw new Exception("variable not found!");
                }

                return getRealValue(value);
            } else {
                List<Field> fields = objectReference.referenceType().allFields();
                Optional<Field> field = fields.stream().filter(f -> f.name().equals(name)).findFirst();
                if (field.isPresent()) {
                    Value value = objectReference.getValue(field.get());
                    return getRealValue(value);
                }

                List<Method> methods = objectReference.referenceType().methods();
                Optional<Method> method = methods.stream().filter(m -> m.name().equals(name)).findFirst();
                if (method.isPresent()) {
                    List<Value> values = getArguments(method.get(), arguments);
                    Value value = objectReference.invokeMethod(JavaDebugger.this.threadReference, method.get(), values, ObjectReference.INVOKE_SINGLE_THREADED);
                    return getRealValue(value);
                }

                throw new Exception("method not found!");
            }
        }

        /**
         * 获取对象引用
         *
         * @param name 对象名称
         * @return 对象引用
         */
        private Value getObjectReference(String name) {
            ThreadReference threadReference;
            synchronized (this) {
                threadReference = JavaDebugger.this.threadReference;
            }

            if (threadReference == null) {
                return null;
            }

            try {
                StackFrame stackFrame = threadReference.frame(0);
                if (name.equals("this")) {
                    return stackFrame.thisObject();
                }

                ObjectReference thisObject = stackFrame.thisObject();
                if (thisObject != null) {
                    ReferenceType classType = thisObject.referenceType();
                    Map<Field, Value> map = thisObject.getValues(classType.allFields());
                    for (Map.Entry<Field, Value> entry : map.entrySet()) {
                        if (entry.getKey().name().equals(name)) {
                            return entry.getValue();
                        }
                    }
                }

                List<LocalVariable> localVariables = stackFrame.visibleVariables();
                for (LocalVariable localVariable : localVariables) {
                    if (localVariable.name().equals(name)) {
                        return stackFrame.getValue(localVariable);
                    }
                }

                return null;
            } catch (IncompatibleThreadStateException | AbsentInformationException e) {
                Tracker.error(e);
                return null;
            }
        }

        private List<Value> getArguments(Method method, Object[] arguments) {
            List<Value> values = new ArrayList<>();

            try {
                List<LocalVariable> variables = method.arguments();
                for (int i = 0; i < arguments.length; ++i) {
                    if (arguments[i] instanceof ObjectReference) {
                        values.add((ObjectReference) arguments[i]);
                        continue;
                    }

                    LocalVariable variable = variables.get(i);
                    Type type = variable.type();
                    if (type instanceof BooleanType) {
                        values.add(vm.mirrorOf((boolean) arguments[i]));
                    } else if (type instanceof ByteType) {
                        values.add(vm.mirrorOf((byte) arguments[i]));
                    } else if (type instanceof CharType) {
                        values.add(vm.mirrorOf((char) arguments[i]));
                    } else if (type instanceof ShortType) {
                        values.add(vm.mirrorOf((short) arguments[i]));
                    } else if (type instanceof IntegerType) {
                        values.add(vm.mirrorOf((int) arguments[i]));
                    } else if (type instanceof LongType) {
                        values.add(vm.mirrorOf((long) arguments[i]));
                    } else if (type instanceof FloatType) {
                        values.add(vm.mirrorOf((float) arguments[i]));
                    } else if (type instanceof DoubleType) {
                        values.add(vm.mirrorOf((double) arguments[i]));
                    } else {
                        values.add(null);
                    }
                }
            } catch (AbsentInformationException | ClassNotLoadedException e) {
                Tracker.error(e);
            }

            return values;
        }

        private Object getRealValue(Value value) {
            Type type = value.type();
            if (type instanceof BooleanType) {
                return Boolean.parseBoolean(value.toString());
            } else if (type instanceof ByteType) {
                return Byte.parseByte(value.toString());
            } else if (type instanceof CharType) {
                return value.toString().charAt(0);
            } else if (type instanceof ShortType) {
                return Short.parseShort(value.toString());
            } else if (type instanceof IntegerType) {
                return Integer.parseInt(value.toString());
            } else if (type instanceof LongType) {
                return Long.parseLong(value.toString());
            } else if (type instanceof FloatType) {
                return Float.parseFloat(value.toString());
            } else if (type instanceof DoubleType) {
                return Double.parseDouble(value.toString());
            } else {
                if (type.name().equals("java.lang.String")) {
                    String string = value.toString();
                    return string.substring(1, string.length() - 1);
                } else {
                    return value.toString();
                }
            }
        }
    }
}

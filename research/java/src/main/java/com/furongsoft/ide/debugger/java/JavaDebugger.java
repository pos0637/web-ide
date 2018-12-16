package com.furongsoft.ide.debugger.java;

import com.furongsoft.core.misc.Tracker;
import com.furongsoft.ide.debugger.entities.Breakpoint;
import com.furongsoft.ide.debugger.entities.DebuggerState;
import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Java调试器
 *
 * @author Alex
 */
public class JavaDebugger implements Runnable {
    // refs: https://blog.csdn.net/ksqqxq/article/details/7419758
    // refs: https://www.cnblogs.com/wade-luffy/p/5991785.html#_label4
    // javac -g -source 1.8 -target 1.8 Test.java
    // java -Xdebug -Xrunjdwp:transport=dt_socket,suspend=y,server=y,address=8000 Test
    private static final String DT_SOCKET = "dt_socket";

    private VirtualMachine vm;
    private DebuggerState debuggerState;
    private List<Breakpoint> breakpoints;

    @Override
    public void run() {
        vm = getRemoteJvm("127.0.0.1", "8000");
        registerEvents("Test");
        // addBreakpoint(new Breakpoint("Test", 14, true));

        EventQueue eventQueue = vm.eventQueue();
        while (true) {
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

    private synchronized void registerEvents(String className) {
        EventRequestManager eventRequestManager = vm.eventRequestManager();
        MethodEntryRequest entryReq = eventRequestManager.createMethodEntryRequest();

        entryReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        entryReq.addClassFilter(className);
        entryReq.enable();

        MethodExitRequest exitReq = eventRequestManager.createMethodExitRequest();
        exitReq.addClassFilter(className);
        exitReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        exitReq.enable();
    }

    private synchronized boolean addBreakpoint(Breakpoint breakpoint) {
        EventRequestManager eventRequestManager = vm.eventRequestManager();
        ClassType clazz = (ClassType) vm.classesByName(breakpoint.getClassName()).get(0);

        try {
            Location location = clazz.locationsOfLine(breakpoint.getLine()).get(0);
            BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location);
            breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            breakpointRequest.enable();

            return true;
        } catch (AbsentInformationException e) {
            Tracker.error(e);
            return false;
        }
    }

    private void execute(Event event) throws Exception {
        if (event instanceof VMStartEvent) {
            Tracker.info("VMStartEvent");
        } else if (event instanceof VMDisconnectEvent) {
            Tracker.info("VMDisconnectEvent");
        } else if (event instanceof MethodEntryEvent) {
            Method method = ((MethodEntryEvent) event).method();
            Tracker.info("MethodEntryEvent: " + method.name());
            if (method.name().contains("clinit")) {
                addBreakpoint(new Breakpoint("Test", 14, true));
            }
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

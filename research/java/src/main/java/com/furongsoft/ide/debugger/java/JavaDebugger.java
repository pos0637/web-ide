package com.furongsoft.ide.debugger.java;

import com.furongsoft.ide.debugger.entities.Breakpoint;
import com.furongsoft.ide.debugger.entities.DebuggerState;
import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.request.EventRequestManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Java调试器
 *
 * @author Alex
 */
public class JavaDebugger {
    // refs: https://blog.csdn.net/ksqqxq/article/details/7419758
    // refs: https://www.cnblogs.com/wade-luffy/p/5991785.html#_label4
    private static final String DT_SOCKET = "dt_socket";

    private VirtualMachine vm;
    private DebuggerState debuggerState;
    private List<Breakpoint> breakpoints;

    public boolean addBreakpoint(Breakpoint breakpoint) {
        if (vm == null) {
            return false;
        }

        EventRequestManager eventRequestManager = vm.eventRequestManager();
        ClassType clazz = (ClassType) vm.classesByName(breakpoint.getClassName()).get(0);
        Location location = clazz.locationsOfLine(breakpoint.getLine()).get(0);
    }

    /**
     * 获取连接器
     *
     * @return 连接器
     */
    private AttachingConnector getAttachingConnector() {
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
    private VirtualMachine getRemoteJvm(String host, String port) {
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
}

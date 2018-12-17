package com.furongsoft.ide.debugger.core;

import com.furongsoft.ide.debugger.entities.Breakpoint;
import com.furongsoft.ide.debugger.entities.DebuggerState;
import com.furongsoft.ide.debugger.entities.StackFrame;
import com.furongsoft.ide.debugger.entities.Variable;

import java.util.List;

/**
 * 调试器
 *
 * @author Alex
 */
public abstract class Debugger {
    /**
     * 状态
     */
    private DebuggerState debuggerState;

    /**
     * 断点列表
     */
    private List<Breakpoint> breakpoints;

    /**
     * 调用堆栈
     */
    private StackFrame stackFrame;

    /**
     * 变量
     */
    private List<Variable> variables;
}

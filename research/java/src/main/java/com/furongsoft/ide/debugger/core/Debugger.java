package com.furongsoft.ide.debugger.core;

import com.furongsoft.ide.debugger.entities.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 调试器
 *
 * @author Alex
 */
public abstract class Debugger implements IDebugger {
    /**
     * 状态
     */
    protected DebuggerState debuggerState = DebuggerState.Idle;

    /**
     * 断点列表
     */
    protected ConcurrentHashMap<String, Breakpoint> breakpoints = new ConcurrentHashMap<>();

    /**
     * 断点位置
     */
    protected Location location;

    /**
     * 调用堆栈
     */
    protected Stack stack;

    /**
     * 变量列表
     */
    protected List<Variable> variables;

    @Override
    public synchronized Collection<Breakpoint> getBreakpoints() {
        return ((breakpoints != null) && !breakpoints.isEmpty()) ? breakpoints.values() : null;
    }

    @Override
    public synchronized boolean addBreakpoint(Breakpoint breakpoint) {
        if (breakpoint == null) {
            return false;
        }

        String sourcePath = breakpoint.getSourcePath();
        String className = sourcePath.substring(0, sourcePath.length() - ".java".length());
        breakpoint.setClassName(className);
        breakpoints.put(breakpoint.key(), breakpoint);

        return true;
    }

    @Override
    public synchronized boolean addBreakpoints(List<Breakpoint> breakpoints) {
        if (breakpoints == null) {
            return false;
        }

        breakpoints.forEach(this::addBreakpoint);

        return true;
    }

    @Override
    public synchronized boolean deleteBreakpoint(Breakpoint breakpoint) {
        if (breakpoints.containsKey(breakpoint.key())) {
            breakpoints.remove(breakpoint.key());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean deleteAllBreakpoint() {
        breakpoints.values().forEach(this::deleteBreakpoint);
        return true;
    }

    @Override
    public DebuggerState getState() {
        return debuggerState;
    }

    @Override
    public synchronized Location getLocation() {
        return location;
    }

    @Override
    public synchronized Stack getStack() {
        return stack;
    }

    @Override
    public synchronized Collection<Variable> getVariables() {
        return variables;
    }
}

package com.furongsoft.ide.debugger.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

/**
 * 调试器信息
 *
 * @author Alex
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Information {
    /**
     * 状态
     */
    private DebuggerState debuggerState;

    /**
     * 断点列表
     */
    private Collection<Breakpoint> breakpoints;

    /**
     * 断点位置
     */
    private Location location;

    /**
     * 调用堆栈
     */
    private Stack stack;

    /**
     * 变量列表
     */
    private Collection<Variable> variables;
}

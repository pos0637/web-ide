package com.furongsoft.ide.debugger.entities;

import lombok.Getter;

/**
 * 调试器状态
 *
 * @author Alex
 */
@Getter
public enum DebuggerState {
    /**
     * 空闲
     */
    Idle(0),

    /**
     * 调试中
     */
    Running(1),

    /**
     * 断点调试中
     */
    Breaking(2);

    /**
     * 调试器状态
     */
    private int state;

    DebuggerState(int value) {
        this.state = value;
    }
}

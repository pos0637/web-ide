package com.furongsoft.ide.debugger.entities;

import lombok.Getter;

/**
 * 变量类型
 *
 * @author Alex
 */
@Getter
public enum VariableType {
    /**
     * 局部变量
     */
    local(0),

    /**
     * 成员变量
     */
    member(1),

    /**
     * 静态成员变量
     */
    staticMember(2),

    /**
     * 全局变量
     */
    global(3);

    /**
     * 变量类型
     */
    private int type;

    VariableType(int type) {
        this.type = type;
    }
}

package com.furongsoft.ide.debugger.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 变量
 *
 * @author Alex
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Variable {
    /**
     * 变量类型
     */
    private VariableType type;

    /**
     * 类型名称
     */
    private String className;

    /**
     * 变量名称
     */
    private String name;

    /**
     * 值
     */
    private Object value;
}

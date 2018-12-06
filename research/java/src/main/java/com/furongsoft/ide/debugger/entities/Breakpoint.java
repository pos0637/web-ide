package com.furongsoft.ide.debugger.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 断点
 *
 * @author Alex
 */
@Getter
@Setter
@AllArgsConstructor
public class Breakpoint {
    /**
     * 类名称
     */
    private String className;

    /**
     * 行号
     */
    private int line;

    /**
     * 是否启用
     */
    private boolean enabled;
}

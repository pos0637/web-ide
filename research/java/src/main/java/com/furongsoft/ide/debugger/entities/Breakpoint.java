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
     * 源代码路径
     */
    private String sourcePath;

    /**
     * 类名称
     */
    private String className;

    /**
     * 行号
     */
    private int lineNumber;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 是否激活
     */
    private boolean active;

    /**
     * 获取键值
     *
     * @return 键值
     */
    public String key() {
        return String.format("%s:%d", className, lineNumber);
    }
}

package com.furongsoft.ide.debugger.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 位置
 *
 * @author Alex
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    /**
     * 源代码路径
     */
    private String path;

    /**
     * 行号
     */
    private int lineNumber;

    /**
     * 方法名称
     */
    private String method;
}

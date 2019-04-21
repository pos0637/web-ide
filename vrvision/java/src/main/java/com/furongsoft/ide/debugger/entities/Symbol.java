package com.furongsoft.ide.debugger.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 符号
 *
 * @author Alex
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Symbol {
    /**
     * 声明
     */
    public static final int SYMBOL_TYPE_DECLARATION = 0;

    /**
     * 引用
     */
    public static final int SYMBOL_TYPE_REFS = 1;

    /**
     * 包
     */
    public static final int SYMBOL_SUB_TYPE_PACKAGE = 0;

    /**
     * 类型
     */
    public static final int SYMBOL_SUB_TYPE_TYPE = 1;

    /**
     * 成员变量
     */
    public static final int SYMBOL_SUB_TYPE_MEMBER_VARIABLE = 2;

    /**
     * 临时变量
     */
    public static final int SYMBOL_SUB_TYPE_LOCAL_VARIABLE = 3;

    /**
     * 方法
     */
    public static final int SYMBOL_SUB_TYPE_METHOD = 4;

    /**
     * 类型
     */
    private int type;

    /**
     * 子类型
     */
    private int subType;

    /**
     * 名称
     */
    private String name;

    /**
     * 类型缩写
     */
    private String key;

    /**
     * 源代码路径
     */
    private String sourcePath;

    /**
     * 源代码中符号位置
     */
    private int position;

    /**
     * 符号长度
     */
    private int length;

    /**
     * 行号
     */
    private int lineNumber;

    /**
     * 列号
     */
    private int columnNumber;

    /**
     * 数据
     */
    @JsonIgnore
    private Object data;

    public Symbol(int type, int subType, String name, String key, int position, int length, Object data) {
        this.type = type;
        this.subType = subType;
        this.name = name;
        this.key = key;
        this.position = position;
        this.length = length;
        this.data = data;
    }
}

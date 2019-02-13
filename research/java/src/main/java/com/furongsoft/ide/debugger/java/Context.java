package com.furongsoft.ide.debugger.java;

import com.furongsoft.ide.debugger.entities.Symbol;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文
 *
 * @author Alex
 */
@Getter
@Setter
public class Context {
    /**
     * 源代码路径
     */
    private String sourcePath;

    /**
     * 编译单元
     */
    private CompilationUnit cu;

    /**
     * 符号定义表
     */
    private ConcurrentHashMap<String, Symbol> declarationSymbols = new ConcurrentHashMap<>();

    /**
     * 符号引用表
     */
    private ConcurrentHashMap<String, List<Symbol>> symbols = new ConcurrentHashMap<>();

    /**
     * 添加符号
     *
     * @param symbol 符号
     */
    public void addSymbol(Symbol symbol) {
        symbol.setSourcePath(sourcePath);
        symbol.setLineNumber(cu.getLineNumber(symbol.getPosition()));
        symbol.setColumnNumber(cu.getColumnNumber(symbol.getPosition()));

        if (symbol.getType() == Symbol.SYMBOL_TYPE_DECLARATION) {
            declarationSymbols.put(symbol.getKey(), symbol);
        } else {
            if (!symbols.containsKey(symbol.getKey())) {
                symbols.put(symbol.getKey(), new LinkedList<>());
            }

            symbols.get(symbol.getKey()).add(symbol);
        }
    }
}

package com.furongsoft.ide.debugger.java;

import com.furongsoft.ide.debugger.entities.Symbol;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private CompilationUnit compilationUnit;

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
        symbol.setLineNumber(compilationUnit.getLineNumber(symbol.getPosition()));
        symbol.setColumnNumber(compilationUnit.getColumnNumber(symbol.getPosition()));

        if (symbol.getType() == Symbol.SYMBOL_TYPE_DECLARATION) {
            declarationSymbols.put(symbol.getKey(), symbol);
        } else {
            if (!symbols.containsKey(symbol.getKey())) {
                symbols.put(symbol.getKey(), new LinkedList<>());
            }

            symbols.get(symbol.getKey()).add(symbol);
        }
    }

    /**
     * 获取符号
     *
     * @param sourcePath 源代码路径
     * @param position   位置
     * @return 符号
     */
    public Symbol getSymbol(String sourcePath, int position) {
        List<Symbol> symbolList = new LinkedList<>();

        symbolList.addAll(declarationSymbols.values().stream().filter(symbol ->
                symbol.getSourcePath().equals(sourcePath) && (position >= symbol.getPosition()) && (position <= symbol.getPosition() + symbol.getLength())
        ).collect(Collectors.toList()));

        for (List<Symbol> list : symbols.values()) {
            symbolList.addAll(list.stream().filter(symbol ->
                    symbol.getSourcePath().equals(sourcePath) && (position >= symbol.getPosition()) && (position <= symbol.getPosition() + symbol.getLength())
            ).collect(Collectors.toList()));
        }

        return symbolList.stream().min(Comparator.comparing(Symbol::getLength)).orElse(null);
    }

    /**
     * 获取符号定义
     *
     * @param sourcePath 源代码路径
     * @param position   位置
     * @return 符号定义
     */
    public Symbol getDeclarationSymbol(String sourcePath, int position) {
        Symbol symbol = getSymbol(sourcePath, position);
        if (symbol == null) {
            return null;
        }

        return declarationSymbols.get(symbol.getKey());
    }
}

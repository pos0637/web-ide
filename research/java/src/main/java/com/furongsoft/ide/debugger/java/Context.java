package com.furongsoft.ide.debugger.java;

import com.furongsoft.core.misc.Tracker;
import com.furongsoft.ide.debugger.entities.Symbol;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

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
     * 根目录
     */
    private String rootPath;

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

    private ConcurrentHashMap<String, VariableDeclaration> memberVariables = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, VariableDeclaration> localVariables = new ConcurrentHashMap<>();

    private List<SimpleName> simpleNames = new LinkedList<>();

    public void linkSymbols() {
        for (SimpleName node : simpleNames) {
            IBinding binding = node.resolveBinding();
            if (memberVariables.containsKey(binding.getKey())) {
                VariableDeclaration v = memberVariables.get(binding.getKey());
                addSymbol(new Symbol(Symbol.SYMBOL_TYPE_REFS, Symbol.SYMBOL_SUB_TYPE_MEMBER_VARIABLE, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength(), binding));
                Tracker.info(String.format("SimpleName(member): %s, (%d)", v.getName(), v.getStartPosition()));
            } else if (localVariables.containsKey(binding.getKey())) {
                VariableDeclaration v = localVariables.get(binding.getKey());
                addSymbol(new Symbol(Symbol.SYMBOL_TYPE_REFS, Symbol.SYMBOL_SUB_TYPE_LOCAL_VARIABLE, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength(), binding));
                Tracker.info(String.format("SimpleName(local): %s, (%d)", v.getName(), v.getStartPosition()));
            } else {
                Tracker.info(String.format("SimpleName: %s, (%d)", binding.getName(), node.getStartPosition()));
            }
        }
    }

    /**
     * 添加符号
     *
     * @param symbol 符号
     */
    public void addSymbol(Symbol symbol) {
        symbol.setSourcePath(sourcePath.replace(rootPath + "/", ""));
        symbol.setKey(getSymbolKey(symbol));
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

    /**
     * 获取符号类型缩写
     *
     * @param symbol 符号
     * @return 类型缩写
     */
    private String getSymbolKey(Symbol symbol) {
        // member: L[file]~[class signature];.[name])[signature] -> [class signature].[name])[signature]
        // local: L[file]~[class signature];.[method name][method signature]#[name] -> [class signature].[method name][method signature]#[name]
        String key = symbol.getKey();
        key = key.replace("\\", "/");

        int pos1 = key.indexOf(rootPath);
        int pos2 = key.indexOf('~');
        if (pos2 >= 0) {
            key = key.substring(0, pos1) + key.substring(pos2);
        }

        key = key.replace("/~", ".");
        key = key.replace("~", "");

        return key;
    }
}

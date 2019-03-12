package com.furongsoft.ide.debugger.java;

import com.furongsoft.core.misc.Tracker;
import com.furongsoft.ide.debugger.entities.Symbol;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Analyzer {
    /**
     * 根目录
     */
    private String rootPath;

    /**
     * 上下文
     */
    private Context context = new Context();

    /**
     * 编译单元列表
     */
    private ConcurrentHashMap<String, CompilationUnit> compilationUnits = new ConcurrentHashMap<>();

    /**
     * 分析源代码
     *
     * @param path 源代码根目录
     * @return 是否成功
     */
    public boolean analyze(String path) {
        final File rootFolder = new File(path);
        final List<String> files = new ArrayList<>();
        final List<String> encodings = new ArrayList<>();

        try {
            rootPath = rootFolder.getAbsolutePath().replace('\\', '/');
            Files.walkFileTree(rootFolder.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toFile().getAbsolutePath().endsWith(".java")) {
                        files.add(file.toFile().getAbsolutePath());
                        encodings.add("UTF-8");
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            Tracker.error(e);
            return false;
        }

        final ASTParser parser = ASTParser.newParser(AST.JLS11);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setEnvironment(new String[0], new String[0], null, true);

        try {
            context = new Context();
            FileASTRequestor requestor = new FileASTRequestor() {
                @Override
                public void acceptAST(String sourceFilePath, CompilationUnit cu) {
                    sourceFilePath = sourceFilePath.replace('\\', '/');
                    compilationUnits.put(sourceFilePath, cu);
                    context.setRootPath(rootPath);
                    context.setSourcePath(sourceFilePath);
                    context.setCompilationUnit(cu);
                    cu.accept(new Visitor(context));
                }
            };
            String[] bindingKeys = new String[]{};

            compilationUnits.clear();
            parser.createASTs(files.toArray(new String[files.size()]), encodings.toArray(new String[encodings.size()]), bindingKeys, requestor, null);
            context.linkSymbols();
        } catch (Exception e) {
            Tracker.error(e);
            return false;
        }

        return true;
    }

    /**
     * 获取符号
     *
     * @param sourcePath   源代码路径
     * @param lineNumber   行号
     * @param columnNumber 列号
     * @return 符号
     */
    public Symbol getSymbol(String sourcePath, int lineNumber, int columnNumber) {
        String path = rootPath + '/' + sourcePath;
        if (!compilationUnits.containsKey(path)) {
            return null;
        }

        return context.getSymbol(path, compilationUnits.get(path).getPosition(lineNumber, columnNumber));
    }

    /**
     * 获取符号定义
     *
     * @param sourcePath   源代码路径
     * @param lineNumber   行号
     * @param columnNumber 列号
     * @return 符号定义
     */
    public Symbol getDeclarationSymbol(String sourcePath, int lineNumber, int columnNumber) {
        String path = rootPath + '/' + sourcePath;
        if (!compilationUnits.containsKey(path)) {
            return null;
        }

        return context.getDeclarationSymbol(sourcePath, compilationUnits.get(path).getPosition(lineNumber, columnNumber));
    }
}

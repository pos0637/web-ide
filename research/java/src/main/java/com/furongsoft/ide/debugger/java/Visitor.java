package com.furongsoft.ide.debugger.java;

import org.eclipse.jdt.core.dom.*;

/**
 * AST访问器
 *
 * @author Alex
 * @hidden https://www.eclipse.org/articles/article.php?file=Article-JavaCodeManipulation_AST/index.html
 */
public class Visitor extends ASTVisitor {
    @Override
    public boolean visit(FieldAccess node) {
        System.out.println(String.format("FieldAccess: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        for (Object obj : node.fragments()) {
            VariableDeclarationFragment v = (VariableDeclarationFragment) obj;
            System.out.println(String.format("FieldDeclaration: %s, (%d)", v.getName(), v.getStartPosition()));
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        System.out.println(String.format("MethodDeclaration: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        System.out.println(String.format("MethodInvocation: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        System.out.println(String.format("TypeDeclaration: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        for (Object obj : node.fragments()) {
            VariableDeclarationFragment v = (VariableDeclarationFragment) obj;
            System.out.println(String.format("VariableDeclarationFragment: %s, (%d)", v.getName(), v.getStartPosition()));
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(SimpleName node) {
        IBinding binding = node.resolveBinding();
        System.out.println(String.format("SimpleName: %s, (%d)", binding.getName(), node.getStartPosition()));
        return super.visit(node);
    }
}

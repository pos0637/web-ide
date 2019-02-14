package com.furongsoft.ide.debugger.java;

import com.furongsoft.ide.debugger.entities.Symbol;
import org.eclipse.jdt.core.dom.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * AST访问器
 *
 * @author Alex
 * @hidden https://www.eclipse.org/articles/article.php?file=Article-JavaCodeManipulation_AST/index.html
 * @hidden https://www.programcreek.com/java-api-examples/?class=org.eclipse.jdt.core.dom.ASTParser&method=createASTs
 */
public class Visitor extends ASTVisitor {
    private Context context;
    private ConcurrentHashMap<IBinding, VariableDeclarationFragment> memberVariables = new ConcurrentHashMap<>();
    private ConcurrentHashMap<IBinding, VariableDeclarationFragment> localVariables = new ConcurrentHashMap<>();

    public Visitor(Context context) {
        this.context = context;
    }

    @Override
    public boolean visit(FieldAccess node) {
        System.out.println(String.format("FieldAccess: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        for (Object obj : node.fragments()) {
            VariableDeclarationFragment v = (VariableDeclarationFragment) obj;
            IBinding binding = v.resolveBinding();
            memberVariables.put(binding, v);
            context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_DECLARATION, Symbol.SYMBOL_SUB_TYPE_MEMBER_VARIABLE, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength()));
            System.out.println(String.format("FieldDeclaration: %s, (%d)", v.getName(), v.getStartPosition()));
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        IBinding binding = node.resolveBinding();
        context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_DECLARATION, Symbol.SYMBOL_SUB_TYPE_METHOD, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength()));
        System.out.println(String.format("MethodDeclaration: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        IBinding binding = node.resolveMethodBinding();
        // context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_REFS, Symbol.SYMBOL_SUB_TYPE_METHOD, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength()));
        System.out.println(String.format("MethodInvocation: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        IBinding binding = node.resolveBinding();
        context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_DECLARATION, Symbol.SYMBOL_SUB_TYPE_TYPE, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength()));
        System.out.println(String.format("TypeDeclaration: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        for (Object obj : node.fragments()) {
            VariableDeclarationFragment v = (VariableDeclarationFragment) obj;
            IBinding binding = v.resolveBinding();
            localVariables.put(binding, v);
            context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_DECLARATION, Symbol.SYMBOL_SUB_TYPE_LOCAL_VARIABLE, binding.getName(), binding.getKey(), v.getStartPosition(), v.getLength()));
            System.out.println(String.format("VariableDeclarationFragment: %s, (%d)", v.getName(), v.getStartPosition()));
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(SimpleName node) {
        IBinding binding = node.resolveBinding();
        if (binding == null) {
            System.out.println(String.format("SimpleName: %s, null", node.getFullyQualifiedName()));
            return super.visit(node);
        }

        if (memberVariables.containsKey(binding)) {
            VariableDeclarationFragment v = memberVariables.get(binding);
            context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_REFS, Symbol.SYMBOL_SUB_TYPE_MEMBER_VARIABLE, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength()));
            System.out.println(String.format("SimpleName(member): %s, (%d)", v.getName(), v.getStartPosition()));
        } else if (localVariables.containsKey(binding)) {
            VariableDeclarationFragment v = localVariables.get(binding);
            context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_REFS, Symbol.SYMBOL_SUB_TYPE_LOCAL_VARIABLE, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength()));
            System.out.println(String.format("SimpleName(local): %s, (%d)", v.getName(), v.getStartPosition()));
        } else {
            System.out.println(String.format("SimpleName: %s, (%d)", binding.getName(), node.getStartPosition()));
        }

        return super.visit(node);
    }
}

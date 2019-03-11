package com.furongsoft.ide.debugger.java;

import com.furongsoft.core.misc.Tracker;
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

    public Visitor(Context context) {
        this.context = context;
    }

    @Override
    public boolean visit(FieldAccess node) {
        Tracker.info(String.format("FieldAccess: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        for (Object obj : node.fragments()) {
            VariableDeclaration v = (VariableDeclaration) obj;
            IBinding binding = v.resolveBinding();
            context.getMemberVariables().put(binding.getKey(), v);
            context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_DECLARATION, Symbol.SYMBOL_SUB_TYPE_MEMBER_VARIABLE, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength(), binding));
            Tracker.info(String.format("FieldDeclaration: %s, (%d)", v.getName(), v.getStartPosition()));
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        IBinding binding = node.resolveBinding();
        context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_DECLARATION, Symbol.SYMBOL_SUB_TYPE_METHOD, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength(), binding));
        Tracker.info(String.format("MethodDeclaration: %s, (%d)", node.getName(), node.getStartPosition()));

        node.parameters().forEach(n -> {
            SingleVariableDeclaration v = (SingleVariableDeclaration) n;
            IBinding binding1 = v.resolveBinding();
            context.getLocalVariables().put(binding1.getKey(), v);
            context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_DECLARATION, Symbol.SYMBOL_SUB_TYPE_LOCAL_VARIABLE, binding1.getName(), binding1.getKey(), v.getStartPosition(), v.getLength(), binding));
            Tracker.info(String.format("MethodParametersDeclaration: %s, (%d)", v.getName(), v.getStartPosition()));
        });

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        IBinding binding = node.resolveMethodBinding();
        if (binding == null) {
            Tracker.info(String.format("MethodInvocation: %s, null", node.getName()));
            return super.visit(node);
        }

        context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_REFS, Symbol.SYMBOL_SUB_TYPE_METHOD, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength(), binding));
        Tracker.info(String.format("MethodInvocation: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        IBinding binding = node.resolveBinding();
        context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_DECLARATION, Symbol.SYMBOL_SUB_TYPE_TYPE, binding.getName(), binding.getKey(), node.getStartPosition(), node.getLength(), binding));
        Tracker.info(String.format("TypeDeclaration: %s, (%d)", node.getName(), node.getStartPosition()));
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        for (Object obj : node.fragments()) {
            VariableDeclaration v = (VariableDeclaration) obj;
            IVariableBinding binding = v.resolveBinding();
            context.getLocalVariables().put(binding.getKey(), v);
            context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_REFS, Symbol.SYMBOL_SUB_TYPE_TYPE, binding.getType().getName(), binding.getType().getKey(), node.getStartPosition(), node.getLength(), binding));
            context.addSymbol(new Symbol(Symbol.SYMBOL_TYPE_DECLARATION, Symbol.SYMBOL_SUB_TYPE_LOCAL_VARIABLE, binding.getName(), binding.getKey(), v.getStartPosition(), v.getLength(), binding));
            Tracker.info(String.format("VariableDeclaration: %s, (%d)", v.getName(), v.getStartPosition()));
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(SimpleName node) {
        IBinding binding = node.resolveBinding();
        if (binding == null) {
            Tracker.info(String.format("SimpleName: %s, null", node.getFullyQualifiedName()));
            return super.visit(node);
        }

        context.getSimpleNames().add(node);

        return super.visit(node);
    }
}

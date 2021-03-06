package com.furongsoft.ide.debugger.core;

import com.furongsoft.ide.debugger.entities.*;

import java.util.Collection;
import java.util.List;

/**
 * 调试器
 *
 * @author Alex
 */
public interface IDebugger {
    /**
     * 销毁
     */
    void dispose();

    /**
     * 编译源代码
     *
     * @param rootPath  根目录
     * @param classPath 类目录列表
     * @return 是否成功
     */
    boolean compile(String rootPath, String classPath);

    /**
     * 分析源代码
     *
     * @param rootPath 根目录
     * @return 是否成功
     */
    boolean analyze(String rootPath);

    /**
     * 获取定义符号
     *
     * @param sourcePath   源代码路径
     * @param lineNumber   行号
     * @param columnNumber 列号
     * @return 定义符号
     */
    Symbol getDeclarationSymbol(String sourcePath, int lineNumber, int columnNumber);

    /**
     * 获取符号值
     *
     * @param sourcePath   源代码路径
     * @param lineNumber   行号
     * @param columnNumber 列号
     * @return 符号值
     */
    String getSymbolValue(String sourcePath, int lineNumber, int columnNumber);

    /**
     * 执行表达式
     *
     * @param expression 表达式
     * @return 结果
     */
    Object evaluation(String expression);

    /**
     * 获取断点
     *
     * @return 断点列表
     */
    Collection<Breakpoint> getBreakpoints();

    /**
     * 添加断点
     *
     * @param breakpoint 断点
     * @return 是否成功
     */
    boolean addBreakpoint(Breakpoint breakpoint);

    /**
     * 添加断点
     *
     * @param breakpoints 断点列表
     * @return 是否成功
     */
    boolean addBreakpoints(List<Breakpoint> breakpoints);

    /**
     * 启用断点
     *
     * @param breakpoint 断点
     * @param enabled    是否启用
     * @return 是否成功
     */
    boolean setBreakpointEnabled(Breakpoint breakpoint, boolean enabled);

    /**
     * 删除断点
     *
     * @param breakpoint 断点
     * @return 是否成功
     */
    boolean deleteBreakpoint(Breakpoint breakpoint);

    /**
     * 删除所有断点
     *
     * @return 是否成功
     */
    boolean deleteAllBreakpoint();

    /**
     * 获取源代码
     *
     * @param sourcePath 源代码路径
     * @return 源代码
     */
    String getCode(String sourcePath);

    /**
     * 保存源代码
     *
     * @param sourcePath 源代码路径
     * @param code       源代码
     * @return 是否成功
     */
    boolean saveCode(String sourcePath, String code);

    /**
     * 获取状态
     *
     * @return 状态
     */
    DebuggerState getState();

    /**
     * 获取断点位置
     *
     * @return 断点位置
     */
    Location getLocation();

    /**
     * 获取调用堆栈
     *
     * @return 调用堆栈
     */
    Stack getStack();

    /**
     * 获取变量列表
     *
     * @return 变量列表
     */
    Collection<Variable> getVariables();

    /**
     * 获取控制台输出
     *
     * @return 控制台输出
     */
    Collection<String> getConsole();

    /**
     * 开始调试
     *
     * @param script    启动脚本
     * @param arguments 参数
     * @return 是否成功
     */
    boolean start(String script, String arguments);

    /**
     * 结束调试
     *
     * @return 是否成功
     */
    boolean stop();

    /**
     * 中断程序
     *
     * @return 是否成功
     */
    boolean suspend();

    /**
     * 继续运行
     *
     * @return 是否成功
     */
    boolean resume();

    /**
     * 进入当前方法
     *
     * @return 是否成功
     */
    boolean stepInto();

    /**
     * 退出当前方法
     *
     * @return 是否成功
     */
    boolean stepOut();

    /**
     * 执行下一条程序
     *
     * @return 是否成功
     */
    boolean stepOver();


}

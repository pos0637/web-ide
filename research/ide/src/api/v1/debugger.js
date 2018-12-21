import { request } from '~/components/request';

/**
 * 获取代码
 *
 * @export
 * @param {*} path 代码路径
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getCode(path, succ, err) {
    request('/api/v1/debugger/codes', 'get', { path: path }, succ, err);
}

/**
 * 获取调试器信息
 *
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getInformation(succ, err) {
    request('/api/v1/debugger/information', 'get', null, succ, err);
}

/**
 * 获取调试器状态
 *
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getState(succ, err) {
    request('/api/v1/debugger/state', 'get', null, succ, err);
}

/**
 * 获取断点位置
 *
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getLocation(succ, err) {
    request('/api/v1/debugger/location', 'get', null, succ, err);
}

/**
 * 获取调用堆栈
 *
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getStack(succ, err) {
    request('/api/v1/debugger/stack', 'get', null, succ, err);
}

/**
 * 获取变量列表
 *
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getVariables(succ, err) {
    request('/api/v1/debugger/variables', 'get', null, succ, err);
}

/**
 * 获取控制台输出
 *
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getConsole(succ, err) {
    request('/api/v1/debugger/console', 'get', null, succ, err);
}

/**
 * 获取断点列表
 *
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getBreakpoints(succ, err) {
    request('/api/v1/debugger/breakpoints', 'get', null, succ, err);
}

/**
 * 添加断点
 *
 * @export
 * @param {*} className 类名称
 * @param {*} lineNumber 行号
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function addBreakpoint(className, lineNumber, succ, err) {
    request('/api/v1/debugger/breakpoints', 'post', { className: className, lineNumber: lineNumber, enabled: true }, succ, err);
}

/**
 * 删除断点
 *
 * @export
 * @param {*} className 类名称
 * @param {*} lineNumber 行号
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function deleteBreakpoint(className, lineNumber, succ, err) {
    request('/api/v1/debugger/breakpoints/delete', 'post', { className: className, lineNumber: lineNumber, enabled: true }, succ, err);
}

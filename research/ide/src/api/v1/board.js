import { request } from '~/components/request';

/**
 * 获取概览数据
 *
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getOverviewData(succ, err) {
    request('/api/v1/board/overview', 'get', null, succ, err);
}

/**
 * 获取主视图数据
 *
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getMainData(succ, err) {
    request('/api/v1/board/main', 'get', null, succ, err);
}

/**
 * 获取负载视图数据
 *
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getLoadData(succ, err) {
    request('/api/v1/board/load', 'get', null, succ, err);
}

/**
 * 获取变压器视图数据
 * 
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getTransformerData(succ, err) {
    request('/api/v1/board/transformer', 'get', null, succ, err);
}

/**
 * 获取BMS视图数据
 * 
 * @export
 * @param {*} succ 成功处理函数
 * @param {*} err 错误处理函数
 */
export function getBmsData(succ, err) {
    request('/api/v1/board/bms', 'get', null, succ, err);
}

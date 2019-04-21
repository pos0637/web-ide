/**
 * 数值四舍五入为指定小数位数的数字
 *
 * @export
 * @param {*} value 数值
 * @param {*} num 规定小数的位数
 */
export function toFixed(value, num) {
    if (typeof value !== 'number') {
        return value;
    } else {
        return value.toFixed(num);
    }
}

/**
 * 数值字符串前补0
 *
 * @export
 * @param {*} value 数值
 * @param {*} num 补0位数
 */
export function pad(value, num) {
    let padding = '';
    for (let i = 0; i < num; i += 1) {
        padding += '0';
    }

    return (padding + value).slice(-num);
}

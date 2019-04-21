import React from 'react';
import PropTypes from 'prop-types';
import { Button as AntdButton } from 'antd';
import BaseComponent from '~/components/baseComponent';
import { request } from '~/components/request';

/**
 * 按钮组件
 *
 * @export
 * @class Button
 * @extends {BaseComponent}
 */
export default class Button extends BaseComponent {
    static propTypes = {
        url: PropTypes.string, // 请求地址
        method: PropTypes.string, // 请求方法
        params: PropTypes.object, // 请求参数
        resolve: PropTypes.func, // 请求成功事件处理函数
        reject: PropTypes.func, // 请求失败事件处理函数
        onClick: PropTypes.func, // 点击事件处理函数
        waitForComplete: PropTypes.bool // 是否等待处理完成
    }

    static defaultProps = {
        url: null,
        method: 'get',
        params: null,
        resolve: null,
        reject: null,
        onClick: null,
        waitForComplete: false
    }

    state = {
        loading: false
    }

    render() {
        return (
            <AntdButton loading={this.state.loading} onClick={() => this._onClick()} {...this.getRestProps()}>{this.props.children}</AntdButton>
        );
    }

    /**
     * 处理完成
     *
     * @memberof Button
     */
    complete() {
        this.setState({ loading: false });
    }

    /**
     * 点击事件处理函数
     *
     * @memberof Button
     */
    _onClick() {
        if (!this.props.url || !this.props.method) {
            this.props.onClick && this.props.waitForComplete && this.setState({ loading: true });
            this.props.onClick && this.props.onClick(this, () => this.setState({ loading: false }));
            return;
        }

        this.setState({ loading: true });
        request(this.props.url, this.props.method, this.props.params, data => {
            this.setState({ loading: false });
            this.props.resolve && this.props.resolve(data);
        }, error => {
            this.setState({ loading: false });
            this.props.reject && this.props.reject(error);
        });
    }
}

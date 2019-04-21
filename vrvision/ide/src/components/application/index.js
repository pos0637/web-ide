import React from 'react';
import PropTypes from 'prop-types';
import intl from 'react-intl-universal';
import { Spin } from 'antd';

// 加载测试模块
(process.env.NODE_ENV === 'development') && require('~/mock');

/**
 * 应用
 *
 * @class Application
 * @extends {React.Component}
 */
export default class Application extends React.Component {
    static propTypes = {
        currentLocale: PropTypes.string,
        locales: PropTypes.object
    }

    static defaultProps = {
        currentLocale: 'ZH-CN.framework',
        locales: {}
    }

    static childContextTypes = {
        view: PropTypes.func // 视图组件
    }

    state = { loadLocale: false }

    /**
     * 视图组件
     *
     * @memberof Application
     */
    view = null

    getChildContext() {
        return { view: () => this.view };
    }

    componentDidMount() {
        const locales = {
            'ZH-CN.framework': require('~/locales/zh-CN.json')
        };

        Object.assign(this.props.locales[this.props.currentLocale] || {}, locales[`${this.props.currentLocale}.framework`]);
        Object.assign(locales, this.props.locales);

        intl.init({
            currentLocale: this.props.currentLocale, locales
        }).then(() =>
            this.setState({ loadLocale: true })
        )
    }

    render() {
        const { loadLocale } = this.state;
        const child = React.cloneElement(this.props.children, { ref: (ref) => { this.view = ref; } });

        return (
            loadLocale ? <div style={{ width: '100%', height: '100%' }}>{child}</div> : <Spin />
        );
    }
}

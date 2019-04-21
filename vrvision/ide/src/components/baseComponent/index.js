import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import PropTypes from 'prop-types';

/**
 * 基础组件
 *
 * @export
 * @class BaseComponent
 * @extends {React.Component}
 */
export default class BaseComponent extends React.Component {
    static childContextTypes = {
        parent: PropTypes.func // 父组件
    }

    static contextTypes = {
        view: PropTypes.func, // 视图组件
        parent: PropTypes.func // 父组件
    }

    /**
     * 子组件列表
     *
     * @memberof BaseComponent
     */
    children = {}

    constructor(props) {
        super(props);
        this.shouldComponentUpdate = PureRenderMixin.shouldComponentUpdate.bind(this);
        this.initialize && this.initialize();
    }

    getChildContext() {
        return { parent: () => this };
    }

    componentDidMount() {
        this.getParent() && this.getName() && this.getParent().registerChildComponent(this);
    }

    componentWillUnmount() {
        this.getParent() && this.getName() && this.getParent().unregisterChildComponent(this);
    }

    /**
     * 获取视图组件
     *
     * @returns 视图组件
     * @memberof BaseComponent
     */
    getView() {
        return !this.context.view ? null : this.context.view();
    }

    /**
     * 获取父组件
     *
     * @returns 父组件
     * @memberof BaseComponent
     */
    getParent() {
        return !this.context.parent ? null : this.context.parent();
    }

    /**
     * 获取其余属性
     *
     * @returns 其余属性
     * @memberof BaseComponent
     */
    getRestProps() {
        if (!this.constructor.propTypes) {
            return this.props;
        }

        const props = {};
        Object.entries(this.props).forEach(prop => {
            const [key, value] = prop;
            if (!this.constructor.propTypes[key]) {
                props[key] = value;
            }
        });

        return props;
    }

    /**
     * 获取组件名称
     *
     * @returns 名称
     * @memberof BaseComponent
     */
    getName() {
        return this.props.name || null;
    }

    /**
     * 获取相对位置
     * 
     * @param {*} left 横坐标
     * @param {*} top 纵坐标
     * @memberof BaseComponent
     */
    getRelativePosition(left, top) {
        const parent = this.getParent();
        if ((parent === null) || (typeof parent.calcRelativePosition === 'undefined')) {
            return { left: left, top: top };
        }
        else {
            return parent.calcRelativePosition(left, top);
        }
    }

    /**
     * 注册子组件
     *
     * @param {*} child 子组件
     * @memberof BaseComponent
     */
    registerChildComponent(child) {
        this.children[child.getName()] = child;
    }

    /**
     * 注销子组件
     *
     * @param {*} child 子组件
     * @memberof BaseComponent
     */
    unregisterChildComponent(child) {
        delete this.children[child.getName()];
    }

    /**
     * 获取子组件
     *
     * @param {*} name 子组件名称
     * @returns 子组件
     * @memberof BaseComponent
     */
    child(name) {
        return this.children[name];
    }
}

import React from 'react';
import PropTypes from 'prop-types';
import { Input as AntdInput, Form } from 'antd';
import BaseComponent from '~/components/baseComponent';

/**
 * 输入框组件
 *
 * @export
 * @class Form
 * @extends {BaseComponent}
 */
export default class Input extends BaseComponent {
    static propTypes = {
        id: PropTypes.string.isRequired, // 索引
        url: PropTypes.string, // 请求地址
        method: PropTypes.string, // 请求方法
        params: PropTypes.object, // 请求参数
        label: PropTypes.string, // 标题
        hasFeedback: PropTypes.bool, // 是否反馈
        help: PropTypes.string, // 帮助内容
        required: PropTypes.bool, // 是否必填
        requiredMessage: PropTypes.string, // 必填提示信息 
        rules: PropTypes.array, // 内容规则
        validator: PropTypes.func, // 内容检查函数
        initialValue: PropTypes.any // 初始值
    }

    static defaultProps = {
        id: '123',
        url: null,
        method: 'get',
        params: null,
        label: undefined,
        hasFeedback: false,
        help: undefined,
        required: false,
        requiredMessage: undefined,
        rules: [],
        validator: undefined,
        initialValue: undefined
    }

    render() {
        const { getFieldDecorator } = this.props.form;

        const formItemLayout = {
            labelCol: {
                xs: { span: 24 },
                sm: { span: 8 }
            },
            wrapperCol: {
                xs: { span: 24 },
                sm: { span: 16 }
            }
        };

        const newRules = [
            ...this.props.rules
        ];

        this.props.required && newRules.push({
            required: this.props.required,
            message: this.props.requiredMessage
        });

        this.props.validator && newRules.push({
            validator: this.props.validator
        });

        const { form, ...props } = this.getRestProps();

        return (
            <Form.Item
                {...formItemLayout}
                label={this.props.label}
                hasFeedback={this.props.hasFeedback}
                help={this.props.help}
            >
                {getFieldDecorator(this.props.id, {
                    rules: newRules,
                    initialValue: this.props.initialValue
                })(
                    <AntdInput {...props}>{this.props.children}</AntdInput>
                )}
            </Form.Item>
        );
    }
}

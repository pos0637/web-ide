import React from 'react';
import PropTypes from 'prop-types';
import intl from 'react-intl-universal';
import { Select as AntdSelect, Spin, Button, Form } from 'antd';
import BaseComponent from '~/components/baseComponent';
import { request } from '~/components/request';

/**
 * 选择器组件
 *
 * @export
 * @class Select
 * @extends {BaseComponent}
 */
export default class Select extends BaseComponent {
    static propTypes = {
        url: PropTypes.string.isRequired, // 请求地址
        multiple: PropTypes.bool, // 是否多选
        width: PropTypes.string, // 宽度
        label: PropTypes.string, // 标题
        hasFeedback: PropTypes.bool, // 是否反馈
        help: PropTypes.string, // 帮助内容
        required: PropTypes.bool, // 是否必填
        requiredMessage: PropTypes.string, // 必填提示信息 
        rules: PropTypes.array, // 内容规则
        validator: PropTypes.func, // 内容检查函数
        initialValue: PropTypes.any // 初始值,数组内容为字符串格式键值
    }

    static defaultProps = {
        url: null,
        multiple: false,
        width: '200px',
        label: undefined,
        hasFeedback: false,
        help: undefined,
        required: false,
        requiredMessage: undefined,
        rules: [],
        validator: undefined,
        initialValue: undefined
    }

    state = {
        data: [],
        loading: false
    }

    constructor(props) {
        super(props);
        this.selectedItems = [];
    }

    componentDidMount() {
        super.componentDidMount();
        this._onSearch();
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
                    <AntdSelect
                        {...props}
                        mode={!this.props.multiple ? undefined : "multiple"}
                        allowClear
                        showSearch
                        optionFilterProp="children"
                        notFoundContent={this.state.loading ? <Spin size="small" /> : this._getEmptyText()}
                        onSearch={(value) => this._onSearch(value)}
                        onChange={(value) => this._onChange(value)}
                        style={{ width: this.props.width }}
                    >
                        {this.state.data.map(d => <AntdSelect.Option key={d.value}>{d.text}</AntdSelect.Option>)}
                        <AntdSelect.Option value="disabled" disabled>{this.state.loading ? <Spin size="small" style={{ paddingRight: "8px" }} /> : null}<Button size="small" icon="reload" onClick={() => this._onSearch()}>刷新</Button></AntdSelect.Option>
                    </AntdSelect>
                )}
            </Form.Item>
        );
    }

    /**
     * 获取选中项目
     *
     * @returns 选中项目,数组内容为字符串格式键值
     * @memberof Table
     */
    getSelectedItems() {
        return this.selectedItems;
    }

    /**
     * 获取空数据视图
     *
     * @returns 空数据视图
     * @memberof Table
     */
    _getEmptyText() {
        return (
            <div>
                <span className="empty_text">{intl.get('components.select.empty_text')}</span>
                <Button size="small" icon="reload" onClick={() => this._onSearch()}>{intl.get('components.select.reload')}</Button>
            </div>
        );
    }

    /**
     * 搜索事件处理函数
     *
     * @param {*} [value=null] 搜索内容
     * @memberof Select
     */
    _onSearch(value = null) {
        this.setState({
            data: [],
            loading: true
        });

        const options = !value ? null : { params: value };

        request(this.props.url, 'get', options, data => {
            this.setState({
                data: data,
                loading: false
            });
        }, () => {
            this.setState({
                data: [],
                loading: false
            });
        });
    }

    /**
     * 选中内容变化事件处理函数
     *
     * @param {*} value 选中内容
     * @memberof Select
     */
    _onChange(value) {
        this.selectedItems = value;
    }
}

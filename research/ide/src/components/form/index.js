import React from 'react';
import PropTypes from 'prop-types';
import { Form as AntdForm } from 'antd';
import BaseComponent from '~/components/baseComponent';

/**
 * 表单组件
 *
 * @export
 * @class _Form
 * @extends {BaseComponent}
 */
class _Form extends BaseComponent {
    static propTypes = {
        url: PropTypes.string,
        method: PropTypes.string,
        params: PropTypes.object
    }

    static defaultProps = {
        url: null,
        method: 'get',
        params: null
    }

    state = {
        loading: false
    }

    render() {
        const { form, ...props } = this.getRestProps();

        return (
            <AntdForm {...props}>
                {React.Children.map(this.props.children, child =>
                    React.cloneElement(child, { form: this.props.form })
                )}
            </AntdForm>
        );
    }


    /**
     * 获取所有字段内容
     *
     * @returns 所有字段内容
     * @memberof _Form
     */
    getFieldsValue() {
        return this.props.form.getFieldsValue();
    }
}

const Form = AntdForm.create()(_Form);
export default Form;

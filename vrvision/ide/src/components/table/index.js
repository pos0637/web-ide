import React from 'react';
import PropTypes from 'prop-types';
import intl from 'react-intl-universal';
import { Table as AntdTable } from 'antd';
import BaseComponent from '~/components/baseComponent';
import Button from '~/components/button';
import { request } from '~/components/request';
import './index.scss';

/**
 * 数据表格组件
 *
 * @export
 * @class Table
 * @extends {BaseComponent}
 */
export default class Table extends BaseComponent {
    static propTypes = {
        url: PropTypes.string.isRequired, // 请求地址
        params: PropTypes.object, // 请求参数
        columns: PropTypes.array.isRequired, // 列属性集合
        checkable: PropTypes.bool, // 是否可以选中
        actionBar: PropTypes.bool, // 是否显示操作列
        actionBarWidth: PropTypes.number, // 操作列宽度
        actionBarView: PropTypes.any, // 操作列视图
        onLoadComplete: PropTypes.func, // 加载完成事件
        onViewButtonClick: PropTypes.func, // 查看按钮点击事件
        onEditButtonClick: PropTypes.func, // 编辑按钮点击事件
        onDeleteButtonClick: PropTypes.func // 删除按钮点击事件
    }

    static defaultProps = {
        url: null,
        params: {},
        columns: [],
        checkable: true,
        actionBar: true,
        actionBarWidth: 260,
        actionBarView: undefined,
        onLoadComplete: undefined,
        onViewButtonClick: undefined,
        onEditButtonClick: undefined,
        onDeleteButtonClick: undefined
    }

    state = {
        data: [],
        pagination: {
            current: 0,
            pageSize: 10,
            total: 0,
            showSizeChanger: true,
            showQuickJumper: true,
            pageSizeOptions: ['10', '20', '50', '100']
        },
        loading: false
    }

    params = this.props.params

    constructor(props) {
        super(props);
        this.selectedItems = [];
    }

    componentDidMount() {
        super.componentDidMount();
        this._onTableChange(this.params, this.state.pagination);
    }

    render() {
        return (
            <AntdTable
                columns={this._getColumns()}
                rowKey={record => record.id}
                dataSource={this.state.data}
                pagination={this.state.pagination}
                loading={this.state.loading && { tip: intl.get('components.table.loading') }}
                locale={{ emptyText: this._getEmptyText() }}
                rowSelection={this.props.checkable ? { onChange: (selectedRowKeys, selectedRows) => this._onRowSelectionChange(selectedRowKeys, selectedRows) } : undefined}
                onChange={(pagination, filters, sorter) => this._onTableChange(this.params, pagination, filters, sorter)}
            />
        );
    }

    /**
     * 获取选中项目
     *
     * @returns 选中项目
     * @memberof Table
     */
    getSelectedItems() {
        return this.selectedItems;
    }

    /**
     * 重新加载
     *
     * @param {*} params 请求参数
     * @memberof Table
     */
    reload(params) {
        this.params = params;
        this._onTableChange(this.params, {
            current: 0,
            pageSize: this.state.pagination.pageSize
        });
    }

    /**
     * 获取列属性
     *
     * @returns 列属性
     * @memberof Table
     */
    _getColumns() {
        const columns = [];
        this.props.columns.forEach(column => {
            columns.push({
                ...column,
                align: 'center'
            });
        });

        return this.props.actionBar ? columns.concat([{
            title: intl.get('components.table.actionbar.title'),
            key: 'operation',
            fixed: 'right',
            align: 'center',
            width: this.props.actionBarWidth,
            render: this._getActionBar()
        }]) : columns;
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
                <span className="empty_text">{intl.get('components.table.empty_text')}</span>
                <Button size="small" icon="reload" onClick={() => this._onTableChange(this.params, this.state.pagination)}>{intl.get('components.table.reload')}</Button>
            </div>
        );
    }

    /**
     * 获取操作栏视图
     *
     * @returns 操作栏视图
     * @memberof Table
     */
    _getActionBar() {
        if (typeof this.props.actionBarView === 'function') {
            return (text, record, index) => this.props.actionBarView((text, record, index));
        } else if (!!this.props.actionBarView) {
            return () => this.props.actionBarView
        } else {
            const self = this;
            return (text, record, index) => (
                <div>
                    <Button size="small" icon="eye" className="action_bar_button" onClick={() => self._onViewButtonClick(index, record)}>{intl.get('components.table.actionbar.view')}</Button>
                    <Button size="small" icon="edit" className="action_bar_button" onClick={() => self._onEditButtonClick(index, record)}>{intl.get('components.table.actionbar.edit')}</Button>
                    <Button size="small" icon="delete" className="action_bar_button" onClick={() => self._onDeleteButtonClick(index, record)}>{intl.get('components.table.actionbar.delete')}</Button>
                </div>
            );
        }
    }

    /**
     * 查看按钮点击事件处理函数
     *
     * @param {*} index 索引
     * @param {*} record 内容
     * @returns
     * @memberof Table
     */
    _onViewButtonClick(index, record) {
        return this.props.onViewButtonClick && this.props.onViewButtonClick(index, record);
    }

    /**
     * 编辑按钮点击事件处理函数
     *
     * @param {*} index 索引
     * @param {*} record 内容
     * @returns
     * @memberof Table
     */
    _onEditButtonClick(index, record) {
        return this.props.onEditButtonClick && this.props.onEditButtonClick(index, record);
    }

    /**
     * 删除按钮点击事件处理函数
     *
     * @param {*} index 索引
     * @param {*} record 内容
     * @returns
     * @memberof Table
     */
    _onDeleteButtonClick(index, record) {
        return this.props.onDeleteButtonClick && this.props.onDeleteButtonClick(index, record);
    }

    /**
     * 表格属性改变事件处理函数
     *
     * @param {*} params 请求参数
     * @param {*} pagination 分页
     * @param {*} filters 过滤条件
     * @param {*} sorter 排序
     * @memberof Table
     */
    /**
     *
     *
     * @param {*} params
     * @param {*} pagination
     * @param {*} [filters={}]
     * @param {*} [sorter=null]
     * @memberof Table
     */
    _onTableChange(params, pagination, filters = {}, sorter = null) {
        const pager = {
            ...this.state.pagination,
            current: pagination.current,
            pageSize: pagination.pageSize
        };
        this.setState({
            pagination: pager,
            loading: true
        });

        const parameters = {
            ...params,
            page: pagination.current,
            results: pagination.pageSize,
            ...filters
        };

        if (sorter && (Object.keys(sorter).length > 0)) {
            parameters.sortField = sorter.field;
            parameters.sortOrder = sorter.order;
        }

        request(this.props.url, 'get', parameters, data => {
            this.props.onLoadComplete && this.props.onLoadComplete(data);
            this.setState({
                data: data,
                loading: false
            });
        }, () => {
            this.props.onLoadComplete && this.props.onLoadComplete();
            this.setState({
                data: [],
                loading: false
            });
        });
    }

    /**
     * 行选中事件处理函数
     *
     * @param {*} selectedRowKeys 选中行键值
     * @memberof Table
     */
    _onRowSelectionChange(selectedRowKeys) {
        this.selectedItems = selectedRowKeys;
    }
}

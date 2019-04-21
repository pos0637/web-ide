import React from 'react';
import PropTypes from 'prop-types';
import { Layout, Menu, Icon } from 'antd';
import BaseComponent from '~/components/baseComponent';

/**
 * 主框架
 *
 * @export
 * @class Framework
 * @extends {BaseComponent}
 */
export default class Framework extends BaseComponent {
    static contextTypes = {
        router: PropTypes.object // 路由
    }

    state = {
        collapsed: false
    }

    render() {
        const { Header, Footer, Sider, Content } = Layout;

        return (
            <Layout>
                <Header>
                    <span style={{ fontSize: "32px", color: "white" }}>web-ide</span>
                </Header>
                <Layout>
                    <Sider
                        collapsible
                        collapsed={this.state.collapsed}
                        onCollapse={this.onCollapse}
                    >
                        <Menu theme="dark" defaultSelectedKeys={['1']} mode="inline">
                            <Menu.Item key="1">
                                <Icon type="project" />
                                <span>Project</span>
                            </Menu.Item>
                            <Menu.Item key="2">
                                <Icon type="search" />
                                <span>Search</span>
                            </Menu.Item>
                            <Menu.Item key="3">
                                <Icon type="tool" />
                                <span>Debug</span>
                            </Menu.Item>
                        </Menu>
                    </Sider>
                    <Content>
                        {this.props.children}
                    </Content>
                </Layout>
                <Footer>Footer</Footer>
            </Layout>
        );
    }

    onCollapse = (collapsed) => {
        this.setState({ collapsed: collapsed });
    }
}

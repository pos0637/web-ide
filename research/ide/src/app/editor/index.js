import React from 'react';
import PropTypes from 'prop-types';
import { Tabs, List, Row, Col } from 'antd';
import { Controlled as CodeMirror } from 'react-codemirror2';
import BaseComponent from '~/components/baseComponent';
import Button from '~/components/button';
import { getCode, getInformation, getConsole, addBreakpoint, deleteBreakpoint } from '~/api/v1/debugger';

require('codemirror/lib/codemirror.css');
require('codemirror/theme/material.css');
require('codemirror/theme/neat.css');
require('codemirror/mode/xml/xml.js');
require('codemirror/mode/javascript/javascript.js');
require('codemirror/addon/selection/active-line.js');

/**
 * 编辑器视图
 *
 * @export
 * @class Editor
 * @extends {BaseComponent}
 */
export default class Editor extends BaseComponent {
    static contextTypes = {
        router: PropTypes.object // 路由
    }

    state = {
        className: 'Test',
        code1: '',
        state: 0,
        stack: [],
        variables: [],
        output: [],
        breakpoints: []
    }

    componentDidMount() {
        super.componentDidMount();

        this.timer1 = setInterval(() => {
            getInformation(information => {
                console.log(information);

                getCode('Test.java', code => {
                    this.setState({ code1: code });
                });

                this.setState({ state: information.debuggerState });

                const marks = this.cm.getAllMarks();
                marks && marks.forEach(mark => mark.clear());
                if (information.location) {
                    const info = this.cm.lineInfo(information.location.lineNumber - 1);
                    this.cm.markText({ line: information.location.lineNumber - 1, ch: 0 }, { line: information.location.lineNumber - 1, ch: info.text.length }, { className: 'styled-background' });
                }

                if ((information.stack) && (information.stack.locations)) {
                    const stack = [];
                    information.stack.locations.forEach(location => {
                        stack.push(`${location.path}:${location.lineNumber}, ${location.method}()`);
                    });
                    this.setState({ stack: stack });
                }
                else {
                    this.setState({ stack: [] });
                }

                if (information.variables) {
                    const variables = [];
                    information.variables.forEach(variable => {
                        variables.push(`[${variable.type}] ${variable.className} ${variable.name}, value: ${variable.value}`);
                    });
                    this.setState({ variables: variables });
                }
                else {
                    this.setState({ variables: [] });
                }

                if (information.breakpoints) {
                    const breakpoints = [];
                    information.breakpoints.forEach(breakpoint => {
                        breakpoints.push(`${breakpoint.className}:${breakpoint.lineNumber}, enabled: ${breakpoint.enabled}, active: ${breakpoint.active}`);
                    });
                    this._updateBreakpoint(information.breakpoints);
                    this.setState({ breakpoints: breakpoints });
                }
                else {
                    this._updateBreakpoint(null);
                    this.setState({ breakpoints: [] });
                }
            });
        }, 2000);

        this.timer2 = setInterval(() => {
            getConsole(information => {
                const output = [].concat(this.state.output, information);
                this.setState({ output: output });
            });
        }, 2000);
    }

    componentWillUnmount() {
        this.timer1 && clearTimeout(this.timer1);
        this.timer2 && clearTimeout(this.timer2);
        super.componentWillUnmount();
    }

    render() {
        const options = {
            mode: 'javascript',
            theme: 'material',
            readOnly: true,
            lineNumbers: true,
            styleActiveLine: true,
            styleSelectedText: true,
            gutters: ['CodeMirror-linenumbers', 'breakpoints']
        };

        return (
            <div style={{ width: '100%', height: '100%', margin: '10px' }}>
                <div>
                    <Button
                        icon="play-circle"
                        disabled={this.state.state !== 'Idle'}
                        url="/api/v1/debugger/start"
                        method="get"
                        params={{
                            script: this.state.className,
                            arguments: {
                                '-classpath': './demos/demo1'
                            }
                        }}
                        resolve={data => {
                            console.log(data);
                        }}
                    >
                        运行
                    </Button>
                    <Button
                        icon="stop"
                        disabled={this.state.state === 'Idle'}
                        url="/api/v1/debugger/stop"
                        method="get"
                        resolve={data => {
                            console.log(data);
                        }}
                    >
                        停止
                    </Button>
                    <Button
                        icon="forward"
                        disabled={this.state.state !== 'Breaking'}
                        url="/api/v1/debugger/resume"
                        method="get"
                        resolve={data => {
                            console.log(data);
                        }}
                    >
                        继续
                    </Button>
                    <Button
                        icon="step-forward"
                        disabled={this.state.state !== 'Breaking'}
                        url="/api/v1/debugger/stepOver"
                        method="get"
                        resolve={data => {
                            console.log(data);
                        }}
                    >
                        单步
                    </Button>
                    <Button
                        icon="arrow-down"
                        disabled={this.state.state !== 'Breaking'}
                        url="/api/v1/debugger/stepInto"
                        method="get"
                        resolve={data => {
                            console.log(data);
                        }}
                    >
                        进入
                    </Button>
                    <Button
                        icon="arrow-up"
                        disabled={this.state.state !== 'Breaking'}
                        url="/api/v1/debugger/stepOut"
                        method="get"
                        resolve={data => {
                            console.log(data);
                        }}
                    >
                        退出
                    </Button>
                </div>
                <div style={{ width: '100%', height: '100%' }}>
                    <div style={{ marginTop: '10px' }}>
                        <Tabs type="card" defaultActiveKey="1">
                            <Tabs.TabPane tab="Test.java" key="1">
                                <CodeMirror
                                    editorDidMount={editor => {
                                        this.cm = editor
                                    }}
                                    value={this.state.code1}
                                    options={options}
                                    onBeforeChange={(editor, data, value) => {
                                        this.setState({ code1: value });
                                    }}
                                    onGutterClick={this._onGutterClick}
                                />
                            </Tabs.TabPane>
                            <Tabs.TabPane tab="Test1.java" key="2" />
                            <Tabs.TabPane tab="Test2.java" key="3" />
                        </Tabs>
                    </div>
                    <div style={{ marginTop: '10px' }}>
                        <Tabs type="card" defaultActiveKey="1">
                            <Tabs.TabPane tab="Debug" key="1">
                                <Row>
                                    <Col span={4}>
                                        <List
                                            bordered
                                            dataSource={this.state.stack}
                                            header={<div>Call stack</div>}
                                            renderItem={item => (<List.Item>{item}</List.Item>)}
                                        />
                                    </Col>
                                    <Col span={4}>
                                        <List
                                            bordered
                                            dataSource={this.state.variables}
                                            header={<div>Variables</div>}
                                            renderItem={item => (<List.Item>{item}</List.Item>)}
                                        />
                                    </Col>
                                </Row>
                            </Tabs.TabPane>
                            <Tabs.TabPane tab="Console" key="2">
                                <List
                                    bordered
                                    dataSource={this.state.output}
                                    header={<div><Button icon="delete" onClick={() => this.setState({ output: [] })}>清空</Button></div>}
                                    renderItem={item => (<List.Item>{item}</List.Item>)}
                                />
                            </Tabs.TabPane>
                            <Tabs.TabPane tab="Breakpoints" key="3">
                                <List
                                    bordered
                                    dataSource={this.state.breakpoints}
                                    renderItem={item => (<List.Item>{item}</List.Item>)}
                                />
                            </Tabs.TabPane>
                        </Tabs>
                    </div>
                </div>
            </div>
        );
    }

    _onGutterClick = (cm, n) => {
        const info = cm.lineInfo(n);
        info.gutterMarkers ? this._deleteBreakpoint(cm, n) : this._addBreakpoint(cm, n);
    }

    _updateBreakpoint(breakpoints) {
        if (this.cm) {
            this.cm.clearGutter('breakpoints');
            breakpoints && breakpoints.forEach((breakpoint) => this.cm.setGutterMarker(breakpoint.lineNumber - 1, 'breakpoints', this._makeMarker()));
        }
    }

    _addBreakpoint(cm, n) {
        addBreakpoint(this.state.className, n + 1, () => {
            cm.setGutterMarker(n, 'breakpoints', this._makeMarker());
        });
    }

    _deleteBreakpoint(cm, n) {
        deleteBreakpoint(this.state.className, n + 1, () => {
            cm.setGutterMarker(n, 'breakpoints', null);
        });
    }

    _makeMarker() {
        const marker = document.createElement('div');
        marker.style.color = '#822';
        marker.innerHTML = '●';
        return marker;
    }
}

import React from 'react';
import PropTypes from 'prop-types';
import { Tabs, List, Row, Col, Input } from 'antd';
import { UnControlled as CodeMirror } from 'react-codemirror2';
import BaseComponent from '~/components/baseComponent';
import Button from '~/components/button';
import { getDeclarationSymbol, getSymbolValue, evaluation, getCode, saveCode, getInformation, getConsole, addBreakpoint, deleteBreakpoint } from '~/api/v1/debugger';

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
        activeTab: "1",
        codes: [],
        state: 0,
        stack: [],
        variables: [],
        output: [],
        breakpoints: [],
        expressionValue: null,
        hint: null,
        images: [],
        image: null
    }

    /**
     * CodeMirror对象列表
     *
     * @memberof Editor
     */
    cms = []

    /**
     * 源代码路径列表
     *
     * @memberof Editor
     */
    sourcePaths = ['Test.java']

    /**
     * 当前编辑器行号
     *
     * @memberof Editor
     */
    lineNumber = null

    /**
     * WebSocket客户端
     *
     * @memberof Editor
     */
    websocket = null

    /**
     * 当前编辑器列号
     *
     * @memberof Editor
     */
    columnNumber = null

    componentDidMount() {
        super.componentDidMount();

        this._getCodes();
        this.timer0 = setInterval(() => {
            this._getCodes();
        }, 2000);

        this._getInformation();
        this.timer1 = setInterval(() => {
            this._getInformation();
        }, 2000);

        this.timer2 = setInterval(() => {
            getConsole(information => {
                const output = [].concat(this.state.output, information);
                this.setState({ output: output });
            });
        }, 2000);

        this._createWebSocket();
        this.timer3 = setInterval(() => {
            if ((this.websocket === null) || (this.websocket.readyState !== 1)) {
                this._createWebSocket();
            }
        }, 5000);

        this.timer4 = setInterval(() => {
            if ((this.websocket !== null) || (this.websocket.readyState === 1)) {
                this.websocket.send('getImages');
            }
        }, 2000);
    }

    componentWillUnmount() {
        this.timer1 && clearTimeout(this.timer1);
        this.timer2 && clearTimeout(this.timer2);
        this.timer3 && clearTimeout(this.timer3);
        super.componentWillUnmount();
    }

    render() {
        let hint = null;
        if (this.state.hint) {
            hint = <div style={{ position: "fixed", color: "rgba(255, 255, 255, 1)", backgroundColor: "rgba(0, 0, 255, 0.9)", left: this.state.hint.left, top: this.state.hint.top - 30 }}>{this.state.hint.content}</div>;
        }

        const tabs = [];
        this.sourcePaths.forEach((sourcePath, index) => {
            const key = `${index + 1}`;
            tabs.push(
                <Tabs.TabPane tab={sourcePath} key={key}>
                    {this._getCodeMirror(index)}
                </Tabs.TabPane>
            );
        });

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
                                '-sourcepath': 'demos/demo3',
                                '-classpath': 'demos/demo3/Java-WebSocket-1.4.0-with-dependencies.jar;demos/demo3/opencv-410.jar'
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
                    <Button
                        icon="scan"
                        disabled={this.state.state !== 'Idle'}
                        url="/api/v1/debugger/analyze"
                        method="get"
                        params={{
                            rootPath: 'demos/demo3'
                        }}
                        resolve={data => {
                            console.log(data);
                        }}
                    >
                        分析
                    </Button>
                    <Button
                        icon="save"
                        disabled={this.state.state !== 'Idle'}
                        onClick={() => this._saveCodes()}
                    >
                        保存
                    </Button>
                </div>
                <div style={{ width: '100%', height: '100%' }}>
                    <div style={{ marginTop: '10px' }}>
                        <Tabs type="card" defaultActiveKey="1" activeKey={this.state.activeTab} onChange={activeKey => { this.setState({ activeTab: activeKey }); }}>
                            {tabs}
                        </Tabs>
                    </div>
                    <div style={{ marginTop: '10px' }}>
                        <Tabs type="card" defaultActiveKey="1">
                            <Tabs.TabPane tab="Debug" key="1">
                                <Row>
                                    <Col span={6}>
                                        <Input.Search
                                            placeholder="请输入表达式"
                                            enterButton="执行"
                                            disabled={this.state.state !== 'Breaking'}
                                            onSearch={value => this._evaluation(value)}
                                        />
                                    </Col>
                                    <Col span={2}>
                                        <Input placeholder="执行结果" disabled value={this.state.state !== 'Breaking' ? null : this.state.expressionValue} />
                                    </Col>
                                </Row>
                                <Row style={{ height: 4 }} />
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
                            <Tabs.TabPane tab="ImageView" key="4">
                                <Row>
                                    <Col span={2}>
                                        <List
                                            bordered
                                            dataSource={this.state.images}
                                            header={<div>图片列表</div>}
                                            renderItem={item => (<List.Item onClick={() => this._onImageListItemClick(item)}>{item}</List.Item>)}
                                        />
                                    </Col>
                                    <Col span={8}>
                                        <Row><img src={this.state.image} alt="预览图片" /></Row>
                                    </Col>
                                </Row>
                            </Tabs.TabPane>
                        </Tabs>
                    </div>
                </div>
                {hint}
            </div>
        );
    }

    _getCodeMirror(tabIndex) {
        const options = {
            mode: 'javascript',
            theme: 'material',
            autofocus: true,
            readOnly: false,
            lineNumbers: true,
            styleActiveLine: true,
            styleSelectedText: true,
            gutters: ['CodeMirror-linenumbers', 'breakpoints'],
            extraKeys: {
                F3: cm => {
                    const info = cm.getCursor();
                    getDeclarationSymbol(this.sourcePaths[tabIndex], info.line + 1, info.ch + 1, symbol => {
                        console.log(symbol);
                        if (symbol) {
                            // TODO: check symbol.sourcePath
                            const id = this._getTabIndex(symbol.sourcePath);
                            if (id >= 0) {
                                if (id === (parseInt(this.state.activeTab, 10) - 1)) {
                                    this.cms[id].setCursor({ line: symbol.lineNumber - 1, ch: symbol.columnNumber });
                                }
                                else {
                                    this.setState({ activeTab: (id + 1).toString() }, () => {
                                        this.cms[id].focus();
                                        this.cms[id].setCursor({ line: symbol.lineNumber - 1, ch: symbol.columnNumber });
                                    });
                                }
                            }
                        }
                    });
                }
            }
        };

        return (
            <CodeMirror
                editorDidMount={editor => {
                    const cm = editor;
                    this.cms[tabIndex] = cm;
                    cm.getWrapperElement().addEventListener('mousemove', e => {
                        const info = cm.coordsChar({ left: e.pageX, top: e.pageY });
                        if ((info.line !== this.lineNumber) || (info.ch !== this.columnNumber)) {
                            this.lineNumber = info.line;
                            this.columnNumber = info.ch;
                            this.timer3 && clearTimeout(this.timer3);
                            this._getSymbol(this.sourcePaths[tabIndex], info.line + 1, info.ch + 1, value => {
                                this.setState({
                                    hint: {
                                        left: e.pageX,
                                        top: e.pageY,
                                        content: value
                                    }
                                });
                            });
                        }
                    });
                }}
                value={this.state.codes[tabIndex]}
                options={options}
                onGutterClick={(cm, n) => this._onGutterClick(this.sourcePaths[tabIndex], cm, n)}
            />
        );
    }

    _getCodes() {
        this.sourcePaths.forEach((sourcePath, index) => {
            getCode(sourcePath, code => {
                const { codes } = this.state;
                if (!this.state.codes[index]) {
                    codes[index] = code;
                    this.setState({ codes: Array.from(codes) });
                }
            });
        });
    }

    _saveCodes() {
        for (let i = 0; i < this.sourcePaths.length; i += 1) {
            if (this.sourcePaths[i] && this.cms[i]) {
                saveCode(this.sourcePaths[i], this.cms[i].getValue());
            }
        }
    }

    _getInformation() {
        getInformation(information => {
            console.log(information);
            this.setState({ state: information.debuggerState });
            this._updateLocation(information.location);

            if ((information.stack) && (information.stack.locations)) {
                const stack = [];
                information.stack.locations.forEach(location => {
                    stack.push(`${location.sourcePath}:${location.lineNumber}, ${location.method}()`);
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
    }

    _onGutterClick(sourcePath, cm, n) {
        const info = cm.lineInfo(n);
        info.gutterMarkers ? this._deleteBreakpoint(sourcePath, cm, n) : this._addBreakpoint(sourcePath, cm, n);
    }

    _updateBreakpoint(breakpoints) {
        this.cms.forEach(cm => cm.clearGutter('breakpoints'));
        breakpoints && breakpoints.forEach(breakpoint => {
            const cm = this._getCodeMirrorInstance(breakpoint.sourcePath);
            if (cm !== null) {
                cm.setGutterMarker(breakpoint.lineNumber - 1, 'breakpoints', this._makeMarker());
            }
        });
    }

    _addBreakpoint(sourcePath, cm, n) {
        addBreakpoint(sourcePath, n + 1, () => {
            cm.setGutterMarker(n, 'breakpoints', this._makeMarker());
        });
    }

    _deleteBreakpoint(sourcePath, cm, n) {
        deleteBreakpoint(sourcePath, n + 1, () => {
            cm.setGutterMarker(n, 'breakpoints', null);
        });
    }

    _makeMarker() {
        const marker = document.createElement('div');
        marker.style.color = '#822';
        marker.innerHTML = '●';
        return marker;
    }

    _updateLocation(location) {
        this.cms.forEach(cm => {
            const marks = cm.getAllMarks();
            marks && marks.forEach(mark => mark.clear());
        });

        if (location) {
            const cm = this._getCodeMirrorInstance(location.sourcePath);
            const info = cm.lineInfo(location.lineNumber - 1);
            cm.markText({ line: location.lineNumber - 1, ch: 0 }, { line: location.lineNumber - 1, ch: info.text.length }, { className: 'styled-background' });
        }
    }

    _getSymbol(sourcePath, lineNumber, columnNumber, succ) {
        this.timer3 = setTimeout(() => {
            getSymbolValue(sourcePath, lineNumber, columnNumber, value => {
                console.log(`(${lineNumber}: ${columnNumber}): ${value}`);
                succ && succ(value);
            });
            this.timer3 = null;
        }, 1000);
    }

    _evaluation(expression) {
        evaluation(expression, value => this.setState({ expressionValue: value }));
    }

    _getCodeMirrorInstance(sourcePath) {
        for (let i = 0; i < this.sourcePaths.length; i += 1) {
            if (this.sourcePaths[i] === sourcePath) {
                return this.cms[i];
            }
        }

        return null;
    }

    _getTabIndex(sourcePath) {
        for (let i = 0; i < this.sourcePaths.length; i += 1) {
            if (this.sourcePaths[i] === sourcePath) {
                return i;
            }
        }

        return -1;
    }

    _createWebSocket() {
        if (this.websocket !== null) {
            this.websocket.close();
        }

        this.websocket = new WebSocket('ws://localhost:8887');
        this.websocket.onmessage = (event) => {
            console.log(`onmessage: ${event.data}`);
            if (event.data.startsWith('images:')) {
                const list = event.data.substring('images:'.length);
                this.setState({ images: list.split(',') });
            } else if (event.data.startsWith('image:')) {
                const image = event.data.substring('image:'.length);
                if (image.length > 0) {
                    this.setState({ image: `data:image/jpg;base64,${image}` });
                }
            }
        };

        this.websocket.onclose = () => {
            this.websocket = null;
        }

        this.websocket.onerror = () => {
            this.websocket = null;
        }
    }

    _onImageListItemClick(item) {
        this.websocket.send(`getImage:${item}`);
    }
}

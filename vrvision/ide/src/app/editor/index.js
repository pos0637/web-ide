import React from 'react';
import PropTypes from 'prop-types';
import { Tabs, List, Row, Col, Input } from 'antd';
import { Controlled as CodeMirror } from 'react-codemirror2';
import BaseComponent from '~/components/baseComponent';
import Button from '~/components/button';
import { getDeclarationSymbol, getSymbolValue, evaluation, getCode, getInformation, getConsole, addBreakpoint, deleteBreakpoint } from '~/api/v1/debugger';

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

        this.timer0 = setInterval(() => {
            this.sourcePaths.forEach((sourcePath, index) => {
                getCode(sourcePath, code => {
                    const { codes } = this.state;
                    if (!this.state.codes[index]) {
                        codes[index] = code;
                        this.setState({ codes: Array.from(codes) });
                    }
                });
            });
        }, 2000);

        this.timer1 = setInterval(() => {
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
                onBeforeChange={(editor, data, value) => { }}
                onGutterClick={(cm, n) => this._onGutterClick(this.sourcePaths[tabIndex], cm, n)}
            />
        );
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
            console.log('onmessage: ' + event.data);
            if (event.data === 'capture') {
                // this.websocket.send('captureResult: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');
            } else if (event.data.startsWith('images:')) {
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

import React from 'react';
import PropTypes from 'prop-types';
import { Controlled as CodeMirror } from 'react-codemirror2';
import BaseComponent from '~/components/baseComponent';

require('codemirror/lib/codemirror.css');
require('codemirror/theme/material.css');
require('codemirror/theme/neat.css');
require('codemirror/mode/xml/xml.js');
require('codemirror/mode/javascript/javascript.js');

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
        value: '123\n12312'
    }

    render() {
        const options = {
            mode: 'javascript',
            theme: 'material',
            readOnly: true,
            lineNumbers: true,
            gutters: ['CodeMirror-linenumbers', 'breakpoints']
        };

        return (
            <div>
                <CodeMirror
                    value={this.state.value}
                    options={options}
                    onBeforeChange={(editor, data, value) => {
                        this.setState({ value });
                    }}
                    onChange={(editor, value) => {
                        console.log('controlled', { value });
                    }}
                    onGutterClick={this.onGutterClick}
                />
            </div>
        );
    }

    onGutterClick = (cm, n) => {
        const info = cm.lineInfo(n);
        cm.setGutterMarker(n, "breakpoints", info.gutterMarkers ? null : this.makeMarker());
    }

    makeMarker() {
        const marker = document.createElement('div');
        marker.style.color = '#822';
        marker.innerHTML = '●';
        return marker;
    }
}

import React from 'react';
import PropTypes from 'prop-types';
import {Controlled as CodeMirror} from 'react-codemirror2';
import BaseComponent from '~/components/baseComponent';

require('codemirror/mode/javascript/javascript');

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

    render() {
        return (
            <div>
                <CodeMirror
                    value="123"
                    options={{
                        mode: 'javascript',
                        theme: 'material',
                        lineNumbers: true
                    }}
                />
            </div>
        );
    }
}

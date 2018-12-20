import React from 'react';
import PropTypes from 'prop-types';
import BaseComponent from '~/components/baseComponent';

export default class Iframe extends BaseComponent {
    static propTypes = {
        url: PropTypes.string.isRequired
    }

    static defaultProps = {
        url: null
    }

    render() {
        return (
            <iframe
                src={this.props.url}
                width="100%"
                height="100%"
                scrolling="no"
                frameBorder="0"
                style={{ width: '100%', height: '100%' }}
                background="none"
            />
        );
    }
}

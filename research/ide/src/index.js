import React from 'react';
import { render } from 'react-dom';
import { MemoryRouter, Switch, Route } from 'react-router-dom'
import Application from '~/components/application';
import Framework from '~/framework';
import Editor from '~/app/editor';

const locales = { 'ZH-CN': require('~/app/locales/zh-CN.json') };
const editor = () => <Application currentLocale='ZH-CN' locales={locales}><Framework><Editor /></Framework></Application>;

render((
    <MemoryRouter>
        <Switch>
            <Route path='/' exact component={editor} />
            <Route path='*' component={editor} />
        </Switch>
    </MemoryRouter>
), document.getElementById('root'));

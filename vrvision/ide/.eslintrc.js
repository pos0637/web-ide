module.exports = {
  parser: 'babel-eslint',
  extends: ['airbnb', 'prettier', 'plugin:compat/recommended'],
  env: {
    browser: true,
    node: true,
    es6: true,
    mocha: true,
    jest: true,
    jasmine: true
  },
  rules: {
    'react/jsx-filename-extension': [1, { extensions: ['.js'] }],
    'react/jsx-wrap-multilines': 0,
    'react/prop-types': 0,
    'react/forbid-prop-types': 0,
    'react/jsx-one-expression-per-line': 0,
    'react/no-find-dom-node': 0,
    'import/no-extraneous-dependencies': [2, { optionalDependencies: true }],
    'jsx-a11y/no-noninteractive-element-interactions': 0,
    'jsx-a11y/click-events-have-key-events': 0,
    'jsx-a11y/no-static-element-interactions': 0,
    'jsx-a11y/anchor-is-valid': 0,
    'jsx-a11y/iframe-has-title': 0,
    'linebreak-style': ['error', 'windows'],
    'react/jsx-indent': [2, 4],
    'react/jsx-indent-props': [2, 4],
    'react/no-access-state-in-setstate': 0,
    'react/destructuring-assignment': 0,
    'no-else-return': 0,
    'no-underscore-dangle': 0,
    'no-extra-boolean-cast': 0,
    'class-methods-use-this': 0,
    'no-unused-expressions': 0,
    'object-shorthand': 0,
    'global-require': 0,
    'prefer-destructuring': 0
  },
  settings: {
    polyfills: ['fetch', 'promises', 'url'],
    'import/resolver': 'babel-plugin-root-import'
  }
}

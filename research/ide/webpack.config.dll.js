const path = require('path');
const Webpack = require('webpack');
const CleanWebpackPlugin = require('clean-webpack-plugin');

const outputPath = './dist/';
const dllPath = `${outputPath}dll/`;

module.exports = {
    entry: [
        'react',
        'react-dom',
        'react-intl-universal',
        'antd'
    ],
    output: {
        path: path.resolve(__dirname, dllPath),
        filename: '[name].bundle.js',
        library: '[name]'
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                use: ['babel-loader'],
                include: path.resolve(__dirname, 'src'),
                exclude: /node_modules/
            },
            {
                test: /\.css$/,
                use: ['style-loader', 'css-loader']
            },
            {
                test: /\.less$/,
                use: ['style-loader', 'css-loader', 'postcss-loader', 'less-loader']
            },
            {
                test: /\.scss$/,
                use: ['style-loader', 'css-loader', 'sass-loader']
            }
        ]
    },
    plugins: [
        new CleanWebpackPlugin([dllPath]),
        new Webpack.DllPlugin({
            context: path.resolve(__dirname, dllPath),
            path: path.resolve(__dirname, `${dllPath}manifest.json`),
            name: '[name]'
        })
    ]
};

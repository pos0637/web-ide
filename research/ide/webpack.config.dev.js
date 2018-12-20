const path = require('path');
const glob = require('glob');
const Webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const CleanWebpackPlugin = require('clean-webpack-plugin');

const outputPath = './dist/';
const dllPath = `${outputPath}dll/`;
const files = glob.sync('./src/**/entry.js');
const entries = {};
const plugins = [];

files.forEach(file => {
    const name = /src\/(.*)\/entry.js/.exec(file)[1].replace(/\//g, '_');
    entries[name] = [
        '@babel/polyfill',
        file
    ];

    plugins.push(new HtmlWebpackPlugin({
        filename: `${name}.html`,
        template: path.resolve(__dirname, './public/index.html'),
        chunks: [name]
    }));
});

module.exports = {
    entry: Object.assign({}, {
        'index': [
            '@babel/polyfill',
            path.resolve(__dirname, './src/index.js')
        ]
    }, entries),
    output: {
        path: path.resolve(__dirname, outputPath),
        filename: 'js/[name].[chunkhash].js'
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
                use: ['style-loader', 'css-loader?modules&importLoaders=1&localIdentName=[path]___[name]__[local]___[hash:base64:5]', 'sass-loader']
            },
            {
                test: /\.(png|jpg|gif)$/,
                loader: 'url-loader'
            }
        ]
    },
    plugins: [
        new CopyWebpackPlugin([{
            from: path.resolve(__dirname, './public/fonts'),
            to: path.resolve(__dirname, `${outputPath}/fonts`)
        }]),
        new HtmlWebpackPlugin({
            filename: 'index.html',
            template: path.resolve(__dirname, './public/index.html'),
            chunks: ['index']
        }),
        new CleanWebpackPlugin([
            path.resolve(__dirname, `${outputPath}*.html`),
            path.resolve(__dirname, `${outputPath}js`)
        ]),
        new Webpack.DllReferencePlugin({
            context: path.resolve(__dirname, dllPath),
            manifest: require(path.resolve(__dirname, `${dllPath}manifest.json`))
        })
    ].concat(plugins),
    devServer: {
        contentBase: path.resolve(__dirname, outputPath),
        host: 'localhost',
        port: 8000
    },
    devtool: 'cheap-module-eval-source-map'
};

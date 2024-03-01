const webpack = require('webpack')

module.exports = {
    mode: 'none',
    entry: {
        main: './index.js'
    },
    plugins: [
        new webpack.ProvidePlugin({
            Buffer: ['buffer', 'Buffer'],
        })
    ],
    output: {filename: 'bundle.js'},
    resolve: {
        fallback: {
            "assert": false,
            "stream": false
        }
    }
}
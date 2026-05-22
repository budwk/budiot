/**
 * @name 代理的配置
 * @see 在生产环境 代理是无法生效的，所以这里没有生产环境的配置
 * -------------------------------
 * The agent cannot take effect in the production environment
 * so there is no configuration of the production environment
 * For details, please see
 * https://pro.ant.design/docs/deploy
 *
 * @doc https://umijs.org/docs/guides/proxy
 */
export default {
  dev: {
    '/api/': {
      target: process.env.API_PROXY_TARGET || 'http://127.0.0.1:9000',
      changeOrigin: true,
      ws: true,
      pathRewrite: { '^/api': '' },
    },
  },
  test: {
    '/api/': {
      target: process.env.API_PROXY_TARGET || 'http://127.0.0.1:9000',
      changeOrigin: true,
      ws: true,
      pathRewrite: { '^/api': '' },
    },
  },
  pre: {
    '/api/': {
      target: process.env.API_PROXY_TARGET || 'http://127.0.0.1:9000',
      changeOrigin: true,
      ws: true,
      pathRewrite: { '^/api': '' },
    },
  },
};

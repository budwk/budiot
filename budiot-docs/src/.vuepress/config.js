const { description } = require('../../package')

module.exports = {
  /**
   * Ref：https://v1.vuepress.vuejs.org/config/#title
   */
  title: 'BudIoT',
  /**
   * Ref：https://v1.vuepress.vuejs.org/config/#description
   */
  description: description,

  /**
   * Extra tags to be injected to the page HTML `<head>`
   *
   * ref：https://v1.vuepress.vuejs.org/config/#head
   */
  head: [
    ['meta', { name: 'theme-color', content: '#3eaf7c' }],
    ['meta', { name: 'mobile-web-app-capable', content: 'yes' }],
    ['meta', { name: 'apple-mobile-web-app-capable', content: 'yes' }],
    ['meta', { name: 'apple-mobile-web-app-status-bar-style', content: 'black' }],
    ['meta', { charset: 'utf-8' }],
    ['meta', { name: 'viewport', content: 'width=device-width,initial-scale=1' }]
  ],

  /**
   * Theme configuration, here is the default theme configuration for VuePress.
   *
   * ref：https://v1.vuepress.vuejs.org/theme/default-theme-config.html
   */
  locales: {
    '/': {
      lang: 'zh-CN',
      title: 'BudIoT',
      description: 'BudIoT 物联网设备接入平台'
    },
    '/en/': {
      lang: 'en-US',
      title: 'BudIoT',
      description: 'BudIoT IoT Device Access Platform'
    }
  },

  themeConfig: {
    repo: '',
    editLinks: false,
    docsDir: '',
    editLinkText: '',
    lastUpdated: false,
    locales: {
      '/': {
        selectText: '语言',
        label: '简体中文',
        nav: [
          {
            text: '指南',
            link: '/guide/',
          },
          {
            text: '演示',
            link: 'https://demo.budiot.com'
          },
          {
            text: '源码',
            items: [
              { text: 'Gitee', link: 'https://gitee.com/budwk/budiot' },
              { text: 'Github', link: 'https://github.com/budwk/budiot' }
            ]
          }
        ],
        sidebar: {
          '/guide/': [
            {
              title: '介绍',
              collapsable: true,
              children: [
                '',
              ]
            },
            {
              title: '开发指南',
              collapsable: true,
              children: [
                'develop',
                'deploy',
                'network',
                'device',
              ]
            }
          ],
        }
      },
      '/en/': {
        selectText: 'Languages',
        label: 'English',
        nav: [
          {
            text: 'Guide',
            link: '/en/guide/',
          },
          {
            text: 'Demo',
            link: 'https://demo.budiot.com'
          },
          {
            text: 'Source',
            items: [
              { text: 'Gitee', link: 'https://gitee.com/budwk/budiot' },
              { text: 'Github', link: 'https://github.com/budwk/budiot' }
            ]
          }
        ],
        sidebar: {
          '/en/guide/': [
            {
              title: 'Introduction',
              collapsable: true,
              children: [
                '',
              ]
            },
            {
              title: 'Development Guide',
              collapsable: true,
              children: [
                'develop',
                'deploy',
                'network',
                'device',
              ]
            }
          ],
        }
      }
    }
  },

  /**
   * Apply plugins，ref：https://v1.vuepress.vuejs.org/zh/plugin/
   */
  plugins: [
    '@vuepress/plugin-back-to-top',
    '@vuepress/plugin-medium-zoom',
  ],

  // 添加基础配置
  base: '/',
  dest: 'dist',
  evergreen: true,

  // 添加生产环境配置
  configureWebpack: {
    optimization: {
      minimize: false
    }
  },

  // 禁用服务端渲染
  ssr: false,

  // 添加构建配置
  chainWebpack: config => {
    config.optimization.minimize(false)
  },

  // 添加 SSR 相关配置
  markdown: {
    lineNumbers: false
  },
}

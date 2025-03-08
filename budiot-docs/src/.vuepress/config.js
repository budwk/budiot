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
    ['meta', { name: 'apple-mobile-web-app-capable', content: 'yes' }],
    ['meta', { name: 'apple-mobile-web-app-status-bar-style', content: 'black' }]
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
  ]
}

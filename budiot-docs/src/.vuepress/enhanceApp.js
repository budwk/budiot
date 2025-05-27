/**
 * Client app enhancement file.
 *
 * https://v1.vuepress.vuejs.org/guide/basic-config.html#app-level-enhancements
 */

export default ({
  Vue, // the version of Vue being used in the VuePress app
  options, // the options for the root Vue instance
  router, // the router instance for the app
  siteData // site metadata
}) => {
  // 确保在客户端环境下执行
  if (typeof window !== 'undefined') {
    // 等待 DOM 完全加载
    window.addEventListener('DOMContentLoaded', () => {
      // 延迟执行以确保 DOM 已经准备好
      setTimeout(() => {
        try {
          // 移除服务端渲染标记
          const app = document.getElementById('app')
          if (app && app.hasAttribute('data-server-rendered')) {
            app.removeAttribute('data-server-rendered')
          }
          
          // 确保所有 Vue 组件都已挂载
          if (Vue && Vue.prototype) {
            Vue.prototype.$nextTick(() => {
              // 在这里可以添加任何必要的 DOM 操作
            })
          }
        } catch (error) {
          console.error('DOM operation error:', error)
        }
      }, 0)
    })
  }
}

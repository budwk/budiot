import { RouteRecordRaw } from 'vue-router'
import PlatformLayout from '/@/layouts/platform/index.vue'
import { c } from '../api/platform/iot/product'

const pageTitle = (name: string): string => {
    return `pagesTitle.${name}`
}

/*
 * 静态路由
 */
const staticRoutes: Array<RouteRecordRaw> = [
    {
        // 首页
        path: '/',
        redirect: '/platform/login'
    },
    {
        // 管理员登录页
        path: '/platform/login',
        component: () => import('/@/views/platform/login.vue'),
        meta: {
            title: pageTitle('platformLogin'),
        },
    },
    {
        path: '/redirect/:path(.*)',
        component: () => import('/@/views/redirect/index.vue'),
        meta: {
            layout: "platform/index"
        }
    },
    {
        // 管理员首页
        path: '/platform',
        redirect: '/platform/dashboard'
    },
    {
        // 无权限访问
        path: '/401',
        component: () => import('/@/views/common/error/401.vue'),
        meta: {
            title: pageTitle('noPower'),
        },
    },
    {
        path: '/:path(.*)*',
        redirect: '/404',
    },
    {
        // 404
        path: '/404',
        component: () => import('/@/views/common/error/404.vue'),
        meta: {
            title: pageTitle('notFound'), // 页面不存在
        },
    },
    {
        // 后台找不到页面了-可能是路由未加载上
        path: '/platform:path(.*)*',
        redirect: (to) => {
            return { path: '/platform/loading', query: { url: to.path, query: JSON.stringify(to.query) } }
        },
    },
    {
        path: '/platform/iot/device/detail/:id',
        component: () => import('/@/views/platform/iot/device/device/detail/index.vue'),
        meta: {
            title: '设备详情',
            jump: true,
            layout: "platform/index"
        },
        children: [
            {
                path: 'base',
                component: () => import('/@/views/platform/iot/device/device/detail/base.vue'),
                meta: {
                    title: '设备信息',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/device'
                },
            },
            {
                path: 'data',
                component: () => import('/@/views/platform/iot/device/device/detail/data.vue'),
                meta: {
                    title: '数据上报',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/device'
                },
            },
            {
                path: 'event',
                component: () => import('/@/views/platform/iot/device/device/detail/event.vue'),
                meta: {
                    title: '事件上报',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/device'
                },
            },
            {
                path: 'raw',
                component: () => import('/@/views/platform/iot/device/device/detail/raw.vue'),
                meta: {
                    title: '通信报文',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/device'
                },
            },
            {
                path: 'cmd',
                component: () => import('/@/views/platform/iot/device/device/detail/cmd.vue'),
                meta: {
                    title: '指令下发',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/device'
                },
            },
        ]
    },
    {
        path: '/platform/iot/device/product/:id',
        component: () => import('/@/views/platform/iot/device/product/id.vue'),
        meta: {
            title: '产品详情',
            jump: true,
            layout: "platform/index"
        },
        children: [
            {
                path: 'detail',
                component: () => import('/@/views/platform/iot/device/product/detail/index.vue'),
                meta: {
                    title: '产品详情',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/product'
                },
            },
            {
                path: 'device',
                component: () => import('/@/views/platform/iot/device/product/device/index.vue'),
                meta: {
                    title: '设备列表',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/product'
                },
            },
            {
                path: 'event',
                component: () => import('/@/views/platform/iot/device/product/event/index.vue'),
                meta: {
                    title: '事件列表',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/product'
                },
            },
            {
                path: 'command',
                component: () => import('/@/views/platform/iot/device/product/command/index.vue'),
                meta: {
                    title: '指令列表',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/product'
                },
            },
            {
                path: 'subscribe',
                component: () => import('/@/views/platform/iot/device/product/subscribe/index.vue'),
                meta: {
                    title: '订阅管理',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/product'
                },
            },
            {
                path: 'dtuparam',
                component: () => import('/@/views/platform/iot/device/product/dtuparam/index.vue'),
                meta: {
                    title: 'DTU参数管理',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/product'
                },
            },
            {
                path: 'firmware',
                component: () => import('/@/views/platform/iot/device/product/firmware/index.vue'),
                meta: {
                    title: '固件管理',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/product'
                },
            },
            {
                path: 'config',
                component: () => import('/@/views/platform/iot/device/product/config/index.vue'),
                meta: {
                    title: '产品配置',
                    jump: true,
                    layout: "platform/index",
                    activeMenu: '/platform/iot/device/product'
                },
            },
        ]
    },
]

/*
 * 后台基础静态路由
 */
const platformBaseRoute: RouteRecordRaw = {
    path: '/platform/loading',
    component: () => import('/@/layouts/platform/index.vue'),
    redirect: '/platform/loading',
    children: [
        {
            path: '/platform/loading',
            component: () => import('/@/layouts/platform/components/Loading/index.vue'),
            meta: {
                title: '加载中...',
            },
        },
    ],
}

const platformDashboard: RouteRecordRaw = {
    // 控制台
    path: '/platform/dashboard',
    name: 'platform-dashboard',
    component: () => import('/@/views/platform/dashboard.vue'),
    meta: {
        title: '控制台',
        affix: true,
        layout: "platform/index"
    },
}

staticRoutes.push(platformBaseRoute)

export { staticRoutes,platformDashboard }

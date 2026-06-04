import { createRouter, createWebHistory } from 'vue-router'
import http from '../utils/http'
import { useStore } from '../store'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import TicketQuery from '../views/TicketQuery.vue'
import BuyTicket from '../views/BuyTicket.vue'
import MyOrders from '../views/MyOrders.vue'
import RouteQuery from '../views/RouteQuery.vue'
import TrainManagement from '../views/TrainManagement.vue'
import TicketManagement from '../views/TicketManagement.vue'
import TrainListView from '../views/TrainListView.vue'

// 用户端布局与页面
import UserLayout from '../views/user/UserLayout.vue'
import UserHome from '../views/user/UserHome.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },

  // ===== 用户端路由（普通用户使用 UserLayout） =====
  {
    path: '/user',
    component: UserLayout,
    children: [
      {
        path: 'home',
        name: 'UserHome',
        component: UserHome
      },
      {
        path: 'buy-ticket',
        name: 'UserBuyTicket',
        component: BuyTicket
      },
      {
        path: 'my-orders',
        name: 'UserMyOrders',
        component: MyOrders
      },
      {
        path: 'route-query',
        name: 'UserRouteQuery',
        component: RouteQuery
      },
      {
        path: 'train-list',
        name: 'UserTrainList',
        component: TrainListView
      }
    ]
  },

  // ===== 管理端路由（管理员使用 App.vue 的管理布局） =====
  {
    path: '/ticket-query',
    name: 'TicketQuery',
    component: TicketQuery
  },
  {
    path: '/train-management',
    name: 'TrainManagement',
    component: TrainManagement
  },
  {
    path: '/ticket-management',
    name: 'TicketManagement',
    component: TicketManagement
  },

  // 以下路由保留给管理员从管理布局访问（与用户端共享组件）
  {
    path: '/buy-ticket',
    name: 'BuyTicket',
    component: BuyTicket
  },
  {
    path: '/my-orders',
    name: 'MyOrders',
    component: MyOrders
  },
  {
    path: '/route-query',
    name: 'RouteQuery',
    component: RouteQuery
  },
  {
    path: '/train-list',
    name: 'TrainListView',
    component: TrainListView
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

let sessionValidated = false
let sessionValidationPromise = null

const ensureSession = async (store) => {
  if (!store.sessionId) {
    sessionValidated = false
    return false
  }

  if (sessionValidated && store.userInfo) {
    return true
  }

  if (!sessionValidationPromise) {
    sessionValidationPromise = http.get('/api/user/validate').then((response) => {
      if (response.data.code === 200) {
        store.setSession(store.sessionId, response.data.data)
        sessionValidated = true
        return true
      }

      store.logout()
      sessionValidated = false
      return false
    }).catch(() => {
      store.logout()
      sessionValidated = false
      return false
    }).finally(() => {
      sessionValidationPromise = null
    })
  }

  return sessionValidationPromise
}

const isUserRoute = (path) => path.startsWith('/user/')
const isAdminOnlyRoute = (path) =>
  ['/ticket-query', '/train-management', '/ticket-management'].includes(path)

router.beforeEach(async (to, from, next) => {
  const store = useStore()
  const publicPages = ['/login', '/register']
  const authRequired = !publicPages.includes(to.path)
  const hasSession = await ensureSession(store)

  // 未登录 -> 跳转登录页
  if (authRequired && !hasSession) {
    next('/login')
    return
  }

  // 已登录用户访问公开页 -> 根据权限跳转
  if (hasSession && publicPages.includes(to.path)) {
    const isAdmin = store.userInfo?.privilege >= 10
    next(isAdmin ? '/ticket-query' : '/user/home')
    return
  }

  // 普通用户无权访问管理端页面
  if (hasSession && isAdminOnlyRoute(to.path) && (!store.userInfo || store.userInfo.privilege < 10)) {
    next('/user/home')
    return
  }

  // 普通用户访问旧版用户页面 -> 重定向到用户端路由
  if (hasSession && store.userInfo?.privilege < 10) {
    const oldUserPages = ['/buy-ticket', '/my-orders', '/route-query', '/train-list']
    if (oldUserPages.includes(to.path)) {
      next('/user' + to.path)
      return
    }
  }

  next()
})

export default router

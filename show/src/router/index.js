import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'
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
  {
    path: '/ticket-query',
    name: 'TicketQuery',
    component: TicketQuery
  },
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
    path: '/train-management',
    name: 'TrainManagement',
    component: TrainManagement
  },
  {
    path: '/ticket-management',
    name: 'TicketManagement',
    component: TicketManagement
  },
  {
    path: '/train-list',
    name: 'TrainListView',
    component: TrainListView
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
    sessionValidationPromise = axios.get('/api/user/validate', {
      headers: {
        Authorization: `Bearer ${store.sessionId}`
      }
    }).then((response) => {
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

router.beforeEach(async (to, from, next) => {
  const store = useStore()
  const publicPages = ['/login', '/register']
  const authRequired = !publicPages.includes(to.path)
  const adminOnlyPages = ['/ticket-query', '/train-management', '/ticket-management']
  const hasSession = await ensureSession(store)

  if (authRequired && !hasSession) {
    next('/login')
    return
  }

  if (hasSession && publicPages.includes(to.path)) {
    next(store.userInfo?.privilege >= 10 ? '/ticket-query' : '/buy-ticket')
    return
  }

  if (hasSession && adminOnlyPages.includes(to.path) && (!store.userInfo || store.userInfo.privilege < 10)) {
    next('/buy-ticket')
    return
  }

  next()
})

export default router

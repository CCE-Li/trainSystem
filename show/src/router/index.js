import { createRouter, createWebHistory } from 'vue-router'
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
    redirect: '/ticket-query'
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

router.beforeEach((to, from, next) => {
  const store = useStore()
  const publicPages = ['/login', '/register']
  const authRequired = !publicPages.includes(to.path)
  const adminOnlyPages = ['/ticket-query', '/train-management', '/ticket-management']

  if (authRequired && !store.sessionId) {
    next('/login')
    return
  }

  if (store.sessionId && publicPages.includes(to.path)) {
    next(store.userInfo?.privilege >= 10 ? '/ticket-query' : '/buy-ticket')
    return
  }

  if (store.sessionId && adminOnlyPages.includes(to.path) && (!store.userInfo || store.userInfo.privilege < 10)) {
    next('/buy-ticket')
    return
  }

  next()
})

export default router

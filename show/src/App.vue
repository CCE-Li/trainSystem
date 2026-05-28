<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">TS</div>
        <div>
          <p class="brand-title">Train System</p>
          <p class="brand-subtitle">票务运营控制台</p>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-if="!userInfo" index="/login">
          <el-icon><User /></el-icon>
          <span>登录</span>
        </el-menu-item>
        <el-menu-item v-if="!userInfo" index="/register">
          <el-icon><UserFilled /></el-icon>
          <span>注册</span>
        </el-menu-item>

        <template v-if="userInfo">
          <el-menu-item index="/ticket-query">
            <el-icon><Search /></el-icon>
            <span>余票查询</span>
          </el-menu-item>
          <el-menu-item index="/buy-ticket">
            <el-icon><ShoppingCart /></el-icon>
            <span>在线购票</span>
          </el-menu-item>
          <el-menu-item index="/my-orders">
            <el-icon><Document /></el-icon>
            <span>我的订单</span>
          </el-menu-item>
          <el-menu-item index="/route-query">
            <el-icon><MapLocation /></el-icon>
            <span>路线查询</span>
          </el-menu-item>
          <el-menu-item index="/train-list">
            <el-icon><List /></el-icon>
            <span>车次总览</span>
          </el-menu-item>

          <template v-if="userInfo.privilege >= 10">
            <el-menu-item index="/train-management">
              <el-icon><Management /></el-icon>
              <span>车次管理</span>
            </el-menu-item>
            <el-menu-item index="/ticket-management">
              <el-icon><SetUp /></el-icon>
              <span>票务管理</span>
            </el-menu-item>
          </template>
        </template>
      </el-menu>
    </aside>

    <main class="main-panel">
      <header class="topbar">
        <div>
          <h1>火车票务管理系统</h1>
          <p>统一处理查询、购票、订单与运维管理</p>
        </div>

        <div v-if="userInfo" class="user-panel">
          <div class="user-chip">
            <strong>{{ userInfo.username }}</strong>
            <span>ID {{ userInfo.userId }}</span>
          </div>
          <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
        </div>
      </header>

      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useStore } from './store'

const router = useRouter()
const route = useRoute()
const store = useStore()

const userInfo = computed(() => store.userInfo)
const activeMenu = computed(() => route.path)

/**
 * 页面刷新后，如果本地还有 sessionId，就向后端确认它是否仍然有效。
 * 这样可以避免前端本地状态和后端真实登录态不一致。
 */
const restoreSession = async () => {
  if (!store.sessionId) {
    return
  }

  try {
    const response = await axios.get('/api/user/validate', {
      headers: {
        Authorization: `Bearer ${store.sessionId}`
      }
    })

    if (response.data.code === 200) {
      store.setSession(store.sessionId, response.data.data)
      return
    }
  } catch (error) {
    // Ignore and clear stale session below.
  }

  store.logout()
  if (route.path !== '/login' && route.path !== '/register') {
    ElMessage.warning('登录状态已失效，请重新登录')
    router.push('/login')
  }
}

/**
 * 即使后端登出请求失败，也强制清空前端本地会话，避免界面继续误判为已登录。
 */
const handleLogout = async () => {
  try {
    await axios.post('/api/user/logout', {}, {
      headers: {
        Authorization: `Bearer ${store.sessionId}`
      }
    })
  } catch (error) {
    // Clear local session even if backend logout fails.
  }

  store.logout()
  router.push('/login')
}

onMounted(() => {
  restoreSession()
})
</script>

<style scoped>
.shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.12), transparent 28%),
    linear-gradient(135deg, #f7fbff 0%, #edf4fb 100%);
}

.sidebar {
  padding: 28px 20px;
  background: #f4f6fb;
  color: #1e3a8a;
  border-right: 1px solid #dbe7f5;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  color: #ffffff;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.brand-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e3a8a;
}

.brand-subtitle {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.menu {
  border-right: none;
  background: transparent;
}

.main-panel {
  padding: 24px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 28px 32px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 18px 60px rgba(28, 52, 84, 0.08);
  backdrop-filter: blur(16px);
}

.topbar h1 {
  margin: 0;
  font-size: 30px;
  color: #0f172a;
}

.topbar p {
  margin: 8px 0 0;
  color: #475569;
}

.user-panel {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-chip {
  display: flex;
  flex-direction: column;
  padding: 10px 14px;
  border-radius: 16px;
  background: #eff6ff;
  color: #1e3a8a;
}

.user-chip span {
  font-size: 12px;
  color: #64748b;
}

.content {
  min-height: calc(100vh - 156px);
}

:deep(.el-menu) {
  background: transparent;
}

:deep(.el-menu-item) {
  margin-bottom: 8px;
  border-radius: 14px;
  color: #334155;
}

:deep(.el-menu-item:hover) {
  background: #e7f0fb;
  color: #1d4ed8;
}

:deep(.el-menu-item.is-active) {
  background: #dbeafe;
  color: #1d4ed8;
}

@media (max-width: 960px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    padding-bottom: 12px;
  }

  .topbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

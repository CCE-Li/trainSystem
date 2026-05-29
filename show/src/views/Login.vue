<template>
  <div class="auth-shell">
    <section class="hero">
      <p class="eyebrow">Passenger Access</p>
      <h2>进入票务控制台</h2>
      <p class="description">查询余票、下单购票、查看订单和路线信息，都从这里开始。</p>
    </section>

    <el-card class="auth-card" shadow="never">
      <template #header>
        <div>
          <h3>用户登录</h3>
          <p>输入用户 ID 或用户名，以及密码继续</p>
        </div>
      </template>

      <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-position="top">
        <el-form-item label="用户 ID 或用户名" prop="account">
          <el-input v-model="loginForm.account" placeholder="例如 123456 或 admin" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            show-password
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item class="actions">
          <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
          <el-button @click="router.push('/register')">去注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useStore } from '../store'

const router = useRouter()
const store = useStore()

const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  account: '',
  password: ''
})

const rules = {
  account: [
    { required: true, message: '请输入用户 ID 或用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) {
    return
  }

  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    const response = await axios.post('/api/user/login', {
      account: loginForm.account,
      password: loginForm.password
    })

    if (response.data.code === 200) {
      const { sessionId, user } = response.data.data
      store.setSession(sessionId, user)
      ElMessage.success('登录成功')
      router.push(user?.privilege >= 10 ? '/ticket-query' : '/buy-ticket')
      return
    }

    ElMessage.error(response.data.message || '登录失败')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-shell {
  min-height: 72vh;
  display: flex;
  gap: 24px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}

.hero {
  width: min(420px, 100%);
  padding: 12px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero h2 {
  margin: 0 0 12px;
  font-size: 40px;
  line-height: 1.05;
  color: #0f172a;
}

.description {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

.auth-card {
  width: 420px;
  border: none;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
}

.auth-card h3 {
  margin: 0;
  font-size: 24px;
}

.auth-card p {
  margin: 8px 0 0;
  color: #64748b;
}

.actions :deep(.el-form-item__content) {
  display: flex;
  gap: 12px;
}

@media (max-width: 640px) {
  .hero h2 {
    font-size: 32px;
  }
}
</style>

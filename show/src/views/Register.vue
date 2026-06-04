<template>
  <div class="auth-shell">
    <section class="hero">
      <p class="eyebrow">新用户注册</p>
      <h2>加入票务系统</h2>
      <p class="description">注册后即可使用在线购票、订单查询、路线查询等功能。</p>
    </section>

    <el-card class="auth-card" shadow="never">
      <template #header>
        <div>
          <h3>用户注册</h3>
          <p>注册时无需填写用户 ID，系统会自动分配编号。</p>
        </div>
      </template>
      <el-form ref="registerFormRef" :model="registerForm" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" />
          <div v-if="registerForm.password" class="password-strength">
            <div class="strength-bar">
              <div class="strength-fill" :style="{ width: passwordStrength.percent + '%', background: passwordStrength.color }"></div>
            </div>
            <span class="strength-label" :style="{ color: passwordStrength.color }">{{ passwordStrength.label }}</span>
          </div>
        </el-form-item>
        <el-form-item class="actions">
          <el-button type="primary" @click="handleRegister" :loading="loading">注册</el-button>
          <el-button @click="router.push('/login')">返回登录</el-button>
        </el-form-item>
      </el-form>

      <!-- 注册成功提示 -->
      <el-result
        v-if="registerSuccess"
        icon="success"
        title="注册成功"
        :sub-title="`你的用户 ID 是 ${registeredUserId}，${ countdown }秒后自动跳转登录页`"
      >
        <template #extra>
          <el-button type="primary" @click="router.push('/login')">立即登录</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'
import { ElMessage } from 'element-plus'

const router = useRouter()

const registerFormRef = ref(null)
const loading = ref(false)
const registerSuccess = ref(false)
const registeredUserId = ref('')
const countdown = ref(5)
let countdownTimer = null

const registerForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名至少 2 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, message: '密码长度至少 4 位', trigger: 'blur' }
  ]
}

const passwordStrength = computed(() => {
  const pwd = registerForm.password
  if (!pwd) return { percent: 0, label: '', color: '#ccc' }

  let score = 0
  if (pwd.length >= 4) score += 20
  if (pwd.length >= 8) score += 20
  if (/[A-Z]/.test(pwd)) score += 20
  if (/[0-9]/.test(pwd)) score += 20
  if (/[^A-Za-z0-9]/.test(pwd)) score += 20

  if (score <= 20) return { percent: 20, label: '弱', color: '#ef4444' }
  if (score <= 40) return { percent: 40, label: '较弱', color: '#f97316' }
  if (score <= 60) return { percent: 60, label: '中等', color: '#eab308' }
  if (score <= 80) return { percent: 80, label: '较强', color: '#22c55e' }
  return { percent: 100, label: '强', color: '#15803d' }
})

const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const response = await http.post('/api/user/register', registerForm)

    if (response.data.code === 200) {
      const user = response.data.data
      registeredUserId.value = user.userId
      registerSuccess.value = true
      countdown.value = 5

      countdownTimer = setInterval(() => {
        countdown.value -= 1
        if (countdown.value <= 0) {
          clearInterval(countdownTimer)
          router.push('/login')
        }
      }, 1000)
    } else {
      ElMessage.error(response.data.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
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

.password-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.strength-bar {
  flex: 1;
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease, background 0.3s ease;
}

.strength-label {
  font-size: 12px;
  font-weight: 700;
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
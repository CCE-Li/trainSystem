<template>
  <div class="user-shell">
    <!-- ===== 悬浮导航栏 ===== -->
    <header class="header">
      <div class="header-top">
        <div class="header-left">
          <div class="logo" @click="router.push('/user/home')">
            <span class="logo-icon">
              <img :src="projectLogo" alt="Railway Logo" class="logo-image" />
            </span>
            <div class="logo-text-wrap">
              <span class="logo-text">RAIL<span class="logo-accent">WAY</span></span>
              <span class="logo-sub">智能票务系统</span>
            </div>
          </div>
        </div>

        <nav class="nav">
          <router-link to="/user/home" class="nav-link" active-class="nav-active">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M2 7L9 1L16 7V15C16 15.6 15.6 16 15 16H3C2.4 16 2 15.6 2 15V7Z"/>
            </svg>
            <span>首页</span>
          </router-link>
          <router-link to="/user/buy-ticket" class="nav-link" active-class="nav-active">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="1" y="3" width="16" height="12" rx="2"/>
              <path d="M6 3V15M12 3V15M1 7.5H17M1 10.5H17"/>
            </svg>
            <span>购票</span>
          </router-link>
          <router-link to="/user/my-orders" class="nav-link" active-class="nav-active">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 2H14C14.6 2 15 2.4 15 3V16L12 14L9 16L6 14L3 16V3C3 2.4 3.4 2 4 2Z"/>
            </svg>
            <span>我的订单</span>
          </router-link>
          <router-link to="/user/route-query" class="nav-link" active-class="nav-active">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="9" cy="9" r="7"/>
              <path d="M2 9h14M9 2c2 2 3.5 4.5 3.5 7S11 14 9 16C7 14 5.5 11.5 5.5 9S7 4 9 2Z"/>
            </svg>
            <span>路线查询</span>
          </router-link>
          <router-link to="/user/train-list" class="nav-link" active-class="nav-active">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="2" y="2" width="14" height="14" rx="2"/>
              <path d="M2 6h14M6 2v14"/>
            </svg>
            <span>车次总览</span>
          </router-link>
        </nav>
      </div>

      <div class="header-bottom">
        <div class="quick-search">
          <div class="quick-search-field">
            <span class="quick-search-label">出发站</span>
            <el-autocomplete
              v-model="quickSearch.departureStation"
              :fetch-suggestions="queryDepartureSuggestions"
              placeholder="输入出发站"
              clearable
              class="quick-input"
            />
          </div>

          <button class="quick-switch" type="button" @click="swapQuickStations" aria-label="交换站点">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M4 6H14M14 6L11.5 3.5M14 6L11.5 8.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M14 12H4M4 12L6.5 9.5M4 12L6.5 14.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>

          <div class="quick-search-field">
            <span class="quick-search-label">到达站</span>
            <el-autocomplete
              v-model="quickSearch.arrivalStation"
              :fetch-suggestions="queryArrivalSuggestions"
              placeholder="输入到达站"
              clearable
              class="quick-input"
            />
          </div>

          <button class="quick-submit" type="button" @click="goToQuickSearch">
            查询余票
          </button>
        </div>
      </div>
    </header>

    <div class="page-user-corner">
      <div class="user-menu">
        <el-dropdown trigger="click" placement="bottom-end">
          <button class="user-btn">
            <span class="user-avatar">{{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
            <div class="user-info">
              <span class="user-name">{{ userInfo?.username || '用户' }}</span>
              <span class="user-id">ID {{ userInfo?.userId || '-' }}</span>
            </div>
            <svg width="14" height="14" viewBox="0 0 12 12" fill="none" class="chevron">
              <path d="M3 5L6 8L9 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <template #dropdown>
            <el-dropdown-menu class="dropdown-menu">
              <el-dropdown-item class="dropdown-item">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" style="margin-right:8px">
                  <circle cx="8" cy="5" r="3" stroke="#083f79" stroke-width="1.2"/>
                  <path d="M2 15C2 11.5 4.5 9 8 9C11.5 9 14 11.5 14 15" stroke="#083f79" stroke-width="1.2" stroke-linecap="round"/>
                </svg>
                个人信息
              </el-dropdown-item>
              <el-dropdown-item divided class="dropdown-item" @click="handleLogout">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" style="margin-right:8px">
                  <path d="M6 2H3C2.4 2 2 2.4 2 3V13C2 13.6 2.4 14 3 14H6" stroke="#ef4444" stroke-width="1.2" stroke-linecap="round"/>
                  <path d="M11 11L14 8L11 5" stroke="#ef4444" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M14 8H6" stroke="#ef4444" stroke-width="1.2" stroke-linecap="round"/>
                </svg>
                <span style="color:#ef4444">退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- ===== 主体内容 ===== -->
    <main class="main">
      <router-view />
    </main>

    <!-- ===== 页脚 ===== -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-top">
          <div class="footer-brand">
            <span class="footer-logo">
              <img :src="projectLogo" alt="Railway Logo" class="footer-logo-image" />
            </span>
            <span class="footer-brand-text">RAILWAY</span>
          </div>
          <div class="footer-links">
            <a href="#">关于我们</a>
            <a href="#">帮助中心</a>
            <a href="#">服务条款</a>
            <a href="#">隐私政策</a>
          </div>
        </div>
        <div class="footer-divider"></div>
        <div class="footer-bottom">
          <p>&copy; 2026 Railway 票务系统. All rights reserved.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useStore } from '../../store'
import projectLogo from '../../assets/logo.png'

const router = useRouter()
const store = useStore()

const userInfo = computed(() => store.userInfo)
const stationOptions = ref([])
const quickSearch = reactive({
  departureStation: '',
  arrivalStation: ''
})

const normalizeStationSuggestion = (query, station) => ({
  value: station,
  query,
  label: station
})

const buildSuggestions = (query, excludeStation = '') => {
  const keyword = query.trim().toLowerCase()
  return stationOptions.value
    .filter((station) => station && station !== excludeStation)
    .filter((station) => !keyword || station.toLowerCase().includes(keyword))
    .slice(0, 8)
    .map((station) => normalizeStationSuggestion(query, station))
}

const queryDepartureSuggestions = (queryString, callback) => {
  callback(buildSuggestions(queryString, quickSearch.arrivalStation))
}

const queryArrivalSuggestions = (queryString, callback) => {
  callback(buildSuggestions(queryString, quickSearch.departureStation))
}

const swapQuickStations = () => {
  const departure = quickSearch.departureStation
  quickSearch.departureStation = quickSearch.arrivalStation
  quickSearch.arrivalStation = departure
}

const loadStations = async () => {
  try {
    const response = await axios.get('/api/route/stations')
    if (response.data.code === 200) {
      stationOptions.value = response.data.data || []
      return
    }
    ElMessage.warning(response.data.message || '站点候选加载失败')
  } catch (error) {
    ElMessage.warning(error.response?.data?.message || '站点候选加载失败')
  }
}

const goToQuickSearch = () => {
  if (!quickSearch.departureStation || !quickSearch.arrivalStation) {
    ElMessage.warning('请先输入出发站和到达站')
    return
  }

  if (quickSearch.departureStation === quickSearch.arrivalStation) {
    ElMessage.warning('出发站和到达站不能相同')
    return
  }

  router.push({
    path: '/user/buy-ticket',
    query: {
      departureStation: quickSearch.departureStation,
      arrivalStation: quickSearch.arrivalStation
    }
  })
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'btn-primary'
    })

    try {
      await axios.post('/api/user/logout', {}, {
        headers: { Authorization: `Bearer ${store.sessionId}` }
      })
    } catch {
      // 后端登出失败也清除本地会话
    }

    store.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadStations()
})
</script>

<style scoped>
/* ===== 全局变量 ===== */
.user-shell {
  --color-primary: #083f79;
  --color-primary-light: #0d5aa7;
  --color-primary-dark: #062e59;
  --color-accent: #f2d98b;
  --color-bg: #FFFFFF;
  --color-surface: #FFFFFF;
  --color-text: #13253b;
  --color-text-secondary: #5f6f86;
  --color-border: #dde3ea;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;

  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* ===== 悬浮导航栏 =====
     - 固定长宽比 1752 : 186
     - 距左右边框始终保持 ~400px
     - 最大宽度 1752px（对应高度 186px）
     - 距顶 26px
     - 毛玻璃悬浮效果
*/
.header {
  position: fixed;
  top: 15px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;

  /* 核心尺寸：宽高比固定，左右留白略收窄，放大导航区 */
  width: min(1860px, calc(100vw - 560px));
  min-height: 138px;

  background: transparent;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  border: none;
  border-radius: 10px;
  box-shadow: none;
  overflow: hidden;
}

.header-top {
  height: 58px;
  width: 100%;
  padding: 0 32px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) 156px;
  align-items: center;
  column-gap: 20px;
  background: #f9f9f9;
  border: 1px solid rgba(214, 221, 230, 0.9);
  border-bottom: none;
  box-shadow: 0 10px 28px rgba(20, 45, 77, 0.06);
}

.header-bottom {
  width: 100%;
  padding: 18px 0px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(214, 221, 230, 0.9);
  border-top: none;
  box-shadow:
    0 16px 38px rgba(20, 45, 77, 0.08),
    0 2px 8px rgba(18, 31, 47, 0.04);
}

/* ===== 左侧: Logo ===== */
.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.logo-icon {
  display: flex;
  align-items: center;
}

.logo-image {
  width: 44px;
  height: 44px;
  object-fit: contain;
  display: block;
}

.logo-text-wrap {
  display: flex;
  flex-direction: column;
}

.logo-text {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-text);
  letter-spacing: 2px;
  line-height: 1.1;
}

.logo-accent {
  color: #0368F0;
  font-weight: 900;
}

.logo-sub {
  font-size: 10px;
  color: var(--color-text-secondary);
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-top: 2px;
}

/* ===== 中间: 导航链接 ===== */
.nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-width: 0;
}

.header-user-anchor {
  width: 156px;
  height: 1px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  padding: 0;
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  transition: color 0.2s ease, opacity 0.2s ease;
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.nav-link::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -8px;
  width: 100%;
  height: 2px;
  background: currentColor;
  transform: scaleX(0);
  transform-origin: left center;
  opacity: 0;
  transition: transform 0.24s ease, opacity 0.24s ease;
}

.nav-link:hover {
  color: var(--color-primary);
  opacity: 0.86;
}

.nav-link:hover::after {
  transform: scaleX(1);
  opacity: 0.9;
}

.nav-active {
  color: var(--color-primary) !important;
  font-weight: 700;
}

.nav-active:hover {
  color: var(--color-primary) !important;
  opacity: 1;
}

.nav-active::after {
  transform: scaleX(1);
  opacity: 1;
}

.quick-search {
  width: min(100%, 1180px);
  display: flex;
  gap: 14px;
  align-items: flex-end;
  justify-content: flex-start;
  margin-left: 10px;
  margin-right: 10px;
  min-width: 0;
}

.quick-search-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1 1 0;
  min-width: 0;
}

.quick-search-label {
  font-size: 12px;
  font-weight: 700;
  color: #35506f;
  letter-spacing: 1.2px;
}

.quick-input {
  width: 100%;
  min-width: 0;
}

.quick-switch,
.quick-submit {
  height: 44px;
  flex: 0 0 auto;
  border-radius: 12px;
  border: 1px solid rgba(11, 66, 127, 0.16);
  font-family: inherit;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-switch {
  width: 44px;
  background: rgba(255, 255, 255, 0.76);
  color: var(--color-primary);
}

.quick-submit {
  min-width: 96px;
  margin-left: 10px;
  padding: 0 16px;
  background: rgba(8, 63, 121, 0.86);
  color: #ffffff;
  box-shadow: 0 10px 20px rgba(8, 63, 121, 0.12);
}

.quick-switch:hover,
.quick-submit:hover {
  transform: translateY(-1px);
}

:deep(.quick-input .el-input__wrapper) {
  min-height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 0 0 1px rgba(11, 66, 127, 0.08) inset;
}

:deep(.quick-input .el-input__inner) {
  color: var(--color-text);
}

:deep(.quick-input .el-input__inner::placeholder) {
  color: #8a98aa;
}

/* ===== 页面外层右上角: 用户菜单 ===== */
.page-user-corner {
  position: fixed;
  top: 24px;
  right: max(24px, calc((100vw - min(1820px, calc(100vw - 600px))) / 2));
  z-index: 1100;
}

.user-btn {
  display: flex;
  margin-top: -9px;
  align-items: center;
  gap: 12px;
  padding: 8px 16px 8px 8px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.66);
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
  box-shadow: 0 8px 18px rgba(18, 31, 47, 0.04);
}

.user-btn:hover {
  border-color: var(--color-primary-light);
  box-shadow: 0 10px 20px rgba(8, 63, 121, 0.07);
  transform: translateY(-1px);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), #1a5ea6);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
}

.user-info {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.2;
}

.user-id {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.chevron {
  color: var(--color-text-secondary);
  transition: transform 0.2s;
  flex-shrink: 0;
}

.user-btn:hover .chevron {
  transform: rotate(180deg);
}

/* ===== 主体 ===== */
.main {
  flex: 1;
  max-width: min(1820px, calc(100vw - 600px));
  width: 100%;
  margin: 0 auto;
  /*
    避开固定导航栏:
    top: 26px
    + nav height（1752:186 等比缩放）
    + 间距 36px
  */
  margin-top: 170px;
  padding: 0 32px 40px;
}

/* ===== 页脚 ===== */
.footer {
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}

.footer-inner {
  max-width: min(1820px, calc(100vw - 600px));
  margin: 0 auto;
  padding: 32px 32px 24px;
}

.footer-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-logo {
  display: flex;
  align-items: center;
}

.footer-logo-image {
  width: 24px;
  height: 24px;
  object-fit: contain;
  display: block;
}

.footer-brand-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: 1px;
}

.footer-links {
  display: flex;
  gap: 24px;
}

.footer-links a {
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: 13px;
  transition: color 0.2s;
}

.footer-links a:hover {
  color: var(--color-primary);
}

.footer-divider {
  height: 1px;
  background: var(--color-border);
  margin: 20px 0;
}

.footer-bottom {
  display: flex;
  justify-content: center;
}

.footer-bottom p {
  margin: 0;
  color: #94A3B8;
  font-size: 12px;
}

/* ===================================================================
   响应式布局 — 5 个断点
   导航栏与内容容器保持相同宽度，等比缩小侧边间距
   =================================================================== */

/* ---- 1400px 以下：缩小侧边间距 ---- */
@media (max-width: 1400px) {
  .header {
    width: min(1752px, calc(100vw - 120px));
  }

  .main,
  .footer-inner {
    max-width: min(1752px, calc(100vw - 120px));
  }

  .main {
    margin-top: calc(26px + (min(1752px, 100vw - 120px) * 186 / 1752) + 36px);
  }

  .header-top,
  .header-bottom {
    padding-left: 28px;
    padding-right: 28px;
  }

  .header-top {
    grid-template-columns: auto minmax(0, 1fr) 148px;
  }

  .header-user-anchor {
    width: 148px;
  }

  .header-bottom {
    padding-top: 18px;
    padding-bottom: 20px;
  }

  .page-user-corner {
    right: 36px;
  }
}

/* ---- 1200px 以下：紧凑导航链接 ---- */
@media (max-width: 1200px) {
  .header {
    width: min(1752px, calc(100vw - 80px));
  }

  .main,
  .footer-inner {
    max-width: min(1752px, calc(100vw - 80px));
  }

  .main {
    margin-top: calc(26px + (min(1752px, 100vw - 80px) * 186 / 1752) + 36px);
  }

  .header-top,
  .header-bottom {
    padding-left: 24px;
    padding-right: 24px;
  }

  .header-top {
    grid-template-columns: auto minmax(0, 1fr) 140px;
  }

  .header-bottom {
    padding-top: 18px;
    padding-bottom: 20px;
  }

  .page-user-corner {
    right: 28px;
  }

  .nav-link svg {
    width: 15px;
    height: 15px;
  }

  .logo-text {
    font-size: 20px;
    letter-spacing: 1.5px;
  }

  .nav {
    gap: 14px;
  }

  .header-user-anchor {
    width: 140px;
  }
}

/* ---- 992px 以下：平板布局 ---- */
@media (max-width: 992px) {
  .header {
    width: calc(100vw - 48px);
    min-height: 150px;
  }

  .main,
  .footer-inner {
    max-width: calc(100vw - 48px);
  }

  .main {
    margin-top: calc(26px + ((100vw - 48px) * 186 / 1752) + 32px);
    padding: 0 24px 36px;
  }

  .header-top,
  .header-bottom {
    padding-left: 24px;
    padding-right: 24px;
  }

  .header-top {
    grid-template-columns: auto minmax(0, 1fr) 124px;
  }

  .page-user-corner {
    top: 74px;
    right: 24px;
  }

  .quick-search {
    flex-wrap: wrap;
  }

  .quick-submit {
    margin-left: auto;
  }

  .nav {
    gap: 10px;
  }

  .nav-link {
    font-size: 13px;
    gap: 4px;
  }

  .nav-link span {
    font-size: 12px;
  }

  .nav-link svg {
    display: none;
  }

  .logo-text {
    font-size: 20px;
    letter-spacing: 1px;
  }

  .logo-sub {
    display: none;
  }

  .user-btn {
    padding: 6px 12px 6px 6px;
    gap: 8px;
  }

  .user-avatar {
    width: 34px;
    height: 34px;
    font-size: 14px;
  }

  .user-name {
    font-size: 13px;
  }

  .header-user-anchor {
    width: 124px;
  }

  .footer-inner {
    padding: 28px 24px 20px;
  }

  .footer-links {
    gap: 16px;
  }
}

/* ---- 768px 以下：手机布局 ---- */
@media (max-width: 768px) {
  .header {
    position: fixed;
    top: 12px;
    left: 50%;
    transform: translateX(-50%);
    width: calc(100vw - 24px);
    min-height: auto;
    height: auto;
    border-radius: 14px;
  }

  .header-top,
  .header-bottom {
    padding-left: 16px;
    padding-right: 16px;
  }

  .header-top {
    height: auto;
    padding-top: 12px;
    padding-bottom: 12px;
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .header-bottom {
    padding-top: 14px;
    padding-bottom: 16px;
  }

  .page-user-corner {
    top: 18px;
    right: 12px;
  }

  .logo-icon svg {
    width: 36px;
    height: 36px;
  }

  .logo-text {
    font-size: 18px;
  }

  .nav {
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    gap: 4px;
    padding: 4px 0 2px;
  }

  .nav-link {
    padding: 7px 12px;
    font-size: 12px;
    white-space: nowrap;
  }

  .user-btn {
    padding: 4px 10px 4px 4px;
    gap: 6px;
  }

  .user-avatar {
    width: 30px;
    height: 30px;
    font-size: 12px;
  }

  .user-name {
    font-size: 12px;
  }

  .user-id {
    display: none;
  }

  .quick-search {
    flex-direction: column;
    align-items: stretch;
  }

  .quick-switch,
  .quick-submit {
    width: 100%;
  }

  .main {
    max-width: calc(100vw - 24px);
    padding: 0 12px 24px;
    margin-top: 220px;
  }

  .footer-inner {
    max-width: calc(100vw - 24px);
    padding: 24px 16px 18px;
  }

  .footer-top {
    flex-direction: column;
    gap: 14px;
    align-items: center;
    text-align: center;
  }

  .footer-links {
    flex-wrap: wrap;
    justify-content: center;
    gap: 12px;
  }

  .footer-links a {
    font-size: 12px;
  }

  .footer-brand-text {
    font-size: 14px;
  }
}

/* ---- 480px 以下：小屏手机 ---- */
@media (max-width: 480px) {
  .header {
    top: 8px;
    min-height: auto;
    border-radius: 12px;
  }

  .header-top,
  .header-bottom {
    padding-left: 12px;
    padding-right: 12px;
  }

  .page-user-corner {
    top: 14px;
    right: 8px;
  }

  .logo-icon svg {
    width: 30px;
    height: 30px;
  }

  .logo-text {
    font-size: 15px;
    letter-spacing: 0.5px;
  }

  .nav-link {
    padding: 4px 8px;
    font-size: 11px;
  }

  .user-btn {
    padding: 3px 8px 3px 3px;
    gap: 4px;
  }

  .user-avatar {
    width: 26px;
    height: 26px;
    font-size: 10px;
  }

  .user-name {
    font-size: 11px;
  }

  .main {
    margin-top: 212px;
    padding: 0 8px 20px;
  }

  .footer-inner {
    padding: 20px 12px 16px;
  }

  .footer-bottom p {
    font-size: 11px;
  }
}
</style>

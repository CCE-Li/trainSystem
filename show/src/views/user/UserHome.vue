<template>
  <div class="home">
    <section class="hero-shell">
      <div class="hero-grid">
        <div class="hero-media">
          <div class="hero-media-card">
            <div class="media-main hero-photo">
              <img :src="train001" alt="Train hero" class="media-photo" />
            </div>
            <div class="media-cut hero-cut-photo">
              <img :src="train002" alt="Train detail" class="media-photo" />
            </div>
          </div>
        </div>

        <div class="hero-copy">
          <span class="eyebrow">旅客出行体验</span>
          <h1 class="hero-title">更顺畅的铁路出行<br>从这儿开始</h1>
          <p class="hero-text">
            欢迎使用我们的铁路客运系统，为您提供便捷、舒适的出行体验。
          </p>
          <div class="hero-actions">
            <button class="primary-btn" @click="router.push('/user/buy-ticket')">
              立即购票
            </button>
            <button class="secondary-btn" @click="router.push('/user/route-query')">
              查看线路
            </button>
          </div>
          <div class="hero-meta">
            <div class="hero-meta-item">
              <strong>{{ stats.trains }}</strong>
              <span>运营车次</span>
            </div>
            <div class="hero-meta-item">
              <strong>{{ stats.stations }}</strong>
              <span>覆盖车站</span>
            </div>
            <div class="hero-meta-item">
              <strong>{{ stats.dailyPassengers }}+</strong>
              <span>日均服务</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="section section-news">
      <div class="section-head">
        <div>
          <span class="section-kicker">最新动态</span>
          <h2 class="section-title">发现最新动态</h2>
        </div>
        <div class="section-switch">
          <button class="switch-btn" type="button" aria-label="上一组">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M11 4L6 9L11 14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <button class="switch-btn" type="button" aria-label="下一组">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M7 4L12 9L7 14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="news-grid">
        <article
          v-for="card in newsCards"
          :key="card.title"
          class="news-card"
          :class="[card.tone, card.image ? 'has-image' : 'text-only']"
        >
          <div v-if="card.image" class="news-image">
            <img :src="train003" alt="Train operations" class="media-photo news-photo" />
          </div>
          <div class="news-body">
            <span class="news-label">{{ card.label }}</span>
            <h3 class="news-title">{{ card.title }}</h3>
            <p class="news-desc">{{ card.desc }}</p>
            <button class="news-link" type="button" @click="router.push(card.to)">
              {{ card.cta }}
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M5 3L10 8L5 13" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
        </article>
      </div>
    </section>

    <section class="section section-figures">
      <div class="section-head compact">
        <div>
          <span class="section-kicker">关键数据</span>
          <h2 class="section-title">铁路服务关键数据</h2>
        </div>
      </div>

      <div class="figures-grid">
        <article class="figure-card figure-photo">
          <img :src="train004" alt="Train service view" class="media-photo figure-photo-image" />
          <div class="figure-caption">驾驶舱视角与线路调度体验</div>
        </article>

        <article class="figure-card figure-stat highlight">
          <span class="figure-overline">运力</span>
          <div class="figure-value">{{ stats.dailyPassengers }}+</div>
          <div class="figure-name">日均服务人次</div>
          <p class="figure-note">面向通勤、跨城和中长途场景提供稳定的购票与查询体验。</p>
        </article>

        <article class="figure-card figure-photo">
          <img :src="train002" alt="Train network" class="media-photo figure-photo-image" />
          <div class="figure-caption">站场、列车与实时运行网络</div>
        </article>

        <article class="figure-card figure-stat">
          <span class="figure-overline">覆盖</span>
          <div class="mini-stats">
            <div class="mini-item">
              <strong>{{ stats.routes }}</strong>
              <span>运行路线</span>
            </div>
            <div class="mini-item">
              <strong>{{ stats.stations }}</strong>
              <span>覆盖车站</span>
            </div>
            <div class="mini-item">
              <strong>{{ stats.trains }}</strong>
              <span>可查车次</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section class="section section-actions">
      <div class="section-head compact">
        <div>
          <span class="section-kicker">快捷入口</span>
          <h2 class="section-title">常用功能入口</h2>
        </div>
      </div>

      <div class="action-grid">
        <button class="action-card" type="button" @click="router.push('/user/buy-ticket')">
          <span class="action-index">01</span>
          <h3>购票中心</h3>
          <p>按日期和区间快速查询余票并完成下单。</p>
        </button>
        <button class="action-card" type="button" @click="router.push('/user/my-orders')">
          <span class="action-index">02</span>
          <h3>订单管理</h3>
          <p>集中查看历史订单、退票状态与行程信息。</p>
        </button>
        <button class="action-card" type="button" @click="router.push('/user/train-list')">
          <span class="action-index">03</span>
          <h3>车次总览</h3>
          <p>浏览当前系统维护的全部列车与运行时刻。</p>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import train001 from '../../assets/train_001.jpg'
import train002 from '../../assets/train_002.jpg'
import train003 from '../../assets/train_003.jpg'
import train004 from '../../assets/train_004.jpg'

const router = useRouter()

const stats = reactive({
  trains: 0,
  stations: 0,
  routes: 0,
  dailyPassengers: '0'
})

const loadStats = async () => {
  try {
    const [trainsRes, stationsRes] = await Promise.all([
      http.get('/api/train/list'),
      http.get('/api/route/stations')
    ])
    if (trainsRes.data.code === 200) {
      stats.trains = trainsRes.data.data?.length || 0
    }
    if (stationsRes.data.code === 200) {
      stats.stations = stationsRes.data.data?.length || 0
    }
    // 计算路线数：站数-1 (粗略估计)
    stats.routes = Math.max(stats.stations - 1, 0)
    stats.dailyPassengers = stats.trains > 0 ? String(Math.round(stats.trains * 19.5)) : '0'
  } catch {
    // 加载失败保持默认值
  }
}

const newsCards = [
  {
    label: '购票服务',
    title: '新版购票流程已上线，减少关键步骤切换。',
    desc: '更聚焦的流程布局把查询、筛选和确认订单压缩在更短的操作链路里。',
    cta: '前往购票',
    to: '/user/buy-ticket',
    tone: 'tone-apricot',
    image: false
  },
  {
    label: '运营管理',
    title: '铁路运行环境规划科学、管理到位。',
    desc: '人车互不干扰，整体通行效率高、出行安全系数强，软硬件配套齐全，是兼具效率与舒适度的优质铁路运行空间。',
    cta: '查看车次',
    to: '/user/train-list',
    tone: 'tone-ivory',
    image: true
  },
  {
    label: '线路查询',
    title: '线路查询页面支持更直观地查看出发地与目的地路线。',
    desc: '首页保留内容卡片入口，让首次访问用户更容易理解系统可完成的任务。',
    cta: '查看线路',
    to: '/user/route-query',
    tone: 'tone-rose',
    image: false
  }
]

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 84px;
  color: #413927;
  margin-top: calc(-1 * (26px + (min(1860px, 100vw - 560px) * 186 / 1752) + 36px) - 7px);
}

.section {
  display: flex;
  flex-direction: column;
}

.hero-shell {
  margin-top: 0;
  width: 100vw;
  margin-left: calc(50% - 50vw);
  margin-right: calc(50% - 50vw);
  height: 600px;
  padding: 240px 130px 25px;
  background: #EED483;
  border-radius: 0 0 12px 12px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(380px, 0.92fr) minmax(360px, 1.08fr);
  align-items: center;
  gap: 72px;
}

.hero-media-card {
  position: relative;
  width: 100%;
  min-height: 410px;
}

.media-placeholder {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(215, 194, 131, 0.45)),
    repeating-linear-gradient(
      -45deg,
      rgba(81, 63, 24, 0.10) 0,
      rgba(81, 63, 24, 0.10) 14px,
      rgba(255, 255, 255, 0.14) 14px,
      rgba(255, 255, 255, 0.14) 28px
    );
  border: 1px solid rgba(129, 100, 43, 0.18);
}

.media-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.media-placeholder::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0));
}

.media-tag {
  position: absolute;
  left: 22px;
  top: 22px;
  z-index: 2;
  padding: 7px 12px;
  border-radius: 12px;
  background: rgba(8, 45, 92, 0.88);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.4px;
}

.media-main {
  position: absolute;
  inset: 0 40px 0 0;
  clip-path: polygon(4% 0, 42% 3%, 74% 9%, 100% 22%, 92% 58%, 100% 70%, 90% 100%, 4% 100%, 0 95%, 0 4%);
  border-radius: 12px;
  overflow: hidden;
}

.hero-photo {
  border: 1px solid rgba(129, 100, 43, 0.18);
  background: #efe8cf;
}

.media-cut {
  position: absolute;
  right: 0;
  bottom: 66px;
  width: 228px;
  height: 134px;
  clip-path: polygon(18% 0, 100% 0, 79% 100%, 0 100%);
  background:
    linear-gradient(135deg, rgba(199, 166, 73, 0.58), rgba(255, 255, 255, 0.74)),
    repeating-linear-gradient(
      45deg,
      rgba(56, 47, 21, 0.11) 0,
      rgba(56, 47, 21, 0.11) 12px,
      rgba(255, 255, 255, 0.18) 12px,
      rgba(255, 255, 255, 0.18) 24px
    );
  border: 1px solid rgba(129, 100, 43, 0.18);
  overflow: hidden;
}

.hero-cut-photo {
  background: #e6dcc0;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-right: 8px;
}

.eyebrow {
  display: inline-flex;
  align-self: flex-start;
  padding: 7px 13px;
  border-radius: 12px;
  background: rgba(255, 249, 231, 0.68);
  color: #715617;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.3px;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  font-size: clamp(38px, 3.3vw, 62px);
  line-height: 1.08;
  letter-spacing: -1.2px;
  font-weight: 800;
  color: #4a3b20;
  max-width: 600px;
}

.hero-text {
  margin: 0;
  max-width: 560px;
  font-size: 17px;
  line-height: 1.64;
  color: rgba(74, 59, 32, 0.88);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.primary-btn,
.secondary-btn,
.action-card,
.switch-btn,
.news-link {
  font-family: inherit;
}

.primary-btn,
.secondary-btn {
  height: 54px;
  padding: 0 26px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, background-color 0.22s ease, color 0.22s ease;
}

.primary-btn {
  border: 1px solid #0b427f;
  background: #083f79;
  color: #ffffff;
  box-shadow: 0 14px 34px rgba(8, 63, 121, 0.22);
}

.secondary-btn {
  border: 1px solid rgba(74, 59, 32, 0.18);
  background: rgba(255, 249, 231, 0.72);
  color: #4a3b20;
}

.primary-btn:hover,
.secondary-btn:hover,
.action-card:hover,
.news-card:hover,
.switch-btn:hover {
  transform: translateY(-2px);
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  padding-top: 8px;
}

.hero-meta-item {
  min-width: 148px;
  padding: 18px 18px 16px;
  border-radius: 12px;
  background: rgba(255, 249, 231, 0.6);
  border: 1px solid rgba(118, 89, 29, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.hero-meta-item strong {
  display: block;
  margin-bottom: 5px;
  font-size: 28px;
  line-height: 1;
  color: #103f77;
}

.hero-meta-item span {
  font-size: 13px;
  color: rgba(74, 59, 32, 0.7);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 32px;
}

.section-head.compact {
  margin-bottom: 28px;
}

.section-kicker {
  display: inline-block;
  margin-bottom: 10px;
  color: #8f7b44;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.7px;
  text-transform: uppercase;
}

.section-title {
  margin: 0;
  font-size: clamp(34px, 3vw, 60px);
  font-weight: 800;
  letter-spacing: -1.2px;
  color: #3c3225;
}

.section-switch {
  display: flex;
  gap: 10px;
}

.switch-btn {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  border: 1px solid #d7deea;
  background: #ffffff;
  color: #0c4a8b;
  box-shadow: 0 12px 28px rgba(12, 74, 139, 0.08);
  cursor: pointer;
}

.news-grid {
  display: grid;
  grid-template-columns: 1.06fr 1.08fr 0.76fr;
  gap: 22px;
}

.news-card {
  min-height: 540px;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(96, 78, 31, 0.08);
  box-shadow: 0 24px 54px rgba(91, 74, 29, 0.08);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.news-card.has-image {
  background: #ffffff;
}

.tone-apricot {
  background: #f8bf88;
}

.tone-rose {
  background: #f2b8b1;
}

.tone-ivory {
  background: #ffffff;
}

.news-image {
  padding: 0;
  height: 264px;
  background: #e9dfbd;
}

.news-photo {
  height: 264px;
}

.small-media {
  height: 100%;
  border: none;
  border-radius: 0;
}

.news-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: flex-start;
  padding: 30px 28px 28px;
}

.news-label {
  display: inline-block;
  margin-bottom: 16px;
  padding: 5px 8px;
  background: rgba(255, 255, 255, 0.78);
  color: #5a4526;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.news-title {
  margin: 0 0 18px;
  font-size: clamp(30px, 2.1vw, 48px);
  line-height: 1.15;
  letter-spacing: -0.8px;
  color: #382d1c;
}

.news-desc {
  margin: 0;
  font-size: 16px;
  line-height: 1.72;
  color: rgba(56, 45, 28, 0.88);
}

.news-link {
  margin-top: auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: none;
  background: transparent;
  color: #083f79;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.figures-grid {
  display: grid;
  grid-template-columns: 1.02fr 0.92fr;
  gap: 22px;
}

.figure-card {
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid rgba(96, 78, 31, 0.08);
  box-shadow: 0 22px 48px rgba(90, 74, 37, 0.06);
}

.figure-photo {
  min-height: 320px;
}

.figure-photo-image {
  min-height: 262px;
  max-height: 262px;
}

.figure-media {
  min-height: 262px;
  border: none;
  border-radius: 0;
}

.figure-media.alt {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(184, 205, 217, 0.54)),
    repeating-linear-gradient(
      -45deg,
      rgba(10, 67, 115, 0.10) 0,
      rgba(10, 67, 115, 0.10) 14px,
      rgba(255, 255, 255, 0.16) 14px,
      rgba(255, 255, 255, 0.16) 28px
    );
}

.figure-caption {
  padding: 18px 22px 22px;
  font-size: 15px;
  color: rgba(59, 49, 29, 0.8);
}

.figure-stat {
  padding: 28px 30px;
}

.figure-stat.highlight {
  display: flex;
  flex-direction: column;
  justify-content: center;
  background:
    linear-gradient(180deg, rgba(255, 247, 225, 0.95), rgba(250, 250, 248, 0.95)),
    #ffffff;
}

.figure-overline {
  display: inline-block;
  margin-bottom: 18px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.6px;
  text-transform: uppercase;
  color: #8f7b44;
}

.figure-value {
  font-size: clamp(54px, 4.2vw, 88px);
  line-height: 0.95;
  font-weight: 800;
  letter-spacing: -2px;
  color: #1a1f20;
}

.figure-name {
  margin-top: 10px;
  font-size: 24px;
  font-weight: 700;
  color: #3a2f1c;
}

.figure-note {
  margin: 18px 0 0;
  max-width: 420px;
  font-size: 15px;
  line-height: 1.7;
  color: rgba(58, 47, 28, 0.78);
}

.mini-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.mini-item {
  padding: 20px 18px;
  border-radius: 12px;
  background: #faf6ea;
  border: 1px solid rgba(118, 89, 29, 0.08);
}

.mini-item strong {
  display: block;
  margin-bottom: 8px;
  font-size: 32px;
  line-height: 1;
  color: #0c4a8b;
}

.mini-item span {
  font-size: 13px;
  color: rgba(59, 49, 29, 0.74);
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.action-card {
  text-align: left;
  padding: 28px;
  border-radius: 12px;
  border: 1px solid rgba(96, 78, 31, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 250, 238, 0.94), rgba(255, 255, 255, 0.98)),
    #ffffff;
  box-shadow: 0 18px 42px rgba(90, 74, 37, 0.06);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.action-index {
  display: inline-block;
  margin-bottom: 16px;
  color: #977b32;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1.6px;
}

.action-card h3 {
  margin: 0 0 10px;
  font-size: 24px;
  line-height: 1.15;
  color: #352b1b;
}

.action-card p {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: rgba(53, 43, 27, 0.78);
}

@media (max-width: 1440px) {
  .hero-grid {
    gap: 46px;
  }

  .news-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1200px) {
  .home {
    gap: 68px;
    margin-top: calc(-1 * (26px + (min(1752px, 100vw - 120px) * 186 / 1752) + 36px) - 7px);
  }

  .hero-shell {
    padding: 102px 24px 72px;
  }

  .hero-grid,
  .news-grid,
  .figures-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }

  .hero-media-card {
    min-height: 360px;
  }

  .news-card {
    min-height: auto;
  }

  .mini-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .home {
    gap: 52px;
    margin-top: calc(-1 * (12px + 64px + 24px) - 8px);
  }

  .hero-shell {
    padding: 84px 16px 56px;
    border-radius: 0 0 12px 12px;
  }

  .hero-grid {
    gap: 34px;
  }

  .hero-media-card {
    min-height: 320px;
  }

  .media-main {
    inset: 0 16px 0 0;
    clip-path: polygon(4% 0, 58% 3%, 92% 20%, 84% 58%, 100% 72%, 86% 100%, 4% 100%, 0 94%, 0 6%);
  }

  .media-cut {
    right: 4px;
    bottom: 38px;
    width: 148px;
    height: 88px;
  }

  .hero-title {
    font-size: 34px;
    letter-spacing: -0.9px;
  }

  .hero-text {
    font-size: 15px;
  }

  .hero-meta {
    gap: 10px;
  }

  .hero-meta-item {
    min-width: calc(50% - 5px);
  }

  .section-title {
    font-size: 34px;
  }

  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .news-title {
    font-size: 32px;
  }

  .figure-value {
    font-size: 56px;
  }

  .mini-stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .home {
    margin-top: calc(-1 * (8px + 56px + 20px) - 7px);
  }

  .hero-title {
    font-size: 28px;
  }

  .primary-btn,
  .secondary-btn {
    width: 100%;
    justify-content: center;
  }

  .hero-meta-item {
    min-width: 100%;
  }

  .news-body,
  .figure-stat,
  .action-card {
    padding: 22px 20px;
  }

  .action-card h3 {
    font-size: 21px;
  }
}
</style>

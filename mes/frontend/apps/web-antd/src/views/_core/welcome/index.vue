<script lang="ts" setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

import { IconifyIcon } from '@vben/icons';
import { useAccessStore, useUserStore } from '@vben/stores';

import {
  resolveAuthorizedHomePath,
  WELCOME_HOME_PATH,
} from '#/router/home-entry';

defineOptions({ name: 'WelcomeHome' });

const router = useRouter();
const accessStore = useAccessStore();
const userStore = useUserStore();

const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '用户',
);

const workbenchHomePath = computed(() =>
  resolveAuthorizedHomePath(accessStore.accessMenus),
);

const hasWorkbenchHome = computed(
  () => workbenchHomePath.value !== WELCOME_HOME_PATH,
);

function openWorkbench() {
  if (hasWorkbenchHome.value) {
    router.push(workbenchHomePath.value);
  }
}
</script>

<template>
  <div class="hc-welcome">
    <section class="hc-welcome__hero">
      <div class="hc-welcome__identity">
        <div class="hc-welcome__logo">
          <img alt="DoorMes 门窗工厂" src="/doormes-logo.svg" />
        </div>
        <div>
          <span>DoorMes · 门窗工厂</span>
          <h1>欢迎回来，{{ displayName }}</h1>
          <p>订单需求、门窗设计、材料映射、图纸版本与制造协同的业务入口。</p>
        </div>
      </div>

      <div class="hc-welcome__hero-side">
        <div class="hc-welcome__state">
          <IconifyIcon icon="lucide:shield-check" />
          已登录
        </div>
        <button
          v-if="hasWorkbenchHome"
          class="hc-welcome__primary"
          type="button"
          @click="openWorkbench"
        >
          <IconifyIcon icon="lucide:arrow-right" />
          进入工作台
        </button>
      </div>
    </section>

    <section class="hc-welcome__content">
      <div class="hc-welcome__status-panel">
        <div class="hc-welcome__section-title">
          <IconifyIcon icon="lucide:layout-dashboard" />
          当前入口状态
        </div>
        <h2>当前角色未配置工作台入口</h2>
        <p>
          系统已按角色加载左侧授权菜单。配置“门窗业务”及工作台权限后，登录自动进入门窗工厂工作台。
        </p>

        <div class="hc-welcome__steps">
          <div>
            <span>1</span>
            <strong>角色授权</strong>
            <p>在角色管理中勾选门窗业务及对应子菜单。</p>
          </div>
          <div>
            <span>2</span>
            <strong>重新登录</strong>
            <p>系统按授权菜单计算默认入口。</p>
          </div>
          <div>
            <span>3</span>
            <strong>进入业务</strong>
            <p>自动打开工作台首个可见子菜单。</p>
          </div>
        </div>
      </div>

      <aside class="hc-welcome__side-panel">
        <div class="hc-welcome__section-title">
          <IconifyIcon icon="lucide:list-checks" />
          可用操作
        </div>
        <ul>
          <li>继续使用左侧已授权业务菜单。</li>
          <li>通过系统管理维护角色菜单权限。</li>
          <li>授权工作台后默认首页自动切换。</li>
        </ul>
      </aside>
    </section>

    <section class="hc-welcome__domains">
      <div>
        <IconifyIcon icon="lucide:clipboard-list" />
        <span>订单需求</span>
        <strong>标准设计、定制需求</strong>
      </div>
      <div>
        <IconifyIcon icon="lucide:shield-check" />
        <span>设计研发</span>
        <strong>门窗绘图、材料映射</strong>
      </div>
      <div>
        <IconifyIcon icon="lucide:boxes" />
        <span>产品与组件</span>
        <strong>标准图纸、材质型号</strong>
      </div>
      <div>
        <IconifyIcon icon="lucide:workflow" />
        <span>制造协同</span>
        <strong>图纸定稿、版本变更</strong>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hc-welcome {
  min-height: calc(100vh - 88px);
  padding: 24px;
  background: #f3f6fa;
}

.hc-welcome__hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 172px;
  padding: 28px 32px;
  border-radius: 6px;
  background:
    linear-gradient(90deg, #f8fbff 0%, #ffffff 55%),
    #fff;
  box-shadow: 0 8px 24px rgb(29 48 72 / 6%);
}

.hc-welcome__identity {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.hc-welcome__logo {
  display: grid;
  width: 58px;
  height: 58px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 6px;
  background: #eef5ff;
}

.hc-welcome__logo img {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.hc-welcome__identity span {
  display: block;
  margin-bottom: 8px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 800;
}

.hc-welcome__identity h1 {
  margin: 0;
  color: #17233d;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: 0;
}

.hc-welcome__identity p {
  margin: 12px 0 0;
  color: #5f6f84;
  font-size: 15px;
  line-height: 1.7;
}

.hc-welcome__hero-side {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
}

.hc-welcome__state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid #bfe3d1;
  border-radius: 999px;
  background: #f0fbf5;
  color: #168449;
  font-size: 13px;
  font-weight: 600;
}

.hc-welcome__primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 4px;
  background: #1677ff;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.hc-welcome__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
  margin-top: 16px;
}

.hc-welcome__status-panel,
.hc-welcome__side-panel {
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 6px 18px rgb(29 48 72 / 5%);
}

.hc-welcome__status-panel {
  min-width: 0;
  padding: 24px 28px;
}

.hc-welcome__section-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #506176;
  font-size: 13px;
  font-weight: 700;
}

.hc-welcome__status-panel h2 {
  margin: 18px 0 10px;
  color: #18263a;
  font-size: 22px;
  font-weight: 750;
  letter-spacing: 0;
}

.hc-welcome__status-panel > p {
  max-width: 780px;
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.8;
}

.hc-welcome__steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.hc-welcome__steps > div {
  min-height: 112px;
  padding: 16px;
  border-left: 3px solid #d7e7ff;
  border-radius: 6px;
  background: #f8fafc;
}

.hc-welcome__steps span {
  display: inline-grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border-radius: 50%;
  background: #1677ff;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.hc-welcome__steps strong {
  display: block;
  margin-top: 10px;
  color: #24364b;
  font-size: 15px;
}

.hc-welcome__steps p {
  margin: 6px 0 0;
  color: #718096;
  font-size: 13px;
  line-height: 1.6;
}

.hc-welcome__side-panel {
  padding: 24px;
}

.hc-welcome__side-panel ul {
  display: grid;
  gap: 12px;
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
}

.hc-welcome__side-panel li {
  padding-left: 12px;
  border-left: 3px solid #dfe8f5;
  color: #526274;
  font-size: 14px;
  line-height: 1.7;
}

.hc-welcome__domains {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.hc-welcome__domains > div {
  min-height: 96px;
  padding: 18px;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 6px 18px rgb(29 48 72 / 5%);
}

.hc-welcome__domains svg {
  color: #1677ff;
  font-size: 22px;
}

.hc-welcome__domains span,
.hc-welcome__domains strong {
  display: block;
}

.hc-welcome__domains span {
  margin-top: 14px;
  color: #1f2d3d;
  font-size: 15px;
  font-weight: 700;
}

.hc-welcome__domains strong {
  margin-top: 6px;
  color: #748094;
  font-size: 13px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .hc-welcome {
    padding: 16px;
  }

  .hc-welcome__hero,
  .hc-welcome__hero-side {
    align-items: flex-start;
    flex-direction: column;
  }

  .hc-welcome__hero {
    padding: 22px;
  }

  .hc-welcome__content {
    grid-template-columns: 1fr;
  }

  .hc-welcome__identity {
    align-items: flex-start;
    flex-direction: column;
  }

  .hc-welcome__identity h1 {
    font-size: 24px;
  }

  .hc-welcome__steps,
  .hc-welcome__domains {
    grid-template-columns: 1fr;
  }
}
</style>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { IconifyIcon } from '@vben/icons';
import { useAccessStore, useUserStore } from '@vben/stores';
import { FACTORY_ROLE_NAMES, getAuthorizedFactoryModules } from '../factory-navigation';
import DesignCenter from '../designs/index.vue';

defineOptions({ name: 'DoorMesWorkbench' });
const router = useRouter();
const accessStore = useAccessStore();
const userStore = useUserStore();
const modules = computed(() => getAuthorizedFactoryModules(accessStore.accessMenus));
const roleNames = computed(() => (userStore.userRoles || []).map((role) => FACTORY_ROLE_NAMES[role] || role).join(' / '));
const isDesignWorkbench = computed(() => (userStore.userRoles || []).some((role) => ['factory_design', 'super_admin'].includes(role)));
</script>

<template>
  <div class="workbench-root">
  <DesignCenter v-if="isDesignWorkbench" />
  <main v-else class="factory-workbench">
    <header class="factory-workbench__header">
      <div>
        <span class="factory-workbench__eyebrow">DOORMES · 门窗工厂</span>
        <h1>工厂工作台</h1>
        <p>{{ userStore.userInfo?.nickname || userStore.userInfo?.username }} · {{ roleNames }}</p>
      </div>
      <img alt="DoorMes" src="/doormes-logo.svg" width="52" height="52" />
    </header>
    <section class="factory-workbench__notice">
      <IconifyIcon icon="lucide:info" />
      <p>定制需求、研发领用、明细 2D/3D 绘图及后端图纸版本保存/加载已接通。目录选型、资产上传、发布审核与生产协同仍按计划接入；图纸草稿不等于正式下料单。</p>
    </section>
    <section class="factory-workbench__modules" aria-label="当前角色授权业务">
      <button v-for="module in modules" :key="module.key" type="button" @click="router.push(`/factory/${module.key}`)">
        <IconifyIcon :icon="module.icon" />
        <h2>{{ module.title }}</h2>
        <p>{{ module.purpose }}</p>
        <span>进入模块 <IconifyIcon icon="lucide:arrow-right" /></span>
      </button>
      <p v-if="!modules.length">当前角色尚未分配门窗业务菜单，请由管理员维护角色权限。</p>
    </section>
    <footer>基础管理保留用户、角色、菜单、组织、岗位、字典等功能。旧行业页面已移入管理员的“备份菜单”，不作为门窗业务入口。</footer>
  </main>
  </div>
</template>

<style scoped>
.workbench-root { height: 100%; min-height: 0; overflow: auto; }
.factory-workbench { padding: 24px; background: #f3f6f9; min-height: calc(100vh - 100px); color: #1e293b; }
.factory-workbench__header { display: flex; justify-content: space-between; align-items: center; padding: 24px 28px; background: white; border: 1px solid #e2e8f0; border-radius: 6px; }
.factory-workbench__eyebrow { font-size: 12px; letter-spacing: 1px; color: #41647e; }
h1 { margin: 8px 0; font-size: 26px; font-weight: 600; } header p { margin: 0; color: #64748b; }
.factory-workbench__notice { display: flex; gap: 12px; align-items: center; margin: 18px 0; padding: 14px 18px; background: #eaf3fa; border: 1px solid #c9dfee; border-radius: 5px; color: #315b7a; }
.factory-workbench__notice p { margin: 0; line-height: 1.7; }
.factory-workbench__modules { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; }
.factory-workbench__modules button { display: flex; flex-direction: column; align-items: flex-start; min-height: 224px; padding: 22px; text-align: left; background: white; border: 1px solid #dce4ec; border-radius: 5px; cursor: pointer; transition: border-color .15s; }
.factory-workbench__modules button:hover { border-color: #407eab; }
.factory-workbench__modules button > svg { width: 26px; height: 26px; color: #28638c; }
h2 { margin: 16px 0 8px; font-size: 17px; font-weight: 600; }
.factory-workbench__modules p { color: #64748b; line-height: 1.7; }
.factory-workbench__modules span { display: flex; gap: 8px; align-items: center; margin-top: auto; padding-top: 16px; color: #28638c; }
footer { padding-top: 24px; font-size: 13px; line-height: 1.7; color: #64748b; }
</style>

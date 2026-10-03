import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    name: 'WelcomeHome',
    path: '/welcome',
    component: () => import('#/views/_core/welcome/index.vue'),
    meta: {
      hideInMenu: true,
      icon: 'lucide:factory',
      title: '欢迎',
    },
  },
  {
    meta: {
      hideInMenu: true,
      icon: 'lucide:layout-dashboard',
      order: -1,
      title: $t('page.dashboard.title'),
    },
    name: 'Dashboard',
    path: '/dashboard',
    children: [
      {
        name: 'Workspace',
        path: '/workspace',
        redirect: '/welcome',
        meta: {
          hideInMenu: true,
          hideInTab: true,
          icon: 'carbon:workspace',
          title: $t('page.dashboard.workspace'),
        },
      },
      {
        name: 'ApprovalWorkbench',
        path: '/approval-workbench',
        redirect: '/welcome',
        meta: {
          hideInMenu: true,
          hideInTab: true,
          icon: 'lucide:workflow',
          title: $t('page.dashboard.approval-workbench'),
        },
      },
      {
        name: 'MyNotifyMessage',
        path: '/system/notify-message',
        component: () => import('#/views/system/notify/my/index.vue'),
        meta: {
          icon: 'ant-design:message-filled',
          title: '我的站内信',
        },
      },
      {
        name: 'Analytics',
        path: '/analytics',
        redirect: '/welcome',
        meta: {
          hideInMenu: true,
          hideInTab: true,
          icon: 'lucide:area-chart',
          title: $t('page.dashboard.analytics'),
        },
      },
    ],
  },
  {
    name: 'Profile',
    path: '/profile',
    component: () => import('#/views/_core/profile/index.vue'),
    meta: {
      icon: 'ant-design:profile-outlined',
      title: $t('ui.widgets.profile'),
      hideInMenu: true,
    },
  },
];

export default routes;

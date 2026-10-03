import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/dashboard/oa/ecology/message-test',
    name: 'OaEcologyMessageTest',
    alias: '/oa/ecology/message-test',
    component: () => import('#/views/oa/ecology/message-test/index.vue'),
    meta: {
      hideInMenu: true,
      icon: 'lucide:message-square-text',
      keepAlive: false,
      title: '泛微消息测试',
    },
  },
];

export default routes;

import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/mes/quality/statistics/yield-analysis/source-preview',
    alias: '/mes/quality/quality-analysis/yield-analysis/source-preview',
    name: 'MesQmsYieldAnalysisSourcePreview',
    component: () =>
      import('#/views/mes/quality/statistics/yield-analysis/source-preview.vue'),
    meta: {
      activePath: '/mes/quality/statistics/yield-analysis',
      hideInMenu: true,
      icon: 'lucide:file-search',
      keepAlive: false,
      title: '良品率原始记录',
    },
  },
];

export default routes;

<script lang="ts" setup>
import type {
  WorkbenchProjectItem,
  WorkbenchQuickNavItem,
  WorkbenchTodoItem,
  WorkbenchTrendItem,
} from '@vben/common-ui';

import { ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  AnalysisChartCard,
  WorkbenchHeader,
  WorkbenchProject,
  WorkbenchQuickNav,
  WorkbenchTodo,
  WorkbenchTrends,
} from '@vben/common-ui';
import { preferences } from '@vben/preferences';
import { useUserStore } from '@vben/stores';
import { openWindow } from '@vben/utils';

// import AnalyticsVisitsSource from '../analytics/analytics-visits-source.vue';
// 注释掉原有的访问来源图表，实际MES中这里应该放 "缺陷分布帕累托图" 或 "每小时产量监控"

const userStore = useUserStore();
const router = useRouter();

// --- MES 改造：核心生产工序/车间监控 ---
const projectItems: WorkbenchProjectItem[] = [
  {
    color: '#d94738', // 红色系，代表高温/熔炼
    content: '当前炉次: #H20251001 (QT450-10)',
    date: '运行中', // 借用date字段显示状态
    group: '铁水温度: 1480℃', // 借用group字段显示关键参数
    icon: 'mdi:fire', // 熔炼图标
    title: '熔炼车间 (Melting)',
    url: '/mes/melting/monitor',
  },
  {
    color: '#6DB33F', // 绿色，代表正常运行
    content: '在产模具: 缸体-A12',
    date: 'OEE: 92%',
    group: '造型节拍: 45s',
    icon: 'icon-park-outline:components', // 造型/组件
    title: '静压造型线 (Molding)',
    url: '/mes/molding/line1',
  },
  {
    color: '#e18525', // 橙色，代表浇注液体
    content: '待浇注砂箱: 15 箱',
    date: '孕育剂正常',
    group: '浇注温度: 1390℃',
    icon: 'mdi:bucket-outline', // 浇注包
    title: '浇注工段 (Pouring)',
    url: '/mes/pouring/index',
  },
  {
    color: '#1890ff', // 蓝色，理化/质量
    content: '待判定批次: 3 批',
    date: '抽检中',
    group: '光谱/拉力/金相',
    icon: 'carbon:chemistry', // 化学/理化
    title: '理化实验室 (Lab)',
    url: '/mes/qa/lab',
  },
  {
    color: '#7c3aed', // 紫色，工装模具
    content: '需保养模具: 2 套',
    date: '库位正常',
    group: '总库存: 185 套',
    icon: 'mdi:shape-outline', // 模具形状
    title: '模具管理 (Mold)',
    url: '/mes/mold/list',
  },
  {
    color: '#409EFF', // 蓝色，清理/精整
    content: '抛丸机 #2 运行中',
    date: '积压: 低',
    group: '今日产出: 1200 件',
    icon: 'mdi:air-filter', // 清理/抛丸
    title: '清理车间 (Cleaning)',
    url: '/mes/cleaning/dashboard',
  },
];

// --- MES 改造：现场高频操作快捷入口 ---
const quickNavItems: WorkbenchQuickNavItem[] = [
  {
    color: '#1fdaca',
    icon: 'mdi:barcode-scan', // 扫码
    title: '扫码报工',
    url: '/mes/operate/scan',
  },
  {
    color: '#ff6b6b',
    icon: 'mdi:alarm-light-outline', // 报警
    title: '安灯呼叫', // Andon System
    url: '/mes/andon/report',
  },
  {
    color: '#e18525',
    icon: 'mdi:flask-outline', // 试管
    title: '炉前化验',
    url: '/mes/qa/chemistry',
  },
  {
    color: '#3fb27f',
    icon: 'mdi:calendar-clock', // 排程
    title: '生产排程',
    url: '/mes/plan/schedule',
  },
  {
    color: '#2979ff',
    icon: 'mdi:tools', // 维修
    title: '设备点检',
    url: '/mes/device/check',
  },
  {
    color: '#7c3aed',
    icon: 'mdi:file-document-outline',
    title: '流转卡打印',
    url: '/mes/print/card',
  },
];

// --- MES 改造：生产任务与预警 ---
const todoItems = ref<WorkbenchTodoItem[]>([
  {
    completed: false,
    content: `炉次 <span class="text-primary">#H20251001</span> 光谱分析结果硅(Si)含量偏高，请确认是否放行。`,
    date: '10:30',
    title: '质量判定：炉前成分异常',
  },
  {
    completed: false,
    content: `模具 <span class="text-primary">M-Engine-005</span> 已生产 9800 模，接近额定寿命(10000)，请安排保养。`,
    date: '09:15',
    title: '工装预警：模具寿命临界',
  },
  {
    completed: false,
    content: `2号压铸机液压油温超过 55℃，请设备科检查冷却系统。`,
    date: '08:45',
    title: '设备告警：油温过高',
  },
  {
    completed: true,
    content: `昨日夜班生产日报表已生成，待车间主任审核签字。`,
    date: '08:00',
    title: '生产管理：日报审核',
  },
]);

// --- MES 改造：车间实时动态流 ---
const trendItems: WorkbenchTrendItem[] = [
  {
    avatar: 'svg:avatar-1',
    content: `在 <a>熔炼工位</a> 提交了炉前化验单，结果：<a>合格</a>`,
    date: '刚刚',
    title: '张伟 (熔炼工)',
  },
  {
    avatar: 'svg:avatar-2',
    content: `完成了 <a>造型线#1</a> 的首件检验，判定：<a>放行</a>`,
    date: '5分钟前',
    title: '李娜 (质检员)',
  },
  {
    avatar: 'svg:avatar-3',
    content: `上报了 <a>2号抛丸机</a> 皮带断裂故障，已通知维修。`,
    date: '15分钟前',
    title: '王强 (设备员)',
  },
  {
    avatar: 'svg:avatar-4',
    content: `完成了 <a>缸盖模具 #M003</a> 的领用出库。`,
    date: '30分钟前',
    title: '陈仓管',
  },
  {
    avatar: 'svg:avatar-1',
    content: `扫描流转卡，开始 <a>300件 转向节</a> 的浇注作业。`,
    date: '1小时前',
    title: '赵浇注',
  },
];

function navTo(nav: WorkbenchProjectItem | WorkbenchQuickNavItem) {
  if (nav.url?.startsWith('http')) {
    openWindow(nav.url);
    return;
  }
  if (nav.url?.startsWith('/')) {
    router.push(nav.url).catch((error) => {
      console.error('Navigation failed:', error);
    });
  }
}
</script>

<template>
  <div class="p-5">
    <WorkbenchHeader
      :avatar="userStore.userInfo?.avatar || preferences.app.defaultAvatar"
    >
      <template #title>
        早安, {{ userStore.userInfo?.nickname }}, 铸造车间今日运行平稳！
      </template>
      <template #description>
        当前班次：早班 (08:00 - 20:00) | 此时车间温度：32℃
      </template>
    </WorkbenchHeader>

    <div class="mt-5 flex flex-col lg:flex-row">
      <div class="mr-4 w-full lg:w-3/5">
        <!-- 标题改为 关键工序监控 -->
        <WorkbenchProject :items="projectItems" title="关键工序监控" @click="navTo" />
        <!-- 标题改为 现场实时动态 -->
        <WorkbenchTrends :items="trendItems" class="mt-5" title="现场实时动态" />
      </div>
      <div class="w-full lg:w-2/5">
        <WorkbenchQuickNav
          :items="quickNavItems"
          class="mt-5 lg:mt-0"
          title="常用作业入口"
          @click="navTo"
        />
        <WorkbenchTodo :items="todoItems" class="mt-5" title="生产待办 / 预警" />

        <!-- 如果你有缺陷图表组件，可以取消注释 -->
        <!-- <AnalysisChartCard class="mt-5" title="本周废品缺陷分布">
          <AnalyticsVisitsSource />
        </AnalysisChartCard> -->
      </div>
    </div>
  </div>
</template>

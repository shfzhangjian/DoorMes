<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { useAccess } from '@vben/access';

import { getAdhesiveConsoleTaskList as getAdhesive2TaskList } from '#/api/mes/hc/execution/adhesive2-console';
import { getAdhesiveConsoleTaskList as getAdhesive1TaskList } from '#/api/mes/hc/execution/adhesive-console';
import { getCutRoundConsoleTaskList } from '#/api/mes/hc/execution/cut-round-console';
import { getFormulaReportTaskList } from '#/api/mes/hc/execution/formula-report';
import { getPackagingTaskList } from '#/api/mes/hc/execution/packaging-console';
import { getAdhesiveConsoleTaskList as getPressSlotTaskList } from '#/api/mes/hc/execution/press-slot-console';
import { getRoughGrindingReportTaskList } from '#/api/mes/hc/execution/rough-grinding-report';
import { getSlittingConsoleTaskList } from '#/api/mes/hc/execution/slitting-console';
import {
  getActiveWetWaterChangeApply,
  getWetReportTaskList,
} from '#/api/mes/hc/execution/wet-report';
import {
  getFgPackageBoxList,
  getShippingNoticePage,
} from '#/api/mes/hc/package-fg/finished-packaging';
import { getPlanOrderPage } from '#/api/mes/hc/planorder';
import { getProductionInstructionUnreadCount } from '#/api/mes/hc/production-instruction';

import WorkbenchGuidePage from '../components/WorkbenchGuidePage.vue';

defineOptions({ name: 'Workspace' });

type ArtifactType =
  | 'adhesive'
  | 'box'
  | 'cut'
  | 'disc'
  | 'flask'
  | 'groove'
  | 'layers'
  | 'measure'
  | 'paper'
  | 'scan'
  | 'slab'
  | 'strip';

interface ProductionStage {
  artifact: ArtifactType;
  auth: string[];
  detail: string;
  icon: string;
  key: string;
  output: string;
  path: string;
  title: string;
}

interface ProductionDomainItem {
  auth?: string[];
  directory?: boolean;
  icon: string;
  key: string;
  path?: string;
  title: string;
}

interface TaskLike {
  status?: string;
}

const { hasAccessByCodes } = useAccess();
const badgeCounts = reactive<Record<string, number>>({});
const activeDirectory = ref<'edgeConsumable' | 'productionRecord' | null>(null);
const waterChangeDescription = ref('湿法报工');

const productionRecordAuthCodes = [
  'mes:pp:production-record:query',
  'mes:pp:rough-grinding-production-record:query',
  'mes:pp:slitting-press-production-record:query',
  'mes:sfc:wet-production-record:list',
];

const edgeConsumableAuthCodes = [
  'mes:md:package-aux-stock:list',
  'mes:md:tooling-consumable-ledger:query',
  'mes:md:tooling-process-consumable:query',
];

const productionStages: ProductionStage[] = [
  {
    artifact: 'paper',
    auth: ['mes:pp:schedule-workbench:query'],
    detail: '生产计划、BOM、工艺路线与车间工序同步下达',
    icon: 'lucide:calendar-check',
    key: 'plan',
    output: '生产工单',
    path: '/mes/plan/schedule-workbench',
    title: '生产计划',
  },
  {
    artifact: 'flask',
    auth: ['mes:sfc:formula-report:list'],
    detail: '主料、辅料、助剂按配方称量混合',
    icon: 'lucide:flask-conical',
    key: 'formula',
    output: '抛光垫浆料',
    path: '/mes/execution/formula-report',
    title: '配料',
  },
  {
    artifact: 'slab',
    auth: ['mes:sfc:wet-report:list'],
    detail: '发泡、浇注、固化形成连续垫坯',
    icon: 'lucide:waves',
    key: 'wet',
    output: '湿法垫坯',
    path: '/mes/execution/wet-report',
    title: '湿法成型',
  },
  {
    artifact: 'disc',
    auth: ['mes:sfc:rough-grinding-console-prototype:list'],
    detail: '表面处理与厚度修整，形成基础工作面',
    icon: 'lucide:scan-face',
    key: 'roughGrinding',
    output: '磨皮垫片',
    path: '/mes/execution/rough-grinding-console-prototype',
    title: '磨皮',
  },
  {
    artifact: 'layers',
    auth: ['mes:sfc:adhesive-console-prototype:list'],
    detail: '基材与功能层贴合，形成复合半成品',
    icon: 'lucide:layers',
    key: 'adhesive1',
    output: '一粘复合片',
    path: '/mes/execution/adhesive-console-prototype',
    title: '粘胶1',
  },
  {
    artifact: 'strip',
    auth: ['mes:sfc:slitting-console-prototype:list'],
    detail: '按规格分条、分片，为压槽准备尺寸基准',
    icon: 'lucide:split-square-horizontal',
    key: 'slitting',
    output: '分切半成品',
    path: '/mes/execution/slitting-console-prototype',
    title: '分切',
  },
  {
    artifact: 'groove',
    auth: ['mes:sfc:press-slot-report:list'],
    detail: '按沟槽图样加工排液纹路和工作结构',
    icon: 'lucide:circle-dashed',
    key: 'pressSlot',
    output: '沟槽抛光垫',
    path: '/mes/execution/press-slot-report',
    title: '压槽',
  },
  {
    artifact: 'adhesive',
    auth: ['mes:sfc:adhesive2-report:list'],
    detail: '背胶、防护膜与垫片二次复合',
    icon: 'lucide:component',
    key: 'adhesive2',
    output: '背胶复合垫',
    path: '/mes/execution/adhesive2-report',
    title: '粘胶2',
  },
  {
    artifact: 'cut',
    auth: ['mes:sfc:cut-round-report:list'],
    detail: '外形裁切、边缘修整，进入最终检验',
    icon: 'lucide:scissors',
    key: 'cutRound',
    output: '成品胶板',
    path: '/mes/execution/cut-round-report',
    title: '裁切',
  },
  {
    artifact: 'box',
    auth: ['mes:sfc:packaging-report:list'],
    detail: '贴标、内包、装箱，形成可入库包装成品',
    icon: 'lucide:package-check',
    key: 'packaging',
    output: '包装成品',
    path: '/mes/execution/packaging-report',
    title: '包装',
  },
  {
    artifact: 'box',
    auth: ['mes:inv:fg-inbound:list'],
    detail: '已包装成品上架入库，形成库位库存',
    icon: 'lucide:package-plus',
    key: 'fgInbound',
    output: '成品上架',
    path: '/mes/package-fg/fg-inbound',
    title: '成品包装入库',
  },
  {
    artifact: 'scan',
    auth: ['mes:inv:fg-outbound:list'],
    detail: '按发货需求锁定成品片号并完成配货',
    icon: 'lucide:truck',
    key: 'fgOutbound',
    output: '发货配货',
    path: '/mes/finished-shipping/fg-outbound',
    title: '发货配货',
  },
  {
    artifact: 'paper',
    auth: ['mes:inv:fg-shipping-confirm:list'],
    detail: '对已配货需求执行出货确认与流程交接',
    icon: 'lucide:clipboard-check',
    key: 'shippingConfirm',
    output: '出货确认',
    path: '/mes/package-fg/fg-shipping-confirm',
    title: '出货管理',
  },
  {
    artifact: 'box',
    auth: ['mes:inv:fg-shipping-package:list'],
    detail: '发货外包装、标签打印、箱号聚合与包装确认',
    icon: 'lucide:package-check',
    key: 'shippingPackage',
    output: '外包装放行',
    path: '/mes/finished-shipping/fg-shipping-package',
    title: '发货包装',
  },
];

const productionRecordStages: ProductionStage[] = [
  {
    artifact: 'cut',
    auth: ['mes:pp:production-record:query'],
    detail: '裁切工序片级生产记录、尺寸与确认状态维护',
    icon: 'lucide:scissors',
    key: 'recordCutRound',
    output: '裁切记录',
    path: '/mes/production-record/cmp-cut-round-production-record',
    title: '裁切生产记录表',
  },
  {
    artifact: 'disc',
    auth: ['mes:pp:rough-grinding-production-record:query'],
    detail: '磨皮加工记录、耗材联动与生产数据维护',
    icon: 'lucide:scan-face',
    key: 'recordRoughGrinding',
    output: '磨皮记录',
    path: '/mes/production-record/cmp-rough-grinding-production-record',
    title: '磨皮生产记录表',
  },
  {
    artifact: 'groove',
    auth: ['mes:pp:slitting-press-production-record:query'],
    detail: '分切与压槽生产记录集中维护和导出',
    icon: 'lucide:table-2',
    key: 'recordSlittingPress',
    output: '分切压槽记录',
    path: '/mes/production-record/cmp-slitting-press-production-record',
    title: '分切压槽生产记录表',
  },
  {
    artifact: 'slab',
    auth: ['mes:sfc:wet-production-record:list'],
    detail: '湿法涂台生产记录、换水信息与异常记录维护',
    icon: 'lucide:waves',
    key: 'recordWet',
    output: '湿法记录',
    path: '/mes/production-record/wet-production-record',
    title: '湿法生产记录表',
  },
];

const edgeConsumableStages: ProductionStage[] = [
  {
    artifact: 'box',
    auth: ['mes:md:package-aux-stock:list'],
    detail: '包装辅材边库库存、入库和领用管理',
    icon: 'lucide:package-plus',
    key: 'edgePackageAuxStock',
    output: '包装辅材',
    path: '/mes/tooling-consumable-ledger/package-aux-stock',
    title: '库存耗材边库管理',
  },
  {
    artifact: 'disc',
    auth: ['mes:md:tooling-consumable-ledger:query'],
    detail: '磨皮工序耗材领用、用完标记和台账查询',
    icon: 'lucide:scan-face',
    key: 'edgeRoughGrinding',
    output: '磨皮耗材',
    path: '/mes/tooling-consumable-ledger/rough-grinding',
    title: '磨皮耗材领用台账',
  },
  {
    artifact: 'slab',
    auth: ['mes:md:tooling-consumable-ledger:query'],
    detail: '湿法工序耗材领用、库存联动和台账查询',
    icon: 'lucide:waves',
    key: 'edgeWet',
    output: '湿法耗材',
    path: '/mes/tooling-consumable-ledger/wet',
    title: '湿法耗材领用台账',
  },
  {
    artifact: 'layers',
    auth: ['mes:md:tooling-consumable-ledger:query'],
    detail: '粘胶1工序耗材领用和台账维护',
    icon: 'lucide:layers',
    key: 'edgeAdhesive1',
    output: '粘胶1耗材',
    path: '/mes/tooling-consumable-ledger/adhesive1',
    title: '粘胶1耗材领用台账',
  },
  {
    artifact: 'adhesive',
    auth: ['mes:md:tooling-consumable-ledger:query'],
    detail: '粘胶2工序耗材领用和台账维护',
    icon: 'lucide:component',
    key: 'edgeAdhesive2',
    output: '粘胶2耗材',
    path: '/mes/tooling-consumable-ledger/adhesive2',
    title: '粘胶2耗材领用台账',
  },
  {
    artifact: 'groove',
    auth: ['mes:md:tooling-consumable-ledger:query'],
    detail: '压槽工序耗材领用、消耗和台账查询',
    icon: 'lucide:circle-dashed',
    key: 'edgePressSlot',
    output: '压槽耗材',
    path: '/mes/tooling-consumable-ledger/press-slot',
    title: '压槽耗材领用台账',
  },
  {
    artifact: 'cut',
    auth: ['mes:md:tooling-consumable-ledger:query'],
    detail: '裁切工序耗材领用和消耗台账查询',
    icon: 'lucide:scissors',
    key: 'edgeCutRound',
    output: '裁切耗材',
    path: '/mes/tooling-consumable-ledger/cut-round',
    title: '裁切耗材领用台账',
  },
  {
    artifact: 'box',
    auth: ['mes:md:tooling-consumable-ledger:query'],
    detail: '包装工序耗材领用、消耗和台账查询',
    icon: 'lucide:package-check',
    key: 'edgePackaging',
    output: '包装耗材',
    path: '/mes/tooling-consumable-ledger/packaging',
    title: '包装耗材领用台账',
  },
  {
    artifact: 'paper',
    auth: ['mes:md:tooling-process-consumable:query'],
    detail: '维护各工序可领用耗材字典和基础配置',
    icon: 'lucide:list-checks',
    key: 'edgeConfig',
    output: '耗材配置',
    path: '/mes/tooling-consumable-ledger/tooling-process-consumable',
    title: '工序耗材字典',
  },
  {
    artifact: 'scan',
    auth: ['mes:md:tooling-consumable-ledger:query'],
    detail: '查看边库耗材余额、库存占用和剩余数量',
    icon: 'lucide:boxes',
    key: 'edgeBalance',
    output: '余额查询',
    path: '/mes/tooling-consumable-ledger/tooling-consumable-balance',
    title: '边库耗材余额查询',
  },
  {
    artifact: 'paper',
    auth: ['mes:md:tooling-consumable-ledger:query'],
    detail: '按工序、批次和耗材追溯消耗记录',
    icon: 'lucide:clipboard-list',
    key: 'edgeConsumeRecord',
    output: '消耗记录',
    path: '/mes/tooling-consumable-ledger/tooling-consumable-consume-record',
    title: '耗材消耗记录查询',
  },
];

const visibleProductionStages = computed(() => {
  if (activeDirectory.value === 'productionRecord') {
    return productionRecordStages;
  }

  if (activeDirectory.value === 'edgeConsumable') {
    return edgeConsumableStages;
  }

  return productionStages.map((stage) => ({
    ...stage,
    badge: badgeCounts[stage.key] || 0,
  }));
});

const domainItems = computed<ProductionDomainItem[]>(() => {
  const items: ProductionDomainItem[] = [];

  if (activeDirectory.value) {
    items.push({
      directory: true,
      icon: 'lucide:arrow-left',
      key: 'main',
      title: '返回主工作台',
    });
  }

  if (activeDirectory.value !== 'productionRecord') {
    items.push({
      auth: productionRecordAuthCodes,
      directory: true,
      icon: 'lucide:clipboard-list',
      key: 'productionRecord',
      title: '生产记录表',
    });
  }

  if (activeDirectory.value !== 'edgeConsumable') {
    items.push({
      auth: edgeConsumableAuthCodes,
      directory: true,
      icon: 'lucide:boxes',
      key: 'edgeConsumable',
      title: '边库物料',
    });
  }

  if (!activeDirectory.value) {
    items.push({
      auth: ['mes:plan:batch-trace:list'],
      icon: 'lucide:git-branch',
      key: 'batchTrace',
      path: '/mes/plan/batch-trace',
      title: '批次追溯',
    });
  }

  return items;
});

const workbenchTitle = computed(() => {
  if (activeDirectory.value === 'productionRecord') {
    return '生产记录表';
  }

  if (activeDirectory.value === 'edgeConsumable') {
    return '边库耗材';
  }

  return '生产工作台';
});

const workbenchIntro = computed(() => {
  if (activeDirectory.value === 'productionRecord') {
    return '按二级目录展开各工序生产记录表，进入具体记录维护前先确认当前账号菜单权限。';
  }

  if (activeDirectory.value === 'edgeConsumable') {
    return '按二级目录展开边库耗材各子菜单，覆盖工序领用、余额、消耗记录和基础配置。';
  }

  return '围绕生产计划、工序执行、边库物料、设备状态与批次追溯，承接半导体抛光垫现场作业主线。';
});

const workbenchIcon = computed(() => {
  if (activeDirectory.value === 'productionRecord') {
    return 'lucide:clipboard-list';
  }

  if (activeDirectory.value === 'edgeConsumable') {
    return 'lucide:boxes';
  }

  return 'lucide:factory';
});

const reminders = computed(() => [
  {
    auth: ['mes:pp:production-instruction:list'],
    count: badgeCounts.productionInstruction || 0,
    description: '未读指令',
    icon: 'lucide:megaphone',
    key: 'productionInstruction',
    path: '/mes/plan/production-instruction',
    title: '生产指令提醒',
    tone: 'warning' as const,
  },
  {
    auth: ['mes:sfc:wet-report:list'],
    count: badgeCounts.waterChange || 0,
    description: waterChangeDescription.value,
    icon: 'lucide:droplets',
    key: 'waterChange',
    path: '/mes/execution/wet-report',
    title: '换水提醒',
    tone: 'success' as const,
  },
]);

function canAccess(auth: string[]) {
  return auth.length === 0 || hasAccessByCodes(auth);
}

function handleDomainClick(item: ProductionDomainItem) {
  if (item.key === 'main') {
    activeDirectory.value = null;
    return;
  }

  if (item.key === 'productionRecord') {
    activeDirectory.value = 'productionRecord';
    return;
  }

  if (item.key === 'edgeConsumable') {
    activeDirectory.value = 'edgeConsumable';
  }
}

function countOpenTasks(rows: TaskLike[] = []) {
  const closedStatuses = new Set([
    'CANCELED',
    'CANCELLED',
    'CLOSED',
    'COMPLETED',
    'DONE',
    'FINISHED',
    'OUTBOUND',
    'SHIPPED',
  ]);

  return rows.filter((row) => {
    const status = String(row.status || '').toUpperCase();
    return !status || !closedStatuses.has(status);
  }).length;
}

async function readOpenTaskCount(loader: (params: any) => Promise<TaskLike[]>) {
  const rows = await loader({ taskStatus: 'ALL' });
  return countOpenTasks(rows);
}

async function readPageTotal(
  loader: (params: any) => Promise<{ list?: unknown[]; total?: number }>,
  params: Record<string, any>,
) {
  const page = await loader({ pageNo: 1, pageSize: 1, ...params });
  return Number(page?.total ?? page?.list?.length ?? 0);
}

async function setBadge(
  key: string,
  auth: string[],
  loader: () => Promise<number>,
) {
  if (!canAccess(auth)) {
    badgeCounts[key] = 0;
    return;
  }

  try {
    badgeCounts[key] = await loader();
  } catch {
    badgeCounts[key] = 0;
  }
}

async function loadWorkbenchBadges() {
  await Promise.all([
    setBadge('plan', ['mes:pp:schedule-workbench:query'], () =>
      readPageTotal(getPlanOrderPage, { planStatuses: ['RELEASED', 'PAUSED'] }),
    ),
    setBadge('formula', ['mes:sfc:formula-report:list'], () =>
      readOpenTaskCount(getFormulaReportTaskList),
    ),
    setBadge('wet', ['mes:sfc:wet-report:list'], () =>
      readOpenTaskCount(getWetReportTaskList),
    ),
    setBadge(
      'roughGrinding',
      ['mes:sfc:rough-grinding-console-prototype:list'],
      () => readOpenTaskCount(getRoughGrindingReportTaskList),
    ),
    setBadge('adhesive1', ['mes:sfc:adhesive-console-prototype:list'], () =>
      readOpenTaskCount(getAdhesive1TaskList),
    ),
    setBadge('slitting', ['mes:sfc:slitting-console-prototype:list'], () =>
      readOpenTaskCount(getSlittingConsoleTaskList),
    ),
    setBadge('pressSlot', ['mes:sfc:press-slot-report:list'], () =>
      readOpenTaskCount(getPressSlotTaskList),
    ),
    setBadge('adhesive2', ['mes:sfc:adhesive2-report:list'], () =>
      readOpenTaskCount(getAdhesive2TaskList),
    ),
    setBadge('cutRound', ['mes:sfc:cut-round-report:list'], () =>
      readOpenTaskCount(getCutRoundConsoleTaskList),
    ),
    setBadge('packaging', ['mes:sfc:packaging-report:list'], () =>
      readOpenTaskCount(getPackagingTaskList),
    ),
    setBadge('fgInbound', ['mes:inv:fg-inbound:list'], async () => {
      const rows = await getFgPackageBoxList({ boxType: 'INBOUND' });
      return rows.filter(
        (row) =>
          ['INBOUND_LOCKED', 'PACKED'].includes(String(row.status || '')) &&
          !(row as Record<string, any>).locationCode,
      ).length;
    }),
    setBadge('fgOutbound', ['mes:inv:fg-outbound:list'], () =>
      readPageTotal(getShippingNoticePage, {
        noticeStatus: 'PENDING_OUTBOUND',
      }),
    ),
    setBadge('shippingConfirm', ['mes:inv:fg-shipping-confirm:list'], () =>
      readPageTotal(getShippingNoticePage, { noticeStatus: 'PICKED' }),
    ),
    setBadge('shippingPackage', ['mes:inv:fg-shipping-package:list'], () =>
      readPageTotal(getShippingNoticePage, { noticeStatus: 'INSPECTED' }),
    ),
    setBadge(
      'productionInstruction',
      ['mes:pp:production-instruction:list'],
      () =>
        getProductionInstructionUnreadCount({
          pageNo: 1,
          pageSize: 1,
          readStatus: 'UNREAD',
        }),
    ),
    setBadge('waterChange', ['mes:sfc:wet-report:list'], async () => {
      const activeApply = await getActiveWetWaterChangeApply();
      waterChangeDescription.value = activeApply?.areaDesc || '湿法报工';
      return activeApply ? 1 : 0;
    }),
  ]);
}

onMounted(() => {
  void loadWorkbenchBadges();
});
</script>

<template>
  <WorkbenchGuidePage
    accent="production"
    :domain-items="domainItems"
    :icon="workbenchIcon"
    :intro="workbenchIntro"
    :reminders="reminders"
    :stages="visibleProductionStages"
    :title="workbenchTitle"
    @domain-click="handleDomainClick"
  />
</template>

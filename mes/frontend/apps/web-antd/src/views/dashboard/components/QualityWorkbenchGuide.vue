<script lang="ts" setup>
import { computed, onMounted, reactive } from 'vue';

import { useAccess } from '@vben/access';

import { getCutRoundFqcPage } from '#/api/mes/quality/cut-round-fqc';
import { getFaiPage, getGlueBoardFaiPage } from '#/api/mes/quality/fai';
import { getFgShippingFqcPage } from '#/api/mes/quality/fg-shipping-fqc';
import { getIqcPage } from '#/api/mes/quality/iqc';
import { getWetPoreSelfCheckPage } from '#/api/mes/quality/wet-pore-self-check';

import WorkbenchGuidePage from './WorkbenchGuidePage.vue';

defineOptions({ name: 'QualityWorkbenchGuide' });

type ArtifactType =
  | 'adhesive'
  | 'box'
  | 'cut'
  | 'disc'
  | 'groove'
  | 'layers'
  | 'paper'
  | 'scan'
  | 'slab';

interface QualityStage {
  artifact: ArtifactType;
  auth: string[];
  detail: string;
  icon: string;
  key: string;
  output: string;
  path: string;
  title: string;
}

const { hasAccessByCodes } = useAccess();
const badgeCounts = reactive<Record<string, number>>({});

const pendingStatus = 'PENDING';
const inProcessQualityRouteBase = '/mes/quality/in-process';

const qualityStages: QualityStage[] = [
  {
    key: 'iqcInspection',
    title: '进料送检单',
    output: 'IQC送检',
    detail: '来料送检、标准确认与检验结果待上报',
    icon: 'lucide:clipboard-plus',
    artifact: 'paper',
    auth: ['mes:iqc-inspection:list'],
    path: '/mes/quality/iqc/inspection',
  },
  {
    key: 'faiWet',
    title: '湿法首件检验',
    output: '过程FAI',
    detail: '湿法首件样品、外观和关键指标检测上报',
    icon: 'lucide:waves',
    artifact: 'slab',
    auth: ['mes:fai:list'],
    path: `${inProcessQualityRouteBase}/fai?processCategory=WET`,
  },
  {
    key: 'faiGrinding',
    title: '磨皮首件检验',
    output: '过程FAI',
    detail: '磨皮厚度、表面和首件质量结果上报',
    icon: 'lucide:scan-face',
    artifact: 'disc',
    auth: ['mes:fai:list'],
    path: `${inProcessQualityRouteBase}/fai?processCategory=GRINDING`,
  },
  {
    key: 'faiGlue1',
    title: '粘胶1首件检验',
    output: '过程FAI',
    detail: '一粘复合片首件贴合与外观质量上报',
    icon: 'lucide:layers',
    artifact: 'layers',
    auth: ['mes:fai:list'],
    path: `${inProcessQualityRouteBase}/fai?processCategory=GLUE_1`,
  },
  {
    key: 'faiGrooving',
    title: '压槽首件检验',
    output: '过程FAI',
    detail: '压槽纹路、尺寸和首件沟槽质量上报',
    icon: 'lucide:circle-dashed',
    artifact: 'groove',
    auth: ['mes:fai:list'],
    path: `${inProcessQualityRouteBase}/fai?processCategory=GROOVING`,
  },
  {
    key: 'faiGlue2',
    title: '粘胶2首件检验',
    output: '过程FAI',
    detail: '背胶复合首件与贴合质量结果上报',
    icon: 'lucide:component',
    artifact: 'adhesive',
    auth: ['mes:fai:list'],
    path: `${inProcessQualityRouteBase}/fai?processCategory=GLUE_2`,
  },
  {
    key: 'glueBoard',
    title: '胶板检验',
    output: '胶板待检',
    detail: '胶板型号、批次与检验标准待录入/复核',
    icon: 'lucide:layers-3',
    artifact: 'layers',
    auth: ['mes:glue-board-fai:list'],
    path: `${inProcessQualityRouteBase}/glue-board-fai`,
  },
  {
    key: 'cutRoundFqc',
    title: '裁切成品检验',
    output: '片级FQC',
    detail: '裁切成品片号、外观与检测结果待上报',
    icon: 'lucide:scissors',
    artifact: 'cut',
    auth: ['mes:cut-round-fqc:list'],
    path: `${inProcessQualityRouteBase}/cut-round-fqc`,
  },
  {
    key: 'fgShippingFqc',
    title: '发货成品检验',
    output: '发货FQC',
    detail: '发货成品与配货清单对齐、检验放行上报',
    icon: 'lucide:shield-check',
    artifact: 'box',
    auth: ['mes:fg-shipping-fqc:list'],
    path: `${inProcessQualityRouteBase}/fg-shipping-fqc`,
  },
  {
    key: 'poreSelfCheck',
    title: '泡孔自检',
    output: '湿法自检',
    detail: '湿法泡孔图片上传、自检结果与异常记录上报',
    icon: 'lucide:image-up',
    artifact: 'scan',
    auth: ['mes:pore-self-check:list'],
    path: `${inProcessQualityRouteBase}/pore-self-check`,
  },
];

const visibleQualityStages = computed(() =>
  qualityStages.map((stage) => {
    const badge = badgeCounts[stage.key] || 0;

    return {
      ...stage,
      badge,
      highlight: stage.key === 'glueBoard' && badge > 0,
    };
  }),
);

function canAccess(auth: string[]) {
  return auth.length === 0 || hasAccessByCodes(auth);
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

async function loadQualityBadges() {
  await Promise.all([
    setBadge('iqcInspection', ['mes:iqc-inspection:list'], () =>
      readPageTotal(getIqcPage, { status: pendingStatus }),
    ),
    setBadge('faiWet', ['mes:fai:list'], () =>
      readPageTotal(getFaiPage, {
        processCategory: 'WET',
        status: pendingStatus,
      }),
    ),
    setBadge('faiGrinding', ['mes:fai:list'], () =>
      readPageTotal(getFaiPage, {
        processCategory: 'GRINDING',
        status: pendingStatus,
      }),
    ),
    setBadge('faiGlue1', ['mes:fai:list'], () =>
      readPageTotal(getFaiPage, {
        processCategory: 'GLUE_1',
        status: pendingStatus,
      }),
    ),
    setBadge('faiGrooving', ['mes:fai:list'], () =>
      readPageTotal(getFaiPage, {
        processCategory: 'GROOVING',
        status: pendingStatus,
      }),
    ),
    setBadge('faiGlue2', ['mes:fai:list'], () =>
      readPageTotal(getFaiPage, {
        processCategory: 'GLUE_2',
        status: pendingStatus,
      }),
    ),
    setBadge('glueBoard', ['mes:glue-board-fai:list'], () =>
      readPageTotal(getGlueBoardFaiPage, { status: pendingStatus }),
    ),
    setBadge('cutRoundFqc', ['mes:cut-round-fqc:list'], () =>
      readPageTotal(getCutRoundFqcPage, { status: pendingStatus }),
    ),
    setBadge('fgShippingFqc', ['mes:fg-shipping-fqc:list'], () =>
      readPageTotal(getFgShippingFqcPage, { status: pendingStatus }),
    ),
    setBadge('poreSelfCheck', ['mes:pore-self-check:list'], () =>
      readPageTotal(getWetPoreSelfCheckPage, { imageStatus: 'MISSING' }),
    ),
  ]);
}

onMounted(() => {
  void loadQualityBadges();
});
</script>

<template>
  <WorkbenchGuidePage
    accent="quality"
    :domain-items="[]"
    :stages="visibleQualityStages"
    icon="lucide:shield-check"
    intro="围绕来料送检、过程首件、胶板、裁切成品、发货成品与泡孔自检，按权限聚合待上报质量任务。"
    title="质量工作台"
  />
</template>

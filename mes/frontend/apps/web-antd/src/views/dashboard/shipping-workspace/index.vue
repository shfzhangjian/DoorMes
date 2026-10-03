<script lang="ts" setup>
import WorkbenchGuidePage from '../components/WorkbenchGuidePage.vue';

defineOptions({ name: 'DashboardShippingWorkspace' });

const stages: InstanceType<typeof WorkbenchGuidePage>['$props']['stages'] = [
  {
    key: 'shippingPick',
    title: '发货配货',
    detail: '按发货需求选择成品片号，完成配货与核对',
    output: '配货清单',
    icon: 'lucide:truck',
    artifact: 'scan',
    auth: ['mes:inv:fg-outbound:list'],
    path: '/mes/finished-shipping/fg-outbound',
  },
  {
    key: 'shippingPackage',
    title: '发货包装',
    detail: '发货外包装、标签打印、箱号聚合与包装确认',
    output: '发货包装',
    icon: 'lucide:package-check',
    artifact: 'box',
    auth: ['mes:inv:fg-shipping-package:list'],
    path: '/mes/finished-shipping/fg-shipping-package',
  },
  {
    key: 'shippingFqc',
    title: '发货成品检验',
    detail: '核对配货成品并填写、上报发货成品检验记录',
    output: '发货 FQC',
    icon: 'lucide:shield-check',
    artifact: 'measure',
    auth: ['mes:fg-shipping-fqc:list'],
    path: '/mes/quality/in-process/fg-shipping-fqc',
  },
  {
    key: 'shippingAlignment',
    title: '发货客户批号对齐',
    detail: '核对实际发货片号与客户要求批号的对应关系',
    output: '客户批号对齐',
    icon: 'lucide:git-compare-arrows',
    artifact: 'scan',
    auth: ['mes:fg-shipping-alignment:list'],
    path: '/mes/quality/in-process/fg-shipping-alignment',
  },
  {
    key: 'shippingOqc',
    title: '出货检验单（OQC）',
    detail: '查看出货检验单，填写检验记录并按现有流程审核',
    output: '出货检验单',
    icon: 'lucide:clipboard-check',
    artifact: 'paper',
    auth: ['mes:oqc:list'],
    path: '/mes/quality/oqc/list',
  },
];
</script>

<template>
  <WorkbenchGuidePage
    :domain-items="[]"
    :stages="stages"
    icon="lucide:truck"
    intro="集中进入发货配货、包装、成品检验、客户批号对齐与出货检验，按业务需要选择入口。"
    title="发货工作台"
  />
</template>

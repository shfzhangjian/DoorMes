<script lang="ts" setup>
import GenericWorkstationTaskDetailModal from '../shared/GenericWorkstationTaskDetailModal.vue';
import WorkstationProcessReportPage from '../shared/WorkstationProcessReportPage.vue';

defineOptions({ name: 'MesExecutionSlittingReport' });

const scene = {
  title: '分切报工',
  processLabel: '分切',
  detailComponent: GenericWorkstationTaskDetailModal,
  workstationName: '分切工位一线',
  deviceRole: '分切主机',
  deviceId: 'SLIT-01',
  deviceName: '分切机',
};
</script>

<template>
  <WorkstationProcessReportPage :scene="scene" />
</template>

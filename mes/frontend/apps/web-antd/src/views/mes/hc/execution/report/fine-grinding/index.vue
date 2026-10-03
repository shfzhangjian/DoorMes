<script lang="ts" setup>
import GenericWorkstationTaskDetailModal from '../shared/GenericWorkstationTaskDetailModal.vue';
import WorkstationProcessReportPage from '../shared/WorkstationProcessReportPage.vue';

defineOptions({ name: 'MesExecutionFineGrindingReport' });

const scene = {
  title: '精磨报工',
  processLabel: '精磨',
  detailComponent: GenericWorkstationTaskDetailModal,
  workstationName: '精磨工位一线',
  deviceRole: '精磨主机',
  deviceId: 'FINE-01',
  deviceName: '精磨机',
};
</script>

<template>
  <WorkstationProcessReportPage :scene="scene" />
</template>

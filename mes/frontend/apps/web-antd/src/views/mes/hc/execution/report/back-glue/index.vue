<script lang="ts" setup>
import GenericWorkstationTaskDetailModal from '../shared/GenericWorkstationTaskDetailModal.vue';
import WorkstationProcessReportPage from '../shared/WorkstationProcessReportPage.vue';

defineOptions({ name: 'MesExecutionBackGlueReport' });

const scene = {
  title: '单片背胶报工',
  processLabel: '单片背胶',
  detailComponent: GenericWorkstationTaskDetailModal,
  workstationName: '单片背胶工位一线',
  deviceRole: '背胶主线',
  deviceId: 'GLUE-01',
  deviceName: 'CMP软垫粘胶线',
};
</script>

<template>
  <WorkstationProcessReportPage :scene="scene" />
</template>

<script lang="ts" setup>
import OriginalTaskDetailModal from '#/views/mes/work-order-booking/modules/task-detail-modal.vue';

const emit = defineEmits(['refresh']);
</script>

<template>
  <OriginalTaskDetailModal @refresh="emit('refresh')" />
</template>

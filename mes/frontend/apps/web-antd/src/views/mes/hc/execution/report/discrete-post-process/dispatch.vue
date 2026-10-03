<script lang="ts" setup>
import DiscretePostProcessConsole from './DiscretePostProcessConsole.vue';

defineOptions({ name: 'MesExecutionDiscretePostProcessDispatch' });

const allowedOperations = [
  { label: '压槽', sourceOpName: '分切', value: 'WC-GROOVE' },
  { label: '粘胶2', sourceOpName: '压槽', value: 'WC-ADH2' },
  { label: '裁切', sourceOpName: '粘胶2', value: 'WC-CUT' },
] as const;
</script>

<template>
  <DiscretePostProcessConsole
    :allowed-operations="allowedOperations"
    dispatch-only
    op-code="WC-GROOVE"
    op-name="后加工"
    source-op-name="分切"
  />
</template>

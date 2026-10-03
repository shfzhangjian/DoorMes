<script lang="ts" setup>
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Table, InputNumber, Switch, Tag, message } from 'ant-design-vue';
import { deviceCheckColumns, mockCheckData } from '../data';

const checkList = ref<any[]>([]);

const [Modal, modalApi] = useVbenModal({
  title: '🛠️ 开机点检与工艺参数采集',
  width: 800,
  onConfirm() {
    const isAllFilled = checkList.value.every(item => item.type === 'check' ? item.actual !== null : item.actual !== null && item.actual !== '');
    if (!isAllFilled) return message.warning('请完成所有点检项！');
    message.success('参数采集与点检提交成功！');
    modalApi.close();
  },
  onOpenChange(isOpen) {
    if (isOpen) checkList.value = JSON.parse(JSON.stringify(mockCheckData));
  }
});

const validateInput = (record: any) => {
  if (record.actual === null || record.actual === '') record.result = null;
  else if (record.actual >= record.min && record.actual <= record.max) record.result = 'OK';
  else record.result = 'NG';
};
</script>

<template>
  <Modal>
    <div class="p-4">
      <div class="mb-4 text-amber-600 bg-amber-50 p-2 rounded border border-amber-200 text-sm">
        ⚠️ 提示：开工前必须完成首件点检。如果参数 NG，将不允许开工。
      </div>
      <Table :columns="deviceCheckColumns" :dataSource="checkList" :pagination="false" bordered size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actual'">
            <Switch v-if="record.type === 'check'" v-model:checked="record.actual" checked-children="合格" un-checked-children="不合格" @change="record.result = record.actual ? 'OK' : 'NG'" />
            <InputNumber v-else v-model:value="record.actual" class="w-full" placeholder="输入实测值" @blur="validateInput(record)" />
          </template>
          <template v-if="column.key === 'result'">
            <Tag v-if="record.result === 'OK'" color="success">OK</Tag>
            <Tag v-else-if="record.result === 'NG'" color="error">NG</Tag>
            <span v-else class="text-slate-300">-</span>
          </template>
        </template>
      </Table>
    </div>
  </Modal>
</template>

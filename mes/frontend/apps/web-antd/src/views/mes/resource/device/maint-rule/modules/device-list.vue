<script lang="ts" setup>
import { ref, h, watch, nextTick } from 'vue';
import { Button, DatePicker } from 'ant-design-vue';
import { Plus } from "@vben/icons";
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import dayjs from 'dayjs';

import { useDeviceGridColumns } from '../data';
import { getRuleDeviceListById } from '#/api/mes/resource/device/maint-rule';
import DeviceSelectModal from '../../fault-repair/modules/device-select-modal.vue'; // 复用之前的台账提取器

defineOptions({ name: 'MaintRuleDeviceListGrid' });

const props = defineProps<{ ruleId?: string | number; disabled?: boolean }>();
const deviceSelectModalRef = ref<InstanceType<typeof DeviceSelectModal>>();

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useDeviceGridColumns(),
    data: [], border: true, showOverflow: true, keepSource: true,
    height: 300, rowConfig: { keyField: 'id', isCurrent: true, isHover: true },
    pagerConfig: { enabled: false }, toolbarConfig: { enabled: false },
  },
});

function handleAdd() {
  if (!props.disabled) deviceSelectModalRef.value?.open();
}

async function handleDeviceSelected(device: any) {
  if (!gridApi.grid) return;
  // 检查是否重复添加
  const exists = gridApi.grid.getData().some((r: any) => r.deviceCode === device.deviceCode);
  if (exists) return;

  const { row } = await gridApi.grid.insertAt({
    id: Date.now().toString(),
    sort: gridApi.grid.getData().length + 1,
    deviceCode: device.deviceCode,
    deviceName: device.deviceName,
    deviceType: device.deviceType,
    lastMaintDate: dayjs().format('YYYY-MM-DD') // 默认填入今天
  }, -1);

  await gridApi.grid.scrollToRow(row);
  await gridApi.grid.setCurrentRow(row);
}

const handleDelete = async (row: any) => { if (!props.disabled && gridApi.grid) await gridApi.grid.remove(row); };

defineExpose({
  getData: () => {
    if (!gridApi.grid) return [];
    const data = gridApi.grid.getData();
    const removes = gridApi.grid.getRemoveRecords();
    const inserts = gridApi.grid.getInsertRecords();
    return data.filter((row: any) => !removes.some((r: any) => r.id === row.id)).concat(inserts.map((row: any) => ({ ...row, id: undefined })));
  },
});

watch(() => props.ruleId, async (val) => {
  if (!val) { gridApi.setGridOptions({ data: [] }); return; }
  await nextTick();
  gridApi.setGridOptions({ data: await getRuleDeviceListById(val) });
}, { immediate: true });
</script>

<template>
  <div class="flex flex-col h-full bg-slate-50">
    <div class="flex items-center justify-between px-4 py-2 border-b border-indigo-100 bg-white">
      <div class="flex items-center gap-2">
        <IconifyIcon icon="lucide:monitor-smartphone" class="text-indigo-500" />
        <span class="font-bold text-slate-600 text-sm">适用设备清单与上次维保时间</span>
      </div>
      <Button v-if="!disabled" size="small" type="primary" ghost @click="handleAdd" :icon="h(Plus)">挂载设备</Button>
    </div>

    <div class="p-2 bg-white">
      <BaseGrid>
        <template #sort="{ row }"><span class="text-slate-500">{{ row.sort }}</span></template>

        <template #lastDate="{ row }">
          <DatePicker v-if="!disabled" v-model:value="row.lastMaintDate" value-format="YYYY-MM-DD" class="w-full" size="small" placeholder="设定基准时间" />
          <span v-else class="font-bold text-indigo-600">{{ row.lastMaintDate || '-' }}</span>
        </template>

        <template #actions="{ row }"><Button v-if="!disabled" size="small" type="link" danger @click="handleDelete(row)">移除</Button><span v-else>-</span></template>
      </BaseGrid>
    </div>

    <DeviceSelectModal ref="deviceSelectModalRef" @select="handleDeviceSelected" />
  </div>
</template>

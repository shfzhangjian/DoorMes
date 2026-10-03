<script lang="ts" setup>
import { ref, watch } from 'vue';
import { Modal, Table, Tag, Button, Spin } from 'ant-design-vue';
import { fetchCheckTaskList } from './device-check-data';

const props = defineProps<{
  visible: boolean;
  process: string;
  activeTaskId?: string;
}>();

const emit = defineEmits(['update:visible', 'select']);

const isLoading = ref(false);
const taskList = ref<any[]>([]);

// 每次打开弹窗，利用独立 API 获取最新列表状态
watch(() => props.visible, async (val) => {
  if (val && props.process) {
    isLoading.value = true;
    taskList.value = await fetchCheckTaskList(props.process);
    isLoading.value = false;
  }
});

const handleSelect = (record: any) => {
  emit('select', record);
  emit('update:visible', false);
};
</script>

<template>
  <Modal :open="visible" @update:open="val => emit('update:visible', val)" title="📋 切换设备点检与确认任务" :footer="null" :width="850" centered :zIndex="100005" destroyOnClose>
    <Spin :spinning="isLoading">
      <div class="py-2">
        <Table :dataSource="taskList" :pagination="false" size="middle" bordered :rowKey="'id'">
          <Table.Column title="点检任务名称" dataIndex="name">
            <template #default="{ text, record }">
              <span class="font-bold" :class="activeTaskId === record.id ? 'text-indigo-600' : 'text-slate-700'">
                {{ text }} <Tag v-if="activeTaskId === record.id" color="blue" class="ml-2 border-none">当前执行</Tag>
              </span>
            </template>
          </Table.Column>

          <Table.Column title="任务状态" dataIndex="status" align="center" width="100">
            <template #default="{ text }">
              <Tag v-if="text === 'PENDING_CHECK'" color="warning" class="font-bold border-none m-0">未填写</Tag>
              <Tag v-else-if="text === 'PENDING_CONFIRM'" color="processing" class="font-bold border-none m-0">未确认</Tag>
              <Tag v-else color="success" class="font-bold border-none m-0">已确认</Tag>
            </template>
          </Table.Column>

          <Table.Column title="记录/点检人" dataIndex="checker">
            <template #default="{ text }"><span class="text-slate-500">{{ text || '-' }}</span></template>
          </Table.Column>

          <Table.Column title="复核确认人" dataIndex="confirmer">
            <template #default="{ text }"><span class="text-slate-500">{{ text || '-' }}</span></template>
          </Table.Column>

          <Table.Column title="现场操作" align="center" width="120">
            <template #default="{ record }">
              <Button size="small" type="primary" class="font-bold shadow-sm w-full"
                      :class="activeTaskId === record.id ? 'bg-slate-300 border-slate-300 text-slate-500' : (record.status === 'COMPLETED' ? 'bg-slate-500 border-slate-500' : 'bg-indigo-600')"
                      @click="handleSelect(record)" :disabled="activeTaskId === record.id">
                {{ activeTaskId === record.id ? '执行中' : (record.status === 'COMPLETED' ? '查看记录' : '立即切换') }}
              </Button>
            </template>
          </Table.Column>
        </Table>
      </div>
    </Spin>
  </Modal>
</template>

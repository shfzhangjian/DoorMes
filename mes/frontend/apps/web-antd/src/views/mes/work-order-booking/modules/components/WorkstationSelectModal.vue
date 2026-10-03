<script lang="ts" setup>
import { ref, watch } from 'vue';
import { Modal, Table, Tag, Button, Spin, Input } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { fetchWorkstationList, type Workstation } from './workstation-api';

const props = defineProps<{ visible: boolean; currentDevice: string }>();
const emit = defineEmits(['update:visible', 'select']);

const isLoading = ref(false);
const deviceList = ref<Workstation[]>([]);
const activeProcess = ref('全部');
const searchKeyword = ref('');

// 🌟 补齐包装工序
const processList = ['全部', '配料', '温法', '粗磨', '精磨', '粘双面胶', '分切', '单片压槽', '单片背胶', '裁圆', '包装'];

const loadData = async () => {
  isLoading.value = true;
  let list = await fetchWorkstationList(activeProcess.value);
  if (searchKeyword.value) {
    // 搜索同时匹配工位名称和内部机台名称
    list = list.filter(d =>
      d.name.includes(searchKeyword.value) ||
      d.id.includes(searchKeyword.value) ||
      d.devices.some(dev => dev.name.includes(searchKeyword.value) || dev.id.includes(searchKeyword.value))
    );
  }
  deviceList.value = list;
  isLoading.value = false;
};

watch(() => props.visible, (val) => { if (val) loadData(); });
watch(activeProcess, () => loadData());

const columns = [
  { title: '所属工序', dataIndex: 'process', width: 90 },
  { title: '工位编号', dataIndex: 'id', width: 130 },
  { title: '工位名称', dataIndex: 'name', width: 180 },
  { title: '工装设备清单', key: 'devices' },
  { title: '运行状态', dataIndex: 'status', width: 100, align: 'center' },
  { title: '现场操作', key: 'action', width: 110, align: 'center' }
];

const handleSelect = (record: Workstation) => {
  emit('select', record);
  emit('update:visible', false);
};
</script>

<template>
  <Modal :open="visible" @update:open="val => emit('update:visible', val)" title="🖥️ 全车间工位与机台切换中心" :footer="null" :width="1100" centered destroyOnClose>
    <div class="flex h-[550px] -mx-6 -mb-6 border-t border-slate-200 mt-4 bg-slate-50">

      <div class="w-40 bg-white border-r border-slate-200 flex flex-col h-full overflow-y-auto py-2">
        <div v-for="proc in processList" :key="proc"
             class="px-4 py-3 cursor-pointer border-l-4 transition-all font-bold text-sm flex items-center justify-between"
             :class="activeProcess === proc ? 'border-indigo-600 bg-indigo-50/50 text-indigo-700' : 'border-transparent text-slate-600 hover:bg-slate-100'"
             @click="activeProcess = proc">
          {{ proc }}
          <IconifyIcon v-if="activeProcess === proc" icon="lucide:chevron-right" class="text-indigo-400" />
        </div>
      </div>

      <div class="flex-1 flex flex-col p-4 min-w-0">
        <div class="flex items-center justify-between mb-3 shrink-0">
          <span class="font-black text-slate-700">机台选择列表 <Tag color="blue" class="ml-2 border-none">{{ activeProcess }}</Tag></span>
          <Input.Search v-model:value="searchKeyword" placeholder="搜索工位或具体机台设备" class="w-72" @search="loadData" allowClear />
        </div>

        <div class="flex-1 bg-white border border-slate-200 shadow-sm rounded-lg overflow-y-auto custom-scrollbar">
          <Spin :spinning="isLoading">
            <Table :columns="columns" :dataSource="deviceList" :pagination="false" size="small" :rowKey="'id'">

              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'id'">
                  <span class="font-mono font-bold text-slate-500">{{ record.id }}</span>
                </template>

                <template v-if="column.key === 'name'">
                  <span class="font-bold text-slate-800">{{ record.name }}</span>
                </template>

                <template v-if="column.key === 'devices'">
                  <div class="flex flex-col gap-1.5 py-1">
                    <div v-for="dev in record.devices" :key="dev.id" class="flex items-center text-[13px] bg-slate-50 px-2 py-1 rounded border border-slate-100">
                      <span class="text-indigo-500 font-bold min-w-[70px]">【{{ dev.role }}】</span>
                      <span class="font-mono text-slate-400 mx-2">{{ dev.id }}</span>
                      <span class="text-slate-700 font-bold truncate">{{ dev.name }}</span>
                    </div>
                  </div>
                </template>

                <template v-if="column.key === 'status'">
                  <Tag v-if="record.status === '运行中'" color="success" class="border-none m-0 font-bold">运行中</Tag>
                  <Tag v-else-if="record.status === '空闲待机'" color="warning" class="border-none m-0 font-bold">空闲待机</Tag>
                  <Tag v-else-if="record.status === '维护中'" color="error" class="border-none m-0 font-bold">维护中</Tag>
                  <Tag v-else color="default" class="border-none m-0 font-bold">离线</Tag>
                </template>

                <template v-if="column.key === 'action'">
                  <Button size="small" type="primary" class="font-bold shadow-sm w-full"
                          :class="currentDevice === record.name ? 'bg-slate-300 text-slate-600 border-none' : 'bg-indigo-600'"
                          :disabled="currentDevice === record.name || record.status === '维护中' || record.status === '离线'"
                          @click="handleSelect(record)">
                    {{ currentDevice === record.name ? '当前所在' : '切换上线' }}
                  </Button>
                </template>
              </template>

            </Table>
          </Spin>
        </div>
      </div>

    </div>
  </Modal>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background-color: #cbd5e1; border-radius: 4px; }
</style>

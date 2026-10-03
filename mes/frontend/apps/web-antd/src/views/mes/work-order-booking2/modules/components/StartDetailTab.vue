<script lang="ts" setup>
import { computed } from 'vue';
import { Form, FormItem, Input, InputNumber, Button, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const props = defineProps<{ task: any, startForm: any }>();


// 🌟 新增：根据当前工序智能推导单位 (UOM)
const displayUom = computed(() => {
  const process = props.startForm.process;
  if (['配料'].includes(process)) return 'kg';
  if (['湿法', '粘双面胶', '粗磨', '精磨'].includes(process)) return 'm (米)';
  return 'pcs (片)';
});

const poolTotalQty = computed(() => props.startForm.batchPool.reduce((sum: number, item: any) => sum + (item.qty || 0), 0));
const addPoolItem = () => props.startForm.batchPool.push({ id: Date.now().toString(), batchNo: '', qty: 0 });
const removePoolItem = (index: number) => props.startForm.batchPool.splice(index, 1);
</script>

<template>
  <div class="h-full flex flex-col bg-slate-50 p-4 overflow-y-auto min-h-0">
    <div class="bg-white border border-slate-200 rounded-xl p-5 shadow-sm flex-1 flex flex-col min-h-0 shrink-0">

      <div class="text-base font-black text-slate-800 border-b border-slate-100 pb-3 mb-4 flex items-center justify-between shrink-0">
        <div class="flex items-center">
          <IconifyIcon icon="lucide:file-text" class="mr-2 text-blue-500"/>
          生产指令与开工配置
        </div>
        <div class="text-xs font-normal text-slate-500">
          当前操作员：<span class="font-bold text-slate-700 ml-1">{{ startForm.operator }}</span>
        </div>
      </div>

      <Form layout="vertical" class="flex-1 flex flex-col overflow-y-auto pr-2 custom-compact-form">

        <div class="grid grid-cols-4 gap-5 mb-5 bg-slate-50/70 p-4 rounded-lg border border-slate-200 shadow-inner">
          <FormItem label="计划编号" class="mb-0">
            <Input :value="task?.planNo" readonly class="font-mono bg-white text-slate-500" />
          </FormItem>
          <FormItem label="加工产品" class="mb-0">
            <Input :value="task?.product" readonly class="font-bold text-indigo-600 bg-white" />
          </FormItem>
          <FormItem label="计划数量 (PCS)" class="mb-0">
            <InputNumber :value="task?.planQty" readonly class="w-full font-bold bg-white" />
          </FormItem>
          <FormItem label="当前执行工序" class="mb-0">
            <Input :value="startForm.process" readonly class="bg-blue-50 text-blue-700 font-bold border-blue-200" />
          </FormItem>
        </div>

        <div class="text-sm font-bold text-slate-700 mb-3 flex items-center">
          <IconifyIcon icon="lucide:settings-2" class="mr-1 text-emerald-500"/> 动态工艺参数配置
        </div>

        <div v-if="['配料', '温法'].includes(startForm.process)" class="bg-blue-50/40 p-4 rounded-lg border border-blue-100 grid grid-cols-3 gap-6 mb-4">
          <FormItem label="配料单号" class="mb-0"><Input v-model:value="startForm.batchingNo" /></FormItem>
          <FormItem label="设定母批次号" class="mb-0"><Input v-model:value="startForm.motherBatchNo" /></FormItem>
          <FormItem :label="startForm.process === '配料' ? '计划产出量 (kg)' : '计划产出量 (米)'" class="mb-0"><InputNumber v-model:value="startForm.planBaseQty" class="w-full" /></FormItem>
        </div>

        <div v-if="['粗磨', '精磨', '粘双面胶', '分切'].includes(startForm.process)" class="bg-amber-50/40 p-4 rounded-lg border border-amber-100 grid grid-cols-3 gap-6 mb-4">
          <FormItem :label="startForm.process === '粗磨' ? '母批次号' : startForm.process === '精磨' ? '粗磨批次号' : startForm.process === '粘双面胶' ? '精磨批次号' : '粘双面批次号'" class="mb-0">
            <Input v-model:value="startForm.motherBatchNo" class="font-bold text-indigo-600" />
          </FormItem>
          <FormItem :label="startForm.process === '粗磨' ? '带入母批长度(米)' : '前置批次长度(米)'" class="mb-0"><InputNumber v-model:value="startForm.motherLength" class="w-full" /></FormItem>
          <FormItem label="计划加工长度(米)" class="mb-0"><InputNumber v-model:value="startForm.planBaseQty" class="w-full" /></FormItem>
        </div>

        <div v-if="['单片压槽', '单片背胶', '裁圆'].includes(startForm.process)" class="flex flex-col gap-3 mb-3">
          <div class="grid grid-cols-4 gap-4 bg-emerald-50/40 p-4 rounded-lg border border-emerald-100 items-center">
            <FormItem label="计划加工总片数 (PCS)" class="mb-0"><InputNumber v-model:value="startForm.planPieceQty" class="w-full font-bold text-emerald-600" /></FormItem>
            <div class="col-span-3 text-right text-slate-600 font-bold text-sm pt-4">
              加工池累计片数：<span class="text-xl font-black text-indigo-600 mx-2">{{ poolTotalQty }}</span> PCS
            </div>
          </div>

          <div class="border border-slate-200 rounded-md overflow-hidden shadow-sm">
            <div class="bg-slate-100 px-3 py-2 font-bold text-slate-700 flex justify-between items-center border-b border-slate-200 text-sm">
              <span>前置批次加工池关联 (汇聚多批)</span><Button size="small" type="primary" ghost @click="addPoolItem">➕ 添加批次</Button>
            </div>
            <table class="w-full text-left bg-white text-xs">
              <thead class="border-b border-slate-200 text-slate-500"><tr><th class="p-2 w-1/2">{{ startForm.process === '单片压槽' ? '分切批次号' : startForm.process === '单片背胶' ? '压槽批次号' : '单片背胶批次号' }}</th><th class="p-2">对应片数</th><th class="p-2 w-12 text-center">删</th></tr></thead>
              <tbody>
              <tr v-for="(item, idx) in startForm.batchPool" :key="item.id" class="border-b border-slate-50 hover:bg-slate-50">
                <td class="p-1"><Input v-model:value="item.batchNo" placeholder="录入批次" size="small" /></td>
                <td class="p-1"><InputNumber v-model:value="item.qty" class="w-full" size="small" /></td>
                <td class="p-1 text-center"><Button type="text" danger size="small" @click="removePoolItem(idx)"><IconifyIcon icon="lucide:trash-2"/></Button></td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>

      </Form>
    </div>
  </div>
</template>

<style scoped>
.custom-compact-form :deep(.ant-form-item) { margin-bottom: 12px; }
</style>

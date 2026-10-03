<script lang="ts" setup>
import { ref, onMounted, computed, watch } from 'vue';
import { Button, Input, InputNumber, Tag, message, Spin, RadioGroup, RadioButton, TimePicker } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import dayjs from 'dayjs';
import { fetchProcessSteps, type StepModule } from './bom-step-data';

const props = defineProps<{ task?: any, startForm?: any }>();

const isLoading = ref(false);
const steps = ref<StepModule[]>([]);
const activeStepId = ref('');

const loadSteps = async () => {
  isLoading.value = true;
  const processName = props.startForm?.process || '配料';
  const modelName = props.startForm?.spec || 'RX26';

  steps.value = await fetchProcessSteps(processName, modelName);
  if (steps.value.length > 0) {
    activeStepId.value = steps.value[0].id;
    steps.value[0].status = 'IN_PROGRESS';
  }
  isLoading.value = false;
};

onMounted(() => loadSteps());
watch(() => props.startForm?.process, () => loadSteps());

const currentStep = computed(() => steps.value.find(s => s.id === activeStepId.value));

const switchStep = (id: string) => {
  activeStepId.value = id;
  const targetStep = steps.value.find(s => s.id === id);
  if (targetStep && targetStep.status === 'PENDING') {
    targetStep.status = 'IN_PROGRESS';
  }
};

const scanInput = ref('');
const handleScan = () => {
  if (!scanInput.value || !currentStep.value?.materials) return;
  const isMatched = currentStep.value.materials.find(m => m.status === 'WAITING');
  if (isMatched) {
    isMatched.batchNo = scanInput.value.toUpperCase();
    message.success(`物料 ${isMatched.itemNo} 批号已识别，请称重！`);
  } else {
    message.error('当前步骤所有物料已核对完毕，或扫入错误条码！');
  }
  scanInput.value = '';
};

const checkMaterialTolerance = (material: any) => {
  if (material.actualQty === null) {
    material.status = 'WAITING';
    return;
  }
  const min = material.standardQty - material.tolerance;
  const max = material.standardQty + material.tolerance;
  if (material.actualQty >= min && material.actualQty <= max) {
    material.status = 'OK';
  } else {
    material.status = 'NG';
    message.error(`称重异常！${material.itemNo} 的公差范围应为 ${min} ~ ${max} kg`);
  }
};

const calculateDuration = () => {
  if (!currentStep.value?.operation) return;
  const op = currentStep.value.operation;

  if (op.startTime && op.endTime) {
    const start = dayjs(`2000-01-01 ${op.startTime}`);
    const end = dayjs(`2000-01-01 ${op.endTime}`);
    let diff = end.diff(start, 'minute');
    if (diff < 0) diff += 24 * 60;
    op.duration = diff;
  }
};

// 🌟 新增：暂存记录方法，不强制校验阻断
const saveCurrentStep = () => {
  if (!currentStep.value) return;
  message.success('当前阶段数据已暂存，您可以稍后继续填写或签核！');
};

// 签核流转方法 (带严格校验)
const finishCurrentStep = () => {
  if (!currentStep.value) return;

  if (!currentStep.value.confirmer) {
    return message.error('防呆拦截：请刷入或填写当前阶段的确认人信息！');
  }

  if (currentStep.value.modules.hasMaterials) {
    const hasNgOrWaiting = currentStep.value.materials?.some(m => m.status !== 'OK');
    if (hasNgOrWaiting) return message.error('防呆拦截：必须完成当前步骤所有物料的称重，且数值在公差范围内！');
  }

  if (currentStep.value.modules.hasOperation) {
    const op = currentStep.value.operation;
    if (op?.speedLabel && !op?.actualSpeed) return message.error(`防呆拦截：请录入实际${op.speedLabel}！`);
    if (op?.vacuumLabel && !op?.actualVacuum) return message.error(`防呆拦截：请录入实际${op.vacuumLabel}！`);
    if (op?.visualCheckLabel && !op?.visualCheckResult) return message.error(`防呆拦截：请确认${op.visualCheckLabel}！`);

    if (op?.timeLabel) {
      if (!op?.startTime || !op?.endTime) return message.error(`防呆拦截：请选择完整的 ${op.startLabel||'开始'} 与 ${op.endLabel||'结束'} ！`);
      if (op?.duration === null || op?.duration === undefined) return message.error('防呆拦截：请确保计算出了实际时长！');
    }
  }

  if (currentStep.value.modules.hasInspection) {
    const ins = currentStep.value.inspection;
    if (ins?.items.some(i => !i.value)) return message.error('防呆拦截：请录入所有的检验项实测值！');
  }

  currentStep.value.status = 'COMPLETED';

  const nextPendingIndex = steps.value.findIndex(s => s.status !== 'COMPLETED');
  if (nextPendingIndex !== -1) {
    const nextStep = steps.value[nextPendingIndex];
    activeStepId.value = nextStep.id;
    if (nextStep.status === 'PENDING') nextStep.status = 'IN_PROGRESS';
    message.success('当前节点签核完成，自动进入下一环节！');
  } else {
    message.success('🎉 恭喜！当前批次过程作业已全部录入完毕！');
  }
};
</script>

<template>
  <div class="h-full bg-slate-100 relative overflow-hidden -m-2 custom-spin-wrapper">
    <Spin :spinning="isLoading">
      <div class="flex h-full w-full">

        <div class="w-64 bg-white border-r border-slate-200 flex flex-col h-full shrink-0 shadow-[2px_0_8px_rgba(0,0,0,0.02)] z-10">
          <div class="h-14 flex items-center px-5 border-b border-slate-100 shrink-0 bg-slate-50">
            <IconifyIcon icon="lucide:git-merge" class="text-indigo-600 text-lg mr-2"/>
            <span class="font-black text-slate-800 tracking-wide">过程工序 (e-SOP)</span>
          </div>
          <div class="flex-1 overflow-y-auto p-4 space-y-3">
            <div v-for="step in steps" :key="step.id"
                 class="relative p-3 rounded-xl border-2 transition-all cursor-pointer hover:border-indigo-300"
                 :class="[
                   activeStepId === step.id ? 'border-indigo-500 bg-indigo-50/50 shadow-md' : 'border-transparent bg-slate-50',
                   step.status === 'COMPLETED' ? 'opacity-70 border-emerald-200 bg-emerald-50' : ''
                 ]"
                 @click="switchStep(step.id)"
            >
              <div class="flex items-center justify-between mb-1">
                <span class="text-xs font-black" :class="activeStepId === step.id ? 'text-indigo-600' : 'text-slate-400'">【{{ step.category }}】</span>
                <IconifyIcon v-if="step.status === 'COMPLETED'" icon="lucide:check-circle-2" class="text-emerald-500 text-base" />
                <IconifyIcon v-else-if="step.status === 'IN_PROGRESS'" icon="lucide:loader-2" class="text-indigo-500 text-base animate-spin" />
                <IconifyIcon v-else icon="lucide:circle-dashed" class="text-slate-300 text-base" />
              </div>
              <div class="font-bold text-sm" :class="activeStepId === step.id ? 'text-indigo-900' : 'text-slate-600'">{{ step.name }}</div>
            </div>
          </div>
        </div>

        <div class="flex-1 flex flex-col min-w-0 bg-slate-50 relative">
          <div v-if="currentStep" class="flex-1 flex flex-col min-h-0">

            <div class="px-6 pt-6 pb-2 shrink-0 flex items-center justify-between">
              <h2 class="text-2xl font-black text-slate-800 tracking-tight">{{ currentStep.category }} - {{ currentStep.name }}</h2>
              <Tag color="processing" class="border-none px-3 py-1 text-sm font-bold shadow-sm">
                {{ currentStep.status === 'COMPLETED' ? '已记录归档' : '节点执行中' }}
              </Tag>
            </div>

            <div class="flex-1 overflow-y-auto px-6 pb-6 flex flex-col">

              <div class="space-y-6 flex-1">
                <div v-if="!currentStep.modules.hasEnv && !currentStep.modules.hasMaterials && !currentStep.modules.hasOperation && !currentStep.modules.hasInspection" class="bg-white border border-slate-200 border-dashed rounded-2xl p-10 flex flex-col items-center justify-center text-slate-400">
                  <IconifyIcon icon="lucide:clipboard-check" class="text-5xl mb-3 opacity-50 text-indigo-400" />
                  <span class="font-bold text-base text-slate-600">此节点无特定表单参数要求</span>
                  <span class="text-xs mt-1">请确认现场工艺动作合规后，直接在下方签字确认。</span>
                </div>

                <div v-if="currentStep.modules.hasEnv && currentStep.env" class="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
                  <div class="flex items-center mb-4 text-slate-700 font-bold"><IconifyIcon icon="lucide:thermometer-sun" class="mr-2 text-amber-500"/> 现场环境确认</div>
                  <div class="grid grid-cols-2 gap-6">
                    <div class="flex items-center gap-3">
                      <span class="text-sm font-bold w-16">环境温度</span>
                      <InputNumber v-model:value="currentStep.env.temp" class="flex-1" :placeholder="`要求: ${currentStep.env.tempStd}`"><template #addonAfter>℃</template></InputNumber>
                    </div>
                    <div class="flex items-center gap-3">
                      <span class="text-sm font-bold w-16">环境湿度</span>
                      <InputNumber v-model:value="currentStep.env.humidity" class="flex-1" :placeholder="`要求: ${currentStep.env.humidityStd}`"><template #addonAfter>%RH</template></InputNumber>
                    </div>
                  </div>
                </div>

                <div v-if="currentStep.modules.hasMaterials && currentStep.materials" class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
                  <div class="bg-blue-50/50 border-b border-blue-100 p-4 flex gap-4 items-center shrink-0">
                    <IconifyIcon icon="lucide:scan-barcode" class="text-2xl text-blue-600"/>
                    <Input.Search v-model:value="scanInput" placeholder="请扫码枪扫入物料批次条码..." enter-button="防错校验" size="large" @search="handleScan" class="flex-1 max-w-md shadow-sm" />
                  </div>
                  <div class="p-2 bg-slate-50">
                    <div v-for="(mat, idx) in currentStep.materials" :key="mat.id" class="flex items-center bg-white border rounded-xl p-3 mb-2 shadow-sm transition-all" :class="mat.status==='OK'?'border-emerald-300 bg-emerald-50/30':(mat.status==='NG'?'border-red-400 bg-red-50/50':'border-slate-200')">
                      <div class="w-8 font-black text-slate-300 text-lg ml-2">{{ idx + 1 }}</div>
                      <div class="w-32 flex flex-col"><span class="text-xs text-slate-400 font-bold mb-0.5">要求料号</span><span class="font-black text-indigo-700 text-base font-mono">{{ mat.itemNo }}</span></div>
                      <div class="w-48 flex flex-col"><span class="text-xs text-slate-400 font-bold mb-0.5">扫入批号</span><Input v-model:value="mat.batchNo" size="small" placeholder="扫码带出" class="font-mono font-bold bg-slate-50"/></div>
                      <div class="flex-1 flex items-center justify-center gap-4 px-6 border-x border-slate-100 mx-4">
                        <div class="text-center"><div class="text-[11px] text-slate-400 font-bold">标准要求</div><div class="font-black text-slate-700">{{ mat.standardQty }}<span class="text-xs font-normal">kg</span></div></div>
                        <div class="text-slate-300">±</div>
                        <div class="text-center"><div class="text-[11px] text-slate-400 font-bold">公差范围</div><div class="font-black text-amber-500">{{ mat.tolerance }}<span class="text-xs font-normal">kg</span></div></div>
                        <IconifyIcon icon="lucide:arrow-right" class="text-slate-300 ml-2" />
                        <div class="flex-1 ml-4 flex flex-col max-w-[200px]">
                          <span class="text-[11px] font-bold mb-1" :class="mat.status==='NG'?'text-red-500':'text-indigo-500'">实际称重</span>
                          <InputNumber v-model:value="mat.actualQty" :min="0" class="w-full font-black text-lg" @change="checkMaterialTolerance(mat)" :class="{'ring-2 ring-red-400': mat.status==='NG', 'ring-2 ring-emerald-400': mat.status==='OK'}"><template #addonAfter>kg</template></InputNumber>
                        </div>
                      </div>
                      <div class="w-24 text-center">
                        <Tag v-if="mat.status==='WAITING'" color="default" class="border-none font-bold">待称重</Tag>
                        <Tag v-else-if="mat.status==='OK'" color="success" class="border-none font-bold shadow-sm px-3"><IconifyIcon icon="lucide:check" class="mr-1"/>合格</Tag>
                        <Tag v-else color="error" class="border-none font-bold shadow-sm px-3 animate-pulse"><IconifyIcon icon="lucide:x" class="mr-1"/>超差</Tag>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="currentStep.modules.hasOperation && currentStep.operation" class="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col xl:flex-row gap-6">
                  <div v-if="currentStep.operation.speedLabel || currentStep.operation.vacuumLabel || currentStep.operation.visualCheckLabel" class="flex-1 grid grid-cols-1 gap-4" :class="{'border-r border-slate-100 pr-6': currentStep.operation.timeLabel}">
                    <div class="flex items-center mb-1 text-slate-700 font-bold"><IconifyIcon icon="lucide:settings-2" class="mr-2 text-indigo-500"/> 现场操作确认</div>

                    <div v-if="currentStep.operation.speedLabel" class="bg-slate-50 p-3 rounded-xl border border-slate-100 flex items-center justify-between">
                      <div><div class="text-xs text-slate-400 font-bold mb-1">{{ currentStep.operation.speedLabel }}要求</div><div class="text-base font-black text-slate-700">{{ currentStep.operation.speedStd }}</div></div>
                      <div class="w-48"><div class="text-xs text-indigo-500 font-bold mb-1">实测记录</div><InputNumber v-model:value="currentStep.operation.actualSpeed" class="w-full font-bold" placeholder="输入实测值" /></div>
                    </div>

                    <div v-if="currentStep.operation.visualCheckLabel" class="bg-slate-50 p-4 rounded-xl border border-slate-100 flex items-center justify-between">
                      <div class="text-sm font-black text-slate-700">{{ currentStep.operation.visualCheckLabel }}</div>
                      <RadioGroup v-model:value="currentStep.operation.visualCheckResult" button-style="solid" class="font-bold custom-radio-group">
                        <RadioButton value="OK" class="data-[state=checked]:!bg-emerald-500">已确认完成</RadioButton>
                        <RadioButton value="NG" class="data-[state=checked]:!bg-red-500">发现异常</RadioButton>
                      </RadioGroup>
                    </div>
                  </div>

                  <div v-if="currentStep.operation.timeLabel" class="flex-1 flex flex-col justify-center">
                    <div class="flex items-center mb-4 text-slate-700 font-bold"><IconifyIcon icon="lucide:timer" class="mr-2 text-emerald-500"/> {{ currentStep.operation.timeLabel }}管控 (要求: {{ currentStep.operation.timeStd }})</div>
                    <div class="flex gap-4 mb-4">
                      <div class="flex-1">
                        <div class="text-xs text-indigo-500 font-bold mb-1">{{ currentStep.operation.startLabel || '开始时间' }}</div>
                        <TimePicker v-model:value="currentStep.operation.startTime" valueFormat="HH:mm:ss" size="large" class="w-full font-bold bg-indigo-50/30" @change="calculateDuration" placeholder="选择时间" />
                      </div>
                      <div class="flex-1">
                        <div class="text-xs text-indigo-500 font-bold mb-1">{{ currentStep.operation.endLabel || '结束时间' }}</div>
                        <TimePicker v-model:value="currentStep.operation.endTime" valueFormat="HH:mm:ss" size="large" class="w-full font-bold bg-indigo-50/30" @change="calculateDuration" placeholder="选择时间" />
                      </div>
                    </div>
                    <div class="bg-slate-50 p-3 rounded-lg border border-slate-200 flex items-center justify-between shadow-inner">
                      <span class="text-sm font-bold text-slate-600">计算流转时长 (可手动修正)</span>
                      <InputNumber v-model:value="currentStep.operation.duration" :min="0" class="w-32 font-black text-lg text-emerald-600 custom-huge-input" placeholder="自动计算"><template #addonAfter>MIN</template></InputNumber>
                    </div>
                  </div>
                </div>

                <div v-if="currentStep.modules.hasInspection && currentStep.inspection" class="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
                  <div class="flex items-center mb-4 text-slate-700 font-bold"><IconifyIcon icon="lucide:flask-conical" class="mr-2 text-purple-500"/> {{ currentStep.category }} 质量参数实测记录</div>
                  <div class="grid grid-cols-2 gap-6">
                    <div v-for="item in currentStep.inspection.items" :key="item.id" class="flex items-center gap-3">
                      <div class="flex flex-col w-20">
                        <span class="text-sm font-bold text-slate-700">{{ item.label }}</span>
                        <span v-if="item.std" class="text-[10px] text-slate-400 font-normal truncate" :title="item.std">标:{{ item.std }}</span>
                      </div>
                      <Input v-model:value="item.value" class="flex-1 font-bold border-indigo-200" placeholder="录入实测数据">
                        <template v-if="item.unit" #addonAfter>{{ item.unit }}</template>
                      </Input>
                    </div>
                  </div>
                </div>
              </div>

              <div class="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm mt-6 shrink-0">
                <div class="flex items-center mb-4 text-slate-700 font-bold"><IconifyIcon icon="lucide:user-check" class="mr-2 text-indigo-500"/> 阶段提报与人员签字</div>
                <div class="flex gap-6">
                  <div class="flex-1">
                    <div class="text-xs text-slate-400 font-bold mb-1">异常说明与备注 (若有)</div>
                    <Input.TextArea v-model:value="currentStep.remark" placeholder="如该阶段运行平稳无异常，此项可留空..." :rows="2" class="w-full bg-slate-50 border-slate-200" />
                  </div>
                  <div class="w-64 flex flex-col justify-end">
                    <div class="text-xs text-red-500 font-bold mb-1">* 确认人签名 (必填)</div>
                    <Input v-model:value="currentStep.confirmer" placeholder="请刷入工号或输入姓名" size="large" class="w-full font-bold border-red-200 focus:ring-red-400 bg-red-50/30" />
                  </div>
                </div>
              </div>

            </div>

            <div class="p-4 bg-white border-t border-slate-200 shrink-0 flex justify-end shadow-[0_-4px_10px_rgba(0,0,0,0.03)] z-10">
              <Button size="large" class="font-bold px-8 h-12 text-lg mr-4 border-slate-300 text-slate-700 hover:text-indigo-600 hover:border-indigo-600" @click="saveCurrentStep" :disabled="currentStep.status === 'COMPLETED'">
                <IconifyIcon icon="lucide:save" class="mr-2" />
                保存记录
              </Button>
              <Button size="large" type="primary" class="bg-indigo-600 font-black px-12 shadow-md h-12 text-lg" @click="finishCurrentStep" :disabled="currentStep.status === 'COMPLETED'">
                <IconifyIcon icon="lucide:check-square-2" class="mr-2" />
                确认无误，签核当前环节
              </Button>
            </div>

          </div>

          <div v-else class="h-full flex flex-col items-center justify-center text-slate-400">
            <IconifyIcon icon="lucide:file-json-2" class="text-6xl mb-4 opacity-50" />
            <span class="font-bold">未读取到工序对应的动态 e-SOP 模板</span>
          </div>
        </div>
      </div>
    </Spin>
  </div>
</template>

<style scoped>
:deep(.ant-input-group-addon), :deep(.ant-input-number-group-addon) { background-color: #f8fafc; border-color: #e2e8f0; font-weight: bold; color: #64748b; }
:deep(.custom-huge-input .ant-input) { text-align: center !important; }
.custom-spin-wrapper :deep(.ant-spin-nested-loading),
.custom-spin-wrapper :deep(.ant-spin-container) { height: 100%; width: 100%; display: flex; flex-direction: column; }

/* Radio 深度重写 */
:deep(.custom-radio-group .ant-radio-button-wrapper) { border-radius: 6px !important; border: 1px solid #d9d9d9 !important; transition: all 0.2s; margin-right: 8px; }
:deep(.custom-radio-group .ant-radio-button-wrapper::before) { display: none !important; }
:deep(.custom-radio-group .ant-radio-button-wrapper-checked) { color: white !important; border-color: transparent !important; box-shadow: 0 2px 4px rgba(0,0,0, 0.1) !important; }
</style>

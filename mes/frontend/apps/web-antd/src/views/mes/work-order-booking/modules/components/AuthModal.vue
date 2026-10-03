<script lang="ts" setup>
import { nextTick, ref, watch } from 'vue';

import { Button, Input, Modal, Select, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

import { getUserPage } from '#/api/system/user';

interface EquipmentOption {
  code?: string;
  label: string;
  name?: string;
  value: number;
}

const props = withDefaults(
  defineProps<{
    actionName?: string;
    authMode?: 'card' | 'username';
    equipmentId?: number;
    equipmentLabel?: string;
    equipmentOptions?: EquipmentOption[];
    expectedNickname?: string;
    expectedUsername?: string;
    foamingEquipmentId?: number;
    foamingOptions?: EquipmentOption[];
    mixerEquipmentId?: number;
    mixerOptions?: EquipmentOption[];
    showEquipmentFields?: boolean;
    title?: string;
    visible?: boolean;
    workstation?: string;
  }>(),
  {
    actionName: '操作认证',
    authMode: 'card',
    equipmentId: undefined,
    equipmentLabel: '机台编号',
    equipmentOptions: () => [],
    expectedNickname: '',
    expectedUsername: '',
    foamingEquipmentId: undefined,
    foamingOptions: () => [],
    mixerEquipmentId: undefined,
    mixerOptions: () => [],
    showEquipmentFields: false,
    title: '安全认证：操作人员身份签核',
    visible: false,
    workstation: '',
  },
);

const emit = defineEmits(['update:visible', 'success', 'cancel']);

const authInput = ref('');
const inputRef = ref();
const currentProfile = ref<{ nickname?: string; username?: string }>({});
const profileLoading = ref(false);
const equipmentId = ref<number>();
const mixerEquipmentId = ref<number>();
const foamingEquipmentId = ref<number>();
const inlineError = ref('');

watch(
  () => props.visible,
  (val) => {
    if (!val) return;
    authInput.value = '';
    inlineError.value = '';
    equipmentId.value = props.equipmentId;
    mixerEquipmentId.value = props.mixerEquipmentId;
    foamingEquipmentId.value = props.foamingEquipmentId;
    currentProfile.value = {};
    nextTick(() => {
      inputRef.value?.focus();
    });
  },
);

function handleCancel() {
  inlineError.value = '';
  emit('update:visible', false);
  emit('cancel');
}

function getOptionMeta(options: EquipmentOption[], id?: number) {
  return options.find((item) => item.value === id);
}

function resolvePopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || triggerNode;
}

async function queryUserByUsername(username: string) {
  const page = await getUserPage({
    pageNo: 1,
    pageSize: 20,
    username,
  } as any);
  const list = page?.list || [];
  return list.find((item: any) => String(item.username || '').trim() === username.trim());
}

async function handleConfirm() {
  if (!authInput.value) {
    if (props.authMode === 'username') {
      inlineError.value = '请输入操作人员用户名并回车完成身份确认。';
      return;
    }
    return message.warning('防呆拦截：请刷入员工卡或输入登录工号！');
  }

  if (props.authMode === 'username') {
    inlineError.value = '';
    profileLoading.value = true;
    let matchedUser: any;
    try {
      matchedUser = await queryUserByUsername(authInput.value.trim());
    } finally {
      profileLoading.value = false;
    }
    if (!matchedUser) {
      inlineError.value = `未查询到用户名【${authInput.value.trim()}】对应的用户信息，请输入工号并回车。`;
      currentProfile.value = {};
      return;
    }
    currentProfile.value = {
      nickname: matchedUser.nickname,
      username: matchedUser.username,
    };
    if (props.equipmentOptions.length > 0 && !props.showEquipmentFields) {
      if (!equipmentId.value) {
        inlineError.value = `请选择${props.equipmentLabel}。`;
        return;
      }
    }
    if (props.showEquipmentFields) {
      if (!mixerEquipmentId.value) {
        inlineError.value = '请选择搅拌机台。';
        return;
      }
      if (!foamingEquipmentId.value) {
        inlineError.value = '请选择脱泡机台。';
        return;
      }
    }
    const equipmentOption = getOptionMeta(props.equipmentOptions, equipmentId.value);
    const mixerOption = getOptionMeta(props.mixerOptions, mixerEquipmentId.value);
    const foamingOption = getOptionMeta(props.foamingOptions, foamingEquipmentId.value);
    emit('success', {
      empNo: matchedUser.username,
      empName: matchedUser.nickname || matchedUser.username,
      equipmentCode: equipmentOption?.code,
      equipmentId: equipmentId.value,
      equipmentName: equipmentOption?.name || equipmentOption?.label,
      userId: matchedUser.id,
      username: matchedUser.username,
      mixerEquipmentCode: mixerOption?.code,
      mixerEquipmentId: mixerEquipmentId.value,
      mixerEquipmentName: mixerOption?.name || mixerOption?.label,
      foamingEquipmentCode: foamingOption?.code,
      foamingEquipmentId: foamingEquipmentId.value,
      foamingEquipmentName: foamingOption?.name || foamingOption?.label,
    });
    emit('update:visible', false);
    return;
  }

  let operatorName = authInput.value;
  if (authInput.value === '8801') operatorName = '张师傅 (8801)';
  else if (authInput.value === '8802') operatorName = '李班长 (8802)';
  else operatorName = `员工鉴权通过 (${authInput.value})`;
  emit('success', { empNo: authInput.value, empName: operatorName });
  emit('update:visible', false);
}
</script>

<template>
  <Modal
    :open="visible"
    @update:open="(val) => emit('update:visible', val)"
    :title="title"
    centered
    :width="authMode === 'username' && showEquipmentFields ? 560 : authMode === 'username' && equipmentOptions.length > 0 ? 480 : 420"
    :closable="false"
    :maskClosable="false"
    :zIndex="100000"
  >
    <div class="py-6 flex flex-col items-center">
      <IconifyIcon icon="lucide:fingerprint" class="mb-4 text-6xl text-indigo-500 opacity-80" />

      <div
        v-if="workstation"
        class="mb-3 rounded-full bg-slate-100 px-4 py-1.5 text-sm font-black text-slate-500"
      >
        当前机台: <span class="text-slate-800">{{ workstation }}</span>
      </div>

      <div class="mb-4 text-base font-bold text-slate-700">
        当前申请：<span class="text-indigo-600">{{ actionName }}</span>
      </div>

      <div v-if="authMode === 'username'" class="w-full">
        <Input
          ref="inputRef"
          v-model:value="authInput"
          size="large"
          placeholder="请输入操作人员用户名..."
          class="w-full text-center text-lg font-bold tracking-widest bg-slate-50"
          @pressEnter="handleConfirm"
          @input="inlineError = ''"
        >
          <template #prefix>
            <IconifyIcon icon="lucide:user-round-check" class="mr-1 text-slate-400" />
          </template>
        </Input>

        <div v-if="inlineError" class="mt-2 text-sm font-semibold text-red-500">
          {{ inlineError }}
        </div>

        <div class="mt-3 rounded border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-500">
          <div>
            已识别人员：
            <span class="font-bold text-slate-700">{{ currentProfile.nickname || '-' }}</span>
          </div>
          <div>
            用户名：
            <span class="font-mono font-bold text-slate-700">{{ currentProfile.username || '-' }}</span>
          </div>
          <div v-if="profileLoading" class="mt-1 text-slate-400">正在查询用户表信息...</div>
        </div>
        <div class="mt-2 text-xs text-slate-400">提示：输入工号并回车，系统将查询用户表并自动带出人员昵称。</div>

        <div v-if="equipmentOptions.length > 0 && !showEquipmentFields" class="mt-4">
          <div class="mb-1 text-xs font-bold text-slate-500">{{ equipmentLabel }}</div>
          <Select
            v-model:value="equipmentId"
            :options="equipmentOptions"
            :field-names="{ label: 'label', value: 'value' }"
            :placeholder="`请选择${equipmentLabel}`"
            class="w-full"
            :get-popup-container="resolvePopupContainer"
          />
        </div>

        <div v-if="showEquipmentFields" class="mt-4 space-y-3">
          <div>
            <div class="mb-1 text-xs font-bold text-slate-500">搅拌机台</div>
            <Select
              v-model:value="mixerEquipmentId"
              :options="mixerOptions"
              :field-names="{ label: 'label', value: 'value' }"
              placeholder="请选择搅拌机台"
              class="w-full"
              :get-popup-container="resolvePopupContainer"
            />
          </div>
          <div>
            <div class="mb-1 text-xs font-bold text-slate-500">脱泡机台</div>
            <Select
              v-model:value="foamingEquipmentId"
              :options="foamingOptions"
              :field-names="{ label: 'label', value: 'value' }"
              placeholder="请选择脱泡机台"
              class="w-full"
              :get-popup-container="resolvePopupContainer"
            />
          </div>
        </div>
      </div>

      <template v-else>
        <Input.Password
          ref="inputRef"
          v-model:value="authInput"
          size="large"
          placeholder="请刷入员工卡或键盘录入工号..."
          class="w-full text-center text-lg tracking-widest bg-slate-50 font-bold"
          @pressEnter="handleConfirm"
        >
          <template #prefix><IconifyIcon icon="lucide:credit-card" class="mr-1 text-slate-400" /></template>
        </Input.Password>

        <div class="mt-4 text-center text-xs leading-relaxed text-slate-400">
          提示：对接厂内真实工牌读卡器或扫码设备。<br />
          测试可输入 <span class="font-bold text-indigo-500">8801</span> 或 <span class="font-bold text-indigo-500">8802</span>。
        </div>
      </template>
    </div>

    <template #footer>
      <Button size="large" @click="handleCancel" class="border-slate-300 font-bold">取消操作</Button>
      <Button
        size="large"
        type="primary"
        class="border-none bg-indigo-600 px-8 font-bold shadow-md hover:bg-indigo-500"
        @click="handleConfirm"
      >
        确认身份
      </Button>
    </template>
  </Modal>
</template>

<script lang="ts" setup>
import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createMountRecord, getMountRecord, teardownMold } from '#/api/mes/resource/mold/mount-record';
import { useMountFormSchema, useTeardownFormSchema } from '../data';
import dayjs from 'dayjs';

defineOptions({ name: 'MoldMountActionModal' });
const emit = defineEmits(['success']);

const actionType = ref<'mount' | 'teardown'>('mount');
const formData = ref<any>({});
const currentOutputPerCycle = ref(1);

// 💡 联动计算引擎：供下模表单使用
const handleReportedQtyChange = async (qty: number) => {
  if (!qty || qty <= 0) {
    await teardownFormApi.setFieldValue('producedQty', 0);
    return;
  }
  // 折算模次 = 向上取整 (总产量 / 一模几穴)
  const consumedCycles = Math.ceil(qty / currentOutputPerCycle.value);
  await teardownFormApi.setFieldValue('producedQty', consumedCycles);
};

// ==========================================
// 1. 独立实例化【上模表单】
// ==========================================
const [MountForm, mountFormApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 140 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3 gap-x-6 gap-y-2',
  schema: useMountFormSchema().map(item => {
    if (item.fieldName === 'remark') return { ...item, formItemClass: 'col-span-3' };
    return item;
  }),
  showDefaultActions: false,
});

// ==========================================
// 2. 独立实例化【下模表单】
// ==========================================
const [TeardownForm, teardownFormApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 140 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3 gap-x-6 gap-y-2',
  schema: useTeardownFormSchema(handleReportedQtyChange).map(item => {
    if (['remark', 'afterStatus'].includes(item.fieldName)) return { ...item, formItemClass: 'col-span-3' };
    return item;
  }),
  showDefaultActions: false,
});

// ==========================================
// 弹窗逻辑总控
// ==========================================
const [BaseModal, modalApi] = useVbenModal({
  title: computed(() => actionType.value === 'mount' ? '🚀 工装模具上机生产登记' : '🔽 模具下线与寿命折算归档'),
  class: 'w-[1050px]',
  onConfirm: async () => {
    const currentApi = actionType.value === 'mount' ? mountFormApi : teardownFormApi;

    const { valid } = await currentApi.validate();
    // 💡 注意：如果表单验证不通过，这里会静默返回，弹窗自然不会关闭
    if (!valid) return;

    let isSuccess = false; // 新增成功标记

    // 建议使用 confirmLoading 仅让按钮转圈，而不是整个弹窗 loading
    modalApi.setState({ confirmLoading: true });
    try {
      const data = await currentApi.getValues();
      data.id = formData.value.id;

      if (actionType.value === 'mount') {
        data.recordStatus = 'MOUNTED';
        await createMountRecord(data);
        message.success('上模成功！模具与派工单已绑定。');
      } else {
        data.recordStatus = 'TEARDOWN';
        await teardownMold(data);
        message.success(`下模成功！扣减寿命 [${data.producedQty}次]，已同步模具台账。`);
      }

      isSuccess = true;
    } catch (error) {
      // 发生 API 错误时，保持弹窗开启状态以便用户调整
      console.error(error);
    } finally {
      // 必须先解除 loading 锁定
      modalApi.setState({ confirmLoading: false });
    }

    // 在解除锁定之后再执行关闭
    if (isSuccess) {
      emit('success');
      modalApi.close();
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();
    actionType.value = data?.action || 'mount';

    await nextTick();

    if (actionType.value === 'mount') {
      await mountFormApi.resetForm();
      // 模拟上模时：扫码解析工单后自动带出的数据
      await mountFormApi.setValues({
        workOrderNo: 'WO-20260221-001',
        productName: 'iPhone 15 标准版外壳',
        planQty: 10000,
        moldCode: 'MD-INJ-001',
        moldName: 'IP15手机壳热流道注塑模',
        outputPerCycle: 4,
        deviceCode: 'EQ-INJ-05',
        deviceName: '5#海天注塑机',
        mountTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
        mounter: '当前操作工'
      });
    } else if (data.id) {
      modalApi.setState({ loading: true });
      try {
        await teardownFormApi.resetForm();
        formData.value = await getMountRecord(data.id);
        currentOutputPerCycle.value = 4; // 模拟后台带出该模具的单次产出量(4件)

        await teardownFormApi.setValues({
          ...formData.value,
          currentLife: 465000,
          outputPerCycle: currentOutputPerCycle.value,
          reportedQty: 0,
          producedQty: 0,
          teardownTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
          teardowner: '当前操作工',
          afterStatus: 10
        });
      } finally {
        modalApi.setState({ loading: false });
      }
    }
  },
});
</script>

<template>
  <BaseModal class="w-[1200px]">
    <div class="flex flex-col h-full min-h-[70vh] w-full max-w-7xl mx-auto px-6 py-2  overflow-hidden relative">
      <div class="flex-1 overflow-y-auto custom-scrollbar pr-2 pb-6 flex flex-col gap-6 relative">
        <MountForm v-show="actionType === 'mount'" class="mx-6 mt-6 mb-8" />
        <TeardownForm v-show="actionType === 'teardown'" class="mx-6 mt-6 mb-8" />
      </div>
    </div>
  </BaseModal>
</template>

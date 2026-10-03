<script lang="ts" setup>
import type { MesHcFinishedGlueBoardMapApi } from '#/api/mes/hc/finishedglueboardmap';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import { getFinishedGlueBoardMapModelOptions } from '#/api/mes/hc/finishedglueboardmap';
import {
  createToolingConsumableLedger,
  getToolingConsumableLedger,
  getToolingProcessConsumableListByProcess,
  updateToolingConsumableLedger,
} from '#/api/mes/hc/tooling-consumable-ledger';

import {
  consumableTypeOptionsByProcess,
  defaultConsumableForProcess,
  defaultProcessForConsumable,
  isProcessAllowed,
  processText,
  useLedgerFormSchema,
} from '../data';

type LedgerModalData = MesHcToolingConsumableLedgerApi.Ledger & {
  fixedProcessCode?: string;
};

const emit = defineEmits(['success']);
const userStore = useUserStore();
const formData = ref<MesHcToolingConsumableLedgerApi.Ledger>();
const fixedProcessCode = ref<string>();
const processConfigs = ref<MesHcToolingConsumableLedgerApi.ProcessConsumable[]>(
  [],
);
type GlueBoardModelOption = {
  label: string;
  materialCodes: string[];
  value: string;
};

const glueBoardModelOptions = ref<GlueBoardModelOption[]>([]);
const glueBoardModelLoading = ref(false);
let glueBoardModelLoadSeq = 0;

const currentUserId = computed(
  () => userStore.userInfo?.id as number | undefined,
);
const currentUserName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '',
);
const getTitle = computed(() =>
  formData.value?.id ? '编辑边库耗材领用' : '新增边库耗材领用',
);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 130,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: useLedgerFormSchema(),
  showDefaultActions: false,
});

function getConfigConsumableOptions(processCode?: string) {
  if (processConfigs.value.length > 0) {
    return processConfigs.value.map((item) => ({
      label: item.consumableTypeName || item.consumableType || '',
      value: item.consumableType || '',
    }));
  }
  return consumableTypeOptionsByProcess(processCode);
}

function findProcessConfig(consumableType?: string) {
  return processConfigs.value.find(
    (item) => item.consumableType === consumableType,
  );
}

async function loadProcessConfigs(processCode?: string) {
  if (!processCode) {
    processConfigs.value = [];
    return;
  }
  processConfigs.value =
    await getToolingProcessConsumableListByProcess(processCode);
}

function normalizeText(value?: unknown) {
  return String(value || '').trim();
}

function normalizeGlueProcess(processCode?: string) {
  const process = normalizeText(processCode).toUpperCase();
  if (process === 'ADHESIVE' || process === 'ADHESIVE1') {
    return 'ADHESIVE1';
  }
  return process === 'ADHESIVE2' ? process : undefined;
}

function toGlueBoardModelOptions(
  items: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem[],
  currentModel?: string,
  keepCurrentModel = false,
) {
  const optionMap = new Map<string, { materialCodes: Set<string>; model: string }>();
  items.forEach((item) => {
    const model = normalizeText(item.glueBoardModel);
    if (!model) return;
    const key = model.toUpperCase();
    const option = optionMap.get(key) || {
      materialCodes: new Set<string>(),
      model,
    };
    const materialCode = normalizeText(item.glueBoardMaterialCode);
    if (materialCode) option.materialCodes.add(materialCode);
    optionMap.set(key, option);
  });
  const model = normalizeText(currentModel);
  if (keepCurrentModel && model && !optionMap.has(model.toUpperCase())) {
    optionMap.set(model.toUpperCase(), {
      materialCodes: new Set<string>(),
      model,
    });
  }
  return [...optionMap.values()].map(({ materialCodes, model }) => {
    const codes = [...materialCodes];
    let materialText = '（未维护料号）';
    if (codes.length === 1) {
      materialText = `（料号：${codes[0]}）`;
    } else if (codes.length > 1) {
      materialText = `（多个料号：${codes.join('、')}）`;
    }
    return {
      label: `${model}${materialText}`,
      materialCodes: codes,
      value: model,
    };
  });
}

function findGlueBoardModelOption(model?: string) {
  const normalizedModel = normalizeText(model).toUpperCase();
  if (!normalizedModel) return undefined;
  return glueBoardModelOptions.value.find(
    (item) => item.value.toUpperCase() === normalizedModel,
  );
}

async function syncGlueBoardMaterialCode(model?: string, notify = false) {
  const selectedOption = findGlueBoardModelOption(model);
  if (!normalizeText(model)) {
    await formApi.setValues({ erpMaterialCode: '' });
    return;
  }
  if (!selectedOption || selectedOption.materialCodes.length !== 1) {
    await formApi.setValues({ erpMaterialCode: '' });
    if (notify) {
      message.warning(
        selectedOption?.materialCodes.length
          ? '该胶板型号对应多个料号，请先维护成品胶板对照表'
          : '该胶板型号未维护料号，请先维护成品胶板对照表',
      );
    }
    return;
  }
  await formApi.setValues({ erpMaterialCode: selectedOption.materialCodes[0] });
}

function updateErpMaterialCodeSchema(consumableType?: string) {
  const isGlueBoard = consumableType === 'GLUE_BOARD';
  formApi.updateSchema([
    {
      fieldName: 'erpMaterialCode',
      label: isGlueBoard ? 'ERP料号（型号带出）' : 'ERP料号',
      component: 'Input',
      componentProps: {
        disabled: isGlueBoard,
        placeholder: isGlueBoard ? '选择胶板型号后自动带出' : '可选',
      },
    },
  ]);
}

function updateModelSchema(
  consumableType?: string,
  currentModel?: string,
  keepCurrentModel = false,
) {
  updateErpMaterialCodeSchema(consumableType);
  if (consumableType !== 'GLUE_BOARD') {
    formApi.updateSchema([
      {
        fieldName: 'model',
        component: 'Input',
        componentProps: { placeholder: '请输入型号' },
      },
    ]);
    return;
  }
  formApi.updateSchema([
    {
      fieldName: 'model',
      component: 'Select',
      componentProps: {
        allowClear: true,
        class: 'w-full',
        loading: glueBoardModelLoading.value,
        optionFilterProp: 'label',
        onChange: (value?: string) => {
          void syncGlueBoardMaterialCode(value, true);
        },
        options: glueBoardModelOptions.value,
        placeholder: '请选择胶板型号',
        showSearch: true,
      },
    },
  ]);
}

async function loadGlueBoardModelOptions(
  processCode?: string,
  currentModel?: string,
  keepCurrentModel = false,
) {
  const loadSeq = ++glueBoardModelLoadSeq;
  glueBoardModelLoading.value = true;
  updateModelSchema('GLUE_BOARD', currentModel, keepCurrentModel);
  try {
    const glueProcess = normalizeGlueProcess(processCode);
    const items = await getFinishedGlueBoardMapModelOptions(
      glueProcess ? { glueProcess } : undefined,
    );
    if (loadSeq !== glueBoardModelLoadSeq) return;
    glueBoardModelOptions.value = toGlueBoardModelOptions(
      items,
      currentModel,
      keepCurrentModel,
    );
  } catch {
    if (loadSeq !== glueBoardModelLoadSeq) return;
    glueBoardModelOptions.value = toGlueBoardModelOptions(
      [],
      currentModel,
      keepCurrentModel,
    );
    message.warning('胶板型号选项加载失败，请检查成品胶板对照表或权限');
  } finally {
    if (loadSeq === glueBoardModelLoadSeq) {
      glueBoardModelLoading.value = false;
      updateModelSchema('GLUE_BOARD', currentModel, keepCurrentModel);
      await syncGlueBoardMaterialCode(currentModel);
    }
  }
}

async function syncModelSchema(
  consumableType?: string,
  processCode?: string,
  currentModel?: string,
  keepCurrentModel = false,
) {
  if (consumableType !== 'GLUE_BOARD') {
    glueBoardModelLoadSeq += 1;
    updateModelSchema(consumableType, currentModel);
    return;
  }
  await loadGlueBoardModelOptions(processCode, currentModel, keepCurrentModel);
}

async function applyConsumableDefaults(
  consumableType?: string,
  overwrite = false,
) {
  const config = findProcessConfig(consumableType);
  if (!config) {
    return;
  }
  const values =
    (await formApi.getValues()) as MesHcToolingConsumableLedgerApi.Ledger;
  const nextValues: MesHcToolingConsumableLedgerApi.Ledger = {};
  if (
    consumableType !== 'GLUE_BOARD' &&
    config.defaultErpMaterialCode &&
    (overwrite || !values.erpMaterialCode)
  ) {
    nextValues.erpMaterialCode = config.defaultErpMaterialCode;
  }
  if (config.defaultBatchNo && (overwrite || !values.batchNo)) {
    nextValues.batchNo = config.defaultBatchNo;
  }
  if (config.defaultUomId && (overwrite || !values.uomId)) {
    nextValues.uomId = config.defaultUomId;
    nextValues.uomCode = config.defaultUomCode;
    nextValues.uomName = config.defaultUomName;
    nextValues.uom = config.defaultUomName || config.defaultUomCode;
  }
  if (Object.keys(nextValues).length > 0) {
    await formApi.setValues(nextValues);
  }
}

function defaultValues(data?: Partial<LedgerModalData>) {
  const processCode = data?.fixedProcessCode || data?.processCode;
  const consumableType =
    data?.consumableType ||
    processConfigs.value[0]?.consumableType ||
    defaultConsumableForProcess(processCode) ||
    'SANDPAPER';
  const config = findProcessConfig(consumableType);
  return {
    batchNo: data?.batchNo || config?.defaultBatchNo || '',
    consumableType,
    erpMaterialCode:
      data?.erpMaterialCode ||
      (consumableType === 'GLUE_BOARD' ? '' : config?.defaultErpMaterialCode) ||
      '',
    id: data?.id,
    model: data?.model || '',
    processCode: processCode || defaultProcessForConsumable(consumableType),
    receiveQty: data?.receiveQty ?? 0,
    receiveTime: data?.receiveTime || dayjs().format('YYYY-MM-DD HH:mm:ss'),
    receiverId: data?.receiverId || currentUserId.value,
    receiverName: data?.receiverName || currentUserName.value,
    remark: data?.remark || '',
    uom: data?.uom || config?.defaultUomName || config?.defaultUomCode || '',
    uomCode: data?.uomCode || config?.defaultUomCode,
    uomId: data?.uomId || config?.defaultUomId,
    uomName: data?.uomName || config?.defaultUomName,
  };
}

function applyProcessSchema(processCode?: string) {
  formApi.updateSchema([
    {
      fieldName: 'consumableType',
      componentProps: {
        allowClear: false,
        options: getConfigConsumableOptions(processCode),
        onChange: (value?: string) => {
          void (async () => {
            await applyConsumableDefaults(value, true);
            await formApi.setValues({ erpMaterialCode: '', model: undefined });
            const values =
              (await formApi.getValues()) as MesHcToolingConsumableLedgerApi.Ledger;
            await syncModelSchema(
              value,
              values.processCode || fixedProcessCode.value,
              values.model,
            );
          })();
        },
      },
    },
  ]);
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'w-[980px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values =
      (await formApi.getValues()) as MesHcToolingConsumableLedgerApi.Ledger;
    if (fixedProcessCode.value) {
      values.processCode = fixedProcessCode.value;
    }
    if (!isProcessAllowed(values.consumableType, values.processCode)) {
      message.warning(
        `当前耗材种类不能选择 ${processText(values.processCode)} 工序`,
      );
      return;
    }
    modalApi.lock();
    try {
      await (values.id
        ? updateToolingConsumableLedger(values)
        : createToolingConsumableLedger(values));
      await modalApi.close();
      emit('success');
      message.success('保存成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      fixedProcessCode.value = undefined;
      processConfigs.value = [];
      glueBoardModelLoadSeq += 1;
      glueBoardModelOptions.value = [];
      glueBoardModelLoading.value = false;
      await formApi.resetForm();
      updateModelSchema();
      return;
    }
    const data = modalApi.getData<LedgerModalData>();
    fixedProcessCode.value = data?.fixedProcessCode;
    await loadProcessConfigs(fixedProcessCode.value || data?.processCode);
    applyProcessSchema(fixedProcessCode.value);
    if (!data?.id) {
      formData.value = undefined;
      await formApi.resetForm();
      await formApi.setValues(defaultValues(data || {}));
      const values =
        (await formApi.getValues()) as MesHcToolingConsumableLedgerApi.Ledger;
      await applyConsumableDefaults(values.consumableType, false);
      await syncModelSchema(
        values.consumableType,
        values.processCode,
        values.model,
        true,
      );
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getToolingConsumableLedger(data.id);
      await formApi.resetForm();
      await formApi.setValues(
        defaultValues({
          ...formData.value,
          fixedProcessCode: fixedProcessCode.value,
        }),
      );
      const values =
        (await formApi.getValues()) as MesHcToolingConsumableLedgerApi.Ledger;
      await syncModelSchema(
        values.consumableType,
        values.processCode,
        values.model,
        true,
      );
    } finally {
      modalApi.unlock();
    }
  },
  showCancelButton: false,
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="px-2 pb-4">
      <Form />
    </div>
  </Modal>
</template>

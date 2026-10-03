<script setup lang="ts">
import type { Component } from 'vue';
import type { MesHcProductionReportApi } from '#/api/mes/hc/production-report';

import { nextTick, ref, shallowRef } from 'vue';
import { message } from 'ant-design-vue';
import { getPressSlotFormRecordPage } from '#/api/mes/hc/execution/press-slot-console';

type Form = MesHcProductionReportApi.FormRecord;
type Viewer = {
  openDetail?: (row: any) => void | Promise<void>;
  openRecord?: (row: { id: number }, mode: 'view') => Promise<void>;
};
const component = shallowRef<Component>();
const viewer = ref<Viewer>();
const processCode = ref('');
const processName = ref('');
const loading = ref(false);

const loaders: Record<string, () => Promise<{ default: Component }>> = {
  配料: () => import('../../../base/process-form-fill/formula/index.vue'),
  湿法: () => import('../../../base/process-form-fill/wet/index.vue'),
  磨皮: () => import('../../../base/process-form-fill/rough-grinding/index.vue'),
  粘胶1: () => import('../../../base/process-form-fill/adhesive1/index.vue'),
  粘胶2: () => import('../../../base/process-form-fill/adhesive2/index.vue'),
  压槽: () => import('../../../base/process-form-fill/press-slot/index.vue'),
  裁切: () => import('../../../base/process-form-fill/cut-round/index.vue'),
};

async function mountViewer(loader: () => Promise<{ default: Component }>) {
  component.value = undefined;
  await nextTick();
  component.value = (await loader()).default;
  await nextTick();
  if (!viewer.value) throw new Error('表单查看组件未加载，请重试');
  return viewer.value;
}

// 与表单填写菜单使用同一个详情接口和展示组件；只读模式不挂载列表、不提供导入操作。
async function open(form: Form, operationName: string, planNo?: string) {
  if (loading.value || !form.id) return;
  loading.value = true;
  processName.value = operationName || form.processName || '';
  processCode.value = form.processCode || '';
  try {
    const isProcess = form.sourceType === 'PROCESS_FORM';
    let context: Record<string, any> = {};
    try { context = JSON.parse(form.contextJson || '{}') || {}; } catch { /* 无历史绑定时按记录本身查看。 */ }
    if (operationName === '压槽' && planNo) {
      // 压槽菜单按业务表单聚合，必须按来源与记录 ID 匹配，不能拿不同表的同号记录。
      let pageNo = 1;
      let total = 0;
      do {
        const page = await getPressSlotFormRecordPage({ planNo, pageNo, pageSize: 100 });
        total = page.total;
        const row = page.list.find((item) => item.planNo === planNo && (
          isProcess
            ? Number(item.payload?.runtimeRecord?.id ?? item.payload?.record?.id) === form.id
              || (context.bindType === 'PRESS_SLOT_INTERMEDIATE_RECORD'
                && context.sourceProcess === 'PRESS_SLOT'
                && item.sourceType === 'INTERMEDIATE_RECORD'
                && item.sourceId === Number(context.sourceId))
            : item.sourceType === 'STATION_RECORD' && item.sourceId === form.id
        ));
        if (row) {
          await (await mountViewer(loaders.压槽!)).openDetail!(row);
          return;
        }
        pageNo += 1;
      } while ((pageNo - 1) * 100 < total);
    }
    const loader = loaders[operationName];
    if (loader && operationName !== '压槽' && (operationName !== '粘胶1' || !isProcess || form.mirrorRecordId)) {
      const stationMirror = operationName === '粘胶1' && isProcess;
      const adhesive2Intermediate = isProcess && operationName === '粘胶2'
        && context.bindType === 'ADHESIVE2_INTERMEDIATE_RECORD'
        && context.sourceProcess === 'ADHESIVE2' && Number(context.sourceId) > 0;
      await (await mountViewer(loader)).openDetail!({
        id: stationMirror ? form.mirrorRecordId : form.id,
        formCode: form.templateCode,
        formName: form.templateName,
        sourceKind: isProcess && !stationMirror ? 'process-form' : 'station-record',
        processRecordId: isProcess && !stationMirror ? form.id : undefined,
        planNo,
        batchNo: form.batchNo,
        modelCode: form.modelCode,
        operationName,
        ...(adhesive2Intermediate ? {
          adhesive2IntermediateRecordId: Number(context.sourceId),
          headerDataJson: JSON.stringify({
            recordSource: 'ADHESIVE2_INTERMEDIATE_BUSINESS',
            adhesive2IntermediateRecordId: Number(context.sourceId),
          }),
        } : {}),
      });
      return;
    }
    if (!isProcess) throw new Error('该工序的表单查看方式尚不支持，请从表单填写菜单查看');
    const instance = await mountViewer(() => import('../../../base/process-form-fill/shared/FillPage.vue'));
    await instance.openRecord!({ id: form.id }, 'view');
  } catch (error) {
    message.error(error instanceof Error ? error.message : '完整表单加载失败，请重试');
  } finally {
    loading.value = false;
  }
}

defineExpose({ open, loading });
</script>

<template>
  <component
    :is="component"
    v-if="component"
    ref="viewer"
    :viewer-only="true"
    :process-code="processCode"
    :process-name="processName"
  />
</template>

<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';

import { DocAlert, Page, useVbenModal } from '@vben/common-ui';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getProcessInstance,
  getProcessInstanceCopyPage,
} from '#/api/bpm/processInstance';
import { $t } from '#/locales';
import NcrDetailModal from '#/views/mes/quality/abnormal/ncr/modules/detail-modal.vue';

import BusinessFormModal from '../../processInstance/detail/modules/business-form-modal.vue';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'BpmCopyTask' });

const NCR_PROCESS_KEYS = [
  'qms_ncr_disposition',
  'qms_raw_material_ncr_disposition',
];
const NCR_FORM_PATHS = [
  '/mes/quality/abnormal/ncr',
  '/mes/quality/abnormal/raw-material-ncr',
];

/** 查看业务单据详情 */
async function handleBusinessFormDetail(
  row: BpmProcessInstanceApi.ProcessInstanceCopyRespVO,
) {
  const instance = await getProcessInstance(row.processInstanceId);
  if (tryOpenNcrDetail(instance)) {
    return;
  }
  businessFormModalApi
    .setData({
      activityId: row.activityId,
      businessKey: instance?.businessKey,
      formVariables: instance?.formVariables,
      processDefinition: instance?.processDefinition,
      processInstanceId: row.processInstanceId,
      processInstance: instance,
      taskId: row.taskId,
      title: `单据详情 - ${row.processInstanceName || row.processInstanceId}`,
    })
    .open();
}

function tryOpenNcrDetail(instance?: BpmProcessInstanceApi.ProcessInstance) {
  const businessKey =
    instance?.businessKey ||
    instance?.formVariables?.ncRecordId ||
    instance?.formVariables?.businessKey ||
    instance?.formVariables?.id;
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  const isNcr =
    NCR_PROCESS_KEYS.includes(processKey) ||
    NCR_PROCESS_KEYS.some((key) => processDefinitionId.startsWith(`${key}:`)) ||
    NCR_FORM_PATHS.includes(formPath);
  if (!isNcr || !businessKey) {
    return false;
  }
  ncrDetailModalApi.setData({ id: Number(businessKey) }).open();
  return true;
}

const [Grid] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getProcessInstanceCopyPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<BpmProcessInstanceApi.ProcessInstanceCopyRespVO>,
});

const [BusinessFormDetailModal, businessFormModalApi] = useVbenModal({
  connectedComponent: BusinessFormModal,
  destroyOnClose: true,
});

const [NcrDetailModalHost, ncrDetailModalApi] = useVbenModal({
  connectedComponent: NcrDetailModal,
  destroyOnClose: true,
});
</script>

<template>
  <Page auto-content-height>
    <BusinessFormDetailModal />
    <NcrDetailModalHost />
    <template #doc-disabled>
      <DocAlert
        title="审批转办、委派、抄送"
        url="https://doc.iocoder.cn/bpm/task-delegation-and-cc/"
      />
    </template>

    <Grid table-title="抄送任务">
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: $t('common.detail'),
              type: 'link',
              icon: ACTION_ICON.VIEW,
              auth: ['bpm:task:query'],
              onClick: handleBusinessFormDetail.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>

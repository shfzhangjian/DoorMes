<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import { computed, ref, watch } from 'vue';

import { Descriptions, Empty, Spin, Table, Tag } from 'ant-design-vue';

import {
  getExceptionEvent,
  getExceptionPage,
  type MesExceptionApi,
} from '#/api/mes/quality/abnormal/exception';
import DictTag from '#/components/dict-tag/dict-tag.vue';
import { QMS_EXCEPTION_DICT } from '#/views/mes/quality/abnormal/exception/data';

defineOptions({ name: 'QmsExceptionInlineDetail' });

const props = defineProps<{
  id?: number | string;
}>();

const loading = ref(false);
const record = ref<MesExceptionApi.ExceptionRecord>();

const containmentTasks = computed(() =>
  (record.value?.groupTasks || []).filter(
    (item) => item.taskType !== 'ROOT_CAUSE_PREVENTIVE',
  ),
);
const rootCauseTasks = computed(() =>
  (record.value?.groupTasks || []).filter(
    (item) => item.taskType === 'ROOT_CAUSE_PREVENTIVE',
  ),
);
const relationRows = computed(() => record.value?.relations || []);
const flowRows = computed(() => record.value?.flowLogs || []);

const containmentColumns: TableColumnsType = [
  { dataIndex: 'containmentDeptName', key: 'containmentDeptName', title: '部门', width: 130 },
  {
    customRender: ({ record: row }) => displayHandlers(row),
    key: 'handlers',
    title: '办理人',
    width: 180,
  },
  { dataIndex: 'containmentDeadline', key: 'containmentDeadline', title: '处理期限', width: 170 },
  { dataIndex: 'actualFinishTime', key: 'actualFinishTime', title: '填写日期', width: 170 },
  { dataIndex: 'actionDescription', key: 'actionDescription', title: '围堵措施' },
  {
    customRender: ({ record: row }) => displayAttachments(row.attachmentUrls),
    key: 'attachmentUrls',
    title: '附件',
    width: 90,
  },
  {
    customRender: ({ text }) => translateTaskStatus(text),
    dataIndex: 'taskStatus',
    key: 'taskStatus',
    title: '状态',
    width: 110,
  },
];

const rootCauseColumns: TableColumnsType = [
  { dataIndex: 'containmentDeptName', key: 'containmentDeptName', title: '责任单位', width: 130 },
  {
    customRender: ({ record: row }) => displayHandlers(row),
    key: 'handlers',
    title: '办理人',
    width: 180,
  },
  { dataIndex: 'actualFinishTime', key: 'actualFinishTime', title: '填写日期', width: 170 },
  { dataIndex: 'rootCauseCategory', key: 'rootCauseCategory', title: '4M1E', width: 100 },
  { dataIndex: 'rootCause', key: 'rootCause', title: '根因分析' },
  { dataIndex: 'preventiveAction', key: 'preventiveAction', title: '纠正预防措施与标准化' },
  {
    customRender: ({ record: row }) => displayAttachments(row.attachmentUrls),
    key: 'attachmentUrls',
    title: '附件',
    width: 90,
  },
  {
    customRender: ({ text }) => translateTaskStatus(text),
    dataIndex: 'taskStatus',
    key: 'taskStatus',
    title: '状态',
    width: 110,
  },
];

const relationColumns: TableColumnsType = [
  { dataIndex: 'relationType', key: 'relationType', title: '关系类型', width: 120 },
  { dataIndex: 'relatedObjectNo', key: 'relatedObjectNo', title: '关联单号', width: 170 },
  { dataIndex: 'relatedObjectName', key: 'relatedObjectName', title: '关联对象' },
  { dataIndex: 'relationStatus', key: 'relationStatus', title: '状态', width: 100 },
];

const flowColumns: TableColumnsType = [
  { dataIndex: 'actionName', key: 'actionName', title: '动作', width: 120 },
  { dataIndex: 'fromNodeName', key: 'fromNodeName', title: '原节点', width: 140 },
  { dataIndex: 'toNodeName', key: 'toNodeName', title: '目标节点', width: 140 },
  { dataIndex: 'handlerUserName', key: 'handlerUserName', title: '办理人', width: 110 },
  { dataIndex: 'handleTime', key: 'handleTime', title: '办理时间', width: 170 },
  { dataIndex: 'opinion', key: 'opinion', title: '办理意见' },
];

watch(
  () => props.id,
  () => {
    void loadDetail();
  },
  { immediate: true },
);

async function loadDetail() {
  const rawId = props.id;
  if (rawId === undefined || rawId === null || rawId === '') {
    record.value = undefined;
    return;
  }
  loading.value = true;
  try {
    const value = String(rawId);
    if (/^\d+$/.test(value)) {
      record.value = await getExceptionEvent(Number(value));
      return;
    }
    const page = await getExceptionPage({
      exceptionNo: value,
      pageNo: 1,
      pageSize: 1,
    });
    const first = page?.list?.[0];
    record.value = first?.id ? await getExceptionEvent(first.id) : first;
  } finally {
    loading.value = false;
  }
}

function displayValue(value: any) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (Array.isArray(value)) {
    return value.filter(Boolean).join('、') || '-';
  }
  return String(value);
}

function displayBoolean(value?: boolean) {
  if (value === undefined || value === null) {
    return '-';
  }
  return value ? '是' : '否';
}

function displayAttachments(urls?: string[]) {
  return urls?.length ? `有（${urls.length}）` : '-';
}

function displayHandlers(task: MesExceptionApi.GroupTask) {
  if (task.members?.length) {
    return task.members
      .map((member) => `${member.deptName || task.containmentDeptName || '-'}：${member.userName || '-'}`)
      .join('、');
  }
  return displayValue(task.executorUserName || task.rootCauseOwnerName || task.submitterUserName);
}

function translateTaskStatus(value?: string) {
  const map: Record<string, string> = {
    ACCEPTED: '已接收',
    COMPLETED: '已完成',
    DISPATCHED: '待填写',
    RETURNED: '已退回',
    SUBMITTED: '待会签',
  };
  return map[value || ''] || displayValue(value);
}
</script>

<template>
  <Spin :spinning="loading">
    <div v-if="record" class="qms-exception-inline-detail">
      <section class="qms-exception-inline-detail__section">
        <div class="qms-exception-inline-detail__title">
          <span>{{ displayValue(record.exceptionNo) }}</span>
          <DictTag
            v-if="record.status"
            :type="QMS_EXCEPTION_DICT.status"
            :value="record.status"
          />
        </div>
        <Descriptions :column="{ md: 3, sm: 1, xs: 1 }" bordered size="small">
          <Descriptions.Item label="发现部门">
            {{ displayValue(record.discoverDeptName) }}
          </Descriptions.Item>
          <Descriptions.Item label="发现时间">
            {{ displayValue(record.discoverTime) }}
          </Descriptions.Item>
          <Descriptions.Item label="发现人">
            {{ displayValue(record.discovererName) }}
          </Descriptions.Item>
          <Descriptions.Item label="异常类别">
            <DictTag
              v-if="record.exceptionType"
              :type="QMS_EXCEPTION_DICT.type"
              :value="record.exceptionType"
            />
            <span v-else>-</span>
          </Descriptions.Item>
          <Descriptions.Item label="异常等级">
            <DictTag
              v-if="record.exceptionLevel"
              :type="QMS_EXCEPTION_DICT.level"
              :value="record.exceptionLevel"
            />
            <span v-else>-</span>
          </Descriptions.Item>
          <Descriptions.Item label="当前节点">
            {{ displayValue(record.currentNodeName) }}
          </Descriptions.Item>
          <Descriptions.Item label="关联产品">
            {{ displayBoolean(record.isRelatedProduct) }}
          </Descriptions.Item>
          <Descriptions.Item :span="2" label="关联NCR">
            {{ displayValue(record.relatedNcrNo) }}
          </Descriptions.Item>
          <Descriptions.Item :span="3" label="异常描述">
            {{ displayValue(record.description) }}
          </Descriptions.Item>
        </Descriptions>
      </section>

      <section class="qms-exception-inline-detail__section">
        <div class="qms-exception-inline-detail__section-title">确认</div>
        <Descriptions :column="{ md: 3, sm: 1, xs: 1 }" bordered size="small">
          <Descriptions.Item label="确认部门">
            {{ displayValue(record.confirmDeptName) }}
          </Descriptions.Item>
          <Descriptions.Item label="确认时间">
            {{ displayValue(record.confirmTime) }}
          </Descriptions.Item>
          <Descriptions.Item label="确认人">
            {{ displayValue(record.confirmerName) }}
          </Descriptions.Item>
          <Descriptions.Item :span="3" label="围堵建议">
            {{ displayValue(record.containmentAction) }}
          </Descriptions.Item>
          <Descriptions.Item label="处理期限">
            {{ displayValue(record.containmentDeadline) }}
          </Descriptions.Item>
        </Descriptions>
      </section>

      <section class="qms-exception-inline-detail__section">
        <div class="qms-exception-inline-detail__section-title">
          临时小组围堵措施会签
        </div>
        <Table
          :columns="containmentColumns"
          :data-source="containmentTasks"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 1100 }"
          size="small"
        />
      </section>

      <section class="qms-exception-inline-detail__section">
        <div class="qms-exception-inline-detail__section-title">确认责任部门</div>
        <Descriptions :column="{ md: 3, sm: 1, xs: 1 }" bordered size="small">
          <Descriptions.Item :span="3" label="最终围堵措施">
            {{ displayValue(record.containmentAction) }}
          </Descriptions.Item>
          <Descriptions.Item label="责任部门">
            {{ displayValue(record.actionDeptName) }}
          </Descriptions.Item>
          <Descriptions.Item label="责任人">
            {{ displayValue(record.actionOwnerName) }}
          </Descriptions.Item>
          <Descriptions.Item label="完成时间">
            {{ displayValue(record.containmentFinishTime) }}
          </Descriptions.Item>
        </Descriptions>
      </section>

      <section class="qms-exception-inline-detail__section">
        <div class="qms-exception-inline-detail__section-title">
          根因分析和纠正预防措施与标准化
        </div>
        <Descriptions :column="{ md: 2, sm: 1, xs: 1 }" bordered size="small">
          <Descriptions.Item label="4M1E">
            {{ displayValue(record.rootCauseCategory) }}
          </Descriptions.Item>
          <Descriptions.Item label="责任部门">
            {{ displayValue(record.actionDeptName) }}
          </Descriptions.Item>
          <Descriptions.Item :span="2" label="根因分析">
            {{ displayValue(record.rootCause) }}
          </Descriptions.Item>
          <Descriptions.Item :span="2" label="纠正预防措施与标准化">
            {{ displayValue(record.preventiveAction) }}
          </Descriptions.Item>
        </Descriptions>
        <Table
          class="qms-exception-inline-detail__table"
          :columns="rootCauseColumns"
          :data-source="rootCauseTasks"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 1200 }"
          size="small"
        />
      </section>

      <section class="qms-exception-inline-detail__section">
        <div class="qms-exception-inline-detail__section-title">
          品质部门闭环确认
        </div>
        <Descriptions :column="{ md: 3, sm: 1, xs: 1 }" bordered size="small">
          <Descriptions.Item :span="3" label="闭环确认">
            {{ displayValue(record.effectConfirm) }}
          </Descriptions.Item>
          <Descriptions.Item label="是否有效">
            <Tag :color="record.qaConfirmValid ? 'success' : 'default'">
              {{ record.qaConfirmValid ? '有效' : '-' }}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="确认人">
            {{ displayValue(record.qaConfirmerName || record.closeUserName) }}
          </Descriptions.Item>
          <Descriptions.Item label="确认日期">
            {{ displayValue(record.finishTime) }}
          </Descriptions.Item>
        </Descriptions>
      </section>

      <section
        v-if="relationRows.length > 0"
        class="qms-exception-inline-detail__section"
      >
        <div class="qms-exception-inline-detail__section-title">关联单据</div>
        <Table
          :columns="relationColumns"
          :data-source="relationRows"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 760 }"
          size="small"
        />
      </section>

      <section class="qms-exception-inline-detail__section">
        <div class="qms-exception-inline-detail__section-title">业务流转记录</div>
        <Table
          :columns="flowColumns"
          :data-source="flowRows"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 900 }"
          size="small"
        />
      </section>
    </div>
    <Empty v-else description="暂无异常事件单据详情" />
  </Spin>
</template>

<style lang="scss" scoped>
.qms-exception-inline-detail {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 14px;
}

.qms-exception-inline-detail__section {
  min-width: 0;
}

.qms-exception-inline-detail__title,
.qms-exception-inline-detail__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #172033;
  font-weight: 700;
}

.qms-exception-inline-detail__title {
  font-size: 16px;
}

.qms-exception-inline-detail__table {
  margin-top: 10px;
}

.qms-exception-inline-detail :deep(.ant-descriptions-item-label) {
  width: 116px;
  color: #536176;
  font-weight: 600;
}

.qms-exception-inline-detail :deep(.ant-table) {
  font-size: 13px;
}
</style>

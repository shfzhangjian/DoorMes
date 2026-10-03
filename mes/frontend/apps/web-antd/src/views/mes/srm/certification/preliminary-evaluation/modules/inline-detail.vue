<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { SrmPreliminaryEvaluationApi } from '#/api/mes/srm/preliminary-evaluation';

import { computed, ref, watch } from 'vue';

import { Descriptions, Empty, Spin, Table, Tag } from 'ant-design-vue';

import { getEvaluation } from '#/api/mes/srm/preliminary-evaluation';

defineOptions({ name: 'SrmPreliminaryEvaluationInlineDetail' });

const props = withDefaults(
  defineProps<{
    hideScoringOwner?: boolean;
    id?: number | string;
    maskSensitiveFields?: boolean;
    showFlowLog?: boolean;
  }>(),
  {
    hideScoringOwner: false,
    id: undefined,
    maskSensitiveFields: false,
    showFlowLog: true,
  },
);

const loading = ref(false);
const record = ref<SrmPreliminaryEvaluationApi.Evaluation>();

const itemRows = computed(() => record.value?.items || []);
const logRows = computed(() => record.value?.logs || []);

const itemColumns = computed<TableColumnsType>(() => {
  const columns: TableColumnsType = [
    {
      dataIndex: 'groupNameSnapshot',
      key: 'groupNameSnapshot',
      title: '维度',
      width: 120,
    },
    {
      dataIndex: 'indicatorNameSnapshot',
      key: 'indicatorNameSnapshot',
      title: '指标',
      width: 180,
    },
    {
      dataIndex: 'scoringRuleSnapshot',
      key: 'scoringRuleSnapshot',
      title: '评分规则',
      width: 320,
    },
    {
      dataIndex: 'maxScoreSnapshot',
      key: 'maxScoreSnapshot',
      title: '满分',
      width: 80,
    },
    {
      customRender: ({ record: row }) =>
        displayVeto(row as SrmPreliminaryEvaluationApi.Item),
      dataIndex: 'veto',
      key: 'veto',
      title: '否决线',
      width: 100,
    },
  ];
  if (!props.hideScoringOwner) {
    columns.push(
      {
        dataIndex: 'defaultDeptNamesSnapshot',
        key: 'defaultDeptNamesSnapshot',
        title: '评分部门',
        width: 120,
      },
      {
        customRender: ({ record: row }) =>
          displayValue(
            (row as SrmPreliminaryEvaluationApi.Item).scorerUserNameDisplay ||
              (row as SrmPreliminaryEvaluationApi.Item).scorerUserName,
          ),
        dataIndex: 'scorerUserNameDisplay',
        key: 'scorerUserNameDisplay',
        title: '评分人',
        width: 120,
      },
    );
  }
  columns.push(
    {
      customRender: ({ record: row }) =>
        displayValue(
          (row as SrmPreliminaryEvaluationApi.Item).actualScoreDisplay ||
            (row as SrmPreliminaryEvaluationApi.Item).actualScore,
        ),
      dataIndex: 'actualScoreDisplay',
      key: 'actualScoreDisplay',
      title: '实际得分',
      width: 100,
    },
    {
      customRender: ({ record: row }) =>
        displayValue(
          (row as SrmPreliminaryEvaluationApi.Item).scoringDescriptionDisplay ||
            (row as SrmPreliminaryEvaluationApi.Item).scoringDescription,
        ),
      dataIndex: 'scoringDescriptionDisplay',
      key: 'scoringDescriptionDisplay',
      title: '评分说明',
      width: 260,
    },
    {
      customRender: ({ text }) => scoreStatusText(String(text || '')),
      dataIndex: 'scoreStatus',
      key: 'scoreStatus',
      title: '状态',
      width: 90,
    },
  );
  return columns;
});

const logColumns: TableColumnsType = [
  { dataIndex: 'createTime', key: 'createTime', title: '时间', width: 170 },
  {
    dataIndex: 'operatorName',
    key: 'operatorName',
    title: '操作人',
    width: 120,
  },
  {
    customRender: ({ text }) => flowActionText(String(text || '')),
    dataIndex: 'action',
    key: 'action',
    title: '动作',
    width: 120,
  },
  {
    customRender: ({ record: row }) => {
      const log = row as SrmPreliminaryEvaluationApi.Log;
      return `${statusText(log.fromStatus)} → ${statusText(log.toStatus)}`;
    },
    dataIndex: 'statusChange',
    key: 'statusChange',
    title: '状态变化',
    width: 190,
  },
  { dataIndex: 'actionDescription', key: 'actionDescription', title: '说明' },
];

watch(
  () => props.id,
  () => {
    void loadDetail();
  },
  { immediate: true },
);

async function loadDetail() {
  const id = Number(props.id || 0);
  if (!id) {
    record.value = undefined;
    return;
  }
  loading.value = true;
  try {
    record.value = await getEvaluation(id);
  } finally {
    loading.value = false;
  }
}

function displayValue(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function displaySensitiveValue(value: unknown) {
  return props.maskSensitiveFields ? '*' : displayValue(value);
}

function statusText(status?: string) {
  return (
    (
      {
        DRAFT: '草稿',
        GM_REVIEW: '总经理办理',
        PENDING_CALCULATION: '待计算得分',
        PENDING_DECISION: '待最终判定',
        PENDING_PUBLISH: '待发布',
        PUBLISHED: '已发布',
        SCORING: '评分中',
      } as Record<string, string>
    )[status || ''] ||
    status ||
    '-'
  );
}

function statusColor(status?: string) {
  return (
    (
      {
        DRAFT: 'default',
        GM_REVIEW: 'purple',
        PENDING_CALCULATION: 'cyan',
        PENDING_DECISION: 'gold',
        PENDING_PUBLISH: 'orange',
        PUBLISHED: 'success',
        SCORING: 'processing',
      } as Record<string, string>
    )[status || ''] || 'default'
  );
}

function decisionText(value?: string) {
  return (
    (
      {
        QUALIFIED: '合格',
        UNQUALIFIED: '不合格',
      } as Record<string, string>
    )[value || ''] || '-'
  );
}

function decisionColor(value?: string) {
  if (value === 'QUALIFIED') {
    return 'success';
  }
  if (value === 'UNQUALIFIED') {
    return 'error';
  }
  return 'default';
}

function scoreStatusText(value?: string) {
  return value === 'COMPLETED' ? '已评分' : '待评分';
}

function displayVeto(item: SrmPreliminaryEvaluationApi.Item) {
  if (!item.vetoOperatorSnapshot || item.vetoScoreSnapshot === undefined) {
    return '-';
  }
  const operator = (
    { EQ: '=', GE: '≥', GT: '>', LE: '≤', LT: '<' } as Record<string, string>
  )[item.vetoOperatorSnapshot];
  return `${operator || item.vetoOperatorSnapshot}${item.vetoScoreSnapshot}`;
}

function flowActionText(action?: string) {
  return (
    (
      {
        ALL_SCORED: '全部评分完成',
        ASSIGN_SCORER: '维护评分人',
        CALCULATE: '计算得分',
        CREATE: '创建',
        DECISION: '最终判定',
        GM_OPINION: '总经理意见',
        PUBLISH: '发布',
        SCORE: '提交评分',
        SEND: '确认发送',
        SEND_GM: '发送总经理',
        UPDATE: '更新',
      } as Record<string, string>
    )[String(action || '').toUpperCase()] ||
    action ||
    '-'
  );
}
</script>

<template>
  <Spin :spinning="loading">
    <div v-if="record" class="srm-pre-eval-inline-detail">
      <section class="srm-pre-eval-inline-detail__section">
        <div class="srm-pre-eval-inline-detail__title">
          <span>{{ record.evaluationNo || '供应商选择初评' }}</span>
          <Tag :color="statusColor(record.status)">
            {{ statusText(record.status) }}
          </Tag>
        </div>
        <Descriptions :column="{ md: 2, sm: 1, xs: 1 }" bordered size="small">
          <Descriptions.Item label="供应商编号">
            {{ displayValue(record.supplierCode) }}
          </Descriptions.Item>
          <Descriptions.Item label="供应商名称">
            {{ displayValue(record.supplierName) }}
          </Descriptions.Item>
          <Descriptions.Item label="初评项目">
            {{
              displayValue(
                [record.projectCode, record.projectName]
                  .filter(Boolean)
                  .join(' / '),
              )
            }}
          </Descriptions.Item>
          <Descriptions.Item label="评估模板">
            {{
              displayValue(
                [record.templateCodeSnapshot, record.templateNameSnapshot]
                  .filter(Boolean)
                  .join(' / '),
              )
            }}
          </Descriptions.Item>
          <Descriptions.Item label="模板版本">
            {{ displayValue(record.templateVersionSnapshot) }}
          </Descriptions.Item>
          <Descriptions.Item label="满分/合格线">
            {{ displayValue(record.totalScoreBaseline) }} /
            {{ displayValue(record.qualificationScoreSnapshot) }}
          </Descriptions.Item>
          <Descriptions.Item label="最终得分">
            {{ displayValue(record.totalScoreDisplay || record.totalScore) }}
          </Descriptions.Item>
          <Descriptions.Item label="发起人">
            {{ displayValue(record.initiatorName) }}
          </Descriptions.Item>
          <Descriptions.Item label="发起时间">
            {{ displayValue(record.sendTime || record.createTime) }}
          </Descriptions.Item>
        </Descriptions>
      </section>

      <section class="srm-pre-eval-inline-detail__section">
        <div class="srm-pre-eval-inline-detail__section-title">判定结果</div>
        <Descriptions :column="{ md: 2, sm: 1, xs: 1 }" bordered size="small">
          <Descriptions.Item label="自动判定">
            <Tag :color="decisionColor(record.autoDecision)">
              {{ decisionText(record.autoDecision) }}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="最终判定">
            <Tag :color="decisionColor(record.finalDecision)">
              {{ decisionText(record.finalDecision) }}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="总经理">
            {{ displaySensitiveValue(record.generalManagerUserName) }}
          </Descriptions.Item>
          <Descriptions.Item label="总经理办理时间">
            {{ displaySensitiveValue(record.generalManagerHandleTime) }}
          </Descriptions.Item>
          <Descriptions.Item :span="2" label="最终判定说明">
            {{ displaySensitiveValue(record.finalDescription) }}
          </Descriptions.Item>
          <Descriptions.Item :span="2" label="总经理意见">
            {{
              displaySensitiveValue(
                record.generalManagerOpinionDisplay ||
                  record.generalManagerOpinion,
              )
            }}
          </Descriptions.Item>
        </Descriptions>
      </section>

      <section class="srm-pre-eval-inline-detail__section">
        <div class="srm-pre-eval-inline-detail__section-title">评估指标</div>
        <Table
          :columns="itemColumns"
          :data-source="itemRows"
          bordered
          :pagination="false"
          row-key="id"
          :scroll="{ x: 1410 }"
          size="small"
        />
      </section>

      <section
        v-if="props.showFlowLog"
        class="srm-pre-eval-inline-detail__section"
      >
        <div class="srm-pre-eval-inline-detail__section-title">流程日志</div>
        <Table
          :columns="logColumns"
          :data-source="logRows"
          bordered
          :pagination="false"
          row-key="id"
          :scroll="{ x: 760 }"
          size="small"
        />
      </section>
      <section v-else class="srm-pre-eval-inline-detail__section">
        <div class="srm-pre-eval-inline-detail__section-title">流程日志</div>
        <div class="srm-pre-eval-inline-detail__masked-tip">
          流程日志已按资源权限脱敏
        </div>
      </section>
    </div>
    <Empty v-else description="暂无选择初评业务详情" />
  </Spin>
</template>

<style lang="scss" scoped>
.srm-pre-eval-inline-detail {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 14px;
  font-size: 14px;
  line-height: 20px;
}

.srm-pre-eval-inline-detail__section {
  min-width: 0;
  border: 1px solid #dfe6f1;
  border-radius: 10px;
  background: #fff;
  padding: 12px;
}

.srm-pre-eval-inline-detail__title,
.srm-pre-eval-inline-detail__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #172033;
  font-weight: 700;
}

.srm-pre-eval-inline-detail__title {
  font-size: 17px;
}

.srm-pre-eval-inline-detail__section-title {
  font-size: 15px;
}

.srm-pre-eval-inline-detail__masked-tip {
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 14px;
  line-height: 22px;
  padding: 18px 20px;
  text-align: center;
}

.srm-pre-eval-inline-detail :deep(.ant-descriptions-item-label) {
  width: 116px;
  color: #536176;
  font-size: 14px;
  font-weight: 600;
}

.srm-pre-eval-inline-detail :deep(.ant-descriptions-item-content),
.srm-pre-eval-inline-detail :deep(.ant-table) {
  font-size: 14px;
}

.srm-pre-eval-inline-detail :deep(.ant-table-cell) {
  line-height: 20px;
}
</style>

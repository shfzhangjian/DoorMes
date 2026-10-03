<script lang="ts" setup>
import type { MesEightDApi } from '#/api/mes/quality/abnormal/eightD';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { getDictOptions } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Checkbox,
  DatePicker,
  Form,
  Input,
  Select,
  message,
} from 'ant-design-vue';

import {
  closeEightDReport,
  createEightDReport,
  doneEightDActionItem,
  getEightDReport,
  handleEightDReport,
  returnEightDReport,
  updateEightDReport,
} from '#/api/mes/quality/abnormal/eightD';
import { FileUpload } from '#/components/upload';

import { QMS_8D_ACTION_ITEM_TYPE_OPTIONS, QMS_8D_DICT } from '../data';

const emit = defineEmits(['success']);

const SOURCE_TYPE_OPTIONS = [
  { label: '异常事件', value: 'EXCEPTION' },
  { label: '不合格处置(NCR)', value: 'NCR' },
  { label: '客户投诉', value: 'CUSTOMER_COMPLAINT' },
  { label: '审核问题', value: 'AUDIT' },
  { label: '手工立案', value: 'MANUAL' },
];
const MEMBER_ROLE_OPTIONS = [
  { label: '支持者', value: 'CHAMPION' },
  { label: '组长', value: 'LEADER' },
  { label: '成员', value: 'MEMBER' },
];
const ROOT_CAUSE_CATEGORY_OPTIONS = [
  { label: '人', value: 'MAN' },
  { label: '机', value: 'MACHINE' },
  { label: '料', value: 'MATERIAL' },
  { label: '法', value: 'METHOD' },
  { label: '环', value: 'ENVIRONMENT' },
];
const ACTION_ITEM_STATUS_OPTIONS = [
  { label: '待执行', value: 'TODO' },
  { label: '执行中', value: 'DOING' },
  { label: '已完成', value: 'DONE' },
  { label: '已验证', value: 'VERIFIED' },
  { label: '已取消', value: 'CANCELLED' },
];
const STEP_OPTIONS = [
  { label: 'D1-D2 团队与问题定义', value: 'D1_D2' },
  { label: 'D3 临时围堵措施', value: 'D3' },
  { label: 'D4 根本原因分析', value: 'D4' },
  { label: 'D5-D6 永久对策执行', value: 'D5_D6' },
  { label: 'D7-D8 标准化与结案', value: 'D7_D8' },
  { label: '流程结束(归档)', value: 'CLOSED' },
];
const ATTACHMENT_RELATION_TYPE = 'ATTACHMENT';
const attachmentAcceptTypes = [
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'png',
  'jpg',
  'jpeg',
  'zip',
  'rar',
];

const isNew = ref(false);
const formRef = ref();
const formData = ref<MesEightDApi.EightDRecord>({});
const attachmentUrls = ref<string[]>([]);

const canOperate = computed(
  () => isNew.value || formData.value.tabType === 'todo',
);
const canReturn = computed(
  () => !isNew.value && canOperate.value && formData.value.status !== 'PASSED',
);
const canClose = computed(
  () =>
    !isNew.value &&
    canOperate.value &&
    formData.value.currentStep === 'D7_D8' &&
    formData.value.status !== 'PASSED',
);
const disabled = computed(() => !canOperate.value);
const sourceTypeOptions = computed(() =>
  getDictOptionsOrFallback(QMS_8D_DICT.sourceType, SOURCE_TYPE_OPTIONS),
);
const memberRoleOptions = computed(() =>
  getDictOptionsOrFallback(QMS_8D_DICT.memberRole, MEMBER_ROLE_OPTIONS),
);
const rootCauseCategoryOptions = computed(() =>
  getDictOptionsOrFallback(
    QMS_8D_DICT.rootCauseCategory,
    ROOT_CAUSE_CATEGORY_OPTIONS,
  ),
);
const actionItemStatusOptions = computed(() =>
  getDictOptionsOrFallback(
    QMS_8D_DICT.actionItemStatus,
    ACTION_ITEM_STATUS_OPTIONS,
  ),
);
const stepOptions = computed(() =>
  getDictOptionsOrFallback(QMS_8D_DICT.step, STEP_OPTIONS),
);
const currentStepLabel = computed(() =>
  getOptionLabel(stepOptions.value, formData.value.currentStep),
);
const sourceTypeLabel = computed(() =>
  getOptionLabel(sourceTypeOptions.value, formData.value.sourceType),
);

function getDictOptionsOrFallback(
  dictType: string,
  fallbackOptions: Array<{ label: string; value: string }>,
) {
  const options = getDictOptions(dictType, 'string');
  return options.length > 0 ? options : fallbackOptions;
}

const [Modal, modalApi] = useVbenModal({
  title: '',
  class: 'qms-abnormal-workbench-modal qms-8d-erp-modal',
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = modalApi.getData<MesEightDApi.EightDRecord & { isNew?: boolean }>() || {};
    isNew.value = !!data.isNew;
    attachmentUrls.value = [];
    if (isNew.value) {
      formData.value = createDefaultRecord();
      return;
    }
    if (data.id) {
      await loadDetail(data.id, data.tabType);
    } else {
      formData.value = { ...data };
      syncAttachmentUrls(data.relations);
    }
  },
});

function createDefaultRecord(): MesEightDApi.EightDRecord {
  return {
    sourceType: 'MANUAL',
    sourceNo: '',
    issueDate: today(),
    targetDate: offsetDate(30),
    currentStep: 'D1_D2',
    status: 'APPROVING',
    updateSop: false,
    updateFmea: false,
    updateControlPlan: false,
    teamMembers: [
      {
        memberRole: 'LEADER',
        deptName: '品质部',
        userName: '当前用户',
        responsibility: '8D推进',
        sort: 1,
      },
    ],
    actionItems: [],
    relations: [],
    flowLogs: [],
  };
}

async function loadDetail(id: number, tabType?: string) {
  const detail = await getEightDReport(id);
  formData.value = {
    ...detail,
    tabType,
    flowOpinion: '',
    teamMembers: detail.teamMembers || [],
    actionItems: detail.actionItems || [],
    relations: detail.relations || [],
    flowLogs: detail.flowLogs || [],
  };
  syncAttachmentUrls(formData.value.relations);
}

function today() {
  return formatLocalDate(new Date());
}

function offsetDate(days: number) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return formatLocalDate(date);
}

function formatLocalDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getOptionLabel(
  options: Array<{ label: string; value: string }>,
  value?: string,
) {
  if (!value) {
    return '-';
  }
  return options.find((item) => item.value === value)?.label || value;
}

function handleCloseModal() {
  modalApi.close();
}

async function handleSubmit() {
  if (isNew.value) {
    await formRef.value?.validate();
  }
  if (!isNew.value && !formData.value.id) {
    message.warning('缺少 8D 报告ID，无法提交');
    return;
  }
  modalApi.setState({ loading: true });
  try {
    if (isNew.value) {
      await createEightDReport(buildEightDPayload());
      message.success('8D 报告已立案');
    } else {
      await handleEightDReport({
        ...(buildEightDPayload() as MesEightDApi.HandleReq),
        id: formData.value.id!,
        opinion: formData.value.flowOpinion,
      });
      message.success('8D 当前阶段已提交');
    }
    emit('success');
    modalApi.close();
  } finally {
    modalApi.setState({ loading: false });
  }
}

function addTeamMember() {
  formData.value.teamMembers = [
    ...(formData.value.teamMembers || []),
    { memberRole: 'MEMBER', deptName: '', userName: '', sort: 0 },
  ];
}

function removeTeamMember(index: number) {
  formData.value.teamMembers?.splice(index, 1);
}

function addActionItem() {
  formData.value.actionItems = [
    ...(formData.value.actionItems || []),
    {
      actionType: 'CORRECTIVE',
      actionDesc: '',
      itemStatus: 'TODO',
      ownerUserName: '',
      planFinishDate: formData.value.targetDate || offsetDate(7),
      sort: (formData.value.actionItems || []).length + 1,
    },
  ];
}

function removeActionItem(index: number) {
  formData.value.actionItems?.splice(index, 1);
}

async function handleSaveDraft() {
  if (isNew.value || !formData.value.id) {
    await formRef.value?.validate();
  }
  if (!formData.value.id) {
    await createEightDReport(buildEightDPayload());
    message.success('8D 报告已立案');
    emit('success');
    modalApi.close();
    return;
  }
  await updateEightDReport(buildEightDPayload());
  await loadDetail(formData.value.id, formData.value.tabType);
  message.success('保存成功');
  emit('success');
}

async function handleReturn() {
  if (!formData.value.id || !formData.value.flowOpinion) {
    message.warning('请先填写流转意见');
    return;
  }
  await updateEightDReport(buildEightDPayload());
  await returnEightDReport({
    id: formData.value.id,
    opinion: formData.value.flowOpinion,
    targetStep: formData.value.currentStep,
  });
  message.success('已退回');
  emit('success');
  modalApi.close();
}

async function handleClose() {
  if (!formData.value.id) {
    return;
  }
  await updateEightDReport(buildEightDPayload());
  await closeEightDReport({
    id: formData.value.id,
    opinion: formData.value.flowOpinion,
    standardizeDesc: formData.value.standardizeDesc,
    validationResult: formData.value.validationResult,
  });
  message.success('8D 已关闭');
  emit('success');
  modalApi.close();
}

async function handleActionDone(item: MesEightDApi.ActionItem) {
  if (!item.id) {
    message.warning('请先保存行动项');
    return;
  }
  if (!item.finishDesc) {
    message.warning('请填写完成说明');
    return;
  }
  await doneEightDActionItem({
    itemId: item.id,
    finishDesc: item.finishDesc,
    itemStatus: item.itemStatus === 'VERIFIED' ? 'VERIFIED' : 'DONE',
    verificationResult: item.verificationResult,
  });
  await loadDetail(formData.value.id!, formData.value.tabType);
  message.success('行动项已完成');
  emit('success');
}

function syncAttachmentUrls(relations?: MesEightDApi.Relation[]) {
  attachmentUrls.value = (relations || [])
    .filter((relation) => relation.relationType === ATTACHMENT_RELATION_TYPE)
    .map((relation) => relation.remark || relation.relatedObjectNo || '')
    .filter(Boolean);
}

function buildEightDPayload() {
  return {
    ...formData.value,
    relations: buildRelationsPayload(),
  } as MesEightDApi.EightDRecord;
}

function buildRelationsPayload() {
  const sourceRelations = (formData.value.relations || []).filter(
    (relation) => relation.relationType !== ATTACHMENT_RELATION_TYPE,
  );
  return [...sourceRelations, ...buildAttachmentRelations(attachmentUrls.value)];
}

function buildAttachmentRelations(urls: string[]) {
  return urls.filter(Boolean).map((url, index) => ({
    primaryFlag: false,
    relationStatus: 'ACTIVE',
    relationType: ATTACHMENT_RELATION_TYPE,
    relatedObjectName: getAttachmentName(url, index),
    relatedObjectNo: url,
    remark: url,
  }));
}

function getAttachmentName(url: string, index: number) {
  const cleanUrl = String(url || '').split('?')[0] || '';
  const fileName = cleanUrl.split('/').pop();
  if (!fileName) {
    return `附件${index + 1}`;
  }
  try {
    return decodeURIComponent(fileName);
  } catch {
    return fileName;
  }
}

function handleAttachmentPreview(file: any) {
  const url =
    file?.url ||
    file?.response?.url ||
    file?.response?.data ||
    file?.response ||
    '';
  if (url) {
    window.open(String(url), '_blank');
  }
}
</script>

<template>
  <Modal>
    <div class="qms-8d-detail">
      <div class="qms-8d-toolbar">
        <div class="qms-8d-toolbar__placeholder"></div>
        <div class="qms-8d-title-panel">
          <div class="qms-8d-title-panel__name">8D 改善报告（CAPA）</div>
          <div class="qms-8d-title-panel__subtitle">
            <div>单号：{{ formData.reportNo || '新建中' }}</div>
            <div>来源：{{ sourceTypeLabel }} / {{ formData.sourceNo || '-' }}</div>
            <div>节点：{{ formData.currentNodeName || currentStepLabel }}</div>
            <div>处理人：{{ formData.currentHandlerUserName || '-' }}</div>
          </div>
        </div>
        <div class="qms-8d-toolbar__actions">
          <Button
            v-if="canOperate"
            class="qms-8d-toolbar-action"
            size="small"
            type="primary"
            @click="handleSubmit"
          >
            {{ isNew ? '立案' : '提交' }}
          </Button>
          <Button
            v-if="canOperate"
            class="qms-8d-toolbar-action"
            size="small"
            @click="handleSaveDraft"
          >
            保存
          </Button>
          <Button
            v-if="canReturn"
            class="qms-8d-toolbar-action"
            danger
            size="small"
            @click="handleReturn"
          >
            退回
          </Button>
          <Button
            v-if="canClose"
            class="qms-8d-toolbar-action"
            size="small"
            type="primary"
            @click="handleClose"
          >
            关闭
          </Button>
          <Button
            class="qms-8d-toolbar-action"
            size="small"
            @click="handleCloseModal"
          >
            退出
          </Button>
        </div>
      </div>

      <Form
        ref="formRef"
        class="qms-8d-workbench"
        :model="formData"
        layout="vertical"
        :disabled="disabled"
      >
        <section class="qms-8d-form-fieldset">
          <div class="qms-8d-form-legend">一、D0 来源与准备</div>
          <div class="qms-8d-form-grid">
            <label>来源类别</label>
            <div class="qms-8d-form-control">
              <Form.Item
                class="qms-8d-form-item"
                name="sourceType"
                :rules="[{ required: true, message: '请选择来源类别' }]"
              >
                <Select
                  v-model:value="formData.sourceType"
                  placeholder="请选择来源类别"
                  :options="sourceTypeOptions"
                />
              </Form.Item>
            </div>
            <label>追溯凭证号</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-3">
              <Form.Item
                class="qms-8d-form-item"
                name="sourceNo"
                :rules="[{ required: true, message: '请输入追溯凭证号' }]"
              >
                <Input
                  v-model:value="formData.sourceNo"
                  placeholder="请输入异常/NCR/客诉/审核单号；手工立案请填写自定义追溯号"
                />
              </Form.Item>
            </div>
            <label>立案日期</label>
            <div class="qms-8d-form-control">
              <DatePicker
                v-model:value="formData.issueDate"
                class="w-full"
                format="YYYY-MM-DD"
                placeholder="请选择立案日期"
                value-format="YYYY-MM-DD"
              />
            </div>
            <label>要求结案日</label>
            <div class="qms-8d-form-control">
              <Form.Item
                class="qms-8d-form-item"
                name="targetDate"
                :rules="[{ required: true, message: '请选择要求结案日' }]"
              >
                <DatePicker
                  v-model:value="formData.targetDate"
                  class="w-full"
                  format="YYYY-MM-DD"
                  placeholder="请选择要求结案日"
                  value-format="YYYY-MM-DD"
                />
              </Form.Item>
            </div>
            <label>当前处理人</label>
            <div class="qms-8d-form-control">
              <Input
                v-model:value="formData.currentHandlerUserName"
                placeholder="当前处理人"
              />
            </div>
            <label>下一处理人</label>
            <div class="qms-8d-form-control">
              <Input
                v-model:value="formData.nextHandlerUserName"
                placeholder="提交时可指定下一处理人"
              />
            </div>
          </div>
        </section>

        <section class="qms-8d-form-fieldset">
          <div class="qms-8d-form-legend">二、D1-D2 团队与问题定义</div>
          <div class="qms-8d-subtable-title">
            <span>D1 核心团队</span>
            <Button v-if="!disabled" size="small" type="primary" @click="addTeamMember">
              <IconifyIcon icon="lucide:plus" />
              添加成员
            </Button>
          </div>
          <div class="qms-8d-table-wrap">
            <table class="qms-8d-table">
              <thead>
                <tr>
                  <th>角色</th>
                  <th>部门</th>
                  <th>姓名</th>
                  <th>职责</th>
                  <th v-if="!disabled" class="w-16">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(member, index) in formData.teamMembers" :key="index">
                  <td>
                    <Select
                      v-model:value="member.memberRole"
                      class="w-full"
                      placeholder="请选择角色"
                      size="small"
                      :options="memberRoleOptions"
                    />
                  </td>
                  <td>
                    <Input v-model:value="member.deptName" placeholder="部门" size="small" />
                  </td>
                  <td>
                    <Input v-model:value="member.userName" placeholder="姓名" size="small" />
                  </td>
                  <td>
                    <Input
                      v-model:value="member.responsibility"
                      placeholder="职责"
                      size="small"
                    />
                  </td>
                  <td v-if="!disabled" class="text-center">
                    <Button size="small" danger type="link" @click="removeTeamMember(index)">
                      移除
                    </Button>
                  </td>
                </tr>
                <tr v-if="!formData.teamMembers?.length">
                  <td :colspan="disabled ? 4 : 5" class="qms-8d-empty-cell">
                    暂无团队成员
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="qms-8d-form-grid qms-8d-form-grid--mt">
            <label class="qms-8d-form-label--tall">D2 问题描述</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-7">
              <Form.Item
                class="qms-8d-form-item"
                name="problemDesc"
                :rules="[{ required: true, message: '请填写问题描述' }]"
              >
                <Input.TextArea
                  v-model:value="formData.problemDesc"
                  :rows="4"
                  placeholder="按 5W2H 描述异常现象、发生批次、影响数量、不良率和判定依据"
                />
              </Form.Item>
            </div>
          </div>
        </section>

        <section class="qms-8d-form-fieldset">
          <div class="qms-8d-form-legend">三、D3 临时围堵措施</div>
          <div class="qms-8d-form-grid">
            <label class="qms-8d-form-label--tall">紧急围堵</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-7">
              <Input.TextArea
                v-model:value="formData.containmentAction"
                :rows="3"
                placeholder="请填写紧急隔离、筛选、停线、冻结等临时措施"
              />
            </div>
            <label>执行负责人</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-3">
              <Input
                v-model:value="formData.containmentOwnerName"
                placeholder="请输入执行负责人"
              />
            </div>
            <label>落实日期</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-3">
              <DatePicker
                v-model:value="formData.containmentDate"
                class="w-full"
                format="YYYY-MM-DD"
                placeholder="请选择落实日期"
                value-format="YYYY-MM-DD"
              />
            </div>
          </div>
        </section>

        <section class="qms-8d-form-fieldset">
          <div class="qms-8d-form-legend">四、D4 根本原因分析</div>
          <div class="qms-8d-form-grid">
            <label>4M1E归类</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-3">
              <Select
                v-model:value="formData.rootCauseCategory"
                allow-clear
                placeholder="请选择 4M1E 归类"
                :options="rootCauseCategoryOptions"
              />
            </div>
            <label class="qms-8d-form-label--tall">5Why/鱼骨分析</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-7">
              <Input.TextArea
                v-model:value="formData.rootCauseAnalysis"
                :rows="4"
                placeholder="请填写 5Why、鱼骨分析或根因推导过程"
              />
            </div>
          </div>
        </section>

        <section class="qms-8d-form-fieldset">
          <div class="qms-8d-form-legend">五、D5-D6 永久对策与 CAPA 行动项</div>
          <div class="qms-8d-form-grid">
            <label class="qms-8d-form-label--tall">永久对策摘要</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-7">
              <Input.TextArea
                v-model:value="formData.correctiveAction"
                :rows="3"
                placeholder="请填写永久对策摘要"
              />
            </div>
          </div>
          <div class="qms-8d-subtable-title">
            <span>CAPA 行动项</span>
            <Button v-if="!disabled" size="small" type="primary" @click="addActionItem">
              <IconifyIcon icon="lucide:plus" />
              添加行动项
            </Button>
          </div>
          <div class="qms-8d-table-wrap">
            <table class="qms-8d-table qms-8d-action-table">
              <thead>
                <tr>
                  <th>类型</th>
                  <th>行动内容</th>
                  <th>责任人</th>
                  <th>计划日期</th>
                  <th>状态</th>
                  <th>完成/验证</th>
                  <th v-if="!disabled" class="w-24">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in formData.actionItems" :key="item.id || index">
                  <td>
                    <Select
                      v-model:value="item.actionType"
                      class="w-full"
                      size="small"
                      :options="QMS_8D_ACTION_ITEM_TYPE_OPTIONS"
                    />
                  </td>
                  <td>
                    <Input.TextArea
                      v-model:value="item.actionDesc"
                      :rows="2"
                      placeholder="行动内容"
                    />
                  </td>
                  <td>
                    <Input
                      v-model:value="item.ownerUserName"
                      placeholder="责任人"
                      size="small"
                    />
                  </td>
                  <td>
                    <DatePicker
                      v-model:value="item.planFinishDate"
                      class="w-full"
                      format="YYYY-MM-DD"
                      placeholder="计划日期"
                      size="small"
                      value-format="YYYY-MM-DD"
                    />
                  </td>
                  <td>
                    <Select
                      v-model:value="item.itemStatus"
                      class="w-full"
                      placeholder="状态"
                      size="small"
                      :options="actionItemStatusOptions"
                    />
                  </td>
                  <td>
                    <Input
                      v-model:value="item.finishDesc"
                      class="mb-2"
                      placeholder="完成说明"
                      size="small"
                    />
                    <Input
                      v-model:value="item.verificationResult"
                      placeholder="验证结果"
                      size="small"
                    />
                  </td>
                  <td v-if="!disabled" class="text-center">
                    <Button
                      v-if="item.id"
                      size="small"
                      type="link"
                      @click="handleActionDone(item)"
                    >
                      完成
                    </Button>
                    <Button size="small" danger type="link" @click="removeActionItem(index)">
                      移除
                    </Button>
                  </td>
                </tr>
                <tr v-if="!formData.actionItems?.length">
                  <td :colspan="disabled ? 6 : 7" class="qms-8d-empty-cell">
                    暂无 CAPA 行动项
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="qms-8d-form-grid qms-8d-form-grid--mt">
            <label class="qms-8d-form-label--tall">有效性验证</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-7">
              <Input.TextArea
                v-model:value="formData.validationResult"
                :rows="3"
                placeholder="请填写验证结果或效果数据"
              />
            </div>
          </div>
        </section>

        <section class="qms-8d-form-fieldset">
          <div class="qms-8d-form-legend">六、D7-D8 预防再发生与标准化</div>
          <div class="qms-8d-form-grid">
            <label>标准化项</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-7">
              <div class="qms-8d-checkbox-line">
                <Checkbox v-model:checked="formData.updateSop">更新 SOP/SIP</Checkbox>
                <Checkbox v-model:checked="formData.updateFmea">更新 FMEA</Checkbox>
                <Checkbox v-model:checked="formData.updateControlPlan">
                  更新控制计划
                </Checkbox>
              </div>
            </div>
            <label class="qms-8d-form-label--tall">文件升版说明</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-7">
              <Input.TextArea
                v-model:value="formData.standardizeDesc"
                :rows="3"
                placeholder="请填写 SOP/SIP/FMEA/控制计划等文件升版说明"
              />
            </div>
          </div>
        </section>

        <section class="qms-8d-form-fieldset">
          <div class="qms-8d-form-legend">七、办理意见与附件</div>
          <div class="qms-8d-form-grid">
            <label class="qms-8d-form-label--tall">办理意见</label>
            <div class="qms-8d-form-control qms-8d-form-control--span-7">
              <Input.TextArea
                v-model:value="formData.flowOpinion"
                :rows="3"
                placeholder="办理意见、退回原因或关闭说明"
              />
            </div>
            <label class="qms-8d-form-label--tall">附件上传</label>
            <div
              class="qms-8d-form-control qms-8d-form-control--span-7 qms-8d-upload-control"
            >
              <FileUpload
                v-model="attachmentUrls"
                :accept="attachmentAcceptTypes"
                :disabled="disabled"
                directory="mes/qms/8d"
                :max-number="20"
                :max-size="20"
                multiple
                show-description
                @preview="handleAttachmentPreview"
              />
            </div>
          </div>
        </section>
      </Form>
    </div>
  </Modal>
</template>

<style>
.qms-8d-erp-modal .ant-modal-body {
  height: 100%;
  overflow: hidden;
}

.qms-8d-erp-modal .ant-modal-content {
  overflow: hidden;
}
</style>

<style scoped>
.qms-8d-detail {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background:
    linear-gradient(#eef5fc 1px, transparent 1px),
    linear-gradient(90deg, #eef5fc 1px, transparent 1px),
    #f7fbff;
  background-size: 54px 28px;
}

.qms-8d-toolbar {
  display: grid;
  flex-shrink: 0;
  align-items: center;
  min-height: 96px;
  grid-template-columns: minmax(240px, 1fr) auto minmax(420px, 1fr);
  gap: 12px;
  border-bottom: 1px solid #c9d8e8;
  background: #eef6ff;
  padding: 14px 18px;
}

.qms-8d-title-panel {
  min-width: 620px;
  text-align: center;
}

.qms-8d-title-panel__name {
  color: #00557e;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: 0;
}

.qms-8d-title-panel__subtitle {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px 18px;
  margin-top: 8px;
  color: #52677f;
  font-size: 13px;
}

.qms-8d-toolbar__actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.qms-8d-toolbar-action {
  min-width: 72px;
  height: 40px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgb(15 23 42 / 8%);
  font-size: 15px;
}

.qms-8d-workbench {
  display: grid;
  align-content: start;
  min-height: 0;
  flex: 1;
  gap: 14px;
  overflow: auto;
  padding: 14px;
}

.qms-8d-form-fieldset {
  position: relative;
  border: 1px solid #cddbea;
  background: #fff;
  padding: 32px 20px 16px;
}

.qms-8d-form-legend {
  position: absolute;
  top: -13px;
  left: 18px;
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 12px;
  background: #fff;
  color: #0072ff;
  font-size: 17px;
  font-weight: 800;
}

.qms-8d-form-grid {
  display: grid;
  grid-template-columns:
    122px minmax(0, 1fr) 122px minmax(0, 1fr)
    122px minmax(0, 1fr) 122px minmax(0, 1fr);
  border-top: 1px solid #e1e8f0;
  border-left: 1px solid #e1e8f0;
}

.qms-8d-form-grid--mt {
  margin-top: 12px;
}

.qms-8d-form-grid > label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 42px;
  border-right: 1px solid #e1e8f0;
  border-bottom: 1px solid #e1e8f0;
  background: #eef4fa;
  color: #10233d;
  font-weight: 700;
  line-height: 1.25;
  padding: 0 14px;
  text-align: right;
}

.qms-8d-form-label--tall {
  min-height: 88px !important;
}

.qms-8d-form-control {
  display: flex;
  align-items: center;
  min-height: 42px;
  min-width: 0;
  border-right: 1px solid #e1e8f0;
  border-bottom: 1px solid #e1e8f0;
  background: #fff;
  padding: 6px 10px;
}

.qms-8d-form-control--span-3 {
  grid-column: span 3;
}

.qms-8d-form-control--span-7 {
  grid-column: span 7;
}

.qms-8d-form-control :deep(.ant-input),
.qms-8d-form-control :deep(.ant-select),
.qms-8d-form-control :deep(.ant-picker) {
  width: 100%;
}

.qms-8d-form-control :deep(.ant-input),
.qms-8d-form-control :deep(.ant-select-selector),
.qms-8d-form-control :deep(.ant-picker) {
  border-radius: 4px;
}

.qms-8d-form-item {
  width: 100%;
  margin-bottom: 0;
}

.qms-8d-checkbox-line {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 26px;
  align-items: center;
}

.qms-8d-subtable-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 8px;
  color: #10233d;
  font-weight: 800;
}

.qms-8d-subtable-title--flush {
  margin-top: 0;
}

.qms-8d-table-wrap {
  overflow-x: auto;
}

.qms-8d-table {
  width: 100%;
  min-width: 720px;
  table-layout: fixed;
  border-collapse: collapse;
  font-size: 13px;
}

.qms-8d-action-table {
  min-width: 1080px;
}

.qms-8d-table th,
.qms-8d-table td {
  padding: 8px;
  border: 1px solid #cddbea;
  vertical-align: top;
}

.qms-8d-table th {
  background: #eef4fa;
  color: #10233d;
  font-weight: 800;
  text-align: center;
}

.qms-8d-table :deep(.ant-picker),
.qms-8d-table :deep(.ant-select) {
  width: 100%;
}

.qms-8d-empty-cell {
  color: #8a9aab;
  text-align: center;
}

.qms-8d-empty-cell {
  padding: 18px !important;
}

.qms-8d-upload-control {
  align-items: flex-start;
}

.qms-8d-upload-control :deep(.ant-upload-wrapper) {
  width: 100%;
}

@media (max-width: 1280px) {
  .qms-8d-toolbar {
    grid-template-columns: 1fr;
  }

  .qms-8d-toolbar__placeholder {
    display: none;
  }

  .qms-8d-title-panel {
    min-width: 0;
  }

  .qms-8d-toolbar__actions {
    justify-content: center;
    flex-wrap: wrap;
  }

  .qms-8d-form-grid {
    grid-template-columns: 116px minmax(0, 1fr) 116px minmax(0, 1fr);
  }

  .qms-8d-form-control--span-7,
  .qms-8d-form-control--span-3 {
    grid-column: span 3;
  }
}
</style>

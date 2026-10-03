<script lang="ts" setup>
import { ref, computed, nextTick } from 'vue';
import {
  Button, Card, Tag, Input, Table,
  message, Modal, Select, Divider, Drawer, Timeline, Form, Row, Col, Statistic
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const props = defineProps(['operator', 'order', 'process']);
const emit = defineEmits(['close']);
const clone = (data: any) => JSON.parse(JSON.stringify(data));

// ==================================================================================
// 历史记录与状态
// ==================================================================================
const historyDrawerVisible = ref(false);
const historyList = ref<any[]>([]);

function recordHistory(action: string, detail: string, snapshot: any = null) {
  historyList.value.unshift({ time: new Date().toLocaleString(), action, user: props.operator.userName, detail, snapshot });
}

// ==================================================================================
// 检验核心工作台逻辑 (即扫即检)
// ==================================================================================
const scanInputRef = ref();
const pieceScanCode = ref('');
const currentPieceInfo = ref<any>(null);

const currentGrade = ref('A级 (良品)');
const defectCode = ref<string | null>(null);

const qcLogs = ref<any[]>([]);

const qcColumns: TableColumnsType = [
  { title: '单品条码', dataIndex: 'code', width: 180 },
  { title: '溯源批次', dataIndex: 'batch', width: 140 },
  { title: '检验时间', dataIndex: 'time', width: 120 },
  { title: '分级判定', dataIndex: 'grade', width: 120, align: 'center' },
  { title: '缺陷记录', dataIndex: 'defect', width: 150 },
  { title: '检验人', dataIndex: 'user', width: 80 },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

const stats = computed(() => {
  const total = qcLogs.value.length;
  const gradeA = qcLogs.value.filter(p => p.grade === 'A级 (良品)').length;
  const gradeB = qcLogs.value.filter(p => p.grade === 'B级 (降级)').length;
  const ng = qcLogs.value.filter(p => p.grade === '报废 (不良)').length;
  return { total, gradeA, gradeB, ng, pass: gradeA + gradeB };
});

function handleScanEnter() {
  const code = pieceScanCode.value.trim();
  if (!code) return;

  if (qcLogs.value.find(log => log.code === code)) {
    pieceScanCode.value = '';
    return message.error(`单品 [${code}] 已存在检验记录，请勿重复检验！`);
  }

  message.loading({ content: '解析单品信息中...', key: 'fetchPiece', duration: 0.5 }).then(() => {
    currentPieceInfo.value = {
      code: code,
      batch: 'CUT-' + code.slice(-4),
      product: 'T01_Film_裁切圆片',
      source: '裁切工序 (完成)'
    };
    message.success({ content: '解析成功，请判定外观', key: 'fetchPiece' });
  });
}

function handleSubmitQc() {
  if (!currentPieceInfo.value) return message.warning('请先扫描待检验的单品条码！');
  if (currentGrade.value === '报废 (不良)' && !defectCode.value) {
    return message.warning('判定为不良品时，必须选择缺陷代码！');
  }

  qcLogs.value.unshift({
    id: Date.now(),
    code: currentPieceInfo.value.code,
    batch: currentPieceInfo.value.batch,
    time: new Date().toLocaleTimeString('en-US', { hour12: false }),
    grade: currentGrade.value,
    defect: currentGrade.value === '报废 (不良)' ? defectCode.value : '-',
    user: props.operator.userName
  });

  message.success(`单品 ${currentPieceInfo.value.code} 判定已记录`);
  resetWorkstation();
}

function setGrade(grade: string) {
  currentGrade.value = grade;
  if (grade !== '报废 (不良)' && currentPieceInfo.value) {
    handleSubmitQc();
  }
}

function resetWorkstation() {
  pieceScanCode.value = '';
  currentPieceInfo.value = null;
  currentGrade.value = 'A级 (良品)';
  defectCode.value = null;
  nextTick(() => { scanInputRef.value?.focus(); });
}

function handleRemoveLog(id: number) {
  qcLogs.value = qcLogs.value.filter(item => item.id !== id);
}

// ==================================================================================
// 完工结单
// ==================================================================================
const finishModalVisible = ref(false);
const finishForm = ref({ remark: '' });

function handleFinishClick() {
  if (stats.value.total === 0) return message.warning('本次作业暂无检验记录，无法结单！');
  finishModalVisible.value = true;
}

function submitFinish() {
  const detailStr = `检验结单: 共计 ${stats.value.total} PCS。A级 ${stats.value.gradeA}, B级 ${stats.value.gradeB}, NG ${stats.value.ng}`;

  recordHistory('检验批次结单', detailStr, {
    type: 'qc_finish',
    stats: clone(stats.value),
    logs: clone(qcLogs.value)
  });

  Modal.success({
    title: '质检台账已更新',
    content: `系统已汇总本次检验的 ${stats.value.total} 条记录，并生成电子检验报告(FQC)。\n合格品已释放流转，不良品已锁定至隔离库。`,
    onOk: () => {
      finishModalVisible.value = false;
      emit('close');
    }
  });
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-10">
      <div class="flex items-center gap-2 font-bold text-blue-700 text-lg">
        <IconifyIcon icon="lucide:microscope" class="text-2xl" />
        FQC 单片品质检验工作台
      </div>
      <div class="flex items-center gap-4">
        <Tag color="processing" class="!m-0">
          <template #icon><IconifyIcon icon="lucide:loader" class="animate-spin mr-1" /></template>
          持续检验接收中...
        </Tag>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden gap-3">

      <Row :gutter="12" class="shrink-0">
        <Col :span="6"><Card size="small" class="bg-blue-50 border-blue-100 shadow-sm"><Statistic title="本次累计检验 (PCS)" :value="stats.total" valueStyle="color: #2563eb; font-weight: bold;" /></Card></Col>
        <Col :span="6"><Card size="small" class="bg-green-50 border-green-100 shadow-sm"><Statistic title="A级良品 (PCS)" :value="stats.gradeA" valueStyle="color: #16a34a" /></Card></Col>
        <Col :span="6"><Card size="small" class="bg-cyan-50 border-cyan-100 shadow-sm"><Statistic title="B级降级 (PCS)" :value="stats.gradeB" valueStyle="color: #0891b2" /></Card></Col>
        <Col :span="6"><Card size="small" class="bg-red-50 border-red-100 shadow-sm"><Statistic title="NG 报废 (PCS)" :value="stats.ng" valueStyle="color: #dc2626; font-weight: bold;" /></Card></Col>
      </Row>

      <div class="flex-1 flex gap-3 min-h-0">

        <div class="w-[380px] flex flex-col min-h-0">
          <div class="bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col h-full overflow-hidden relative">

            <div class="p-4 border-b shrink-0 bg-slate-50">
              <div class="text-sm font-black text-slate-800 flex items-center gap-2">
                <IconifyIcon icon="lucide:scan-line" class="text-indigo-600 text-lg" /> 单品即扫即检
              </div>
            </div>

            <div class="flex-1 overflow-y-auto p-4 custom-scrollbar flex flex-col">
              <Form layout="vertical" class="flex-1 flex flex-col">
                <Form.Item class="mb-4 shrink-0">
                  <div class="text-xs font-bold text-indigo-800 mb-2">1. 扫描待检单片条码 (自动回车获取)</div>
                  <Input
                    ref="scanInputRef"
                    v-model:value="pieceScanCode"
                    size="large"
                    class="shadow-inner font-mono text-lg border-indigo-300"
                    placeholder="等待扫码..."
                    autofocus
                    @pressEnter="handleScanEnter"
                  >
                    <template #prefix><IconifyIcon icon="lucide:barcode" class="text-slate-400" /></template>
                  </Input>
                </Form.Item>

                <div class="bg-slate-50 border border-slate-200 rounded-lg p-3 mb-4 shrink-0 min-h-[90px]">
                  <template v-if="currentPieceInfo">
                    <div class="text-xs text-slate-400 mb-1">当前检品明细：</div>
                    <div class="font-mono font-bold text-blue-700 text-base mb-1">{{ currentPieceInfo.code }}</div>
                    <div class="text-xs font-bold text-slate-600 flex justify-between">
                      <span>溯源批次: {{ currentPieceInfo.batch }}</span>
                      <span>来源: {{ currentPieceInfo.source }}</span>
                    </div>
                  </template>
                  <template v-else>
                    <div class="flex flex-col items-center justify-center h-full text-slate-400">
                      <IconifyIcon icon="lucide:box" class="text-3xl mb-2 opacity-50" />
                      <span class="text-xs">等待扫码载入单品信息...</span>
                    </div>
                  </template>
                </div>

                <Form.Item class="mb-0 shrink-0">
                  <div class="text-xs font-bold text-slate-700 mb-2">2. 外观分级判定 (选良品自动提交)</div>
                  <div class="flex flex-col gap-2">
                    <Button :type="currentGrade === 'A级 (良品)' ? 'primary' : 'default'"
                            :class="currentGrade === 'A级 (良品)' ? 'bg-green-600 border-green-600 shadow-md' : ''"
                            size="large" block :disabled="!currentPieceInfo" @click="setGrade('A级 (良品)')">
                      <div class="flex justify-between items-center px-4 w-full">
                        <span class="font-bold">A 级 (优良品)</span>
                        <span class="text-[10px] opacity-70">自动提交</span>
                      </div>
                    </Button>
                    <Button :type="currentGrade === 'B级 (降级)' ? 'primary' : 'default'"
                            :class="currentGrade === 'B级 (降级)' ? 'bg-cyan-600 border-cyan-600 shadow-md' : ''"
                            size="large" block :disabled="!currentPieceInfo" @click="setGrade('B级 (降级)')">
                      <div class="flex justify-between items-center px-4 w-full">
                        <span class="font-bold">B 级 (降级品)</span>
                        <span class="text-[10px] opacity-70">自动提交</span>
                      </div>
                    </Button>
                    <Button :type="currentGrade === '报废 (不良)' ? 'primary' : 'default'" danger
                            size="large" block :disabled="!currentPieceInfo" @click="setGrade('报废 (不良)')">
                      <span class="font-bold">NG (不良拦截)</span>
                    </Button>
                  </div>
                </Form.Item>

                <div v-if="currentGrade === '报废 (不良)'" class="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg animate-fade-in shrink-0">
                  <div class="text-xs font-bold text-red-800 mb-2">3. 必须选择主要缺陷原因</div>
                  <Select v-model:value="defectCode" size="large" placeholder="下拉选择..." class="w-full mb-3 shadow-sm" :getPopupContainer="triggerNode => triggerNode.parentNode">
                    <Select.Option value="SC01-表面划伤">SC01 - 表面划伤</Select.Option>
                    <Select.Option value="BU02-内部气泡">BU02 - 内部气泡</Select.Option>
                    <Select.Option value="DI03-黑点脏污">DI03 - 黑点脏污</Select.Option>
                    <Select.Option value="TH04-厚度超差">TH04 - 厚度超差</Select.Option>
                    <Select.Option value="ED05-边缘破损">ED05 - 边缘破损</Select.Option>
                  </Select>
                  <Button type="primary" danger size="large" class="font-bold w-full shadow-md" @click="handleSubmitQc">
                    确认拦截并提交记账
                  </Button>
                </div>

                <div v-if="currentGrade !== '报废 (不良)'" class="mt-auto pt-4 shrink-0">
                  <Button v-if="currentPieceInfo" type="primary" size="large" class="bg-indigo-600 font-bold w-full shadow-lg" @click="handleSubmitQc">
                    手动确认提交
                  </Button>
                </div>
              </Form>
            </div>
          </div>
        </div>

        <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
          <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center justify-between shrink-0">
            <div class="flex items-center gap-2"><IconifyIcon icon="lucide:clipboard-list" /> 本次检验明细流平台账</div>
            <span class="text-[10px] text-slate-400 font-normal">按时间倒序排列</span>
          </div>
          <Table :columns="qcColumns" :dataSource="qcLogs" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 220px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'code'">
                <span class="font-mono font-bold text-slate-700">{{ record.code }}</span>
              </template>
              <template v-if="column.dataIndex === 'grade'">
                <Tag v-if="record.grade === 'A级 (良品)'" color="green">A 级</Tag>
                <Tag v-else-if="record.grade === 'B级 (降级)'" color="cyan">B 级</Tag>
                <Tag v-else color="red">NG 报废</Tag>
              </template>
              <template v-if="column.dataIndex === 'defect'">
                <span :class="record.defect !== '-' ? 'text-red-600 font-bold text-xs' : 'text-slate-300'">{{ record.defect }}</span>
              </template>
              <template v-if="column.dataIndex === 'action'">
                <Button type="text" danger size="small" @click="handleRemoveLog(record.id)">撤销重判</Button>
              </template>
            </template>
            <template #emptyText>
              <div class="py-20 flex flex-col items-center text-slate-400">
                <IconifyIcon icon="lucide:scan-face" class="text-4xl mb-2 opacity-50" />
                <span>暂无检验记录，扫码判定后将自动显示于此。</span>
              </div>
            </template>
          </Table>
        </div>
      </div>

    </main>

    <footer class="flex h-14 shrink-0 items-center justify-between border-t bg-white px-4 shadow-[0_-2px_8px_rgba(0,0,0,0.05)] z-20">
      <div class="flex items-center gap-4">
        <Button @click="emit('close')" class="font-bold text-slate-600 border-slate-300">退出质检台</Button>
        <Divider type="vertical" />
        <Button type="link" class="p-0 font-bold text-blue-600" @click="historyDrawerVisible = true">查看质检交接历史</Button>
      </div>

      <div class="flex gap-3 items-center">
        <Button type="primary" class="bg-slate-800 border-none font-bold px-10 shadow-lg" @click="handleFinishClick">
          <IconifyIcon icon="lucide:flag" class="mr-2" /> 结束本次质检任务并结单
        </Button>
      </div>
    </footer>

    <Modal v-model:open="finishModalVisible" title="FQC 质检批次结单汇总" @ok="submitFinish" width="600px">
      <Form layout="vertical" class="mt-4">
        <div class="bg-blue-50 text-blue-600 p-3 rounded mb-4 text-sm border border-blue-100">
          系统将封存本次会话的所有检验记录，并生成带有您数字签名的电子检验报告。
        </div>
        <Row :gutter="12" class="mb-4 text-center">
          <Col :span="6"><div class="p-2 border rounded bg-slate-50"><div class="text-xs text-slate-500 mb-1">检验总数</div><div class="text-lg font-bold">{{ stats.total }}</div></div></Col>
          <Col :span="6"><div class="p-2 border rounded border-green-200 bg-green-50"><div class="text-xs text-green-600 mb-1">A级良品</div><div class="text-lg font-bold text-green-600">{{ stats.gradeA }}</div></div></Col>
          <Col :span="6"><div class="p-2 border rounded border-cyan-200 bg-cyan-50"><div class="text-xs text-cyan-600 mb-1">B级降级</div><div class="text-lg font-bold text-cyan-600">{{ stats.gradeB }}</div></div></Col>
          <Col :span="6"><div class="p-2 border rounded border-red-200 bg-red-50"><div class="text-xs text-red-600 mb-1">NG报废</div><div class="text-lg font-bold text-red-600">{{ stats.ng }}</div></div></Col>
        </Row>
        <Form.Item label="质检报告整体备注说明">
          <Input.TextArea v-model:value="finishForm.remark" rows="3" placeholder="可选填整体品质评估、异常趋势等..." />
        </Form.Item>
      </Form>
    </Modal>

    <Drawer v-model:open="historyDrawerVisible" title="质检历史追溯" placement="right" width="400">
      <Timeline>
        <Timeline.Item v-for="(log, idx) in historyList" :key="idx" :color="idx === 0 ? 'blue' : 'gray'">
          <div class="font-bold mb-1 text-slate-700">{{ log.action }}</div>
          <div class="text-xs text-slate-500 mb-1">{{ log.time }} / {{ log.user }}</div>
          <div class="text-xs bg-slate-50 p-2 rounded border border-slate-100">{{ log.detail }}</div>
        </Timeline.Item>
      </Timeline>
    </Drawer>
  </div>
</template>

<style scoped>
.vben-schema-table :deep(.ant-table-wrapper), .vben-schema-table :deep(.ant-spin-nested-loading), .vben-schema-table :deep(.ant-spin-container), .vben-schema-table :deep(.ant-table) { height: 100%; display: flex; flex-direction: column; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; color: #64748b !important; font-size: 11px; font-weight: bold; padding: 6px 8px !important; border-bottom: 1px solid #e2e8f0; }
.vben-schema-table :deep(.ant-table-cell) { padding: 8px 12px !important; font-size: 12px; }
.vben-schema-table :deep(.ant-table-row:hover > td) { background: #f1f5f9 !important; }

/* 扫码框高亮光晕 */
:deep(.ant-input-affix-wrapper-focused) { box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15) !important; border-color: #4f46e5 !important; }

/* 优化自定义滚动条 */
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
.custom-scrollbar:hover::-webkit-scrollbar-thumb { background: #94a3b8; }

@keyframes fadeIn { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: translateY(0); } }
.animate-fade-in { animation: fadeIn 0.2s ease-out forwards; }
</style>

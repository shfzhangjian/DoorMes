<script lang="ts" setup>
import { computed, ref, watch } from 'vue';

import { Button, Input, message, Modal, Tag } from 'ant-design-vue';

import {
  evaluateCoaSpecFormula,
  getCoaSpecFormulaKindLabel,
  parseCoaSpecFormula,
} from './coa-spec-formula';

interface FormulaPayload {
  formula: string;
  lowerLimit?: number;
  targetValue?: number;
  upperLimit?: number;
}

const props = withDefaults(
  defineProps<{
    formula?: string;
    itemName?: string;
    open: boolean;
    targetValue?: number;
    unit?: string;
  }>(),
  {
    formula: '',
    itemName: '',
    unit: '',
  },
);

const emit = defineEmits<{
  apply: [payload: FormulaPayload];
  close: [];
}>();

const formula = ref('');
const trialValue = ref('');
const parsed = computed(() => parseCoaSpecFormula(formula.value));
const trialResult = computed(() => evaluateCoaSpecFormula(formula.value, trialValue.value));
const rangeText = computed(() => {
  if (!parsed.value.valid) return parsed.value.message;
  if (parsed.value.kind === 'IN_LIST') {
    return `允许值：${parsed.value.allowedValues?.join('、') || '-'}`;
  }
  if (parsed.value.kind === 'NOT_IN_LIST') {
    return `排除值：${parsed.value.allowedValues?.join('、') || '-'}`;
  }
  const lower = parsed.value.lowerLimit === undefined ? '-∞' : parsed.value.lowerLimit;
  const upper = parsed.value.upperLimit === undefined ? '+∞' : parsed.value.upperLimit;
  return `自动判定范围：${lower} ~ ${upper}${props.unit ? ` ${props.unit}` : ''}`;
});
const formulaTargetValue = computed(() => parsed.value.targetValue ?? props.targetValue);
const formulaDeviation = computed(() => {
  if (
    parsed.value.targetValue === undefined ||
    parsed.value.lowerLimit === undefined ||
    parsed.value.upperLimit === undefined
  ) {
    return undefined;
  }
  const lowerDeviation = parsed.value.targetValue - parsed.value.lowerLimit;
  const upperDeviation = parsed.value.upperLimit - parsed.value.targetValue;
  return lowerDeviation === upperDeviation
    ? `允许公差：±${upperDeviation}${props.unit ? ` ${props.unit}` : ''}`
    : `允许偏差：-${lowerDeviation}/+${upperDeviation}${props.unit ? ` ${props.unit}` : ''}`;
});

watch(
  () => props.open,
  (open) => {
    if (!open) return;
    formula.value = props.formula || '';
    trialValue.value = undefined;
  },
);

function useFormula(value: string) {
  formula.value = value;
}

function apply() {
  if (!parsed.value.valid) {
    message.warning(parsed.value.message || '请修正判定公式');
    return;
  }
  emit('apply', {
    formula: parsed.value.formula,
    lowerLimit: parsed.value.lowerLimit,
    targetValue: parsed.value.targetValue,
    upperLimit: parsed.value.upperLimit,
  });
}
</script>

<template>
  <Modal
    :footer="null"
    :mask-closable="false"
    :open="open"
    title="内控判定公式"
    width="640px"
    :z-index="7200"
    @cancel="emit('close')"
  >
    <div class="coa-spec-formula-modal">
      <div class="formula-meta">
        <span>项目：{{ itemName || '-' }}</span>
        <span>单位：{{ unit || '-' }}</span>
      </div>
      <div class="formula-presets">
        <span>快速输入</span>
        <Button size="small" @click="useFormula('39.7~55.7')">区间</Button>
        <Button size="small" @click="useFormula('50±2')">目标±公差</Button>
        <Button size="small" @click="useFormula('50+3/-2')">非对称公差</Button>
        <Button size="small" @click="useFormula('≥39.7')">不小于</Button>
        <Button size="small" @click="useFormula('≤55.7')">不大于</Button>
        <Button size="small" @click="useFormula('>39.7')">严格下限</Button>
        <Button size="small" @click="useFormula('<55.7')">严格上限</Button>
        <Button size="small" @click="useFormula('=50')">精确值</Button>
        <Button size="small" @click="useFormula('IN(合格,OK)')">允许值集合</Button>
        <Button size="small" @click="useFormula('NOT IN(NG,报废)')">排除值集合</Button>
      </div>
      <label class="formula-label" for="coa-spec-formula-input">判定公式</label>
      <Input
        id="coa-spec-formula-input"
        v-model:value="formula"
        allow-clear
        placeholder="如 39.7~55.7、50±2、50+3/-2、IN(合格,OK)"
      />
      <div class="formula-preview" :class="{ 'formula-preview--invalid': !parsed.valid }">
        <Tag :color="parsed.valid ? 'green' : 'red'">{{ parsed.valid ? '公式有效' : '公式错误' }}</Tag>
        <div class="formula-preview-content">
          <span v-if="parsed.valid">规则：{{ getCoaSpecFormulaKindLabel(parsed.kind) }}</span>
          <span v-if="formulaTargetValue !== undefined">目标值：{{ formulaTargetValue }}{{ unit ? ` ${unit}` : '' }}</span>
          <span v-else>目标值：未设置，按内控上下限判定</span>
          <span v-if="formulaDeviation">{{ formulaDeviation }}</span>
          <span>{{ rangeText }}</span>
        </div>
      </div>
      <div class="formula-trial">
        <label>试算实际值</label>
        <Input v-model:value="trialValue" allow-clear style="width: 180px" />
        <Tag v-if="trialResult.passed !== undefined" :color="trialResult.passed ? 'green' : 'red'">
          {{ trialResult.passed ? '合格' : '不合格' }}
        </Tag>
        <span v-else-if="trialValue && trialResult.message" class="formula-trial-error">{{ trialResult.message }}</span>
      </div>
      <div class="formula-footer">
        <Button @click="emit('close')">取消</Button>
        <Button :disabled="!parsed.valid" type="primary" @click="apply">应用公式</Button>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.coa-spec-formula-modal {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.formula-meta,
.formula-presets,
.formula-trial,
.formula-footer,
.formula-preview {
  display: flex;
  align-items: center;
  gap: 8px;
}

.formula-meta {
  color: #64748b;
  font-size: 13px;
}

.formula-presets {
  flex-wrap: wrap;
}

.formula-presets > span,
.formula-label,
.formula-trial > label {
  min-width: 72px;
  color: #334155;
  font-weight: 600;
}

.formula-label {
  margin-bottom: -6px;
}

.formula-preview {
  min-height: 38px;
  padding: 8px 10px;
  color: #166534;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.formula-preview--invalid {
  color: #b91c1c;
  background: #fff1f2;
  border-color: #fecdd3;
}

.formula-preview-content {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
}

.formula-footer {
  justify-content: flex-end;
  margin-top: 4px;
}

.formula-trial-error {
  color: #b91c1c;
  font-size: 13px;
}
</style>

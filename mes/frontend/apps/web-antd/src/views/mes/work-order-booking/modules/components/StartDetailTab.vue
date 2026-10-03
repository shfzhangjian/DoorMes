<script lang="ts" setup>
const props = defineProps<{ isFormulaReport?: boolean; task: any; reportForm: any }>();
</script>

<template>
  <div class="h-full flex flex-col bg-[#f5f7fa] p-3 overflow-y-auto min-h-0">
    <div class="bg-white border border-slate-200 rounded-none p-4 flex-1 flex flex-col min-h-0 shrink-0">
      <div class="text-[14px] font-bold text-slate-800 border-b border-slate-100 pb-2 mb-3 flex items-center shrink-0">
        报工信息
      </div>

      <div class="pp-form-grid">
        <div class="pp-form-item">
          <label>生产日期</label>
          <div class="pp-readonly-box">{{ props.reportForm.productionDateDisplay || '-' }}</div>
        </div>
        <div class="pp-form-item">
          <label>开始时间</label>
          <div class="pp-readonly-box">{{ props.reportForm.startTimeDisplay || '-' }}</div>
        </div>
        <div class="pp-form-item">
          <label>结束时间</label>
          <div class="pp-readonly-box">{{ props.reportForm.endTimeDisplay || '-' }}</div>
        </div>
        <div class="pp-form-item">
          <label>搅拌机台</label>
          <div class="pp-readonly-box">{{ props.reportForm.mixerEquipmentCode || '-' }}</div>
        </div>
        <div class="pp-form-item">
          <label>脱泡机台</label>
          <div class="pp-readonly-box">{{ props.reportForm.foamingEquipmentCode || '-' }}</div>
        </div>
        <template v-if="props.isFormulaReport">
          <div class="pp-form-item">
            <label>投料重量(kg)</label>
            <div class="pp-readonly-box">{{ props.reportForm.inputWeight ?? '-' }}</div>
          </div>
        </template>
        <div class="pp-form-item">
          <label>配料重量(kg)</label>
          <div class="pp-readonly-box">{{ props.reportForm.feedQty ?? '-' }}</div>
        </div>
        <div class="pp-form-item">
          <label>粘度(mPa.s)</label>
          <div class="pp-readonly-box">{{ props.reportForm.viscosity ?? '-' }}</div>
        </div>
        <div class="pp-form-item">
          <label>浆料温度(℃)</label>
          <div class="pp-readonly-box">{{ props.reportForm.slurryTemperature ?? '-' }}</div>
        </div>
        <template v-if="props.isFormulaReport">
          <div class="pp-form-item">
            <label>滤网批号</label>
            <div class="pp-readonly-box">{{ props.reportForm.filterBatchNo || '-' }}</div>
          </div>
          <div class="pp-form-item">
            <label>配料罐罐号</label>
            <div class="pp-readonly-box">{{ props.reportForm.batchingTankNo || '-' }}</div>
          </div>
          <div class="pp-form-item">
            <label>脱泡罐罐号</label>
            <div class="pp-readonly-box">{{ props.reportForm.defoamingTankNo || '-' }}</div>
          </div>
        </template>
        <div class="pp-form-item pp-form-item--full">
          <label>报工备注</label>
          <div class="pp-readonly-box pp-readonly-box--multiline">{{ props.reportForm.remark || '-' }}</div>
        </div>
        <div class="pp-form-item">
          <label>记录人</label>
          <div class="pp-readonly-box">{{ props.reportForm.recorderName || '-' }}</div>
        </div>
        <div class="pp-form-item">
          <label>记录时间</label>
          <div class="pp-readonly-box">{{ props.reportForm.recorderTimeDisplay || '-' }}</div>
        </div>
        <div class="pp-form-item">
          <label>确认人</label>
          <div class="pp-readonly-box">{{ props.reportForm.confirmerName || '-' }}</div>
        </div>
        <div class="pp-form-item">
          <label>确认时间</label>
          <div class="pp-readonly-box">{{ props.reportForm.confirmerTimeDisplay || '-' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pp-form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px 12px;
}

.pp-form-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.pp-form-item--full {
  grid-column: 1 / -1;
}

.pp-form-item label {
  color: #4b5563;
  font-size: 12px;
  font-weight: 700;
}

.pp-readonly-box {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 4px 11px;
  border: 1px solid #d9d9d9;
  background: #fafafa;
  color: #374151;
  line-height: 1.4;
}

.pp-readonly-box--multiline {
  align-items: flex-start;
  white-space: pre-wrap;
}
</style>

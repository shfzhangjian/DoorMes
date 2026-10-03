<script lang="ts" setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

import { useAccess } from '@vben/access';
import { IconifyIcon } from '@vben/icons';

type GuideAccent = 'neutral' | 'production' | 'quality';

type ArtifactType =
  | 'adhesive'
  | 'box'
  | 'cut'
  | 'disc'
  | 'flask'
  | 'groove'
  | 'layers'
  | 'measure'
  | 'paper'
  | 'scan'
  | 'slab'
  | 'strip';

interface ProcessStage {
  artifact: ArtifactType;
  auth?: string[];
  badge?: number;
  badgeLabel?: string;
  detail: string;
  disabled?: boolean;
  icon: string;
  key?: string;
  output: string;
  path?: string;
  highlight?: boolean;
  noPermission?: boolean;
  title: string;
}

interface WorkbenchReminder {
  auth?: string[];
  count?: number;
  description?: string;
  disabled?: boolean;
  icon: string;
  key: string;
  path?: string;
  title: string;
  tone?: 'info' | 'success' | 'warning';
  noPermission?: boolean;
}

interface WorkbenchDomainItem {
  auth?: string[];
  directory?: boolean;
  disabled?: boolean;
  icon?: string;
  key?: string;
  noPermission?: boolean;
  path?: string;
  title: string;
}

type WorkbenchDomainInput = string | WorkbenchDomainItem;

const props = withDefaults(
  defineProps<{
    accent?: GuideAccent;
    domainItems?: WorkbenchDomainInput[];
    emptyText?: string;
    icon?: string;
    intro?: string;
    label?: string;
    reminders?: WorkbenchReminder[];
    stages?: ProcessStage[];
    title: string;
  }>(),
  {
    accent: 'neutral',
    domainItems: () => ['4M1E', '批次追溯', '工艺配方', 'SPC'],
    emptyText: '当前账号暂无可访问的工作台入口',
    icon: 'lucide:layout-dashboard',
    intro: '连接计划、工艺、设备、物料、质量与现场执行，沉淀数字工厂协同入口。',
    label: '禾臣 MES 数字化工厂',
    reminders: () => [],
    stages: () => [],
  },
);

const emit = defineEmits<{
  (event: 'domainClick', item: WorkbenchDomainItem): void;
}>();

const router = useRouter();
const { hasAccessByCodes } = useAccess();

const productionStages: ProcessStage[] = [
  {
    artifact: 'paper',
    detail: '计划、BOM、配方与工艺路线同步下发',
    icon: 'lucide:calendar-check',
    output: '生产工单',
    title: '生产计划',
  },
  {
    artifact: 'flask',
    detail: '主料、辅料、助剂按配方称量混合',
    icon: 'lucide:flask-conical',
    output: '抛光垫浆料',
    title: '配料',
  },
  {
    artifact: 'slab',
    detail: '发泡、浇注、固化形成连续垫坯',
    icon: 'lucide:waves',
    output: '湿法垫坯',
    title: '湿法成型',
  },
  {
    artifact: 'disc',
    detail: '表面处理与厚度修整，形成基础工作面',
    icon: 'lucide:scan-face',
    output: '磨皮垫片',
    title: '磨皮',
  },
  {
    artifact: 'layers',
    detail: '基材与功能层贴合，形成复合半成品',
    icon: 'lucide:layers',
    output: '一粘复合片',
    title: '粘胶1',
  },
  {
    artifact: 'strip',
    detail: '按规格分条、分片，为压槽准备尺寸基准',
    icon: 'lucide:split-square-horizontal',
    output: '分切半成品',
    title: '分切',
  },
  {
    artifact: 'groove',
    detail: '按沟槽图样加工排液纹路和工作结构',
    icon: 'lucide:circle-dashed',
    output: '沟槽抛光垫',
    title: '压槽',
  },
  {
    artifact: 'adhesive',
    detail: '背胶、防护膜与垫片二次复合',
    icon: 'lucide:component',
    output: '背胶复合垫',
    title: '粘胶2',
  },
  {
    artifact: 'cut',
    detail: '外形裁切、边缘修整，进入最终检验',
    icon: 'lucide:scissors',
    output: '成品胶板',
    title: '裁切',
  },
  {
    artifact: 'box',
    detail: '贴标、装箱、入库，形成可发运成品',
    icon: 'lucide:package-check',
    output: '包装成品',
    title: '包装入库',
  },
];

const qualityStages: ProcessStage[] = [
  {
    artifact: 'paper',
    detail: '检验任务、抽样规则与标准版本确认',
    icon: 'lucide:badge-check',
    output: '检验任务',
    title: '来料/IQC',
  },
  {
    artifact: 'measure',
    detail: '首件尺寸、外观、沟槽基准确认',
    icon: 'lucide:ruler',
    output: '首件记录',
    title: '首件确认',
  },
  {
    artifact: 'scan',
    detail: '厚度、平整度、硬度等关键参数采集',
    icon: 'lucide:scan-line',
    output: '过程数据',
    title: '制程检测',
  },
  {
    artifact: 'measure',
    detail: '厚度、硬度、平整度等关键特性复核',
    icon: 'lucide:gauge',
    output: '尺寸数据',
    title: '关键尺寸',
  },
  {
    artifact: 'groove',
    detail: '沟槽宽度、深度、纹路连续性确认',
    icon: 'lucide:circle-dashed',
    output: '沟槽判定',
    title: '沟槽检验',
  },
  {
    artifact: 'disc',
    detail: '成品外观、边缘、表面缺陷复核',
    icon: 'lucide:eye',
    output: '外观结果',
    title: '外观复核',
  },
  {
    artifact: 'scan',
    detail: '关键质量特性进入趋势监控',
    icon: 'lucide:activity',
    output: 'SPC曲线',
    title: 'SPC监控',
  },
  {
    artifact: 'paper',
    detail: '不合格品隔离、评审与处置闭环',
    icon: 'lucide:file-warning',
    output: 'NCR单据',
    title: '异常闭环',
  },
  {
    artifact: 'box',
    detail: '出货批次、包装状态与标签核对',
    icon: 'lucide:shield-check',
    output: 'OQC放行',
    title: '出货检验',
  },
  {
    artifact: 'paper',
    detail: '批次、工序、检验和处置结果归档追溯',
    icon: 'lucide:history',
    output: '质量档案',
    title: '追溯归档',
  },
];

const defaultStages = computed(() => {
  if (props.accent === 'quality') {
    return qualityStages;
  }

  return productionStages;
});

function canAccess(auth?: string[]) {
  return !auth?.length || hasAccessByCodes(auth);
}

const processStages = computed(() =>
  (props.stages.length > 0 ? props.stages : defaultStages.value).map(
    (stage) => {
      const allowed = canAccess(stage.auth);

      return {
        ...stage,
        badge: allowed ? stage.badge : 0,
        disabled: stage.disabled || !allowed,
        highlight: allowed && stage.highlight,
        noPermission: !allowed,
      };
    },
  ),
);

const visibleReminders = computed(() =>
  props.reminders.map((item) => {
    const allowed = canAccess(item.auth);

    return {
      ...item,
      count: allowed ? item.count : 0,
      disabled: item.disabled || !allowed,
      noPermission: !allowed,
    };
  }),
);

const visibleDomainItems = computed(() =>
  props.domainItems.map((item) => {
    const domainItem = typeof item === 'string' ? { title: item } : item;
    const allowed = canAccess(domainItem.auth);

    return {
      ...domainItem,
      disabled: domainItem.disabled || !allowed,
      icon: domainItem.icon || 'lucide:hexagon',
      key: domainItem.key || domainItem.title,
      noPermission: !allowed,
    };
  }),
);

function formatBadgeCount(count?: number) {
  const value = Number(count || 0);
  if (value > 99) {
    return '99+';
  }
  return String(value);
}

function hasBadge(count?: number) {
  return Number(count || 0) > 0;
}

function openPath(path?: string, disabled?: boolean) {
  if (!path || disabled) {
    return;
  }
  void router.push(path);
}

function handleDomainClick(item: WorkbenchDomainItem) {
  if (item.disabled) {
    return;
  }

  if (item.path) {
    openPath(item.path, item.disabled);
    return;
  }

  if (item.directory) {
    emit('domainClick', item);
  }
}
</script>

<template>
  <section class="workbench-process" :class="`is-${accent}`">
    <div class="process-shell">
      <header class="process-header">
        <div class="title-block">
          <div class="title-line">
            <IconifyIcon :icon="icon" />
            <h1>
              <span>{{ label }}</span>
              <i></i>
              <strong>{{ title }}</strong>
            </h1>
          </div>
          <p>{{ intro }}</p>
        </div>
        <div
          v-if="visibleReminders.length > 0 || visibleDomainItems.length > 0"
          class="header-actions"
        >
          <div v-if="visibleReminders.length > 0" class="reminder-strip">
            <button
              v-for="item in visibleReminders"
              :key="item.key"
              class="reminder-action"
              :class="[
                item.tone ? `is-${item.tone}` : '',
                { 'is-no-permission': item.noPermission },
              ]"
              :disabled="!item.path || item.disabled"
              :title="item.noPermission ? '当前账号无此菜单权限' : undefined"
              type="button"
              @click="openPath(item.path, item.disabled)"
            >
              <span v-if="hasBadge(item.count)" class="reminder-badge">
                {{ formatBadgeCount(item.count) }}
              </span>
              <IconifyIcon :icon="item.icon" />
              <span>{{ item.title }}</span>
              <em v-if="item.description">{{ item.description }}</em>
            </button>
          </div>
          <div v-if="visibleDomainItems.length > 0" class="domain-strip">
            <button
              v-for="item in visibleDomainItems"
              :key="item.key"
              class="domain-action"
              :class="{ 'is-no-permission': item.noPermission }"
              :disabled="item.disabled || (!item.path && !item.directory)"
              :title="item.noPermission ? '当前账号无此菜单权限' : undefined"
              type="button"
              @click="handleDomainClick(item)"
            >
              <IconifyIcon :icon="item.icon" />
              {{ item.title }}
            </button>
          </div>
        </div>
      </header>

      <div class="process-map">
        <div class="map-grid" aria-hidden="true"></div>
        <div class="product-core" aria-hidden="true">
          <div class="core-illustration">
            <span class="core-shadow"></span>
            <span class="core-pad">
              <span></span>
            </span>
            <span class="core-wafer">
              <span></span>
            </span>
            <span class="core-bottle">
              <span></span>
            </span>
            <span class="core-inspector">
              <span></span>
            </span>
            <span class="core-probe">
              <span></span>
            </span>
            <span class="core-chip">
              <span></span>
            </span>
          </div>
        </div>

        <button
          v-for="(stage, index) in processStages"
          :key="stage.key || stage.title"
          :aria-label="stage.path ? `打开${stage.title}` : stage.title"
          class="stage-card"
          :class="[
            `stage-${index + 1}`,
            {
              'has-badge': hasBadge(stage.badge),
              'is-highlight': stage.highlight,
              'is-no-permission': stage.noPermission,
            },
          ]"
          :disabled="!stage.path || stage.disabled"
          :title="stage.noPermission ? '当前账号无此菜单权限' : undefined"
          type="button"
          @click="openPath(stage.path, stage.disabled)"
        >
          <div v-if="hasBadge(stage.badge)" class="stage-badge">
            {{ stage.badgeLabel || formatBadgeCount(stage.badge) }}
          </div>
          <div class="stage-index">
            {{ String(index + 1).padStart(2, '0') }}
          </div>
          <div
            class="artifact"
            :class="`artifact-${stage.artifact}`"
            aria-hidden="true"
          >
            <span class="shape-main"></span>
            <span class="shape-a"></span>
            <span class="shape-b"></span>
            <span class="shape-c"></span>
          </div>
          <div class="stage-copy">
            <div class="stage-title">
              <IconifyIcon :icon="stage.icon" />
              <strong>{{ stage.title }}</strong>
            </div>
            <span>{{ stage.output }}</span>
            <p>{{ stage.detail }}</p>
          </div>
        </button>

        <div v-if="processStages.length === 0" class="stage-empty">
          {{ emptyText }}
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.workbench-process {
  --process-primary: #0f5132;
  --process-secondary: #0f766e;
  --process-accent: #e07812;
  --process-bg: #f5f9fb;
  --process-card: rgb(255 255 255 / 86%);
  --process-line: rgb(15 81 50 / 14%);
  --process-muted: #5d6f83;
  --process-text: #102033;
  --process-height: calc(100dvh - 118px);
  --process-pad-x: clamp(18px, 2vw, 34px);
  --process-pad-y: clamp(14px, 1.8vh, 24px);
  --stage-min-width: clamp(220px, 16vw, 285px);
  --stage-row-height: clamp(120px, 15vh, 190px);

  min-height: var(--process-height);
  padding: var(--process-pad-y) var(--process-pad-x);
  overflow: hidden;
  color: var(--process-text);
  background:
    radial-gradient(
      circle at 78% 92%,
      rgb(15 118 110 / 9%) 0 9%,
      transparent 18%
    ),
    linear-gradient(135deg, rgb(255 255 255 / 86%), rgb(235 243 247 / 92%)),
    repeating-linear-gradient(
      90deg,
      transparent 0,
      transparent 42px,
      rgb(15 81 50 / 4%) 42px,
      rgb(15 81 50 / 4%) 43px
    );
  box-sizing: border-box;
}

.workbench-process.is-quality {
  --process-primary: #164e63;
  --process-secondary: #2563eb;
  --process-accent: #dc2626;
  --process-line: rgb(37 99 235 / 13%);
}

.process-shell {
  display: grid;
  width: 100%;
  height: calc(var(--process-height) - (var(--process-pad-y) * 2));
  min-height: 0;
  gap: clamp(12px, 1.6vh, 20px);
  grid-template-rows: auto minmax(0, 1fr);
}

.process-header {
  display: grid;
  align-items: center;
  gap: clamp(18px, 1.8vw, 30px);
  min-height: clamp(76px, 9vh, 104px);
  padding: 0 4px;
  grid-template-columns: minmax(420px, 1fr) minmax(260px, auto);
}

.title-block {
  min-width: 0;
}

.title-line {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.title-line :deep(svg) {
  flex: none;
  color: var(--process-secondary);
  font-size: 23px;
}

.title-block h1 {
  display: flex;
  align-items: baseline;
  min-width: 0;
  margin: 0;
  color: #0b1728;
  font-size: clamp(25px, 1.8vw, 32px);
  font-weight: 900;
  gap: 8px;
  letter-spacing: 0;
  line-height: 1.12;
  white-space: nowrap;
}

.title-block h1 span {
  color: #0b1728;
  font-weight: 900;
}

.title-block h1 i {
  width: 1px;
  height: 0.78em;
  background: rgb(16 32 51 / 20%);
}

.title-block h1 strong {
  color: #0b1728;
  font-weight: 900;
}

.title-block p {
  max-width: 780px;
  margin: 7px 0 0;
  color: #4e637b;
  font-size: clamp(13px, 0.86vw, 15px);
  line-height: 1.58;
}

.domain-strip {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.header-actions {
  display: flex;
  align-items: flex-end;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
}

.reminder-strip {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.reminder-action {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid rgb(16 32 51 / 9%);
  border-radius: 8px;
  color: #0f5132;
  font: inherit;
  font-size: 12px;
  font-weight: 900;
  background: rgb(255 255 255 / 82%);
  box-shadow: 0 8px 20px rgb(16 32 51 / 8%);
  cursor: pointer;
  gap: 7px;
}

.reminder-action :deep(svg) {
  color: var(--process-accent);
  font-size: 15px;
}

.reminder-action em {
  color: #64748b;
  font-style: normal;
  font-weight: 700;
}

.reminder-action.is-warning {
  color: #92400e;
}

.reminder-action.is-success {
  color: #047857;
}

.reminder-action:disabled {
  cursor: default;
}

.reminder-action.is-no-permission {
  color: #94a3b8;
  background: rgb(241 245 249 / 84%);
  border-color: rgb(148 163 184 / 24%);
  box-shadow: none;
  cursor: not-allowed;
}

.reminder-action.is-no-permission :deep(svg),
.reminder-action.is-no-permission em {
  color: #94a3b8;
}

.reminder-action:not(:disabled):hover {
  border-color: rgb(224 120 18 / 34%);
  box-shadow: 0 10px 24px rgb(224 120 18 / 12%);
  transform: translateY(-1px);
}

.reminder-badge {
  position: absolute;
  top: -7px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border: 2px solid #fff;
  border-radius: 999px;
  color: #fff;
  font-size: 11px;
  font-weight: 900;
  line-height: 14px;
  text-align: center;
  background: #dc2626;
  box-shadow: 0 4px 10px rgb(220 38 38 / 24%);
}

.domain-action {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid rgb(16 32 51 / 9%);
  border-radius: 8px;
  color: #334155;
  font: inherit;
  font-size: 12px;
  font-weight: 900;
  background: rgb(255 255 255 / 70%);
  cursor: pointer;
  gap: 7px;
}

.domain-action:disabled {
  cursor: default;
}

.domain-action.is-no-permission {
  color: #94a3b8;
  background: rgb(241 245 249 / 76%);
  border-color: rgb(148 163 184 / 24%);
  cursor: not-allowed;
}

.domain-action:not(:disabled):hover {
  border-color: rgb(224 120 18 / 34%);
  box-shadow: 0 8px 18px rgb(224 120 18 / 10%);
  transform: translateY(-1px);
}

.domain-action :deep(svg) {
  color: var(--process-accent);
}

.domain-action.is-no-permission :deep(svg) {
  color: #94a3b8;
}

.process-map {
  position: relative;
  display: grid;
  align-content: start;
  min-height: 0;
  padding: clamp(26px, 2.6vh, 42px) clamp(18px, 2vw, 32px)
    clamp(18px, 2vw, 32px);
  overflow: hidden;
  border: 1px solid rgb(16 32 51 / 10%);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 92%), rgb(247 250 252 / 82%)),
    radial-gradient(
      circle at 50% 52%,
      rgb(15 118 110 / 9%) 0 16%,
      transparent 30%
    );
  box-shadow: 0 24px 64px rgb(16 32 51 / 12%);
  gap: clamp(10px, 1vw, 16px);
  grid-auto-flow: row dense;
  grid-auto-rows: var(--stage-row-height);
  grid-template-columns: repeat(
    auto-fit,
    minmax(min(100%, var(--stage-min-width)), 1fr)
  );
}

.map-grid {
  position: absolute;
  border: 1px solid var(--process-line);
  content: '';
  inset: 12% 4%;
  pointer-events: none;
}

.map-grid::before,
.map-grid::after {
  position: absolute;
  background: var(--process-line);
  content: '';
}

.map-grid::before {
  top: 50%;
  right: 0;
  left: 0;
  height: 1px;
}

.map-grid::after {
  top: 0;
  bottom: 0;
  left: 50%;
  width: 1px;
}

.product-core {
  position: absolute;
  z-index: 0;
  right: clamp(18px, 3vw, 44px);
  bottom: clamp(18px, 3vh, 40px);
  width: clamp(245px, 22vw, 340px);
  min-width: 0;
  height: clamp(150px, 17vh, 220px);
  color: var(--process-primary);
  opacity: 0.86;
  pointer-events: none;
}

.core-illustration {
  position: relative;
  width: 100%;
  height: 100%;
}

.core-illustration > span {
  position: absolute;
  display: block;
}

.core-shadow {
  right: 4%;
  bottom: 2%;
  left: 4%;
  height: 30%;
  border-radius: 50%;
  background: radial-gradient(
    ellipse at center,
    rgb(16 32 51 / 15%) 0 24%,
    transparent 72%
  );
  filter: blur(1px);
}

.core-pad {
  bottom: 13%;
  left: 4%;
  width: 45%;
  aspect-ratio: 1;
  border-radius: 50%;
  background:
    radial-gradient(
      circle at 38% 28%,
      rgb(255 255 255 / 92%) 0 8%,
      transparent 9%
    ),
    radial-gradient(
      circle,
      transparent 0 36%,
      rgb(15 118 110 / 20%) 37% 40%,
      transparent 41%
    ),
    repeating-radial-gradient(
      circle,
      rgb(15 81 50 / 10%) 0 1px,
      transparent 2px 13px
    ),
    linear-gradient(145deg, #f4fff9, #afd8c5 72%, #8dbba6);
  box-shadow:
    inset 0 4px 10px rgb(255 255 255 / 78%),
    inset 0 -12px 18px rgb(15 81 50 / 18%),
    0 18px 26px rgb(16 32 51 / 18%);
}

.core-pad > span {
  position: absolute;
  inset: 21%;
  border: 2px dashed rgb(15 81 50 / 26%);
  border-radius: 50%;
}

.core-wafer {
  bottom: 34%;
  left: 30%;
  width: 27%;
  aspect-ratio: 1;
  border-radius: 50%;
  background:
    linear-gradient(
      118deg,
      transparent 0 46%,
      rgb(37 99 235 / 16%) 47% 50%,
      transparent 51%
    ),
    linear-gradient(
      28deg,
      transparent 0 42%,
      rgb(15 118 110 / 16%) 43% 46%,
      transparent 47%
    ),
    radial-gradient(
      circle at 36% 28%,
      rgb(255 255 255 / 88%) 0 10%,
      transparent 12%
    ),
    linear-gradient(145deg, #f4fbff, #c5dce8);
  box-shadow:
    inset 0 2px 7px rgb(255 255 255 / 86%),
    0 13px 18px rgb(16 32 51 / 13%);
}

.core-wafer > span {
  position: absolute;
  inset: 18%;
  border: 1px solid rgb(37 99 235 / 20%);
  border-radius: 50%;
}

.core-bottle {
  right: 21%;
  bottom: 18%;
  width: 18%;
  height: 58%;
  border-radius: 16px 16px 20px 20px;
  background:
    linear-gradient(to top, rgb(15 118 110 / 34%) 0 44%, transparent 45%),
    linear-gradient(145deg, rgb(255 255 255 / 86%), rgb(224 241 239 / 88%));
  box-shadow:
    inset 0 1px 8px rgb(255 255 255 / 86%),
    0 14px 18px rgb(16 32 51 / 15%);
  clip-path: polygon(
    30% 0,
    70% 0,
    70% 18%,
    88% 36%,
    88% 100%,
    12% 100%,
    12% 36%,
    30% 18%
  );
}

.core-bottle > span {
  position: absolute;
  right: 22%;
  bottom: 24%;
  left: 22%;
  height: 4px;
  border-radius: 999px;
  background: rgb(255 255 255 / 78%);
}

.core-inspector {
  right: 1%;
  bottom: 20%;
  width: 26%;
  height: 42%;
  border: 1px solid rgb(16 32 51 / 10%);
  border-radius: 10px;
  background:
    linear-gradient(var(--process-accent), var(--process-accent)) 20% 26% / 48%
      3px no-repeat,
    linear-gradient(rgb(37 99 235 / 22%), rgb(37 99 235 / 22%)) 20% 45% / 62%
      3px no-repeat,
    linear-gradient(rgb(15 118 110 / 20%), rgb(15 118 110 / 20%)) 20% 64% / 38%
      3px no-repeat,
    linear-gradient(145deg, rgb(255 255 255 / 88%), rgb(235 242 247 / 88%));
  box-shadow:
    inset 0 1px 6px rgb(255 255 255 / 78%),
    0 14px 20px rgb(16 32 51 / 13%);
  transform: rotate(-3deg);
}

.core-inspector > span {
  position: absolute;
  right: 13%;
  bottom: 10%;
  width: 24%;
  aspect-ratio: 1;
  border: 2px solid var(--process-accent);
  border-radius: 50%;
}

.core-probe {
  right: 40%;
  bottom: 28%;
  width: 19%;
  height: 38%;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    transparent 0 44%,
    rgb(16 32 51 / 26%) 45% 52%,
    transparent 53%
  );
  transform: rotate(18deg);
}

.core-probe > span {
  position: absolute;
  right: 18%;
  bottom: -2%;
  width: 38%;
  aspect-ratio: 1;
  border-radius: 50%;
  background: var(--process-accent);
  box-shadow: 0 0 0 5px rgb(224 120 18 / 14%);
}

.core-chip {
  right: 11%;
  top: 15%;
  width: 22%;
  aspect-ratio: 1;
  border-radius: 8px;
  background:
    repeating-linear-gradient(
      90deg,
      transparent 0 7px,
      rgb(16 32 51 / 16%) 7px 8px
    ),
    repeating-linear-gradient(
      0deg,
      transparent 0 7px,
      rgb(16 32 51 / 14%) 7px 8px
    ),
    linear-gradient(145deg, #f8fbff, #cfdde9);
  box-shadow:
    inset 0 1px 6px rgb(255 255 255 / 84%),
    0 12px 18px rgb(16 32 51 / 13%);
  transform: rotate(8deg);
}

.core-chip > span {
  position: absolute;
  inset: 28%;
  border-radius: 4px;
  background: rgb(37 99 235 / 20%);
}

.workbench-process.is-quality .core-pad {
  background:
    radial-gradient(
      circle at 38% 28%,
      rgb(255 255 255 / 92%) 0 8%,
      transparent 9%
    ),
    radial-gradient(
      circle,
      transparent 0 36%,
      rgb(37 99 235 / 22%) 37% 40%,
      transparent 41%
    ),
    repeating-radial-gradient(
      circle,
      rgb(37 99 235 / 10%) 0 1px,
      transparent 2px 13px
    ),
    linear-gradient(145deg, #f7fbff, #bdd4f6 72%, #9ab8e7);
}

.workbench-process.is-quality .core-bottle {
  background:
    linear-gradient(to top, rgb(37 99 235 / 34%) 0 44%, transparent 45%),
    linear-gradient(145deg, rgb(255 255 255 / 86%), rgb(224 235 250 / 88%));
}

.workbench-process.is-quality .core-probe > span {
  background: #dc2626;
  box-shadow: 0 0 0 5px rgb(220 38 38 / 14%);
}

.stage-card {
  position: relative;
  z-index: 1;
  display: grid;
  container-type: inline-size;
  min-width: 0;
  min-height: 0;
  padding: clamp(12px, 1.2vw, 18px);
  overflow: hidden;
  border: 1px solid rgb(16 32 51 / 10%);
  border-radius: 16px;
  color: inherit;
  font: inherit;
  text-align: left;
  background: var(--process-card);
  box-shadow: 0 16px 34px rgb(16 32 51 / 11%);
  cursor: pointer;
  gap: clamp(8px, 1vh, 12px);
  grid-template-columns: minmax(74px, 0.72fr) minmax(0, 1fr);
}

.stage-card:disabled {
  cursor: default;
}

.stage-card.is-no-permission {
  color: #94a3b8;
  background: rgb(241 245 249 / 78%);
  border-color: rgb(148 163 184 / 24%);
  box-shadow: none;
  cursor: not-allowed;
}

.stage-card.is-no-permission .artifact {
  opacity: 0.42;
  filter: grayscale(1);
}

.stage-card.is-no-permission .stage-title,
.stage-card.is-no-permission .stage-copy > span,
.stage-card.is-no-permission .stage-copy p {
  color: #94a3b8;
}

.stage-card.is-no-permission .stage-title :deep(svg) {
  color: #94a3b8;
}

.stage-card.is-no-permission .stage-index {
  color: rgb(148 163 184 / 30%);
}

.stage-card:not(:disabled):hover {
  border-color: rgb(224 120 18 / 30%);
  box-shadow: 0 18px 40px rgb(16 32 51 / 14%);
  transform: translateY(-2px);
}

.stage-card.is-highlight {
  border-color: rgb(239 68 68 / 40%);
  background: linear-gradient(
    135deg,
    rgb(255 255 255 / 96%),
    rgb(255 247 237 / 88%)
  );
  box-shadow:
    0 18px 42px rgb(239 68 68 / 14%),
    inset 0 0 0 1px rgb(239 68 68 / 8%);
}

.stage-card.is-highlight .stage-title,
.stage-card.is-highlight .stage-copy > span {
  color: #7f1d1d;
}

.stage-card.is-highlight .stage-title :deep(svg) {
  color: #dc2626;
}

.stage-badge {
  position: absolute;
  top: 10px;
  right: 12px;
  z-index: 2;
  min-width: 24px;
  height: 22px;
  padding: 0 7px;
  border: 2px solid rgb(255 255 255 / 88%);
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  line-height: 18px;
  text-align: center;
  background: #dc2626;
  box-shadow: 0 6px 14px rgb(220 38 38 / 22%);
}

.stage-index {
  position: absolute;
  top: 10px;
  right: 12px;
  color: rgb(16 32 51 / 18%);
  font-size: clamp(18px, 8cqw, 30px);
  font-weight: 900;
}

.stage-card.has-badge .stage-index {
  top: 36px;
}

.stage-empty {
  position: relative;
  z-index: 1;
  display: grid;
  min-height: 160px;
  border: 1px dashed rgb(16 32 51 / 14%);
  border-radius: 16px;
  color: #64748b;
  font-size: 14px;
  font-weight: 800;
  background: rgb(255 255 255 / 62%);
  grid-column: 1 / -1;
  place-items: center;
}

.stage-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
}

.stage-title {
  display: flex;
  align-items: center;
  color: var(--process-primary);
  gap: 7px;
}

.stage-title :deep(svg) {
  color: var(--process-accent);
  font-size: clamp(18px, 6.5cqw, 30px);
}

.stage-title strong {
  overflow: hidden;
  font-size: clamp(16px, 7cqw, 26px);
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-copy > span {
  margin-top: 5px;
  color: #123b2c;
  font-size: clamp(13px, 5.4cqw, 21px);
  font-weight: 900;
}

.stage-copy p {
  display: -webkit-box;
  margin: 6px 0 0;
  overflow: hidden;
  color: var(--process-muted);
  font-size: clamp(12px, 4.25cqw, 18px);
  line-height: 1.48;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.artifact {
  position: relative;
  display: grid;
  align-self: center;
  width: min(100%, clamp(72px, 36cqw, 136px));
  aspect-ratio: 1;
  justify-self: center;
  place-items: center;
}

.artifact span {
  position: absolute;
  display: block;
}

.shape-main {
  width: 72%;
  aspect-ratio: 1;
  border-radius: 18px;
  background: linear-gradient(145deg, #ffffff, #dce9ef);
  box-shadow:
    inset 0 1px 5px rgb(255 255 255 / 88%),
    0 10px 18px rgb(16 32 51 / 12%);
}

.artifact-paper .shape-main {
  width: 58%;
  border-radius: 8px;
  background:
    linear-gradient(#0f766e, #0f766e) 18% 22% / 44% 3px no-repeat,
    linear-gradient(#d6e3ec, #d6e3ec) 18% 42% / 64% 3px no-repeat,
    linear-gradient(#d6e3ec, #d6e3ec) 18% 60% / 54% 3px no-repeat,
    #ffffff;
}

.artifact-paper .shape-a {
  right: 22%;
  bottom: 20%;
  width: 18%;
  aspect-ratio: 1;
  border: 2px solid var(--process-accent);
  border-radius: 50%;
}

.artifact-flask .shape-main {
  width: 48%;
  border-radius: 14px 14px 22px 22px;
  background:
    linear-gradient(to top, rgb(15 118 110 / 42%) 0 46%, transparent 47%),
    linear-gradient(145deg, #ffffff, #e6f2f1);
  clip-path: polygon(
    34% 0,
    66% 0,
    66% 30%,
    86% 74%,
    78% 100%,
    22% 100%,
    14% 74%,
    34% 30%
  );
}

.artifact-flask .shape-a,
.artifact-flask .shape-b,
.artifact-flask .shape-c {
  border-radius: 50%;
  background: rgb(15 118 110 / 36%);
}

.artifact-flask .shape-a {
  bottom: 25%;
  left: 37%;
  width: 7px;
  height: 7px;
}

.artifact-flask .shape-b {
  right: 35%;
  bottom: 36%;
  width: 5px;
  height: 5px;
}

.artifact-slab .shape-main {
  width: 78%;
  height: 46%;
  border-radius: 18px;
  background:
    radial-gradient(
      circle at 28% 45%,
      rgb(15 118 110 / 18%) 0 5px,
      transparent 6px
    ),
    radial-gradient(
      circle at 62% 58%,
      rgb(15 118 110 / 16%) 0 4px,
      transparent 5px
    ),
    linear-gradient(145deg, #f5fff9, #c7e8da);
  transform: perspective(80px) rotateX(18deg);
}

.artifact-disc .shape-main,
.artifact-groove .shape-main,
.artifact-adhesive .shape-main,
.artifact-cut .shape-main,
.artifact-measure .shape-main,
.artifact-scan .shape-main {
  width: 72%;
  border-radius: 50%;
  background:
    radial-gradient(
      circle at 35% 28%,
      rgb(255 255 255 / 90%) 0 9px,
      transparent 10px
    ),
    radial-gradient(
      circle,
      transparent 0 34%,
      rgb(15 118 110 / 22%) 35% 38%,
      transparent 39%
    ),
    linear-gradient(145deg, #effff8, #b9dfce);
}

.artifact-disc .shape-a {
  width: 52%;
  border-top: 3px solid rgb(15 81 50 / 20%);
  transform: rotate(-12deg);
}

.artifact-layers .shape-main,
.artifact-strip .shape-main {
  width: 72%;
  height: 44%;
  border-radius: 12px;
  background: linear-gradient(
    180deg,
    #d9f4e8 0 32%,
    #ffffff 33% 54%,
    #a9d7c3 55% 100%
  );
  transform: skewX(-10deg);
}

.artifact-layers .shape-a {
  bottom: 25%;
  width: 66%;
  height: 12%;
  border-radius: 999px;
  background: rgb(224 120 18 / 28%);
}

.artifact-strip .shape-a,
.artifact-strip .shape-b,
.artifact-strip .shape-c {
  width: 8%;
  height: 58%;
  border-radius: 999px;
  background: rgb(15 81 50 / 22%);
}

.artifact-strip .shape-a {
  left: 36%;
}

.artifact-strip .shape-b {
  left: 49%;
}

.artifact-strip .shape-c {
  left: 62%;
}

.artifact-groove .shape-a {
  width: 58%;
  aspect-ratio: 1;
  border: 2px dashed rgb(15 81 50 / 32%);
  border-radius: 50%;
}

.artifact-groove .shape-b {
  width: 48%;
  height: 4px;
  border-radius: 999px;
  background: rgb(15 81 50 / 28%);
  transform: rotate(-18deg);
}

.artifact-adhesive .shape-a {
  right: 13%;
  bottom: 16%;
  width: 52%;
  height: 16%;
  border-radius: 999px;
  background: rgb(224 120 18 / 32%);
}

.artifact-cut .shape-a,
.artifact-cut .shape-b {
  width: 12%;
  aspect-ratio: 1;
  border-radius: 4px;
  background: rgb(224 120 18 / 34%);
}

.artifact-cut .shape-a {
  top: 18%;
  right: 16%;
}

.artifact-cut .shape-b {
  bottom: 15%;
  left: 20%;
}

.artifact-box .shape-main {
  width: 72%;
  height: 56%;
  border-radius: 10px;
  background:
    linear-gradient(
      135deg,
      transparent 0 46%,
      rgb(15 81 50 / 14%) 47% 52%,
      transparent 53%
    ),
    linear-gradient(145deg, #fff7e8, #e8c88f);
  transform: perspective(100px) rotateX(8deg);
}

.artifact-box .shape-a {
  top: 24%;
  width: 44%;
  height: 2px;
  background: rgb(15 81 50 / 25%);
}

@supports not (font-size: 1cqw) {
  .stage-index {
    font-size: clamp(20px, 1.35vw, 30px);
  }

  .stage-title :deep(svg) {
    font-size: clamp(20px, 1.55vw, 28px);
  }

  .stage-title strong {
    font-size: clamp(16px, 1.25vw, 24px);
  }

  .stage-copy > span {
    font-size: clamp(13px, 1vw, 19px);
  }

  .stage-copy p {
    font-size: clamp(12px, 0.9vw, 16px);
  }

  .artifact {
    width: min(100%, clamp(72px, 6.5vw, 128px));
  }
}

.artifact-measure .shape-a {
  width: 82%;
  height: 8px;
  border-radius: 999px;
  background:
    repeating-linear-gradient(90deg, #164e63 0 2px, transparent 2px 9px),
    #dff0f7;
}

.artifact-scan .shape-a,
.artifact-scan .shape-b {
  width: 72%;
  height: 2px;
  border-radius: 999px;
  background: rgb(37 99 235 / 38%);
}

.artifact-scan .shape-a {
  transform: translateY(-12px);
}

.artifact-scan .shape-b {
  transform: translateY(12px);
}

@media (max-width: 1280px) {
  .process-header {
    grid-template-columns: 1fr;
  }

  .domain-strip {
    justify-content: flex-start;
  }

  .header-actions,
  .reminder-strip {
    align-items: flex-start;
    justify-content: flex-start;
  }

  .stage-card {
    grid-template-columns: minmax(58px, 0.55fr) minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .workbench-process {
    min-height: calc(100dvh - 96px);
    overflow-x: hidden;
    overflow-y: auto;
  }

  .process-shell {
    height: auto;
  }

  .process-header {
    grid-template-columns: 1fr;
  }

  .domain-strip {
    justify-content: flex-start;
  }

  .header-actions,
  .reminder-strip {
    align-items: flex-start;
    justify-content: flex-start;
  }

  .process-map {
    align-content: start;
    grid-auto-rows: auto;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: auto;
  }

  .map-grid,
  .product-core {
    display: none;
  }

  .stage-card {
    grid-column: auto;
    grid-row: auto;
    min-height: 136px;
  }
}

@media (max-width: 640px) {
  .workbench-process {
    padding: 14px;
  }

  .title-block h1 {
    font-size: 30px;
  }

  .title-line {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  .title-block h1 {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .title-block h1 i {
    display: none;
  }

  .process-map {
    padding: 14px;
    grid-template-columns: 1fr;
  }

  .stage-card {
    min-height: 124px;
    grid-template-columns: 74px minmax(0, 1fr);
  }

  .stage-title :deep(svg) {
    font-size: 18px;
  }

  .stage-title strong {
    font-size: 16px;
  }

  .stage-copy > span {
    font-size: 13px;
  }

  .stage-copy p {
    font-size: 12px;
  }

  .artifact {
    width: 72px;
  }
}
</style>

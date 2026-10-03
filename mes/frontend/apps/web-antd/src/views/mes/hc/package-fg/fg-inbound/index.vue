<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';

import { computed, nextTick, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Button,
  Input,
  Modal,
  Switch,
  Tabs,
  TabPane,
  Tag,
  Tree,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  confirmFgInboundPackages,
  directOutboundPendingFgInboundPackages,
  downShelfFgInboundPackages,
  getFgPackageBoxInboundDetail,
  getFgInboundPackagePage,
  getFgInboundPackageSegmentPackageList,
  getFgInboundPackageSegmentPage,
  getFgLocationGrid,
  manualOutboundFgInboundPackages,
} from '#/api/mes/hc/package-fg/finished-packaging';

import '../shared/cut-round-board.css';

defineOptions({ name: 'MesPackageFgInbound' });

type InboundPackage = MesHcFinishedPackagingApi.InboundBox;
type FgLocation = MesHcFinishedPackagingApi.FgLocationGrid;
type InboundPackageDisplayRow = Partial<InboundPackage> & {
  __group?: boolean;
  children?: InboundPackageDisplayRow[];
  groupPackageCount?: number;
  groupPieceCount?: number;
  hasChild?: boolean;
  rowKey: string;
  sampleSliceBatchNos?: string[];
  segmentBatchNo?: string;
  shelfStatus: MesHcFinishedPackagingApi.InboundPackageShelfStatus;
};
type LocationMeta = {
  areaNo: number;
  areaText: string;
  isStructured: boolean;
  layerName?: string;
  layerNo: number;
  layerText: string;
  palletNo: number;
  palletText: string;
  rackName?: string;
  rackNo: string;
};
type LocationTreeNode = {
  availableQty?: number;
  children?: LocationTreeNode[];
  count?: number;
  disabled?: boolean;
  icon?: string;
  key: string;
  locationCode?: string;
  locationName?: string;
  nodeType: 'area' | 'layer' | 'pallet' | 'rack' | 'warehouse';
  occupied?: boolean;
  occupiedSliceBatchNo?: string;
  palletText?: string;
  qualityMatched?: boolean;
  qualityScope?: string;
  statusText?: string;
  title: string;
};

const userStore = useUserStore();
const currentUserName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统',
);

const activeTab = ref('pending');
const pendingQualityStatus = ref<'FROZEN' | 'NG' | 'OK'>('OK');
const pendingViewMode = ref<'flat' | 'group'>('group');
const shelvedViewMode = ref<'flat' | 'group'>('group');
const packageLoading = ref(false);
const locationLoading = ref(false);
const shelfing = ref(false);
const downShelfing = ref(false);
const directOutbounding = ref(false);
const manualOutbounding = ref(false);
const detailVisible = ref(false);
const directOutboundVisible = ref(false);
const manualOutboundVisible = ref(false);
const packageKeyword = ref('');
const locationKeyword = ref('');
const pendingPackageTotal = ref(0);
const shelvedPackageTotal = ref(0);
const locations = ref<FgLocation[]>([]);
const selectedPackage = ref<InboundPackage | null>(null);
const selectedPendingPackages = ref<InboundPackage[]>([]);
const selectedShelvedPackages = ref<InboundPackage[]>([]);
const directOutboundPackages = ref<InboundPackage[]>([]);
const directOutboundReason = ref('');
const manualOutboundPackages = ref<InboundPackage[]>([]);
const manualOutboundReason = ref('');
const selectedLocationCode = ref('');
const freeLocationOnly = ref(false);

const selectedLocation = computed(() =>
  locations.value.find(
    (item) => item.locationCode === selectedLocationCode.value,
  ),
);
const selectedItems = computed(() => selectedPackage.value?.items || []);
const activeViewMode = computed(() =>
  activeTab.value === 'pending' ? pendingViewMode.value : shelvedViewMode.value,
);
const selectedPendingPackageCount = computed(
  () => selectedPendingPackages.value.length,
);
const selectedShelvedPackageCount = computed(
  () => selectedShelvedPackages.value.length,
);
const pendingQualityText = computed(() =>
  pendingQualityStatus.value === 'NG' ? '不合格品' : pendingQualityStatus.value === 'FROZEN' ? '冻结品' : '合格品',
);
const pendingLocationQualityScope = computed(() =>
  pendingQualityStatus.value === 'NG' ? 'QUARANTINE' : 'QUALIFIED',
);
const locationPanelHint = computed(() =>
  activeTab.value === 'pending'
    ? `已展示全部库位；仅可选择${pendingQualityText.value}质量匹配库位`
    : '右侧查看包装成品库库位',
);
const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const filteredLocations = computed(() => {
  const keyword = locationKeyword.value.trim().toLowerCase();
  return locations.value.filter((item) => {
    if (
      freeLocationOnly.value &&
      (item.status !== '启用' || Number(item.availableQty || 0) <= 0)
    ) {
      return false;
    }
    if (!keyword) return true;
    return [
      item.locationCode,
      item.locationName,
      item.positionDesc,
      item.rackName,
      item.layerName,
      item.occupiedSliceBatchNo,
      item.occupiedInnerUnitNo,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword));
  });
});
const locationTreeData = computed(() =>
  buildLocationTree(filteredLocations.value),
);
const selectedTreeKeys = computed(() =>
  selectedLocationCode.value ? [selectedLocationCode.value] : [],
);

function statusText(status?: string) {
  const map: Record<string, string> = {
    INBOUNDED: '已上架',
    INBOUND_LOCKED: '待上架',
    PACKED: '已包装待上架',
  };
  return map[status || ''] || status || '-';
}

function statusColor(status?: string) {
  const map: Record<string, string> = {
    INBOUNDED: 'green',
    INBOUND_LOCKED: 'gold',
    PACKED: 'blue',
  };
  return map[status || ''] || 'default';
}

function resultColor(result?: string) {
  if (result === 'FROZEN') return 'gold';
  if (result === 'OK') return 'green';
  if (result === 'NG') return 'red';
  return 'default';
}

function locationQualityScopeText(qualityScope?: string) {
  if (qualityScope === 'QUALIFIED') return '合格品库位';
  if (qualityScope === 'QUARANTINE') return '不合格品隔离库位';
  return '未配置质量用途';
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function locationText(row?: InboundPackage | null) {
  if (!row) return '-';
  const location = locations.value.find(
    (item) => item.locationCode === row.locationCode,
  );
  return (
    [
      row.locationCode,
      location ? formatLocationHierarchy(location) : row.locationName,
    ]
      .filter(Boolean)
      .join(' / ') || '-'
  );
}

function formatPackageSliceList(row?: Pick<InboundPackage, 'items'> | null) {
  const values = (row?.items || [])
    .map((item) => item.sliceBatchNo || item.productionBatchNo)
    .filter(Boolean);
  return values.join(',') || '-';
}

function hasOutboundLockedPieces(row?: InboundPackage | null) {
  return Number(row?.outboundLockedQty || 0) > 0;
}

function outboundLockedTip(row?: InboundPackage | null) {
  if (!hasOutboundLockedPieces(row)) return '';
  return `部分片号已发货锁定；拆箱剩余片需回到成品包装报工重新包装后再上架，请到发货配货处理锁定片。`;
}

function compareRackCode(a: string, b: string) {
  const an = Number(a);
  const bn = Number(b);
  if (Number.isFinite(an) && Number.isFinite(bn)) return an - bn;
  return a.localeCompare(b, 'zh-CN');
}

function compareLayerNo(a: number, b: number) {
  const an = a > 0 ? a : Number.MAX_SAFE_INTEGER;
  const bn = b > 0 ? b : Number.MAX_SAFE_INTEGER;
  return an - bn;
}

function parseLocationMeta(row?: FgLocation | null): LocationMeta {
  const code = String(row?.locationCode || '');
  const matched = /^(?:[A-Z][A-Z0-9_-]*-)?(\d+)-L(\d+)-(\d+)(?:-(\d+))?$/i.exec(
    code,
  );
  if (matched || row?.rackNo || row?.layerNo || row?.areaNo) {
    const layerNo = Number(row?.layerNo || matched?.[2] || 0);
    const areaNo = Number(row?.areaNo || matched?.[3] || 0);
    const palletNo = Number(matched?.[4] || 0);
    return {
      areaNo,
      areaText: `${areaNo}区`,
      isStructured: true,
      layerName: row?.layerName,
      layerNo,
      layerText: row?.layerName || `L${layerNo}层`,
      palletNo,
      palletText: matched?.[4]
        ? `${String(matched[4]).padStart(2, '0')}托盘`
        : '-',
      rackName: row?.rackName,
      rackNo: String(row?.rackNo || matched?.[1] || ''),
    };
  }
  return {
    areaNo: 0,
    areaText: '未分组',
    isStructured: false,
    layerNo: 0,
    layerText: '未分层',
    palletNo: Number(row?.gridNo || 0),
    palletText: String(row?.gridNo || row?.locationCode || '-'),
    rackNo: '未分组',
  };
}

function formatLocationHierarchy(row?: FgLocation | null) {
  const meta = parseLocationMeta(row);
  if (!meta.isStructured) return row?.locationName || row?.locationCode || '-';
  const rackText =
    meta.rackName ||
    (/^\d+$/.test(meta.rackNo) ? `${meta.rackNo}号货架` : meta.rackNo);
  return [
    row?.warehouseName || row?.warehouseCode,
    rackText,
    meta.layerText,
    meta.areaText,
  ]
    .filter(Boolean)
    .join(' / ');
}

function compareLocation(a: FgLocation, b: FgLocation) {
  const warehouseCompare = String(a.warehouseCode || '').localeCompare(
    String(b.warehouseCode || ''),
    'zh-CN',
  );
  if (warehouseCompare !== 0) return warehouseCompare;
  const am = parseLocationMeta(a);
  const bm = parseLocationMeta(b);
  const rackCompare = compareRackCode(am.rackNo, bm.rackNo);
  if (rackCompare !== 0) return rackCompare;
  if (am.layerNo !== bm.layerNo) return compareLayerNo(am.layerNo, bm.layerNo);
  if (am.areaNo !== bm.areaNo) return am.areaNo - bm.areaNo;
  if (am.palletNo !== bm.palletNo) return am.palletNo - bm.palletNo;
  return Number(a.gridNo || 0) - Number(b.gridNo || 0);
}

function getLocationStatusText(row: FgLocation) {
  if (row.status !== '启用') return '停用';
  if (Number(row.availableQty || 0) > 0) return '空闲';
  return '已满';
}

function getPackageLocationMeta(row?: InboundPackage | null) {
  const currentLocation = locations.value.find(
    (item) => item.locationCode === row?.locationCode,
  );
  const meta = parseLocationMeta(
    currentLocation ||
      ({
        gridNo: 0,
        locationCode: row?.locationCode,
        locationName: row?.locationName,
      } as FgLocation),
  );
  return {
    area: meta.areaNo > 0 ? `${meta.areaNo}区` : '-',
    layer: meta.layerNo > 0 ? meta.layerText : '-',
    pallet: meta.isStructured ? meta.palletText : '-',
    rack:
      meta.rackName ||
      (/^\d+$/.test(meta.rackNo) ? `${meta.rackNo}号货架` : meta.rackNo || '-'),
    warehouse:
      currentLocation?.warehouseName ||
      currentLocation?.warehouseCode ||
      row?.warehouseName ||
      row?.warehouseCode ||
      '-',
  };
}

function buildLocationTree(rows: FgLocation[]): LocationTreeNode[] {
  const warehouseMap = new Map<
    string,
    Map<string, Map<number, FgLocation[]>>
  >();
  rows.forEach((row) => {
    const meta = parseLocationMeta(row);
    const warehouseKey = String(
      row.warehouseId || row.warehouseCode || 'UNASSIGNED',
    );
    if (!warehouseMap.has(warehouseKey))
      warehouseMap.set(warehouseKey, new Map());
    const rackMap = warehouseMap.get(warehouseKey)!;
    if (!rackMap.has(meta.rackNo)) rackMap.set(meta.rackNo, new Map());
    const layerMap = rackMap.get(meta.rackNo)!;
    if (!layerMap.has(meta.layerNo)) layerMap.set(meta.layerNo, []);
    layerMap.get(meta.layerNo)!.push(row);
  });
  return [...warehouseMap.entries()]
    .sort(([aKey], [bKey]) => {
      const aLocation = rows.find(
        (row) =>
          String(row.warehouseId || row.warehouseCode || 'UNASSIGNED') === aKey,
      );
      const bLocation = rows.find(
        (row) =>
          String(row.warehouseId || row.warehouseCode || 'UNASSIGNED') === bKey,
      );
      return String(aLocation?.warehouseCode || '').localeCompare(
        String(bLocation?.warehouseCode || ''),
        'zh-CN',
      );
    })
    .map(([warehouseKey, rackMap]) => {
      const warehouseRows = rows.filter(
        (row) =>
          String(row.warehouseId || row.warehouseCode || 'UNASSIGNED') ===
          warehouseKey,
      );
      const warehouseFirstLocation = warehouseRows[0];
      return {
        children: [...rackMap.entries()]
          .sort((a, b) => compareRackCode(a[0], b[0]))
          .map(([rackNo, layerMap]) => {
            const rackFirstLocation = [...layerMap.values()][0]?.[0];
            return {
              children: [...layerMap.entries()]
                .sort((a, b) => compareLayerNo(a[0], b[0]))
                .map(([layerNo, areas]) => ({
                  children: areas.sort(compareLocation).map((row) => {
                    const meta = parseLocationMeta(row);
                    const statusText = getLocationStatusText(row);
                    const qualityMatched =
                      activeTab.value !== 'pending' ||
                      row.qualityScope === pendingLocationQualityScope.value;
                    return {
                      availableQty: Number(row.availableQty || 0),
                      disabled:
                        Number(row.availableQty || 0) <= 0 ||
                        row.status !== '启用' ||
                        !qualityMatched,
                      icon: 'lucide:grid-2x2',
                      key: row.locationCode || `location-${row.id}`,
                      locationCode: row.locationCode,
                      locationName: row.locationName,
                      nodeType: 'area' as const,
                      occupied: row.occupied,
                      occupiedSliceBatchNo: row.occupiedSliceBatchNo,
                      palletText: meta.areaText,
                      qualityMatched,
                      qualityScope: row.qualityScope,
                      statusText,
                      title: `${row.locationCode || '-'} ${row.locationName || ''} ${locationQualityScopeText(row.qualityScope)} ${Number(row.occupiedQty || 0)}/${Number(row.capacityQty || 0)}片 ${statusText}`,
                    };
                  }),
                  count: areas.length,
                  icon: 'lucide:layers-3',
                  key: `warehouse-${warehouseKey}-rack-${rackNo}-layer-${layerNo}`,
                  nodeType: 'layer' as const,
                  title: areas[0]
                    ? parseLocationMeta(areas[0]).layerText
                    : layerNo > 0
                      ? `L${layerNo}层`
                      : '未分层',
                })),
              count: [...layerMap.values()].reduce(
                (sum, list) => sum + list.length,
                0,
              ),
              icon: 'lucide:archive',
              key: `warehouse-${warehouseKey}-rack-${rackNo}`,
              nodeType: 'rack' as const,
              title: rackFirstLocation
                ? parseLocationMeta(rackFirstLocation).rackName ||
                  (/^\d+$/.test(rackNo) ? `${rackNo}号货架` : rackNo)
                : rackNo,
            };
          }),
        count: warehouseRows.length,
        icon: 'lucide:warehouse',
        key: `warehouse-${warehouseKey}`,
        nodeType: 'warehouse' as const,
        title:
          warehouseFirstLocation?.warehouseName ||
          warehouseFirstLocation?.warehouseCode ||
          '未命名仓库',
      };
    });
}

async function fetchPackages() {
  packageLoading.value = true;
  try {
    await Promise.all([
      pendingPackageGridApi.query(),
      shelvedPackageGridApi.query(),
    ]);
  } finally {
    packageLoading.value = false;
  }
}

async function fetchLocations() {
  locationLoading.value = true;
  try {
    locations.value = await getFgLocationGrid();
    if (
      selectedLocationCode.value &&
      !locations.value.some(
        (item) => item.locationCode === selectedLocationCode.value,
      )
    ) {
      selectedLocationCode.value = '';
    }
  } finally {
    locationLoading.value = false;
  }
}

async function refreshAll() {
  await Promise.all([fetchPackages(), fetchLocations()]);
}

function isInboundPackageGroupRow(row?: InboundPackageDisplayRow) {
  return Boolean(row?.__group);
}

function isInboundPackageRow(
  row?: InboundPackageDisplayRow,
): row is InboundPackageDisplayRow & InboundPackage {
  return Boolean(row && !row.__group && row.id && row.boxNo);
}

function toInboundPackageRow(
  row: InboundPackage,
  shelfStatus: MesHcFinishedPackagingApi.InboundPackageShelfStatus,
): InboundPackageDisplayRow {
  return {
    ...row,
    rowKey: `package-${shelfStatus}-${row.id}`,
    segmentBatchNo: row.motherSegmentBatchNo || '未识别段批次',
    shelfStatus,
  };
}

function toInboundPackageSegmentRow(
  row: MesHcFinishedPackagingApi.InboundPackageSegment,
  shelfStatus: MesHcFinishedPackagingApi.InboundPackageShelfStatus,
  index: number,
): InboundPackageDisplayRow {
  const segmentBatchNo = row.segmentBatchNo || '未识别段批次';
  return {
    __group: true,
    groupPackageCount: Number(row.packageCount || 0),
    groupPieceCount: Number(row.totalPieceCount || 0),
    hasChild: true,
    materialCode: row.materialCode,
    materialName: row.materialName,
    modelCode: row.modelCode,
    planId: row.planId,
    planNo: row.planNo,
    planOperationId: row.planOperationId,
    rowKey: `segment-${shelfStatus}-${segmentBatchNo}-${index}`,
    sampleSliceBatchNos: row.sampleSliceBatchNos || [],
    segmentBatchNo,
    shelfStatus,
    qualityStatus: row.qualityStatus,
  };
}

function formatInboundPackageSegmentSamples(row: InboundPackageDisplayRow) {
  const samples = (row.sampleSliceBatchNos || []).filter(Boolean);
  const summary = `共 ${row.groupPackageCount || 0} 包，${row.groupPieceCount || 0} 片`;
  return samples.length > 0
    ? `${summary}，示例：${samples.join('、')}`
    : `${summary}，展开加载包装单`;
}

async function loadInboundPackageSegmentChildren(
  row: InboundPackageDisplayRow,
) {
  if (!isInboundPackageGroupRow(row) || !row.segmentBatchNo) return [];
  const packages = await getFgInboundPackageSegmentPackageList({
    keyword: packageKeyword.value.trim(),
    qualityStatus:
      row.shelfStatus === 'PENDING'
        ? ((row.qualityStatus || pendingQualityStatus.value) as 'FROZEN' | 'NG' | 'OK')
        : undefined,
    segmentBatchNo: row.segmentBatchNo,
    shelfStatus: row.shelfStatus,
  });
  return (packages || []).map((item) =>
    toInboundPackageRow(item, row.shelfStatus),
  );
}

function clearInboundPackageSelection(
  shelfStatus?: MesHcFinishedPackagingApi.InboundPackageShelfStatus,
) {
  if (!shelfStatus || shelfStatus === 'PENDING')
    selectedPendingPackages.value = [];
  if (!shelfStatus || shelfStatus === 'SHELVED')
    selectedShelvedPackages.value = [];
}

async function handleInboundPackageCheckboxChange(
  records: InboundPackageDisplayRow[],
  shelfStatus: MesHcFinishedPackagingApi.InboundPackageShelfStatus,
) {
  const packageMap = new Map<number, InboundPackage>();
  records
    .filter(isInboundPackageRow)
    .forEach((row) => packageMap.set(row.id, row));
  const groups = records.filter(isInboundPackageGroupRow);
  try {
    const children = await Promise.all(
      groups.map((row) => loadInboundPackageSegmentChildren(row)),
    );
    children
      .flat()
      .filter(isInboundPackageRow)
      .forEach((row) => packageMap.set(row.id, row));
  } catch (error: any) {
    clearInboundPackageSelection(shelfStatus);
    message.error(error?.message || '加载分段包装单失败，请刷新后重新选择');
    return;
  }
  const packages = [...packageMap.values()];
  if (shelfStatus === 'PENDING') {
    selectedPendingPackages.value = packages;
  } else {
    selectedShelvedPackages.value = packages;
  }
  selectedPackage.value = packages[0] || null;
}

function selectPackage(row: InboundPackageDisplayRow) {
  if (isInboundPackageRow(row)) selectedPackage.value = row;
}

async function openDetail(row: InboundPackageDisplayRow) {
  if (!isInboundPackageRow(row)) return;
  selectedPackage.value = row;
  detailVisible.value = true;
  try {
    // 列表改为服务端分页后，明细必须按包装箱 ID 单独加载，避免使用不完整的行数据。
    selectedPackage.value = await getFgPackageBoxInboundDetail(row.id);
  } catch {
    message.error('加载包装明细失败，请重试');
  } finally {
    await nextTick();
    await detailGridApi.query();
  }
}

function handleLocationSelect(keys: any[]) {
  const code = String(keys?.[0] || '');
  const target = filteredLocations.value.find(
    (item) => item.locationCode === code,
  );
  if (!code || !target) return;
  if (
    activeTab.value === 'pending' &&
    target.qualityScope !== pendingLocationQualityScope.value
  ) {
    message.warning(
      `${locationQualityScopeText(target.qualityScope)}不可用于${pendingQualityText.value}包装上架`,
    );
    return;
  }
  selectedLocationCode.value = code;
}

async function confirmShelf(row?: InboundPackageDisplayRow) {
  const targets = isInboundPackageRow(row)
    ? [row]
    : selectedPendingPackages.value;
  if (targets.length === 0) {
    message.warning('请先选择待上架包装单');
    return;
  }
  if (
    targets.some(
      (item) => !['PACKED', 'INBOUND_LOCKED'].includes(item.status || ''),
    )
  ) {
    message.warning('只有待上架包装单可以上架');
    return;
  }
  if (
    targets.some((item) => item.qualityStatus !== pendingQualityStatus.value)
  ) {
    message.warning('合格品与不合格品包装必须分开上架');
    return;
  }
  if (!selectedLocationCode.value) {
    message.warning('请在右侧库位树选择有余量的区');
    return;
  }
  if (
    selectedLocation.value?.qualityScope !== pendingLocationQualityScope.value
  ) {
    message.warning(
      `${pendingQualityText.value}包装只能上架到对应质量用途的库位`,
    );
    return;
  }
  shelfing.value = true;
  try {
    const result = await confirmFgInboundPackages({
      locationCode: selectedLocationCode.value,
      operatorName: currentUserName.value,
      packageIds: targets.map((item) => item.id),
    });
    message.success(
      `已将 ${result.packageCount || targets.length} 个包装单上架到 ${selectedLocationCode.value}`,
    );
    clearInboundPackageSelection('PENDING');
    selectedPackage.value = null;
    await refreshAll();
  } finally {
    shelfing.value = false;
  }
}

function downShelf(row?: InboundPackageDisplayRow) {
  const targets = isInboundPackageRow(row)
    ? [row]
    : selectedShelvedPackages.value;
  if (targets.length === 0) {
    message.warning('请先选择已上架包装单');
    return;
  }
  if (targets.some((item) => item.status !== 'INBOUNDED')) {
    message.warning('只有已上架包装单可以下架');
    return;
  }
  Modal.confirm({
    cancelText: '取消',
    content: `下架后，所选 ${targets.length} 个包装单会回到待上架列表，可重新选择区上架。发货锁定或已出库的片不允许下架。`,
    okButtonProps: { danger: true },
    okText: '确认下架',
    title: '确认下架移库？',
    async onOk() {
      downShelfing.value = true;
      try {
        const result = await downShelfFgInboundPackages({
          operatorName: currentUserName.value,
          packageIds: targets.map((item) => item.id),
        });
        message.success(
          `已下架 ${result.packageCount || targets.length} 个包装单，可重新选择区上架`,
        );
        clearInboundPackageSelection('SHELVED');
        selectedPackage.value = null;
        activeTab.value = 'pending';
        await refreshAll();
      } finally {
        downShelfing.value = false;
      }
    },
  });
}

function openDirectOutbound(row?: InboundPackageDisplayRow) {
  const targets = isInboundPackageRow(row)
    ? [row]
    : selectedPendingPackages.value;
  if (targets.length === 0) {
    message.warning('请先选择合格品待上架包装单');
    return;
  }
  if (targets.some((item) => !['PACKED', 'INBOUND_LOCKED'].includes(item.status || ''))) {
    message.warning('只有待上架包装单可以直接出库');
    return;
  }
  if (targets.some((item) => item.qualityStatus !== 'OK')) {
    message.warning('直接出库仅允许合格品待上架包装，不合格品请走受控处置流程');
    return;
  }
  directOutboundPackages.value = targets;
  directOutboundReason.value = '';
  directOutboundVisible.value = true;
}

async function confirmDirectOutbound() {
  const targets = directOutboundPackages.value;
  const reason = directOutboundReason.value.trim();
  if (targets.length === 0) {
    message.warning('请选择合格品待上架包装单');
    return;
  }
  if (!reason) {
    message.warning('请填写出库原因');
    return;
  }
  directOutbounding.value = true;
  try {
    const result = await directOutboundPendingFgInboundPackages({
      operatorName: currentUserName.value,
      packageIds: targets.map((item) => item.id),
      reason,
    });
    message.success(`已将 ${result.packageCount || targets.length} 个合格品待上架包装直接出库`);
    directOutboundVisible.value = false;
    directOutboundPackages.value = [];
    clearInboundPackageSelection('PENDING');
    selectedPackage.value = null;
    detailVisible.value = false;
    await refreshAll();
  } finally {
    directOutbounding.value = false;
  }
}

function openManualOutbound(row?: InboundPackageDisplayRow) {
  const targets = isInboundPackageRow(row)
    ? [row]
    : selectedShelvedPackages.value;
  if (targets.length === 0) {
    message.warning('请先选择已上架包装单');
    return;
  }
  if (targets.some((item) => item.status !== 'INBOUNDED')) {
    message.warning('只有已上架包装单可以出库');
    return;
  }
  if (targets.some((item) => item.qualityStatus === 'NG')) {
    message.warning('不合格成品请先下架，再按不合格品受控处置流程办理');
    return;
  }
  manualOutboundPackages.value = targets;
  manualOutboundReason.value = '';
  manualOutboundVisible.value = true;
}

async function confirmManualOutbound() {
  const targets = manualOutboundPackages.value;
  const reason = manualOutboundReason.value.trim();
  if (targets.length === 0) {
    message.warning('请选择已上架包装单');
    return;
  }
  if (!reason) {
    message.warning('请填写出库原因');
    return;
  }
  manualOutbounding.value = true;
  try {
    const result = await manualOutboundFgInboundPackages({
      operatorName: currentUserName.value,
      packageIds: targets.map((item) => item.id),
      reason,
    });
    message.success(
      `已将 ${result.packageCount || targets.length} 个包装单手工出库`,
    );
    manualOutboundVisible.value = false;
    manualOutboundPackages.value = [];
    clearInboundPackageSelection('SHELVED');
    selectedPackage.value = null;
    detailVisible.value = false;
    await refreshAll();
  } finally {
    manualOutbounding.value = false;
  }
}

function getInboundPackageViewMode(
  shelfStatus: MesHcFinishedPackagingApi.InboundPackageShelfStatus,
) {
  return shelfStatus === 'PENDING'
    ? pendingViewMode.value
    : shelvedViewMode.value;
}

function handleInboundPackageTabChange() {
  clearInboundPackageSelection();
  selectedPackage.value = null;
  selectedLocationCode.value = '';
}

function handlePendingQualityTabChange() {
  clearInboundPackageSelection('PENDING');
  selectedPackage.value = null;
  selectedLocationCode.value = '';
  void pendingPackageGridApi.query();
}

function changeInboundPackageViewMode(
  shelfStatus: MesHcFinishedPackagingApi.InboundPackageShelfStatus,
  viewMode: 'flat' | 'group',
) {
  const currentMode =
    shelfStatus === 'PENDING' ? pendingViewMode : shelvedViewMode;
  if (currentMode.value === viewMode) return;
  currentMode.value = viewMode;
  clearInboundPackageSelection(shelfStatus);
  selectedPackage.value = null;
  selectedLocationCode.value = '';
  if (shelfStatus === 'PENDING') {
    void pendingPackageGridApi.query();
  } else {
    void shelvedPackageGridApi.query();
  }
}

function toggleActiveViewMode() {
  const shelfStatus = activeTab.value === 'pending' ? 'PENDING' : 'SHELVED';
  changeInboundPackageViewMode(
    shelfStatus,
    activeViewMode.value === 'group' ? 'flat' : 'group',
  );
}

function getPageNo(page?: { currentPage?: number; pageNo?: number }) {
  return Number(page?.currentPage || page?.pageNo || 1);
}

function getPageSize(page?: { pageSize?: number }) {
  return Number(page?.pageSize || 20);
}

function buildPagedResult<T>(
  rows: T[],
  page?: { currentPage?: number; pageNo?: number; pageSize?: number },
) {
  const pageNo = Math.max(getPageNo(page), 1);
  const pageSize = Math.max(getPageSize(page), 1);
  const start = (pageNo - 1) * pageSize;
  return {
    list: rows.slice(start, start + pageSize),
    total: rows.length,
  };
}

async function queryInboundPackagePage(
  shelfStatus: MesHcFinishedPackagingApi.InboundPackageShelfStatus,
  page?: { currentPage?: number; pageNo?: number; pageSize?: number },
) {
  clearInboundPackageSelection(shelfStatus);
  const viewMode = getInboundPackageViewMode(shelfStatus);
  const qualityStatus =
    shelfStatus === 'PENDING' ? pendingQualityStatus.value : undefined;
  const result =
    viewMode === 'group'
      ? await getFgInboundPackageSegmentPage({
          keyword: packageKeyword.value.trim(),
          pageNo: getPageNo(page),
          pageSize: getPageSize(page),
          qualityStatus,
          shelfStatus,
        })
      : await getFgInboundPackagePage({
          keyword: packageKeyword.value.trim(),
          pageNo: getPageNo(page),
          pageSize: getPageSize(page),
          qualityStatus,
          shelfStatus,
        });
  if (shelfStatus === 'PENDING') {
    pendingPackageTotal.value = result.total || 0;
  } else {
    shelvedPackageTotal.value = result.total || 0;
  }
  return {
    ...result,
    list:
      viewMode === 'group'
        ? (result.list || []).map((row, index) =>
            toInboundPackageSegmentRow(
              row as MesHcFinishedPackagingApi.InboundPackageSegment,
              shelfStatus,
              index,
            ),
          )
        : (result.list || []).map((row) =>
            toInboundPackageRow(row as InboundPackage, shelfStatus),
          ),
  };
}

const pendingColumns = [
  { fixed: 'left', type: 'checkbox', width: 46 },
  {
    field: 'boxNo',
    fixed: 'left',
    slots: { default: 'pendingPackageNo' },
    title: '分段批次 / 包装编号',
    treeNode: true,
    width: 250,
  },
  {
    field: 'currentQty',
    slots: { default: 'pendingPackageQty' },
    title: '数量',
    width: 90,
  },
  {
    field: 'qualityStatus',
    slots: { default: 'packageQualityStatus' },
    title: '质量',
    width: 90,
  },
  {
    field: 'sliceList',
    slots: { default: 'pendingSliceList' },
    title: '片号',
    width: 300,
  },
  {
    field: 'shippingLockTip',
    slots: { default: 'shippingLockTip' },
    title: '发货锁定提示',
    width: 320,
  },
  { field: 'lockUserName', title: '包装人', width: 110 },
  {
    field: 'lockTime',
    formatter: ({ row }: { row: InboundPackage }) =>
      formatDateTime(row.lockTime),
    title: '包装时间',
    width: 160,
  },
  {
    align: 'center',
    field: 'action',
    fixed: 'right',
    slots: { default: 'pendingActions' },
    title: '操作',
    width: 136,
  },
];

const shelvedColumns = [
  { fixed: 'left', type: 'checkbox', width: 46 },
  {
    field: 'boxNo',
    fixed: 'left',
    slots: { default: 'shelvedPackageNo' },
    title: '分段批次 / 包装编号',
    treeNode: true,
    width: 250,
  },
  {
    field: 'warehouse',
    formatter: ({ row }: { row: InboundPackageDisplayRow }) =>
      isInboundPackageGroupRow(row)
        ? '-'
        : getPackageLocationMeta(row as InboundPackage).warehouse,
    title: '仓库',
    width: 120,
  },
  {
    field: 'rack',
    formatter: ({ row }: { row: InboundPackageDisplayRow }) =>
      isInboundPackageGroupRow(row)
        ? '-'
        : getPackageLocationMeta(row as InboundPackage).rack,
    title: '货架',
    width: 110,
  },
  {
    field: 'layer',
    formatter: ({ row }: { row: InboundPackageDisplayRow }) =>
      isInboundPackageGroupRow(row)
        ? '-'
        : getPackageLocationMeta(row as InboundPackage).layer,
    title: '层',
    width: 90,
  },
  {
    field: 'area',
    formatter: ({ row }: { row: InboundPackageDisplayRow }) =>
      isInboundPackageGroupRow(row)
        ? '-'
        : getPackageLocationMeta(row as InboundPackage).area,
    title: '区',
    width: 80,
  },
  {
    field: 'currentQty',
    slots: { default: 'shelvedPackageQty' },
    title: '数量',
    width: 80,
  },
  {
    field: 'qualityStatus',
    slots: { default: 'shelvedPackageQualityStatus' },
    title: '质量',
    width: 90,
  },
  {
    field: 'sliceList',
    slots: { default: 'shelvedSliceList' },
    title: '片号',
    width: 300,
  },
  { field: 'inboundUserName', title: '上架人', width: 110 },
  {
    field: 'inboundTime',
    formatter: ({ row }: { row: InboundPackage }) =>
      formatDateTime(row.inboundTime),
    title: '上架时间',
    width: 160,
  },
  {
    align: 'center',
    field: 'action',
    fixed: 'right',
    slots: { default: 'shelvedActions' },
    title: '操作',
    width: 136,
  },
];

const detailColumns = [
  { field: 'sliceBatchNo', title: '片号', minWidth: 190 },
  {
    field: 'qualityStatus',
    slots: { default: 'detailQualityStatus' },
    title: '检验结果',
    width: 110,
  },
  { field: 'scanUserName', title: '包装人', width: 120 },
  {
    field: 'scanTime',
    formatter: ({ row }: { row: MesHcFinishedPackagingApi.InboundBoxItem }) =>
      formatDateTime(row.scanTime),
    title: '包装时间',
    width: 170,
  },
];

function buildInboundPackageGridOptions(
  shelfStatus: MesHcFinishedPackagingApi.InboundPackageShelfStatus,
  columns: any[],
): VxeTableGridOptions<InboundPackageDisplayRow> {
  return {
    border: true,
    checkboxConfig: {
      highlight: true,
      showHeader: false,
      trigger: 'row',
    },
    columns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: ({ page }) => queryInboundPackagePage(shelfStatus, page),
      },
    },
    rowClassName: ({ row }: { row: InboundPackageDisplayRow }) =>
      isInboundPackageGroupRow(row) ? 'fg-inbound-segment-row' : '',
    rowConfig: {
      isHover: true,
      keyField: 'rowKey',
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
    treeConfig: {
      children: 'children',
      hasChild: 'hasChild',
      lazy: true,
      loadMethod: ({ row }: { row: InboundPackageDisplayRow }) =>
        loadInboundPackageSegmentChildren(row),
      reserve: true,
      showLine: true,
    },
  } as VxeTableGridOptions<InboundPackageDisplayRow>;
}

const [PendingPackageGrid, pendingPackageGridApi] = useVbenVxeGrid({
  gridOptions: buildInboundPackageGridOptions('PENDING', pendingColumns),
  gridEvents: {
    checkboxAll: ({ records }: { records: InboundPackageDisplayRow[] }) => {
      void handleInboundPackageCheckboxChange(records, 'PENDING');
    },
    checkboxChange: ({ records }: { records: InboundPackageDisplayRow[] }) => {
      void handleInboundPackageCheckboxChange(records, 'PENDING');
    },
    cellClick: ({ row }: { row: InboundPackageDisplayRow }) =>
      selectPackage(row),
  },
});

const [ShelvedPackageGrid, shelvedPackageGridApi] = useVbenVxeGrid({
  gridOptions: buildInboundPackageGridOptions('SHELVED', shelvedColumns),
  gridEvents: {
    checkboxAll: ({ records }: { records: InboundPackageDisplayRow[] }) => {
      void handleInboundPackageCheckboxChange(records, 'SHELVED');
    },
    checkboxChange: ({ records }: { records: InboundPackageDisplayRow[] }) => {
      void handleInboundPackageCheckboxChange(records, 'SHELVED');
    },
    cellClick: ({ row }: { row: InboundPackageDisplayRow }) =>
      selectPackage(row),
  },
});

const [DetailGrid, detailGridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    columns: detailColumns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 10,
      pageSizes: [10, 20, 50],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => buildPagedResult(selectedItems.value, page),
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
  } as VxeTableGridOptions<MesHcFinishedPackagingApi.InboundBoxItem>,
});

onMounted(refreshAll);
</script>

<template>
  <Page auto-content-height>
    <div class="fg-inbound-shelf package-fg-console">
      <section class="prototype-banner">
        <span class="console-main-icon"
          ><IconifyIcon icon="lucide:warehouse"
        /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">成品包装入库</h2>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">待上架</span>
              <span class="console-meta-value">{{ pendingPackageTotal }}</span>
              <span class="console-meta-sub">{{
                pendingViewMode === 'group' ? '组' : '包'
              }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">已上架</span>
              <span class="console-meta-value">{{ shelvedPackageTotal }}</span>
              <span class="console-meta-sub">{{
                shelvedViewMode === 'group' ? '组' : '包'
              }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">选中库位</span>
              <span class="console-meta-value location-code">{{
                selectedLocationCode || '-'
              }}</span>
            </span>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="refreshAll">
            <IconifyIcon icon="lucide:refresh-cw" />
            <span>刷新</span>
          </button>
          <button
            class="action-tile"
            :disabled="shelfing || downShelfing || directOutbounding || manualOutbounding"
            type="button"
            @click="toggleActiveViewMode"
          >
            <IconifyIcon
              :icon="
                activeViewMode === 'group' ? 'lucide:git-branch' : 'lucide:list'
              "
            />
            <span
              >展示方式：{{
                activeViewMode === 'group' ? '分类' : '不分类'
              }}</span
            >
          </button>
          <button
            v-if="activeTab === 'pending'"
            class="action-tile"
            :class="{
              'is-disabled':
                selectedPendingPackageCount === 0 ||
                !selectedLocationCode ||
                shelfing ||
                directOutbounding,
            }"
            :disabled="
              selectedPendingPackageCount === 0 ||
              !selectedLocationCode ||
              shelfing ||
              directOutbounding
            "
            type="button"
            @click="confirmShelf()"
          >
            <IconifyIcon icon="lucide:move-up-right" />
            <span>批量上架（{{ selectedPendingPackageCount }}）</span>
          </button>
          <button
            v-if="activeTab === 'pending' && pendingQualityStatus === 'OK'"
            class="action-tile action-tile-danger"
            :class="{
              'is-disabled': selectedPendingPackageCount === 0 || directOutbounding,
            }"
            :disabled="selectedPendingPackageCount === 0 || directOutbounding"
            type="button"
            @click="openDirectOutbound()"
          >
            <IconifyIcon icon="lucide:log-out" />
            <span>批量直接出库（{{ selectedPendingPackageCount }}）</span>
          </button>
        </div>
      </section>

      <section class="fg-shelf-layout">
        <div class="fg-left-panel">
          <div class="fg-toolbar">
            <Input
              v-model:value="packageKeyword"
              allow-clear
              placeholder="包装编号 / 批次 / 型号 / 库位"
              @press-enter="fetchPackages"
            />
            <Button
              :loading="packageLoading"
              type="primary"
              @click="fetchPackages"
            >
              <template #icon><IconifyIcon icon="lucide:search" /></template>
              查询
            </Button>
          </div>

          <Tabs
            v-model:active-key="activeTab"
            class="fg-shelf-tabs"
            @change="handleInboundPackageTabChange"
          >
            <TabPane key="pending" tab="待上架包装">
              <Tabs
                v-model:active-key="pendingQualityStatus"
                :animated="false"
                class="fg-pending-quality-tabs"
                size="small"
                @change="handlePendingQualityTabChange"
              >
                <TabPane key="OK" tab="合格品待上架" />
                <TabPane key="FROZEN" tab="冻结品待上架" />
                <TabPane key="NG" tab="不合格品待上架（隔离）" />
              </Tabs>
              <div class="fg-package-operation-bar">
                <span
                  >已选{{ pendingQualityText }}待上架包装
                  {{ selectedPendingPackageCount }} 包</span
                >
                <em
                  >{{ pendingQualityText }}仅可选择对应质量用途的库位上架。</em
                >
              </div>
              <section class="package-fg-grid-panel">
                <PendingPackageGrid table-title="待上架包装">
                  <template #pendingPackageNo="{ row }">
                    <div v-if="row.__group" class="fg-inbound-segment-cell">
                      <strong>{{ row.segmentBatchNo || '-' }}</strong>
                      <em>{{ formatInboundPackageSegmentSamples(row) }}</em>
                    </div>
                    <strong v-else>{{ row.boxNo || '-' }}</strong>
                  </template>
                  <template #pendingPackageQty="{ row }">
                    <span v-if="row.__group"
                      >{{ row.groupPieceCount || 0 }} 片</span
                    >
                    <span v-else>{{ row.currentQty || 0 }} 片</span>
                  </template>
                  <template #packageQualityStatus="{ row }">
                    <Tag v-if="row.__group" color="blue">分组</Tag>
                    <Tag v-else :color="resultColor(row.qualityStatus)">{{
                      row.qualityStatus === 'FROZEN' ? '冻结' : row.qualityStatus || '-'
                    }}</Tag>
                  </template>
                  <template #pendingSliceList="{ row }">
                    <span v-if="row.__group">-</span>
                    <span v-else>{{ formatPackageSliceList(row) }}</span>
                  </template>
                  <template #shippingLockTip="{ row }">
                    <span v-if="row.__group">-</span>
                    <span
                      v-else-if="hasOutboundLockedPieces(row)"
                      class="fg-shipping-lock-cell"
                      >{{ outboundLockedTip(row) }}</span
                    >
                    <span v-else>-</span>
                  </template>
                  <template #pendingActions="{ row }">
                    <div v-if="!row.__group" class="fg-row-actions is-center">
                      <Button
                        size="small"
                        type="link"
                        @click.stop="openDetail(row)"
                        >详情</Button
                      >
                      <Button
                        size="small"
                        type="link"
                        @click.stop="confirmShelf(row)"
                        >上架</Button
                      >
                      <Button
                        v-if="row.qualityStatus === 'OK'"
                        danger
                        :disabled="directOutbounding"
                        size="small"
                        type="link"
                        @click.stop="openDirectOutbound(row)"
                        >直接出库</Button
                      >
                    </div>
                  </template>
                </PendingPackageGrid>
              </section>
            </TabPane>

            <TabPane key="shelved" tab="已上架包装">
              <div class="fg-package-operation-bar">
                <span>已选已上架包装 {{ selectedShelvedPackageCount }} 包</span>
                <div>
                  <Button
                    :disabled="
                      selectedShelvedPackageCount === 0 || downShelfing
                    "
                    @click="downShelf()"
                  >
                    批量下架
                  </Button>
                  <Button
                    danger
                    :disabled="
                      selectedShelvedPackageCount === 0 ||
                      manualOutbounding ||
                      selectedShelvedPackages.some(
                        (item) => item.qualityStatus === 'NG',
                      )
                    "
                    @click="openManualOutbound()"
                  >
                    批量出库
                  </Button>
                </div>
              </div>
              <section class="package-fg-grid-panel">
                <ShelvedPackageGrid table-title="已上架包装">
                  <template #shelvedPackageNo="{ row }">
                    <div v-if="row.__group" class="fg-inbound-segment-cell">
                      <strong>{{ row.segmentBatchNo || '-' }}</strong>
                      <em>{{ formatInboundPackageSegmentSamples(row) }}</em>
                    </div>
                    <strong v-else>{{ row.boxNo || '-' }}</strong>
                  </template>
                  <template #shelvedPackageQty="{ row }">
                    <span v-if="row.__group"
                      >{{ row.groupPieceCount || 0 }} 片</span
                    >
                    <span v-else>{{ row.currentQty || 0 }} 片</span>
                  </template>
                  <template #shelvedPackageQualityStatus="{ row }">
                    <Tag v-if="row.__group" color="blue">分组</Tag>
                    <Tag v-else :color="resultColor(row.qualityStatus)">{{
                      row.qualityStatus === 'FROZEN' ? '冻结' : row.qualityStatus || '-'
                    }}</Tag>
                  </template>
                  <template #shelvedSliceList="{ row }">
                    <span v-if="row.__group">-</span>
                    <span v-else>{{ formatPackageSliceList(row) }}</span>
                  </template>
                  <template #shelvedActions="{ row }">
                    <div v-if="!row.__group" class="fg-row-actions is-center">
                      <Button
                        size="small"
                        type="link"
                        @click.stop="openDetail(row)"
                        >详情</Button
                      >
                      <Button
                        danger
                        :disabled="downShelfing"
                        size="small"
                        type="link"
                        @click.stop="downShelf(row)"
                      >
                        下架
                      </Button>
                      <Button
                        danger
                        :disabled="
                          manualOutbounding || row.qualityStatus === 'NG'
                        "
                        size="small"
                        type="link"
                        @click.stop="openManualOutbound(row)"
                      >
                        出库
                      </Button>
                    </div>
                  </template>
                </ShelvedPackageGrid>
              </section>
            </TabPane>
          </Tabs>
        </div>

        <aside class="fg-location-tree-panel">
          <div class="fg-toolbar fg-location-toolbar">
            <Input
              v-model:value="locationKeyword"
              allow-clear
              placeholder="库位编号 / 片号 / 包装编号"
            />
            <label class="fg-free-switch">
              <span>只看空闲</span>
              <Switch v-model:checked="freeLocationOnly" size="small" />
            </label>
            <Button :loading="locationLoading" @click="fetchLocations">
              <template #icon
                ><IconifyIcon icon="lucide:refresh-cw"
              /></template>
              刷新
            </Button>
          </div>
          <div class="fg-location-summary">
            <strong>{{ selectedLocation?.locationCode || '未选择区' }}</strong>
            <span>{{
              selectedLocation
                ? formatLocationHierarchy(selectedLocation)
                : locationPanelHint
            }}</span>
            <Tag
              v-if="selectedLocation"
              :color="
                selectedLocation.qualityScope === 'QUARANTINE'
                  ? 'red'
                  : selectedLocation.qualityScope === 'QUALIFIED'
                    ? 'green'
                    : 'default'
              "
            >
              {{ locationQualityScopeText(selectedLocation.qualityScope) }}
            </Tag>
            <Tag
              v-if="selectedLocation"
              :color="
                Number(selectedLocation.availableQty || 0) > 0 ? 'green' : 'red'
              "
            >
              {{ Number(selectedLocation.occupiedQty || 0) }}/{{
                Number(selectedLocation.capacityQty || 0)
              }}片
            </Tag>
          </div>
          <div class="fg-tree-scroll">
            <Tree
              :block-node="true"
              :loading="locationLoading"
              :selected-keys="selectedTreeKeys"
              show-line
              :tree-data="locationTreeData"
              default-expand-all
              @select="handleLocationSelect"
            >
              <template #title="{ dataRef }">
                <span
                  v-if="dataRef.nodeType === 'area'"
                  class="fg-pallet-block"
                  :class="{
                    'is-free':
                      dataRef.statusText === '空闲' && dataRef.qualityMatched,
                    'is-occupied':
                      dataRef.occupied &&
                      dataRef.statusText !== '空闲' &&
                      dataRef.qualityMatched,
                    'is-quality-mismatch':
                      activeTab === 'pending' && !dataRef.qualityMatched,
                    'is-unavailable':
                      dataRef.statusText !== '空闲' ||
                      (activeTab === 'pending' && !dataRef.qualityMatched),
                  }"
                  :title="dataRef.title"
                >
                  <strong>{{ dataRef.locationCode || dataRef.title }}</strong>
                  <em
                    v-if="activeTab === 'pending' && !dataRef.qualityMatched"
                  >
                    {{ locationQualityScopeText(dataRef.qualityScope) }}
                  </em>
                  <em v-else>{{ dataRef.availableQty }}片余量</em>
                </span>
                <span
                  v-else
                  class="fg-location-node"
                  :class="`is-${dataRef.nodeType}`"
                >
                  <IconifyIcon :icon="dataRef.icon" />
                  <span>{{ dataRef.title }}</span>
                  <em v-if="dataRef.count">{{ dataRef.count }}</em>
                </span>
              </template>
            </Tree>
          </div>
        </aside>
      </section>

      <Modal v-model:open="detailVisible" title="包装明细" width="820px">
        <div v-if="selectedPackage" class="fg-package-detail">
          <div class="fg-detail-head">
            <div>
              <strong>{{ selectedPackage.boxNo }}</strong>
              <span>{{ locationText(selectedPackage) }}</span>
            </div>
            <Tag :color="statusColor(selectedPackage.status)">{{
              statusText(selectedPackage.status)
            }}</Tag>
          </div>
          <div
            v-if="hasOutboundLockedPieces(selectedPackage)"
            class="fg-shipping-lock-tip"
          >
            {{ outboundLockedTip(selectedPackage) }}
          </div>
          <section class="package-fg-modal-grid-panel">
            <DetailGrid table-title="包装明细">
              <template #detailQualityStatus="{ row }">
                <Tag :color="resultColor(row.qualityStatus)">{{
                  row.qualityStatus === 'FROZEN' ? '冻结' : row.qualityStatus || '-'
                }}</Tag>
              </template>
            </DetailGrid>
          </section>
        </div>
      </Modal>

      <Modal
        v-model:open="directOutboundVisible"
        :confirm-loading="directOutbounding"
        cancel-text="取消"
        ok-text="确认直接出库"
        title="确认合格品待上架包装直接出库"
        @ok="confirmDirectOutbound"
      >
        <div
          v-if="directOutboundPackages.length > 0"
          class="fg-manual-outbound-confirm"
        >
          <p>
            包装数量：<strong>{{ directOutboundPackages.length }} 包</strong>
          </p>
          <p>
            包装编号：{{
              directOutboundPackages
                .map((item) => item.boxNo)
                .filter(Boolean)
                .join('、')
            }}
          </p>
          <p>
            片号：{{
              directOutboundPackages
                .map((item) => formatPackageSliceList(item))
                .filter((item) => item !== '-')
                .join('、')
            }}
          </p>
          <p class="fg-manual-outbound-confirm__warning">
            确认后不执行上架，不产生库位占用；系统将逐片写入成品库存出入库记录。
          </p>
          <Input.TextArea
            v-model:value="directOutboundReason"
            :maxlength="200"
            :rows="3"
            allow-clear
            placeholder="请填写出库原因（必填）"
            show-count
          />
        </div>
      </Modal>

      <Modal
        v-model:open="manualOutboundVisible"
        :confirm-loading="manualOutbounding"
        cancel-text="取消"
        ok-text="确认出库"
        title="确认手工成品出库"
        @ok="confirmManualOutbound"
      >
        <div
          v-if="manualOutboundPackages.length > 0"
          class="fg-manual-outbound-confirm"
        >
          <p>
            包装数量：<strong>{{ manualOutboundPackages.length }} 包</strong>
          </p>
          <p>
            包装编号：{{
              manualOutboundPackages
                .map((item) => item.boxNo)
                .filter(Boolean)
                .join('、')
            }}
          </p>
          <p>
            片号：{{
              manualOutboundPackages
                .map((item) => formatPackageSliceList(item))
                .filter((item) => item !== '-')
                .join('、')
            }}
          </p>
          <p class="fg-manual-outbound-confirm__warning">
            确认后将扣减全部片号库存，并写入成品库存出入库记录。
          </p>
          <Input.TextArea
            v-model:value="manualOutboundReason"
            :maxlength="200"
            :rows="3"
            allow-clear
            placeholder="请填写出库原因（必填）"
            show-count
          />
        </div>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.action-tile-danger:not(:disabled) {
  color: #b91c1c;
  background: #fff7f7;
  border-color: #fca5a5;
}

.fg-inbound-shelf {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.location-code {
  max-width: 150px;
  overflow: hidden;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fg-shelf-layout {
  display: grid;
  grid-template-columns: minmax(0, 7fr) minmax(320px, 3fr);
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.fg-left-panel {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.fg-location-tree-panel {
  display: grid;
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

.fg-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px;
  gap: 8px;
  align-items: center;
  padding: 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.fg-location-toolbar {
  grid-template-columns: minmax(0, 1fr) max-content 72px;
}

.fg-free-switch {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  height: 32px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.fg-shelf-tabs {
  display: flex;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
}

.fg-shelf-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0;
  padding: 0 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
  border-bottom: 0;
}

.fg-shelf-tabs :deep(.ant-tabs-content-holder),
.fg-shelf-tabs :deep(.ant-tabs-content),
.fg-shelf-tabs :deep(.ant-tabs-tabpane) {
  width: 100%;
  max-width: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.fg-shelf-tabs :deep(.ant-tabs-content-holder) {
  flex: 1 1 auto;
}

.fg-shelf-tabs :deep(.ant-tabs-content),
.fg-shelf-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
}

.fg-shelf-tabs :deep(.ant-tabs-tabpane) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fg-pending-quality-tabs {
  flex: 0 0 auto;
  margin: 0;
}

.fg-pending-quality-tabs :deep(.ant-tabs-nav) {
  margin: 0;
}

.fg-pending-quality-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.fg-package-operation-bar {
  display: flex;
  flex: 0 0 auto;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 40px;
  padding: 6px 10px;
  color: #334155;
  font-size: 13px;
  font-weight: 800;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.fg-package-operation-bar em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 500;
}

.fg-package-operation-bar > div {
  display: inline-flex;
  gap: 8px;
}

.fg-inbound-segment-cell {
  display: grid;
  gap: 2px;
}

.fg-inbound-segment-cell strong {
  color: #075985;
}

.fg-inbound-segment-cell em {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.package-fg-grid-panel {
  display: flex;
  box-sizing: border-box;
  flex: 1 1 auto;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  height: auto;
  min-height: 0;
  min-width: 0;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.package-fg-grid-panel :deep(.vben-vxe-grid),
.package-fg-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.package-fg-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.package-fg-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.package-fg-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.package-fg-grid-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.fg-location-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 8px;
  align-items: center;
  min-height: 58px;
  max-height: 68px;
  padding: 10px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.fg-location-summary strong,
.fg-location-summary span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fg-location-summary span {
  color: #64748b;
  font-size: 12px;
}

.fg-tree-scroll {
  height: 100%;
  min-height: 0;
  padding: 8px;
  overflow: auto;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.fg-tree-scroll :deep(.ant-tree) {
  min-width: max-content;
}

.fg-tree-scroll :deep(.ant-tree-treenode) {
  align-items: center;
  width: 100%;
  padding: 2px 0;
}

.fg-tree-scroll :deep(.ant-tree-node-content-wrapper) {
  display: inline-flex;
  align-items: center;
  width: 100%;
  min-height: 28px;
  padding: 0 4px;
  line-height: 28px;
  border-radius: 4px;
}

.fg-tree-scroll :deep(.ant-tree-node-content-wrapper.ant-tree-node-selected) {
  background: #dbeafe;
}

.fg-tree-scroll :deep(.ant-tree-treenode-disabled .ant-tree-node-content-wrapper) {
  color: inherit;
  cursor: not-allowed;
  opacity: 1;
}

.fg-tree-scroll :deep(.ant-tree-title) {
  display: block;
  width: 100%;
  min-width: 0;
}

.fg-location-node {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  height: 28px;
  color: #334155;
  font-weight: 800;
  white-space: nowrap;
}

.fg-location-node :deep(.iconify) {
  flex: 0 0 auto;
  font-size: 15px;
}

.fg-location-node.is-rack {
  color: #075985;
}

.fg-location-node.is-warehouse {
  color: #7c3aed;
}

.fg-location-node.is-layer {
  color: #1d4ed8;
}

.fg-location-node.is-area {
  color: #0f766e;
}

.fg-location-node em {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 18px;
  padding: 0 6px;
  color: #475569;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  line-height: 18px;
  background: #e2e8f0;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
}

.fg-pallet-block {
  display: inline-flex;
  align-items: center;
  min-width: 132px;
  max-width: 180px;
  min-height: 30px;
  padding: 4px 8px;
  gap: 8px;
  color: #334155;
  font-size: 12px;
  line-height: 18px;
  background: #e2e8f0;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
}

.fg-pallet-block.is-free {
  color: #166534;
  background: #dcfce7;
  border-color: #86efac;
}

.fg-pallet-block.is-occupied {
  color: #991b1b;
  background: #fee2e2;
  border-color: #fca5a5;
}

.fg-pallet-block.is-unavailable:not(.is-occupied) {
  color: #475569;
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.fg-pallet-block.is-quality-mismatch {
  color: #64748b;
  background: #f8fafc;
  border-color: #cbd5e1;
  opacity: 0.78;
}

.fg-pallet-block strong {
  flex: 0 0 auto;
  color: inherit;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 13px;
  font-weight: 950;
  white-space: nowrap;
}

.fg-pallet-block em {
  flex: 0 0 auto;
  overflow: hidden;
  color: inherit;
  font-style: normal;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fg-row-actions {
  display: flex;
  width: 100%;
  gap: 2px;
  white-space: nowrap;
}

.fg-row-actions.is-center {
  justify-content: center;
}

.fg-row-actions :deep(.ant-btn-sm) {
  min-width: 36px;
  padding-inline: 2px;
}

.fg-shipping-lock-cell {
  display: inline-block;
  max-width: 100%;
  color: #92400e;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
  white-space: normal;
}

.fg-package-detail {
  display: grid;
  gap: 10px;
}

.fg-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.fg-detail-head > div {
  display: grid;
  gap: 4px;
}

.fg-detail-head span {
  color: #64748b;
  font-size: 12px;
}

.fg-shipping-lock-tip {
  padding: 8px 10px;
  color: #92400e;
  font-size: 13px;
  font-weight: 700;
  background: #fff7ed;
  border: 1px solid #fdba74;
}

.package-fg-modal-grid-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  width: 100%;
  height: 340px;
  min-height: 0;
  overflow: hidden;
}

.package-fg-modal-grid-panel :deep(.vben-vxe-grid),
.package-fg-modal-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.package-fg-modal-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.package-fg-modal-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.package-fg-modal-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}
</style>

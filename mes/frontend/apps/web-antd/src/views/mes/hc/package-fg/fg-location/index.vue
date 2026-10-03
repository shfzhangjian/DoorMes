<script lang="ts" setup>
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LocationQrPrintItem } from '#/views/mes/hc/shared/location-qr-print';

import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  QRCode,
  Select,
  Switch,
  Tag,
  Tabs,
  TabPane,
  Tooltip,
  Tree,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteFgLayer,
  deleteFgLocation,
  deleteFgRack,
  deleteFgWarehouse,
  getFgLocationGrid,
  getFgLocationTree,
  getFgStockLedgerPage,
  saveFgLayer,
  saveFgLocation,
  saveFgRack,
  saveFgWarehouse,
  updateFgWarehouseQualityScope,
} from '#/api/mes/hc/package-fg/finished-packaging';
import { printLocationQrLabels } from '#/views/mes/hc/shared/location-qr-print';

import '../shared/cut-round-board.css';

defineOptions({ name: 'MesPackageFgLocation' });

type FgLocation = MesHcFinishedPackagingApi.FgLocationGrid;
type FgLocationPiece = MesHcFinishedPackagingApi.FgLocationPiece;
type FgLocationForm = Pick<
  MesHcFinishedPackagingApi.FgLocationSaveReq,
  'capacityQty' | 'id' | 'qualityScope'
> & {
  locationCode: string;
  locationName: string;
};
type StockLedger = MesHcFinishedPackagingApi.FgStockLedger;
type LocationMeta = {
  areaNo: number;
  areaText: string;
  isStructured: boolean;
  layerNo: number;
  layerText: string;
  rackNo: string;
  slotNo: number;
  slotText: string;
};
type RackArea = {
  areaNo: number;
  capacity: number;
  code: string;
  locations: FgLocation[];
  occupied: number;
};
type RackExcelSlot = {
  location?: FgLocation;
  piece?: FgLocationPiece;
  sliceBatchNo?: string;
  slotNo: number;
  slotText: string;
};
type RackExcelRow = Omit<RackArea, 'locations'> & {
  layerNo: number;
  slots: RackExcelSlot[];
};
type LocationViewMode = 'table' | 'visual';
type LocationTreeScope = 'layer' | 'rack';
type LocationStructure = {
  areaNo: number;
  layerId?: number;
  layerNo: number;
  rackId?: number;
  rackNo: number;
};
type LocationLayerTreeNode = {
  children?: LocationLayerTreeNode[];
  count?: number;
  key: string;
  layerId?: number;
  layerName?: string;
  layerNo?: number;
  nodeType: 'area' | 'layer' | 'rack' | 'warehouse';
  occupied?: number;
  rackId?: number;
  rackName?: string;
  rackNo: string;
  row?: FgLocation;
  title: string;
  warehouseCode?: string;
  warehouseId?: number;
  warehouseName?: string;
};
type HierarchyFormKind = 'layer' | 'rack' | 'warehouse';
type WarehouseQualityScope = 'QUALIFIED' | 'QUARANTINE';
type HierarchyForm = {
  id?: number;
  layerName: string;
  layerNo: number;
  rackId?: number;
  rackName: string;
  rackNo: number;
  remark: string;
  sortNo: number;
  status: string;
  warehouseCode: string;
  warehouseId?: number;
  warehouseName: string;
};

const DEFAULT_AREA_CAPACITY = 30;
const SLOT_COUNT_PER_AREA = 15;

const loading = ref(false);
const saving = ref(false);
const locations = ref<FgLocation[]>([]);
const warehouseTree = ref<MesHcFinishedPackagingApi.FgLocationTreeWarehouse[]>([]);
const locationTree = computed(() => warehouseTree.value.flatMap((warehouse) => warehouse.racks || []));
const selectedLocationCode = ref<string>();
const selectedTreeNode = ref<LocationLayerTreeNode>();
const selectedTreeKeys = ref<string[]>([]);
const locationKeyword = ref('');
const onlyFreeLocation = ref(false);
const activeRackNo = ref('');
const activeLayerNo = ref<number>(1);
const activeTreeScope = ref<LocationTreeScope>('rack');
const viewMode = ref<LocationViewMode>('visual');
const formVisible = ref(false);
const formWarehouseId = ref<number>();
const hierarchyFormVisible = ref(false);
const hierarchySaving = ref(false);
const hierarchyFormKind = ref<HierarchyFormKind>('rack');
const warehouseQualityVisible = ref(false);
const warehouseQualitySaving = ref(false);
const warehouseQualityScope = ref<WarehouseQualityScope>('QUALIFIED');
const detailVisible = ref(false);
const qrPreviewVisible = ref(false);
const qrPrinting = ref(false);
const qrPreviewRows = ref<FgLocation[]>([]);
const printSelectionMode = ref(false);
const selectedPrintLocationCodes = ref<string[]>([]);
const formStructure = reactive<LocationStructure>({
  areaNo: 1,
  layerNo: 1,
  rackNo: 1,
});
const formModel = reactive<FgLocationForm>({
  capacityQty: DEFAULT_AREA_CAPACITY,
  locationCode: '',
  locationName: '',
  qualityScope: 'QUALIFIED',
});
const hierarchyForm = reactive<HierarchyForm>({
  layerName: '',
  layerNo: 1,
  rackName: '',
  rackNo: 1,
  remark: '',
  sortNo: 0,
  status: '启用',
  warehouseCode: '',
  warehouseId: undefined,
  warehouseName: '',
});

const activeScopeLocations = computed(() => {
  if (!activeRackNo.value) return locations.value;
  return locations.value.filter((item) => {
    const meta = parseLocationMeta(item);
    if (String(item.rackId || '') !== activeRackNo.value) return false;
    return activeTreeScope.value === 'rack' || meta.layerNo === activeLayerNo.value;
  });
});
const filteredLocations = computed(() => {
  const keyword = locationKeyword.value.trim().toLocaleLowerCase();
  return activeScopeLocations.value.filter((item) => {
    if (onlyFreeLocation.value && (item.status !== '启用' || Number(item.availableQty || 0) <= 0)) return false;
    if (!keyword) return true;
    return [
      item.locationCode,
      item.locationName,
      item.positionDesc,
      item.occupiedSliceBatchNo,
      Number(item.availableQty || 0) > 0 ? '空闲' : '',
      ...locationSliceList(item),
      ...locationPieceSearchTexts(item),
    ]
      .filter(Boolean)
      .some((value) => String(value).toLocaleLowerCase().includes(keyword));
  });
});
const matchedLocationCodes = computed(
  () => new Set(filteredLocations.value.map((item) => item.locationCode).filter(Boolean) as string[]),
);

const totalCapacity = computed(() =>
  locations.value.reduce((sum, item) => sum + Number(item.capacityQty || 0), 0),
);
const totalOccupied = computed(() =>
  locations.value.reduce((sum, item) => sum + Number(item.occupiedQty || 0), 0),
);
const totalPercent = computed(() => percent(totalOccupied.value, totalCapacity.value));
const selectedWarehouseForQualityScope = computed(() => {
  if (selectedTreeNode.value?.nodeType !== 'warehouse') return undefined;
  return warehouseTree.value.find((item) => item.id === selectedTreeNode.value?.warehouseId);
});
const selectedWarehouseQualityAreas = computed(() => {
  const warehouseId = selectedWarehouseForQualityScope.value?.id;
  return warehouseId == null
    ? []
    : locations.value.filter((item) => item.warehouseId === warehouseId);
});
const selectedWarehouseQualityStats = computed(() => {
  const areas = selectedWarehouseQualityAreas.value;
  const scopeCounts = areas.reduce<Record<string, number>>((result, item) => {
    const scope = String(item.qualityScope || 'UNASSIGNED').toUpperCase();
    result[scope] = Number(result[scope] || 0) + 1;
    return result;
  }, {});
  return {
    activePieceCount: areas.reduce((sum, item) => sum + Number(item.occupiedQty || 0), 0),
    locationCount: areas.length,
    occupiedLocationCount: areas.filter((item) => Number(item.occupiedQty || 0) > 0).length,
    scopeCounts,
  };
});
const maxPieceCount = computed(() => Math.max(
  DEFAULT_AREA_CAPACITY,
  ...locations.value.map((item) => Math.max(0, Number(item.capacityQty || 0))),
));
const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const selectedLocation = computed(() =>
  locations.value.find((item) => item.locationCode === selectedLocationCode.value),
);
const selectedLocationMeta = computed(() => parseLocationMeta(selectedLocation.value));
const formEditingLocation = computed(() =>
  formModel.id == null
    ? undefined
    : locations.value.find((item) => String(item.id) === String(formModel.id)),
);
const formCanEditStructure = computed(() =>
  formModel.id == null || formEditingLocation.value?.deletable === true,
);
const formWarehouse = computed(() =>
  warehouseTree.value.find((item) => item.id === formWarehouseId.value),
);
const formRackOptions = computed(() => locationTree.value
  .filter((rack) => formWarehouseId.value == null || rack.warehouseId === formWarehouseId.value)
  .map((rack) => ({
    disabled: rack.status !== '启用',
    label: rack.rackName || `${rack.rackNo}#货架`,
    value: rack.id,
  })));
const formLayerOptions = computed(() => {
  const rack = locationTree.value.find((item) => item.id === formStructure.rackId);
  return (rack?.layers || []).map((layer) => ({
    disabled: layer.status !== '启用',
    label: layer.layerName || `L${layer.layerNo}层`,
    value: layer.id,
  }));
});
const rackExcelRows = computed<RackExcelRow[]>(() => buildRackExcelRows(activeRackNo.value));
const layerTreeData = computed<LocationLayerTreeNode[]>(() => buildLayerTreeData());
const activeWorkspaceTab = computed(() => viewMode.value);
const locationTableRows = computed(() =>
  filteredLocations.value.slice().sort(compareLocation),
);
const currentScopeCapacity = computed(() =>
  activeScopeLocations.value.reduce((sum, item) => sum + Number(item.capacityQty || 0), 0),
);
const currentScopeOccupied = computed(() =>
  activeScopeLocations.value.reduce((sum, item) => sum + Number(item.occupiedQty || 0), 0),
);
const currentRack = computed(() =>
  locationTree.value.find((rack) => String(rack.id) === activeRackNo.value),
);
const currentLayer = computed(() =>
  currentRack.value?.layers.find((layer) => layer.layerNo === activeLayerNo.value),
);
const currentPathSegments = computed(() => {
  const segments = [currentRack.value?.warehouseName || '包装成品仓库'];
  if (activeRackNo.value) segments.push(currentRack.value?.rackName || `${currentRack.value?.rackNo || '-'}#货架`);
  if (activeTreeScope.value === 'layer') segments.push(currentLayer.value?.layerName || `L${activeLayerNo.value}层`);
  return segments;
});
const currentScopeAreaText = computed(() =>
  activeTreeScope.value === 'rack' ? '货架下全部层 / 区域' : `L${activeLayerNo.value}层全部区域`,
);
const hasLocationFilter = computed(() => !!locationKeyword.value.trim() || onlyFreeLocation.value);
const visualLayerGroups = computed(() =>
  [...new Set(rackExcelRows.value.map((row) => row.layerNo))].sort(compareLayerNo).map((layerNo) => {
    const layerRows = rackExcelRows.value
      .filter((row) => row.layerNo === layerNo)
      .map((row) => (hasLocationFilter.value ? buildMatchedVisualAreaRow(row) : row))
      .filter((row) => !hasLocationFilter.value || row.slots.length > 0);
    return {
      capacity: layerRows.reduce((sum, row) => sum + Number(row.capacity || 0), 0),
      layerNo,
      occupied: layerRows.reduce((sum, row) => sum + Number(row.occupied || 0), 0),
      rows: layerRows,
    };
  }).filter((group) => {
    if (activeTreeScope.value !== 'rack' && group.layerNo !== activeLayerNo.value) return false;
    return !hasLocationFilter.value || group.rows.length > 0;
  }),
);
const visualAreaRows = computed(() =>
  visualLayerGroups.value.flatMap((group) => group.rows),
);
const visualEmptyText = computed(() =>
  hasLocationFilter.value ? '当前筛选下无满足条件的区' : '请选择左侧货架或层级',
);
const selectedPrintCodeSet = computed(() => new Set(selectedPrintLocationCodes.value));
const selectedPrintLocations = computed(() =>
  locations.value
    .filter((item) => selectedPrintCodeSet.value.has(item.locationCode || ''))
    .sort(compareLocation),
);

const detailColumns: VxeTableGridOptions<StockLedger>['columns'] = [
  { field: 'packageNo', showOverflow: 'tooltip', title: '包装编号', width: 180 },
  { field: 'sliceBatchNo', showOverflow: 'tooltip', title: '片号', width: 180 },
  { field: 'modelCode', showOverflow: 'tooltip', title: '型号', width: 170 },
  { field: 'productionDate', title: '生产日期', width: 120 },
  { field: 'expiryDate', title: '到期日期', width: 120 },
];

function buildLocationColumns(): VxeTableGridOptions<FgLocation>['columns'] {
  return [
    ...(printSelectionMode.value
      ? [{
          align: 'center',
          field: 'printSelect',
          fixed: 'left',
          slots: { default: 'printSelect' },
          title: '选择',
          width: 72,
        } as VxeTableGridOptions<FgLocation>['columns'][number]]
      : []),
    { field: 'locationCode', fixed: 'left', showOverflow: 'tooltip', title: '编号', width: 150 },
    {
      field: 'warehouseName',
      formatter: ({ row }) => row.warehouseName || row.warehouseCode || '-',
      title: '仓库',
      width: 130,
    },
    {
      field: 'rackNo',
      formatter: ({ row }) => `${parseLocationMeta(row).rackNo}#货架`,
      title: '货架',
      width: 100,
    },
    {
      field: 'layerNo',
      formatter: ({ row }) => `${parseLocationMeta(row).layerText}层`,
      title: '层',
      width: 90,
    },
    {
      field: 'areaNo',
      formatter: ({ row }) => parseLocationMeta(row).areaText,
      title: '区',
      width: 80,
    },
    ...buildPieceNoList(maxPieceCount.value).map((pieceNo) => ({
      field: `piece${pieceNo}`,
      formatter: ({ row }: { row: FgLocation }) => areaPieceTableText(row, pieceNo - 1),
      showOverflow: 'tooltip',
      title: String(pieceNo).padStart(2, '0'),
      width: 118,
    })),
  ];
}

function percent(occupied?: number, capacity?: number) {
  const currentCapacity = Number(capacity || 0);
  if (currentCapacity <= 0) return 0;
  return Math.min(100, Math.round((Number(occupied || 0) / currentCapacity) * 100));
}

function statusColor(status?: string) {
  if (status === '启用') return 'green';
  if (status === '停用') return 'default';
  if (status === '空置') return 'blue';
  return 'processing';
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

function buildPieceNoList(capacityQty?: number) {
  const capacity = Math.max(0, Math.floor(Number(capacityQty ?? DEFAULT_AREA_CAPACITY) || 0));
  return Array.from({ length: capacity }, (_, index) => index + 1);
}

function parseLocationMeta(row?: FgLocation | null): LocationMeta {
  const code = String(row?.locationCode || '');
  const matched = /^(?:[A-Z][A-Z0-9_-]*-)?(\d+)-L(\d+)-(\d+)(?:-(\d+))?$/i.exec(code);
  if (matched || row?.rackNo || row?.layerNo || row?.areaNo) {
    const layerNo = Number(row?.layerNo || matched?.[2] || 0);
    const areaNo = Number(row?.areaNo || matched?.[3] || 0);
    const slotNo = Number(matched?.[4] || 0);
    return {
      areaNo,
      areaText: `${areaNo}区`,
      isStructured: true,
      layerNo,
      layerText: `L${layerNo}`,
      rackNo: String(row?.rackNo || matched?.[1] || ''),
      slotNo,
      slotText: matched?.[4] ? String(slotNo).padStart(2, '0') : '',
    };
  }
  return {
    areaNo: 0,
    areaText: '-',
    isStructured: false,
    layerNo: 0,
    layerText: '-',
    rackNo: '未分组',
    slotNo: Number(row?.gridNo || 0),
    slotText: String(row?.gridNo || '-'),
  };
}

function normalizeStructure(value?: Partial<LocationStructure>): LocationStructure {
  const requestedRackNo = Math.max(1, Math.min(999, Number(value?.rackNo || 1)));
  const rack = locationTree.value.find((item) => item.id === value?.rackId)
    || locationTree.value.find((item) =>
      (formWarehouseId.value == null || item.warehouseId === formWarehouseId.value)
      && item.rackNo === requestedRackNo,
    );
  const requestedLayerNo = Math.max(1, Math.min(999, Number(value?.layerNo || 1)));
  const layer = rack?.layers.find((item) =>
    item.id === value?.layerId || item.layerNo === requestedLayerNo,
  );
  return {
    areaNo: Math.max(1, Math.min(999, Number(value?.areaNo || 1))),
    layerId: layer?.id || value?.layerId,
    layerNo: layer?.layerNo || requestedLayerNo,
    rackId: rack?.id || value?.rackId,
    rackNo: rack?.rackNo || requestedRackNo,
  };
}

function calculateGridNo(structureValue: Partial<LocationStructure>) {
  const structure = normalizeStructure(structureValue);
  return structure.rackNo * 1_000_000 + structure.layerNo * 1_000 + structure.areaNo;
}

function buildStructureFromGridNo(gridNo: number): LocationStructure {
  const normalized = Math.max(1_001_001, Number(gridNo || 1_001_001));
  return normalizeStructure({
    areaNo: normalized % 1_000,
    layerNo: Math.floor(normalized / 1_000) % 1_000,
    rackNo: Math.floor(normalized / 1_000_000),
  });
}

function buildStructureFromRow(row?: FgLocation) {
  const meta = parseLocationMeta(row);
  if (meta.isStructured) {
    return normalizeStructure({
      areaNo: meta.areaNo,
      layerId: row?.layerId,
      layerNo: meta.layerNo,
      rackId: row?.rackId,
      rackNo: Number(meta.rackNo),
    });
  }
  return buildStructureFromGridNo(Number(row?.gridNo || 1));
}

function findFirstAvailableStructure(warehouseId?: number): LocationStructure | undefined {
  const candidateRacks = warehouseId == null
    ? locationTree.value
    : locationTree.value.filter((rack) => rack.warehouseId === warehouseId);
  for (const rack of candidateRacks) {
    if (rack.status !== '启用') continue;
    for (const layer of rack.layers || []) {
      if (layer.status !== '启用') continue;
      for (let areaNo = 1; areaNo <= 999; areaNo += 1) {
        const structure = normalizeStructure({
          areaNo,
          layerId: layer.id,
          layerNo: layer.layerNo,
          rackId: rack.id,
          rackNo: rack.rackNo,
        });
        if (!isStructureUsed(structure)) return structure;
      }
    }
  }
  return undefined;
}

function findFirstAvailableArea(
  rack: MesHcFinishedPackagingApi.FgLocationTreeRack,
  layer: MesHcFinishedPackagingApi.FgLocationTreeLayer,
): LocationStructure | undefined {
  for (let areaNo = 1; areaNo <= 999; areaNo += 1) {
    const structure = normalizeStructure({
      areaNo,
      layerId: layer.id,
      layerNo: layer.layerNo,
      rackId: rack.id,
      rackNo: rack.rackNo,
    });
    if (!isStructureUsed(structure)) return structure;
  }
  return undefined;
}

function getStructureKey(row?: FgLocation) {
  const meta = parseLocationMeta(row);
  if (meta.isStructured) {
    return buildLocationFromStructure({
      areaNo: meta.areaNo,
      layerId: row?.layerId,
      layerNo: meta.layerNo,
      rackId: row?.rackId,
      rackNo: Number(meta.rackNo),
    }).code;
  }
  return buildLocationFromStructure(buildStructureFromGridNo(Number(row?.gridNo || 1))).code;
}

function isStructureUsed(structureValue: Partial<LocationStructure>, editingId?: number) {
  const currentCode = buildLocationFromStructure(structureValue).code;
  return locations.value.some(
    (item) => getStructureKey(item) === currentCode
      && (editingId == null || String(item.id) !== String(editingId)),
  );
}

function resolveAvailableStructure(structureValue: Partial<LocationStructure>, editingId?: number) {
  const requested = normalizeStructure(structureValue);
  if (!isStructureUsed(requested, editingId)) {
    return requested;
  }
  for (let areaNo = 1; areaNo <= 999; areaNo += 1) {
    if (!isStructureUsed({ ...requested, areaNo }, editingId)) {
      return { ...requested, areaNo };
    }
  }
  return findFirstAvailableStructure(
    locationTree.value.find((item) => item.id === requested.rackId)?.warehouseId,
  ) || requested;
}

function isCurrentStructureUsed() {
  return isStructureUsed(formStructure, formModel.id);
}

function buildLocationFromStructure(structureValue: Partial<LocationStructure>) {
  const structure = normalizeStructure(structureValue);
  const rack = locationTree.value.find((item) => item.id === structure.rackId);
  const layer = rack?.layers.find((item) => item.id === structure.layerId);
  const warehouseCode = rack?.warehouseCode || 'WAREHOUSE';
  const code = `${warehouseCode}-${structure.rackNo}-L${structure.layerNo}-${structure.areaNo}`;
  return {
    code,
    gridNo: calculateGridNo(structure),
    name: `${rack?.rackName || `${structure.rackNo}#货架`}-${layer?.layerName || `L${structure.layerNo}层`}-${structure.areaNo}区`,
    positionDesc: `${rack?.rackName || `${structure.rackNo}#货架`} ${layer?.layerName || `L${structure.layerNo}层`} 第${structure.areaNo}区域；区域容量${DEFAULT_AREA_CAPACITY}片`,
  };
}

function compareLocation(a: FgLocation, b: FgLocation) {
  const warehouseCompare = String(a.warehouseCode || '').localeCompare(String(b.warehouseCode || ''), 'zh-CN');
  if (warehouseCompare !== 0) return warehouseCompare;
  const am = parseLocationMeta(a);
  const bm = parseLocationMeta(b);
  const rackCompare = compareRackCode(am.rackNo, bm.rackNo);
  if (rackCompare !== 0) return rackCompare;
  if (am.layerNo !== bm.layerNo) return compareLayerNo(am.layerNo, bm.layerNo);
  if (am.areaNo !== bm.areaNo) return am.areaNo - bm.areaNo;
  if (am.slotNo !== bm.slotNo) return am.slotNo - bm.slotNo;
  return Number(a.gridNo || 0) - Number(b.gridNo || 0);
}

function buildLayerTreeData(): LocationLayerTreeNode[] {
  return warehouseTree.value.map((warehouse) => {
    const warehouseAreas = warehouse.racks.flatMap((rack) => rack.layers.flatMap((layer) => layer.areas || []));
    const warehouseCapacity = warehouseAreas.reduce((sum, item) => sum + Number(item.capacityQty || 0), 0);
    const warehouseOccupied = warehouseAreas.reduce((sum, item) => sum + Number(item.occupiedQty || 0), 0);
    return {
      children: warehouse.racks.map((rack) => {
        const rackAreas = rack.layers.flatMap((layer) => layer.areas || []);
        const capacity = rackAreas.reduce((sum, item) => sum + Number(item.capacityQty || 0), 0);
        const occupied = rackAreas.reduce((sum, item) => sum + Number(item.occupiedQty || 0), 0);
        return {
          children: rack.layers.map((layer) => {
            const layerCapacity = (layer.areas || []).reduce((sum, item) => sum + Number(item.capacityQty || 0), 0);
            const layerOccupied = (layer.areas || []).reduce((sum, item) => sum + Number(item.occupiedQty || 0), 0);
            return {
              children: (layer.areas || []).map((area) => ({
                count: Number(area.capacityQty || 0),
                key: `area-${area.id}`,
                layerId: layer.id,
                layerName: layer.layerName,
                layerNo: layer.layerNo,
                nodeType: 'area' as const,
                occupied: Number(area.occupiedQty || 0),
                rackId: rack.id,
                rackName: rack.rackName,
                rackNo: String(rack.rackNo),
                row: area,
                title: area.locationName || `${area.areaNo}区`,
                warehouseCode: warehouse.warehouseCode,
                warehouseId: warehouse.id,
                warehouseName: warehouse.warehouseName,
              })),
              count: layerCapacity,
              key: `layer-${layer.id}`,
              layerId: layer.id,
              layerName: layer.layerName,
              layerNo: layer.layerNo,
              nodeType: 'layer' as const,
              occupied: layerOccupied,
              rackId: rack.id,
              rackName: rack.rackName,
              rackNo: String(rack.rackNo),
              title: layer.layerName || `L${layer.layerNo}层`,
              warehouseCode: warehouse.warehouseCode,
              warehouseId: warehouse.id,
              warehouseName: warehouse.warehouseName,
            };
          }),
          count: capacity,
          key: `rack-${rack.id}`,
          nodeType: 'rack' as const,
          occupied,
          rackId: rack.id,
          rackName: rack.rackName,
          rackNo: String(rack.rackNo),
          title: rack.rackName || `${rack.rackNo}#货架`,
          warehouseCode: warehouse.warehouseCode,
          warehouseId: warehouse.id,
          warehouseName: warehouse.warehouseName,
        };
      }),
      count: warehouseCapacity,
      key: `warehouse-${warehouse.id}`,
      nodeType: 'warehouse' as const,
      occupied: warehouseOccupied,
      rackNo: '',
      title: `${warehouse.warehouseName}（${warehouse.warehouseCode}）`,
      warehouseCode: warehouse.warehouseCode,
      warehouseId: warehouse.id,
      warehouseName: warehouse.warehouseName,
    };
  });
}

function buildRackExcelRows(rackId: string): RackExcelRow[] {
  if (!rackId) return [];
  const rack = locationTree.value.find((item) => String(item.id) === rackId);
  if (!rack) return [];
  return rack.layers.flatMap((layer) => (layer.areas || []).map((area) => ({
    areaNo: Number(area.areaNo || parseLocationMeta(area).areaNo),
    capacity: Number(area.capacityQty || 0),
    code: area.locationCode || `${rack.warehouseCode}-${rack.rackNo}-L${layer.layerNo}-${area.areaNo}`,
    layerNo: layer.layerNo,
    occupied: Number(area.occupiedQty || 0),
    slots: buildAreaPieceSlots(area),
  })));
}

function isMatched(row?: FgLocation) {
  if (!row) return false;
  const keyword = locationKeyword.value.trim();
  return !keyword || matchedLocationCodes.value.has(row.locationCode || '');
}

function locationPieces(row?: FgLocation | null) {
  return (row?.pieces || []).filter((piece): piece is FgLocationPiece => !!piece);
}

function pieceSliceText(piece?: FgLocationPiece | null) {
  return piece?.sliceBatchNo || piece?.stockNo || '';
}

function pieceFullText(piece?: FgLocationPiece | null, emptyText = '空闲') {
  if (!piece) return emptyText;
  return pieceSliceText(piece) || '已占用';
}

function areaPieceAt(row: FgLocation | undefined, index: number) {
  return locationPieces(row)[index];
}

function areaPieceTableText(row: FgLocation | undefined, index: number) {
  return pieceSliceText(areaPieceAt(row, index)) || '空';
}

function locationPieceSearchTexts(row?: FgLocation | null) {
  return locationPieces(row).flatMap((piece) => [
    pieceSliceText(piece),
    piece.materialCode,
    piece.materialName,
    piece.modelCode,
    piece.batchNo,
    piece.qualityStatus,
    piece.stockStatus,
  ]);
}

function buildAreaPieceSlot(location: FgLocation | undefined, slotNo: number): RackExcelSlot {
  const piece = areaPieceAt(location, slotNo - 1);
  return {
    location,
    piece,
    sliceBatchNo: pieceSliceText(piece),
    slotNo,
    slotText: String(slotNo).padStart(2, '0'),
  };
}

function buildAreaPieceSlots(location: FgLocation | undefined) {
  return buildPieceNoList(location?.capacityQty).map((slotNo) => buildAreaPieceSlot(location, slotNo));
}

function locationSliceList(row?: FgLocation | null) {
  const values = locationPieces(row)
    .map((piece) => pieceSliceText(piece))
    .filter(Boolean);
  if (values.length > 0) return values;
  return (row?.occupiedSliceBatchNos || [])
    .map((item) => String(item || '').trim())
    .filter(Boolean);
}

function slotVisualText(slot: RackExcelSlot) {
  if (!slot.location) return '';
  return slot.piece ? pieceFullText(slot.piece) : '空闲';
}

function slotPieceText(slot: RackExcelSlot) {
  return slot.piece ? pieceFullText(slot.piece) : '';
}

function buildMatchedVisualAreaRow(row: RackExcelRow): RackExcelRow {
  return {
    ...row,
    slots: visibleAreaSlots(row),
  };
}

function textIncludesKeyword(values: unknown[], keyword: string) {
  return values
    .filter(Boolean)
    .some((value) => String(value).toLocaleLowerCase().includes(keyword));
}

function isSlotKeywordMatched(slot: RackExcelSlot, keyword: string) {
  if (!slot.location) return !keyword;
  if (textIncludesKeyword([
    slot.location.locationCode,
    slot.location.locationName,
    slot.location.positionDesc,
  ], keyword)) {
    return true;
  }
  if (!slot.piece) return '空闲'.includes(keyword);
  return textIncludesKeyword([
    slot.sliceBatchNo,
    slot.piece.stockNo,
    slot.piece.materialCode,
    slot.piece.materialName,
    slot.piece.modelCode,
    slot.piece.batchNo,
    slot.piece.qualityStatus,
    slot.piece.stockStatus,
  ], keyword);
}

function visibleAreaSlots(row: RackExcelRow) {
  const keyword = locationKeyword.value.trim().toLocaleLowerCase();
  return row.slots.filter((slot) => {
    if (!slot.location) return !onlyFreeLocation.value && !keyword;
    if (onlyFreeLocation.value && (slot.location.status !== '启用' || !!slot.piece)) return false;
    if (!isMatched(slot.location)) return false;
    return !keyword || isSlotKeywordMatched(slot, keyword);
  });
}

function isPrintSelected(row?: FgLocation) {
  return !!row?.locationCode && selectedPrintCodeSet.value.has(row.locationCode);
}

function togglePrintSelection(row?: FgLocation) {
  const code = row?.locationCode;
  if (!code) return;
  if (selectedPrintCodeSet.value.has(code)) {
    selectedPrintLocationCodes.value = selectedPrintLocationCodes.value.filter((item) => item !== code);
    return;
  }
  selectedPrintLocationCodes.value = [...selectedPrintLocationCodes.value, code];
}

function clearPrintSelection() {
  selectedPrintLocationCodes.value = [];
}

function refreshLocationGridColumns() {
  nextTick(() => {
    LocationGridApi.setGridOptions({ columns: buildLocationColumns() as any });
    if (viewMode.value === 'table') LocationGridApi.query();
  });
}

function enterPrintSelectionMode() {
  printSelectionMode.value = true;
  refreshLocationGridColumns();
}

function exitPrintSelectionMode() {
  printSelectionMode.value = false;
  clearPrintSelection();
  refreshLocationGridColumns();
}

function selectCurrentRackLocations() {
  if (!printSelectionMode.value) {
    enterPrintSelectionMode();
  }
  const visibleLocations = visualAreaRows.value
    .flatMap((row) => visibleAreaSlots(row))
    .map((slot) => slot.location)
    .filter((row): row is FgLocation => !!row && isMatched(row));
  const rows = viewMode.value === 'table'
    ? locationTableRows.value
    : [...new Map(visibleLocations.map((row) => [row.locationCode || `${row.id}`, row])).values()];
  const mergedCodes = new Set(selectedPrintLocationCodes.value);
  rows.forEach((row) => {
    if (row.locationCode) mergedCodes.add(row.locationCode);
  });
  selectedPrintLocationCodes.value = [...mergedCodes];
  message.success(`已选择当前${viewMode.value === 'table' ? '表格' : '可视化'}库位：${rows.length} 个`);
}

async function handleSlotClick(row?: FgLocation) {
  if (printSelectionMode.value) {
    togglePrintSelection(row);
    return;
  }
  await selectLocation(row);
}

function openQrPreview(rows = selectedPrintLocations.value) {
  const validRows = rows.filter((row) => !!row?.locationCode).sort(compareLocation);
  if (!validRows.length) {
    message.warning('请先选择需要打印二维码的库位');
    return;
  }
  qrPreviewRows.value = validRows;
  qrPreviewVisible.value = true;
}

function buildLocationQrPrintItem(row: FgLocation) {
  const meta = parseLocationMeta(row);
  const code = row.locationCode || '';
  return {
    fields: [
      { label: '仓库', value: row.warehouseName || row.warehouseCode || '未命名仓库' },
      { label: '库位编号', value: code },
      { label: '货架', value: `${meta.rackNo}#货架` },
      { label: '层', value: meta.layerText },
      { label: '区域', value: meta.areaText },
    ],
    locationCode: code,
    processName: '成品库位',
  } satisfies LocationQrPrintItem;
}

async function printQrRows(rows = qrPreviewRows.value) {
  const validRows = rows.filter((row) => !!row?.locationCode).sort(compareLocation);
  if (!validRows.length) {
    message.warning('请先选择需要打印二维码的库位');
    return;
  }
  qrPrinting.value = true;
  try {
    const result = await printLocationQrLabels(
      validRows.map((row) => buildLocationQrPrintItem(row)),
      '成品库位码',
    );
    message.success(`库位二维码已发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || validRows.length}`);
    qrPreviewVisible.value = false;
    if (printSelectionMode.value) {
      exitPrintSelectionMode();
    }
  } catch (error: any) {
    Modal.warning({
      content: `未能连接或确认本机 Python 打印服务：${error?.message || error}。请先启动 HC-MES-PrintAgent。`,
      title: '打印二维码失败',
    });
  } finally {
    qrPrinting.value = false;
  }
}

function resetForm(row?: FgLocation, initialStructure?: Partial<LocationStructure>) {
  const initial = initialStructure || findFirstAvailableStructure(
    selectedTreeNode.value?.warehouseId || currentRack.value?.warehouseId,
  );
  if (!row && !initial) {
    message.warning('当前仓库没有可新增的启用货架层，请先维护货架和层');
    return false;
  }
  const structure = row
    ? buildStructureFromRow(row)
    : normalizeStructure(initial);
  Object.assign(formStructure, structure);
  formWarehouseId.value = locationTree.value.find((item) => item.id === structure.rackId)?.warehouseId;
  const defaultLocation = buildLocationFromStructure(structure);
  Object.assign(formModel, {
    capacityQty: row ? Number(row.capacityQty || DEFAULT_AREA_CAPACITY) : DEFAULT_AREA_CAPACITY,
    id: row?.id,
    locationCode: defaultLocation.code,
    locationName: row?.locationName || defaultLocation.name,
    qualityScope: row?.qualityScope || 'QUALIFIED',
  });
  return true;
}

function handleStructureChange() {
  if (!formCanEditStructure.value) return;
  if (!formStructure.rackId || !formStructure.layerId) {
    message.warning('请先选择启用的货架和层');
    return;
  }
  const requested = normalizeStructure(formStructure);
  const structure = resolveAvailableStructure(requested, formModel.id);
  Object.assign(formStructure, structure);
  const generated = buildLocationFromStructure(structure);
  Object.assign(formModel, {
    locationCode: generated.code,
    locationName: generated.name,
  });
  if (generated.code !== buildLocationFromStructure(requested).code) {
    message.info(`目标分区已维护库位，已自动切换为可用库位 ${generated.code}`);
  }
}

function handleRackChange(rackId?: number) {
  const rack = locationTree.value.find((item) => item.id === rackId);
  if (!rack) return;
  formWarehouseId.value = rack.warehouseId;
  const layer = rack.layers.find((item) => item.status === '启用') || rack.layers[0];
  Object.assign(formStructure, {
    layerId: layer?.id,
    layerNo: layer?.layerNo || 1,
    rackId: rack.id,
    rackNo: rack.rackNo,
  });
  handleStructureChange();
}

function handleLayerChange(layerId?: number) {
  const rack = locationTree.value.find((item) => item.id === formStructure.rackId);
  const layer = rack?.layers.find((item) => item.id === layerId);
  if (!rack || !layer) return;
  Object.assign(formStructure, {
    layerId: layer.id,
    layerNo: layer.layerNo,
    rackId: rack.id,
    rackNo: rack.rackNo,
  });
  handleStructureChange();
}

async function fetchData() {
  loading.value = true;
  try {
    const [tree, grid] = await Promise.all([getFgLocationTree(), getFgLocationGrid()]);
    warehouseTree.value = tree || [];
    locations.value = (grid || []).slice().sort(compareLocation);
    const validCodes = new Set(locations.value.map((item) => item.locationCode).filter(Boolean) as string[]);
    selectedPrintLocationCodes.value = selectedPrintLocationCodes.value.filter((code) => validCodes.has(code));
    syncSelectedLocation();
    refreshLocationGridColumns();
    await nextTick();
    await detailGridApi.query();
    if (viewMode.value === 'table') {
      await LocationGridApi.query();
    }
  } finally {
    loading.value = false;
  }
}

async function queryDetailPage(page?: { currentPage?: number; pageSize?: number }) {
  if (!selectedLocationCode.value) {
    return { list: [], total: 0 };
  }
  return await getFgStockLedgerPage({
    locationCode: selectedLocationCode.value,
    pageNo: page?.currentPage || 1,
    pageSize: page?.pageSize || 20,
  });
}

function getPageNo(page?: { currentPage?: number; pageNo?: number }) {
  return Number(page?.currentPage || page?.pageNo || 1);
}

function getPageSize(page?: { pageSize?: number }) {
  return Number(page?.pageSize || 20);
}

function buildPagedResult<T>(rows: T[], page?: { currentPage?: number; pageNo?: number; pageSize?: number }) {
  const pageNo = Math.max(getPageNo(page), 1);
  const pageSize = Math.max(getPageSize(page), 1);
  const start = (pageNo - 1) * pageSize;
  return {
    list: rows.slice(start, start + pageSize),
    total: rows.length,
  };
}

function syncSelectedLocation() {
  if (!locationTree.value.some((item) => String(item.id) === activeRackNo.value)) {
    activeRackNo.value = locationTree.value[0] ? String(locationTree.value[0].id) : '';
  }
  const activeRack = locationTree.value.find((item) => String(item.id) === activeRackNo.value);
  if (!activeRack?.layers.some((item) => item.layerNo === activeLayerNo.value)) {
    activeLayerNo.value = activeRack?.layers[0]?.layerNo || 1;
  }
  if (filteredLocations.value.some((item) => item.locationCode === selectedLocationCode.value)) {
    const meta = parseLocationMeta(selectedLocation.value);
    if (selectedLocation.value?.rackId) activeRackNo.value = String(selectedLocation.value.rackId);
    if (meta.layerNo) activeLayerNo.value = meta.layerNo;
    return;
  }
  selectedLocationCode.value = filteredLocations.value[0]?.locationCode;
  const meta = parseLocationMeta(filteredLocations.value[0]);
  if (filteredLocations.value[0]?.rackId) activeRackNo.value = String(filteredLocations.value[0].rackId);
  if (meta.layerNo) activeLayerNo.value = meta.layerNo;
}

async function selectLocation(row?: FgLocation) {
  if (!row) return;
  selectedLocationCode.value = row.locationCode;
  const meta = parseLocationMeta(row);
  if (row.rackId) activeRackNo.value = String(row.rackId);
  if (meta.layerNo) activeLayerNo.value = meta.layerNo;
  detailVisible.value = true;
  await nextTick();
  await detailGridApi.query();
}

async function selectRack(rackId: number) {
  activeTreeScope.value = 'rack';
  activeRackNo.value = String(rackId);
  const rack = locationTree.value.find((item) => item.id === rackId);
  if (!rack) return;
  if (!rack.layers.some((item) => item.layerNo === activeLayerNo.value)) {
    activeLayerNo.value = (rack.layers.find((item) => item.status === '启用') || rack.layers[0])?.layerNo || 1;
  }
  selectedTreeKeys.value = rack ? [`rack-${rack.id}`] : [];
  const rackFirst = locations.value.find((item) => {
    const meta = parseLocationMeta(item);
    return item.rackId === rackId && meta.layerNo === activeLayerNo.value;
  }) || locations.value.find((item) => item.rackId === rackId);
  if (rackFirst && !locations.value.some((item) => item.locationCode === selectedLocationCode.value && item.rackId === rackId)) {
    selectedLocationCode.value = rackFirst.locationCode;
    const meta = parseLocationMeta(rackFirst);
    if (meta.layerNo) activeLayerNo.value = meta.layerNo;
    await detailGridApi.query();
  }
  await nextTick();
  await LocationGridApi.query();
}

async function handleLayerSelect(keys: any[], info?: { node?: LocationLayerTreeNode }) {
  const node = info?.node;
  if (!node) return;
  selectedTreeNode.value = node;
  selectedTreeKeys.value = [node.key];
  if (node?.nodeType === 'rack') {
    await selectRack(node.rackId!);
    return;
  }
  if (node.nodeType === 'warehouse') {
    const context = resolveSelectedLocationContext();
    if (!context) {
      activeRackNo.value = '';
      selectedLocationCode.value = undefined;
      await nextTick();
      await LocationGridApi.query();
      return;
    }
    activeTreeScope.value = 'layer';
    activeRackNo.value = String(context.rack.id);
    activeLayerNo.value = context.layer.layerNo;
    const firstLocation = locations.value.find((item) =>
      item.rackId === context.rack.id && item.layerId === context.layer.id,
    );
    selectedLocationCode.value = firstLocation?.locationCode;
    await nextTick();
    await detailGridApi.query();
    await LocationGridApi.query();
    return;
  }
  if (node.nodeType === 'area') {
    await selectLocation(node.row);
    return;
  }
  if (node.nodeType !== 'layer') return;
  activeTreeScope.value = 'layer';
  activeRackNo.value = String(node.rackId);
  activeLayerNo.value = Number(node.layerNo || activeLayerNo.value);
  const layerFirst = locations.value.find((item) => {
    const meta = parseLocationMeta(item);
    return String(item.rackId || '') === activeRackNo.value && meta.layerNo === activeLayerNo.value;
  });
  if (layerFirst) {
    selectedLocationCode.value = layerFirst.locationCode;
    await detailGridApi.query();
  }
  await nextTick();
  await LocationGridApi.query();
}

function setViewMode(mode: LocationViewMode) {
  viewMode.value = mode;
  if (mode === 'table') {
    nextTick(() => LocationGridApi.query());
  }
}

function handleWorkspaceTabChange(key: string | number) {
  const value = String(key);
  if (value === 'table' || value === 'visual') setViewMode(value);
}

function openForm(row?: FgLocation) {
  if (!row && !locationTree.value.some((rack) => rack.layers.some((layer) => layer.status === '启用'))) {
    message.warning('请先新增至少一个启用的货架和层');
    return;
  }
  if (!row) {
    const context = resolveSelectedLocationContext();
    const structure = context && findFirstAvailableArea(context.rack, context.layer);
    if (!structure || !resetForm(undefined, structure)) return;
  } else if (!resetForm(row)) {
    return;
  }
  formVisible.value = true;
}

function getSelectedWarehouse() {
  const warehouseId = selectedTreeNode.value?.warehouseId || currentRack.value?.warehouseId;
  return warehouseTree.value.find((item) => item.id === warehouseId);
}

function qualityScopeText(scope?: string) {
  if (scope === 'QUALIFIED') return '合格品仓';
  if (scope === 'QUARANTINE') return '不合格品隔离仓';
  return '历史待盘点';
}

function currentWarehouseQualityText() {
  const { locationCount, scopeCounts } = selectedWarehouseQualityStats.value;
  if (!locationCount) return '尚未维护区域';
  return Object.entries(scopeCounts)
    .filter(([, count]) => count > 0)
    .map(([scope, count]) => `${qualityScopeText(scope)} ${count} 个`)
    .join('、');
}

function openWarehouseQualityScopeModal() {
  const warehouse = selectedWarehouseForQualityScope.value;
  if (!warehouse) {
    message.warning('请先在左侧树中选择仓库节点');
    return;
  }
  if (!selectedWarehouseQualityStats.value.locationCount) {
    message.warning('当前仓库尚未维护区域，请先新增货架、层和区域');
    return;
  }
  const scopes = Object.keys(selectedWarehouseQualityStats.value.scopeCounts)
    .filter((scope) => Number(selectedWarehouseQualityStats.value.scopeCounts[scope] || 0) > 0);
  warehouseQualityScope.value = scopes.length === 1 && scopes[0] === 'QUARANTINE'
    ? 'QUARANTINE'
    : 'QUALIFIED';
  warehouseQualityVisible.value = true;
}

async function submitWarehouseQualityScope() {
  const warehouse = selectedWarehouseForQualityScope.value;
  if (!warehouse) {
    message.warning('仓库选择已失效，请重新选择');
    return;
  }
  warehouseQualitySaving.value = true;
  try {
    const result = await updateFgWarehouseQualityScope({
      qualityScope: warehouseQualityScope.value,
      warehouseId: warehouse.id,
    });
    message.success(
      `${result.warehouseName}已设置为${qualityScopeText(result.qualityScope)}，共覆盖${result.locationCount}个库位`,
    );
    warehouseQualityVisible.value = false;
    await fetchData();
  } finally {
    warehouseQualitySaving.value = false;
  }
}

function getSelectedRack() {
  const rackId = selectedTreeNode.value?.rackId
    || locationTree.value.find((item) => String(item.id) === activeRackNo.value)?.id;
  return locationTree.value.find((item) => item.id === rackId);
}

function getSelectedLayer() {
  const rack = getSelectedRack();
  const layerId = selectedTreeNode.value?.layerId;
  return rack?.layers.find((item) => item.id === layerId || item.layerNo === activeLayerNo.value);
}

function resolveSelectedLocationContext() {
  const node = selectedTreeNode.value;
  if (!node) return undefined;
  const warehouse = warehouseTree.value.find((item) => item.id === node.warehouseId);
  const candidateRacks = node.rackId
    ? (warehouse?.racks || []).filter((item) => item.id === node.rackId)
    : warehouse?.racks || [];
  const rack = candidateRacks.find((item) => item.status === '启用'
    && item.layers.some((layer) => layer.status === '启用'));
  if (!rack) return undefined;
  const layer = rack.layers.find((item) => item.id === node.layerId && item.status === '启用')
    || rack.layers.find((item) => item.status === '启用');
  if (!layer) return undefined;
  return { layer, rack };
}

function openWarehouseForm(warehouse?: MesHcFinishedPackagingApi.FgLocationTreeWarehouse) {
  hierarchyFormKind.value = 'warehouse';
  Object.assign(hierarchyForm, {
    id: warehouse?.id,
    layerName: '',
    layerNo: 1,
    rackId: undefined,
    rackName: '',
    rackNo: 1,
    remark: warehouse?.remark || '',
    sortNo: warehouse?.sortNo || 0,
    status: warehouse?.status || '启用',
    warehouseCode: warehouse?.warehouseCode || '',
    warehouseId: warehouse?.id,
    warehouseName: warehouse?.warehouseName || '',
  });
  hierarchyFormVisible.value = true;
}

function openRackForm(rack?: MesHcFinishedPackagingApi.FgLocationTreeRack) {
  const warehouse = rack
    ? warehouseTree.value.find((item) => item.id === rack.warehouseId)
    : getSelectedWarehouse();
  if (!warehouse) {
    message.warning('请先选择仓库，再新增货架');
    return;
  }
  hierarchyFormKind.value = 'rack';
  Object.assign(hierarchyForm, {
    id: rack?.id,
    layerName: '',
    layerNo: 1,
    rackId: undefined,
    rackName: rack?.rackName || '',
    rackNo: rack?.rackNo || Math.max(1, ...(warehouse.racks || []).map((item) => item.rackNo || 0)) + 1,
    remark: rack?.remark || '',
    sortNo: rack?.sortNo || 0,
    status: rack?.status || '启用',
    warehouseCode: warehouse.warehouseCode,
    warehouseId: warehouse.id,
    warehouseName: warehouse.warehouseName,
  });
  hierarchyFormVisible.value = true;
}

function openLayerForm(
  layer?: MesHcFinishedPackagingApi.FgLocationTreeLayer,
  rack?: MesHcFinishedPackagingApi.FgLocationTreeRack,
) {
  if (!layer && selectedTreeNode.value?.nodeType === 'warehouse') {
    message.warning('请先选择货架，再新增层');
    return;
  }
  const parentRack = rack || getSelectedRack();
  if (!parentRack) {
    message.warning('请先选择货架，再新增层');
    return;
  }
  hierarchyFormKind.value = 'layer';
  Object.assign(hierarchyForm, {
    id: layer?.id,
    layerName: layer?.layerName || '',
    layerNo: layer?.layerNo || Math.max(1, ...(parentRack.layers || []).map((item) => item.layerNo || 0)) + 1,
    rackId: parentRack.id,
    rackName: parentRack.rackName || `${parentRack.rackNo}#货架`,
    rackNo: parentRack.rackNo,
    remark: layer?.remark || '',
    sortNo: layer?.sortNo || 0,
    status: layer?.status || '启用',
    warehouseCode: parentRack.warehouseCode || '',
    warehouseId: parentRack.warehouseId,
    warehouseName: parentRack.warehouseName || '',
  });
  hierarchyFormVisible.value = true;
}

function openNewArea() {
  const context = resolveSelectedLocationContext();
  if (!context) {
    message.warning('请先选择仓库、货架或层；该仓库需至少维护一个启用货架和层');
    return;
  }
  const structure = findFirstAvailableArea(context.rack, context.layer);
  if (!structure) {
    message.warning('当前层的区域编号已用完，请先调整现有区域');
    return;
  }
  if (!resetForm(undefined, structure)) return;
  formVisible.value = true;
}

async function submitHierarchyForm() {
  if (hierarchyFormKind.value === 'warehouse') {
    if (!hierarchyForm.warehouseCode.trim() || !hierarchyForm.warehouseName.trim()) {
      message.warning('请填写仓库编码和仓库名称');
      return;
    }
  } else if (hierarchyFormKind.value === 'rack') {
    if (!hierarchyForm.rackName.trim() || Number(hierarchyForm.rackNo || 0) < 1) {
      message.warning('请填写货架名称和有效货架编号');
      return;
    }
  } else if (!hierarchyForm.layerName.trim() || Number(hierarchyForm.layerNo || 0) < 1 || !hierarchyForm.rackId) {
    message.warning('请填写层名称、层编号，并选择所属货架');
    return;
  }
  hierarchySaving.value = true;
  try {
    if (hierarchyFormKind.value === 'warehouse') {
      await saveFgWarehouse({
        id: hierarchyForm.id,
        remark: hierarchyForm.remark.trim() || undefined,
        sortNo: Number(hierarchyForm.sortNo || 0),
        status: hierarchyForm.status,
        warehouseCode: hierarchyForm.warehouseCode.trim().toUpperCase(),
        warehouseName: hierarchyForm.warehouseName.trim(),
      });
    } else if (hierarchyFormKind.value === 'rack') {
      await saveFgRack({
        id: hierarchyForm.id,
        rackName: hierarchyForm.rackName.trim(),
        rackNo: Number(hierarchyForm.rackNo),
        remark: hierarchyForm.remark.trim() || undefined,
        sortNo: Number(hierarchyForm.sortNo || 0),
        status: hierarchyForm.status,
        warehouseId: hierarchyForm.warehouseId!,
      });
    } else {
      await saveFgLayer({
        id: hierarchyForm.id,
        layerName: hierarchyForm.layerName.trim(),
        layerNo: Number(hierarchyForm.layerNo),
        rackId: hierarchyForm.rackId,
        remark: hierarchyForm.remark.trim() || undefined,
        sortNo: Number(hierarchyForm.sortNo || 0),
        status: hierarchyForm.status,
      });
    }
    const nodeName = hierarchyFormKind.value === 'warehouse' ? '仓库' : hierarchyFormKind.value === 'rack' ? '货架' : '层';
    message.success(`${nodeName}已保存`);
    hierarchyFormVisible.value = false;
    await fetchData();
  } finally {
    hierarchySaving.value = false;
  }
}

function editSelectedHierarchy() {
  const node = selectedTreeNode.value;
  if (!node) {
    message.warning('请先在左侧树中选择需要编辑的货架、层或区域');
    return;
  }
  if (node.nodeType === 'area') {
    openForm(node.row);
    return;
  }
  if (node.nodeType === 'warehouse') {
    openWarehouseForm(warehouseTree.value.find((item) => item.id === node.warehouseId));
    return;
  }
  const rack = locationTree.value.find((item) => item.id === node.rackId);
  if (node.nodeType === 'rack') {
    openRackForm(rack);
    return;
  }
  openLayerForm(rack?.layers.find((item) => item.id === node.layerId), rack);
}

function deleteSelectedHierarchy() {
  const node = selectedTreeNode.value;
  if (!node) {
    message.warning('请先在左侧树中选择需要删除的货架、层或区域');
    return;
  }
  if (node.nodeType === 'area') {
    handleDelete(node.row);
    return;
  }
  const typeName = node.nodeType === 'warehouse' ? '仓库' : node.nodeType === 'rack' ? '货架' : '层';
  Modal.confirm({
    content: `删除${node.title}会同时清理其下无库存的区域；只要存在库存，系统将拒绝删除。是否继续？`,
    title: `确认删除${typeName}`,
    onOk: async () => {
      if (node.nodeType === 'warehouse') {
        await deleteFgWarehouse(node.warehouseId!);
      } else if (node.nodeType === 'rack') {
        await deleteFgRack(node.rackId!);
      } else {
        await deleteFgLayer(node.layerId!);
      }
      selectedTreeNode.value = undefined;
      selectedTreeKeys.value = [];
      message.success(`${typeName}已删除`);
      await fetchData();
    },
  });
}

function hasConfiguredArea(row: RackExcelRow) {
  return row.slots.some((slot) => !!slot.location);
}

async function handleLocationFilterChange() {
  syncSelectedLocation();
  await detailGridApi.query();
}

async function submitForm() {
  if (!formModel.locationCode.trim() || !formModel.locationName.trim()) {
    message.warning('请填写库位编号和库位名称');
    return;
  }
  if (Number(formModel.capacityQty || 0) < 1) {
    message.warning('区容量不能小于1片');
    return;
  }
  if (!formStructure.rackId || !formStructure.layerId) {
    message.warning('请选择货架和层');
    return;
  }
  if (isCurrentStructureUsed()) {
    message.warning('该货架、层、区域已维护库位，请选择其他区域');
    return;
  }
  saving.value = true;
  try {
    await saveFgLocation({
      areaNo: formStructure.areaNo,
      capacityQty: formModel.capacityQty,
      id: formModel.id,
      layerId: formStructure.layerId,
      locationName: formModel.locationName.trim(),
      qualityScope: formModel.qualityScope,
      rackId: formStructure.rackId,
    });
    message.success('库位已保存');
    formVisible.value = false;
    await fetchData();
  } finally {
    saving.value = false;
  }
}

function handleDelete(row?: FgLocation) {
  if (!row?.id) return;
  if (!row.deletable) {
    message.warning('当前库位已有库存片号，不能删除');
    return;
  }
  Modal.confirm({
    title: '确认删除库位',
    content: `删除 ${row.locationName || row.locationCode} 后，该库位将从列表移除。`,
    onOk: async () => {
      await deleteFgLocation(row.id!);
      message.success('库位已删除');
      detailVisible.value = false;
      await fetchData();
    },
  });
}

function handlePrint(row?: FgLocation) {
  if (!row) return;
  openQrPreview([row]);
}

watch([locationKeyword, onlyFreeLocation], handleLocationFilterChange);
watch([locationKeyword, onlyFreeLocation], () => {
  LocationGridApi.query();
});

onMounted(fetchData);

const [DetailGrid, detailGridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    border: true,
    columns: detailColumns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => queryDetailPage(page),
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
    toolbarConfig: { enabled: false },
  } as VxeTableGridOptions<StockLedger>,
});

const [LocationGrid, LocationGridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    border: true,
    columns: buildLocationColumns(),
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => buildPagedResult(locationTableRows.value, page),
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'locationCode',
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  } as VxeTableGridOptions<FgLocation>,
  gridEvents: {
    cellClick: ({ row }: { row: FgLocation }) => selectLocation(row),
  },
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console fg-location-page">
      <section class="prototype-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:grid-3x3" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">库位管理</h2>
            <Tag class="console-title-tag" color="blue">库位列表</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">已维护</span>
              <span class="console-meta-value">{{ locations.length }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">总容量</span>
              <span class="console-meta-value">{{ totalCapacity }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">已占</span>
              <span class="console-meta-value">{{ totalOccupied }}</span>
            </span>
          </div>
        </div>
        <div class="location-progress-card">
          <span>总占用率</span>
          <strong>{{ totalPercent }}%</strong>
          <Progress :percent="totalPercent" :show-info="false" size="small" />
        </div>
        <div class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group">
          <button class="action-tile" :class="{ 'is-disabled': loading }" :disabled="loading" type="button" @click="fetchData">
            <IconifyIcon icon="lucide:refresh-cw" />
            <span>刷新</span>
          </button>
          <button v-access:code="['mes:inv:fg-location:create']" class="action-tile" type="button" @click="openWarehouseForm()">
            <IconifyIcon icon="lucide:warehouse" />
            <span>新增仓库</span>
          </button>
          <button v-access:code="['mes:inv:fg-location:create']" class="action-tile" type="button" @click="openRackForm()">
            <IconifyIcon icon="lucide:plus" />
            <span>新增货架</span>
          </button>
          <button
            v-access:code="['mes:inv:fg-location:create']"
            class="action-tile"
            type="button"
            @click="openLayerForm()"
          >
            <IconifyIcon icon="lucide:layers-3" />
            <span>新增层</span>
          </button>
          <button
            v-access:code="['mes:inv:fg-location:create']"
            class="action-tile"
            type="button"
            @click="openNewArea"
          >
            <IconifyIcon icon="lucide:grid-2x2-plus" />
            <span>新增区域</span>
          </button>
          <button
            v-access:code="['mes:inv:fg-location:update']"
            class="action-tile"
            :class="{ 'is-disabled': !selectedTreeNode }"
            :disabled="!selectedTreeNode"
            title="编辑当前树节点"
            type="button"
            @click="editSelectedHierarchy"
          >
            <IconifyIcon icon="lucide:pencil" />
            <span>编辑节点</span>
          </button>
          <button
            v-access:code="['mes:inv:fg-location:update']"
            class="action-tile"
            :class="{ 'is-disabled': !selectedWarehouseForQualityScope }"
            :disabled="!selectedWarehouseForQualityScope"
            :title="selectedWarehouseForQualityScope ? '批量设置当前仓库全部库位的质量用途' : '请先选择仓库节点'"
            type="button"
            @click="openWarehouseQualityScopeModal"
          >
            <IconifyIcon icon="lucide:shield-check" />
            <span>仓库用途</span>
          </button>
          <button
            v-access:code="['mes:inv:fg-location:delete']"
            class="action-tile is-danger"
            :class="{ 'is-disabled': !selectedTreeNode }"
            :disabled="!selectedTreeNode"
            :title="selectedTreeNode ? '删除当前树节点' : '请先选择仓库、货架、层或区域'"
            type="button"
            @click="deleteSelectedHierarchy"
          >
            <IconifyIcon icon="lucide:trash-2" />
            <span>删除节点</span>
          </button>
          <button
            v-if="!printSelectionMode"
            v-access:code="['mes:inv:fg-location:print']"
            class="action-tile"
            type="button"
            @click="enterPrintSelectionMode"
          >
            <IconifyIcon icon="lucide:qr-code" />
            <span>打印二维码</span>
          </button>
          <template v-else>
            <button class="action-tile" type="button" @click="selectCurrentRackLocations">
              <IconifyIcon icon="lucide:check-square" />
              <span>选择当前</span>
            </button>
            <button
              class="action-tile"
              :class="{ 'is-disabled': !selectedPrintLocations.length }"
              :disabled="!selectedPrintLocations.length"
              type="button"
              @click="openQrPreview()"
            >
              <IconifyIcon icon="lucide:eye" />
              <span>预览</span>
            </button>
            <button
              class="action-tile"
              :class="{ 'is-disabled': !selectedPrintLocations.length || qrPrinting }"
              :disabled="!selectedPrintLocations.length || qrPrinting"
              type="button"
              @click="printQrRows(selectedPrintLocations)"
            >
              <IconifyIcon icon="lucide:printer" />
              <span>打印</span>
            </button>
            <button class="action-tile is-danger" type="button" @click="exitPrintSelectionMode">
              <IconifyIcon icon="lucide:x" />
              <span>退出</span>
            </button>
          </template>
        </div>
      </section>

      <section class="location-workspace">
        <aside class="location-layer-tree-panel">
          <header class="location-tree-header">
            <strong>库位层级</strong>
            <span>仓库 / 货架 / 层 / 区域</span>
          </header>
          <div class="location-tree-scroll">
            <Tree
              block-node
              :selected-keys="selectedTreeKeys"
              show-line
              :tree-data="layerTreeData"
              default-expand-all
              @select="handleLayerSelect"
            >
              <template #title="{ dataRef }">
                <span class="location-tree-node" :class="`is-${dataRef.nodeType}`">
                  <IconifyIcon :icon="dataRef.nodeType === 'warehouse' ? 'lucide:warehouse' : dataRef.nodeType === 'rack' ? 'lucide:archive' : dataRef.nodeType === 'layer' ? 'lucide:layers-3' : 'lucide:map-pin'" />
                  <strong>{{ dataRef.title }}</strong>
                  <em>{{ dataRef.occupied || 0 }}/{{ dataRef.count || 0 }}</em>
                </span>
              </template>
            </Tree>
          </div>
        </aside>

        <section class="warehouse-map-panel" :class="{ 'is-loading': loading }">
          <div class="location-map-toolbar">
            <div class="location-path-strip">
              <span class="location-path-label">当前路径</span>
              <template v-for="(segment, index) in currentPathSegments" :key="`${segment}-${index}`">
                <IconifyIcon v-if="index > 0" class="location-path-separator" icon="lucide:chevron-right" />
                <strong class="location-path-segment">{{ segment }}</strong>
              </template>
              <em>{{ currentScopeAreaText }}</em>
              <i>已占 {{ currentScopeOccupied }} / {{ currentScopeCapacity }}</i>
            </div>
            <div class="location-search-row">
              <Input
                v-model:value="locationKeyword"
                allow-clear
                class="location-search"
                placeholder="搜索货位编号、层、区域、片号"
              />
              <label class="location-free-filter">
                <span>只显示空闲</span>
                <Switch v-model:checked="onlyFreeLocation" size="small" />
              </label>
            </div>
          </div>

          <Tabs :active-key="activeWorkspaceTab" class="location-work-tabs" @change="handleWorkspaceTabChange">
            <TabPane key="visual" tab="可视化视图">
              <div class="location-layer-tab-pane">
                <section class="location-visual-scroll">
                  <div v-if="!visualLayerGroups.length" class="rack-empty">{{ visualEmptyText }}</div>
                  <article v-for="group in visualLayerGroups" v-else :key="group.layerNo" class="location-layer-group">
                    <header class="location-layer-group-header">
                      <div>
                        <IconifyIcon icon="lucide:layers-3" />
                        <strong>L{{ group.layerNo }}层</strong>
                        <span>{{ group.rows.length }} 个区域</span>
                      </div>
                      <em>{{ group.occupied }}/{{ group.capacity }}</em>
                    </header>
                    <section class="location-area-groups">
                      <article v-for="row in group.rows" :key="row.code" class="location-area-group">
                        <header class="location-area-group-header">
                          <div>
                            <strong>{{ row.code }}</strong>
                            <span>{{ row.areaNo }}区</span>
                          </div>
                          <em>{{ row.occupied }}/{{ row.capacity }}</em>
                        </header>
                        <div v-if="!hasConfiguredArea(row)" class="location-area-empty">
                          <span>该分区尚未创建库位</span>
                          <small>请使用右上角“新增库位”</small>
                        </div>
                        <div v-else-if="!visibleAreaSlots(row).length" class="location-area-empty">
                          当前筛选下无库位
                        </div>
                        <div v-else class="location-pallet-waterfall">
                          <div
                            v-for="slot in visibleAreaSlots(row)"
                            :key="`${row.code}-${slot.slotNo}`"
                            class="pallet-card-wrap"
                            :class="{
                              'is-missing': !slot.location,
                              'is-occupied': !!slot.piece,
                              'is-print-selected': printSelectionMode && isPrintSelected(slot.location),
                              'is-selection-mode': printSelectionMode,
                              'is-selected': selectedLocationCode === slot.location?.locationCode,
                            }"
                          >
                            <button
                              v-if="printSelectionMode"
                              class="pallet-card-select"
                              :class="{ 'is-checked': isPrintSelected(slot.location) }"
                              :disabled="!slot.location"
                              type="button"
                              @click.stop="togglePrintSelection(slot.location)"
                            >
                              <IconifyIcon :icon="isPrintSelected(slot.location) ? 'lucide:check-square' : 'lucide:square'" />
                            </button>
                            <button
                              class="pallet-card"
                              :disabled="!slot.location"
                              :title="slotVisualText(slot)"
                              type="button"
                              @click="handleSlotClick(slot.location)"
                            >
                              <strong>{{ slot.slotText }}</strong>
                              <template v-if="slot.piece">
                                <span class="pallet-piece">{{ slotPieceText(slot) }}</span>
                              </template>
                              <span v-else-if="slotVisualText(slot)" class="pallet-empty">
                                {{ slotVisualText(slot) }}
                              </span>
                            </button>
                          </div>
                        </div>
                      </article>
                    </section>
                  </article>
                </section>
              </div>
            </TabPane>

            <TabPane key="table" tab="表格视图">
              <section class="location-table-grid-panel">
                <LocationGrid table-title="库位明细">
                  <template #printSelect="{ row }">
                    <Button
                      class="location-table-select-btn"
                      size="small"
                      type="link"
                      @click.stop="togglePrintSelection(row)"
                    >
                      <template #icon>
                        <IconifyIcon :icon="isPrintSelected(row) ? 'lucide:check-square' : 'lucide:square'" />
                      </template>
                    </Button>
                  </template>
                </LocationGrid>
              </section>
            </TabPane>
          </Tabs>
        </section>

      </section>

      <Modal
        v-model:open="detailVisible"
        :footer="null"
        force-render
        title="库位详情"
        width="980px"
      >
        <section class="location-detail-table-panel is-modal">
          <header class="location-detail-header">
            <div class="location-detail-title">
              <span>库位详情</span>
              <strong>{{ selectedLocation?.locationName || '未选择库位' }}</strong>
              <em>{{ selectedLocation?.locationCode || '请选择左侧库位' }}</em>
            </div>
            <div v-if="selectedLocation" class="location-structure-tags">
              <Tag color="geekblue">{{ selectedLocationMeta.rackNo }}#货架</Tag>
              <Tag color="cyan">{{ selectedLocationMeta.layerText }}</Tag>
              <Tag color="blue">{{ selectedLocationMeta.areaText }}</Tag>
            </div>
            <div class="location-detail-actions">
              <Tag v-if="selectedLocation" :color="statusColor(selectedLocation.status)">
                {{ selectedLocation.status || '-' }}
              </Tag>
              <Tooltip title="打印二维码">
                <Button :disabled="!selectedLocation" size="small" @click="handlePrint(selectedLocation)">
                  <template #icon><IconifyIcon icon="lucide:qr-code" /></template>
                </Button>
              </Tooltip>
            </div>
          </header>
          <div v-if="selectedLocation" class="location-info-panel">
            <span>{{ selectedLocation.positionDesc || '-' }}</span>
            <strong>容量 {{ selectedLocation.occupiedQty || 0 }} / {{ selectedLocation.capacityQty || 0 }}</strong>
          </div>
          <DetailGrid />
        </section>
      </Modal>

      <Modal
        v-model:open="formVisible"
        :confirm-loading="saving"
        :title="formModel.id ? '编辑库位' : '新增库位'"
        width="720px"
        destroy-on-close
        @ok="submitForm"
      >
        <div class="location-form-tip">
          <template v-if="formModel.id && formCanEditStructure">
            当前库位无库存片号，可调整所属货架、层、区域、库位名称和容量；库位编码按仓库编码、货架编号、层编号和区域编号自动生成。
          </template>
          <template v-else-if="formModel.id">
            当前库位已有库存片号，不能调整所属货架、层和区域；仍可修改库位名称和容量，且容量不得小于当前库存数量。
          </template>
          <template v-else>
            新增库位以左侧选中的仓库为准；选择货架、层和区域编号后系统自动生成带仓库编码前缀的库位编码和默认名称；已维护的区域编号不可重复使用。
          </template>
        </div>
        <Form layout="vertical" class="location-form">
          <Form.Item label="所属仓库">
            <Input :value="formWarehouse ? `${formWarehouse.warehouseName}（${formWarehouse.warehouseCode}）` : '-'" disabled />
          </Form.Item>
          <Form.Item label="所属货架">
            <Select
              v-model:value="formStructure.rackId"
              :disabled="!formCanEditStructure"
              :options="formRackOptions"
              class="form-control"
              @change="handleRackChange"
            />
          </Form.Item>
          <Form.Item label="所属层">
            <Select
              v-model:value="formStructure.layerId"
              :disabled="!formCanEditStructure"
              :options="formLayerOptions"
              class="form-control"
              @change="handleLayerChange"
            />
          </Form.Item>
          <Form.Item label="区域编号">
            <InputNumber
              v-model:value="formStructure.areaNo"
              :disabled="!formCanEditStructure"
              :max="999"
              :min="1"
              class="form-control"
              @change="handleStructureChange"
            />
          </Form.Item>
          <Form.Item label="库位编号">
            <Input v-model:value="formModel.locationCode" readonly />
          </Form.Item>
          <Form.Item label="分区容量">
            <InputNumber v-model:value="formModel.capacityQty" :min="1" class="form-control" />
          </Form.Item>
          <Form.Item label="质量用途">
            <Select
              v-model:value="formModel.qualityScope"
              :disabled="!!formModel.id && !formCanEditStructure"
              :options="[
                { label: '合格品库位', value: 'QUALIFIED' },
                { label: '不合格品隔离库位', value: 'QUARANTINE' },
                {
                  label: '历史待盘点（不可上架）',
                  value: 'UNASSIGNED',
                  disabled: true,
                },
              ]"
            />
          </Form.Item>
          <Form.Item label="库位名称">
            <Input v-model:value="formModel.locationName" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        v-model:open="hierarchyFormVisible"
        :confirm-loading="hierarchySaving"
        :title="`${hierarchyForm.id ? '编辑' : '新增'}${hierarchyFormKind === 'warehouse' ? '仓库' : hierarchyFormKind === 'rack' ? '货架' : '层'}`"
        width="620px"
        destroy-on-close
        @ok="submitHierarchyForm"
      >
        <div class="location-form-tip">
          <template v-if="hierarchyFormKind === 'warehouse'">
            仓库编码是库位编码前缀。已有货架的仓库不能修改编码，避免二维码和库存库位引用混淆。
          </template>
          <template v-else-if="hierarchyFormKind === 'rack'">
            货架名称可独立维护；货架编号仅在所属仓库内唯一，并用于生成库位编码。
          </template>
          <template v-else>
            层名称可独立维护；层编号用于生成库位编码，已有区域的层不可更改编号或所属货架。
          </template>
        </div>
        <Form class="location-form" layout="vertical">
          <template v-if="hierarchyFormKind === 'warehouse'">
            <Form.Item label="仓库编码">
              <Input v-model:value="hierarchyForm.warehouseCode" :maxlength="32" placeholder="例如：BLACK" />
            </Form.Item>
            <Form.Item label="仓库名称">
              <Input v-model:value="hierarchyForm.warehouseName" :maxlength="100" placeholder="例如：黑垫仓库" />
            </Form.Item>
          </template>
          <Form.Item v-else-if="hierarchyFormKind === 'rack'" label="所属仓库">
            <Input :value="`${hierarchyForm.warehouseName}（${hierarchyForm.warehouseCode}）`" disabled />
          </Form.Item>
          <Form.Item v-if="hierarchyFormKind === 'layer'" label="所属货架">
            <Input :value="hierarchyForm.rackName" disabled />
          </Form.Item>
          <Form.Item v-if="hierarchyFormKind !== 'warehouse'" :label="hierarchyFormKind === 'rack' ? '货架编号' : '层编号'">
            <InputNumber
              v-if="hierarchyFormKind === 'rack'"
              v-model:value="hierarchyForm.rackNo"
              :max="999"
              :min="1"
              class="form-control"
            />
            <InputNumber
              v-else
              v-model:value="hierarchyForm.layerNo"
              :max="999"
              :min="1"
              class="form-control"
            />
          </Form.Item>
          <Form.Item v-if="hierarchyFormKind !== 'warehouse'" :label="hierarchyFormKind === 'rack' ? '货架名称' : '层名称'">
            <Input v-if="hierarchyFormKind === 'rack'" v-model:value="hierarchyForm.rackName" placeholder="例如：A区成品货架" />
            <Input v-else v-model:value="hierarchyForm.layerName" placeholder="例如：低温层" />
          </Form.Item>
          <Form.Item label="状态">
            <Select
              v-model:value="hierarchyForm.status"
              :options="[{ label: '启用', value: '启用' }, { label: '停用', value: '停用' }]"
            />
          </Form.Item>
          <Form.Item label="排序号">
            <InputNumber v-model:value="hierarchyForm.sortNo" :min="0" class="form-control" />
          </Form.Item>
          <Form.Item label="备注">
            <Input v-model:value="hierarchyForm.remark" :maxlength="200" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        v-model:open="warehouseQualityVisible"
        :confirm-loading="warehouseQualitySaving"
        title="设置仓库质量用途"
        width="620px"
        destroy-on-close
        @ok="submitWarehouseQualityScope"
      >
        <div class="location-form-tip">
          本操作会统一更新该仓库下全部库位。系统会先核对现有库存质量；只要存在与目标用途不一致的在库片号，就会拒绝整次修改。
        </div>
        <Form class="location-form" layout="vertical">
          <Form.Item label="目标仓库">
            <Input
              :value="selectedWarehouseForQualityScope ? `${selectedWarehouseForQualityScope.warehouseName}（${selectedWarehouseForQualityScope.warehouseCode}）` : '-'"
              disabled
            />
          </Form.Item>
          <div class="warehouse-quality-summary">
            <span>库位 <strong>{{ selectedWarehouseQualityStats.locationCount }}</strong> 个</span>
            <span>有库存库位 <strong>{{ selectedWarehouseQualityStats.occupiedLocationCount }}</strong> 个</span>
            <span>当前在库 <strong>{{ selectedWarehouseQualityStats.activePieceCount }}</strong> 片</span>
          </div>
          <Form.Item label="当前配置">
            <Input :value="currentWarehouseQualityText()" disabled />
          </Form.Item>
          <Form.Item label="目标质量用途" required>
            <Select
              v-model:value="warehouseQualityScope"
              :options="[
                { label: '合格品仓（只允许 OK 成品）', value: 'QUALIFIED' },
                { label: '不合格品隔离仓（只允许 NG 成品）', value: 'QUARANTINE' },
              ]"
            />
          </Form.Item>
        </Form>
        <div class="warehouse-quality-warning">
          设置完成后，待上架页面只允许选择质量用途匹配的库位；历史待盘点状态将被统一覆盖。
        </div>
      </Modal>

      <Modal
        v-model:open="qrPreviewVisible"
        :footer="null"
        title="库位二维码预览"
        width="980px"
      >
        <div class="qr-preview-toolbar">
          <span>共 {{ qrPreviewRows.length }} 个库位，二维码内容为库位编号</span>
          <Button :loading="qrPrinting" type="primary" @click="printQrRows()">
            <template #icon><IconifyIcon icon="lucide:printer" /></template>
            调用本机服务打印
          </Button>
        </div>
        <div class="qr-preview-grid">
          <article
            v-for="row in qrPreviewRows"
            :key="row.locationCode"
            class="qr-preview-card"
          >
            <QRCode :size="128" :value="row.locationCode || ''" />
            <strong>{{ row.locationCode }}</strong>
            <span>{{ row.locationName }}</span>
          </article>
        </div>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.fg-location-page {
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
}

.fg-location-page .prototype-banner {
  min-height: 78px;
  max-height: 86px;
}

.location-progress-card {
  display: grid;
  flex: 0 0 170px;
  grid-template-columns: minmax(0, 1fr);
  gap: 2px;
  padding: 4px 14px;
  color: #075985;
  border-left: 1px solid rgba(95, 107, 122, 0.36);
}

.location-progress-card span {
  font-size: 12px;
  font-weight: 900;
}

.location-progress-card strong {
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 20px;
  font-weight: 950;
  line-height: 22px;
}

.location-progress-card :deep(.ant-progress) {
  line-height: 1;
}

.location-workspace {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  gap: 12px;
}

.location-layer-tree-panel {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.location-tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 40px;
  padding: 8px 14px;
  background: #eef3f8;
  border-bottom: 1px solid #d8e0ea;
}

.location-tree-header strong {
  color: #172033;
  font-size: 14px;
  font-weight: 950;
}

.location-tree-header span {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.location-tree-scroll {
  min-height: 0;
  padding: 10px 14px;
  overflow: auto;
}

.location-tree-scroll :deep(.ant-tree) {
  min-width: max-content;
  background: transparent;
}

.location-tree-scroll :deep(.ant-tree-node-content-wrapper) {
  min-height: 30px;
  line-height: 30px;
}

.location-tree-scroll :deep(.ant-tree-node-content-wrapper.ant-tree-node-selected) {
  background: #dbeafe;
}

.location-tree-node {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  width: 100%;
  color: #334155;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.location-tree-node :deep(.iconify) {
  flex: 0 0 auto;
  font-size: 15px;
}

.location-tree-node.is-rack {
  color: #075985;
}

.location-tree-node.is-layer {
  color: #1d4ed8;
}

.location-tree-node.is-area {
  color: #475569;
}

.location-tree-node strong {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.location-tree-node em {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 42px;
  height: 18px;
  padding: 0 6px;
  color: #475569;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  line-height: 18px;
  background: #e2e8f0;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
}

.warehouse-map-panel {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  height: 100%;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.warehouse-map-panel.is-loading {
  opacity: 0.72;
}

.location-map-toolbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #d8e0ea;
}

.location-view-switch {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 6px;
  min-width: max-content;
}

.view-mode-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  color: #334155;
  font-size: 13px;
  font-weight: 900;
  background: #fff;
  border: 1px solid #a6b2c2;
  border-radius: 2px;
  cursor: pointer;
}

.view-mode-btn.is-active {
  color: #075985;
  background: #e0f2fe;
  border-color: #0ea5e9;
  box-shadow: inset 0 0 0 1px #0ea5e9;
}

.location-search {
  flex: 1 1 auto;
  min-width: 0;
}

.location-search-row {
  display: flex;
  flex: 1 1 100%;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex-wrap: nowrap;
}

.location-path-strip {
  display: inline-flex;
  flex: 1 1 100%;
  align-items: center;
  gap: 6px;
  min-width: 0;
  min-height: 34px;
  padding: 6px 10px;
  color: #334155;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.location-path-label,
.location-path-strip em,
.location-path-strip i {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.location-path-label {
  color: #075985;
}

.location-path-segment {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  max-width: 220px;
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-path-separator {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 14px;
}

.location-path-strip i {
  margin-left: auto;
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
}

.location-filter-status {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 10px;
  color: #075985;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
  border-radius: 2px;
}

.location-filter-status span,
.location-filter-status strong {
  font-size: 12px;
  font-weight: 950;
  white-space: nowrap;
}

.location-filter-status strong {
  color: #334155;
  font-family: Consolas, 'Microsoft YaHei', monospace;
}

.location-free-filter {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  background: #fff;
  border: 1px solid #d8e0ea;
  border-radius: 2px;
}

.location-free-filter span {
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.location-work-tabs {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
}

.location-work-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0;
}

.location-work-tabs :deep(.ant-tabs-content-holder),
.location-work-tabs :deep(.ant-tabs-content),
.location-work-tabs :deep(.ant-tabs-tabpane) {
  min-width: 0;
  min-height: 0;
}

.location-work-tabs :deep(.ant-tabs-content-holder) {
  display: flex;
  flex: 1 1 auto;
}

.location-work-tabs :deep(.ant-tabs-content) {
  flex: 1 1 auto;
}

.location-work-tabs :deep(.ant-tabs-tabpane-active) {
  display: flex;
  height: 100%;
  flex-direction: column;
  overflow: hidden;
}

.location-layer-tab-pane {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
}

.rack-summary-strip {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: max-content max-content max-content minmax(120px, 1fr) max-content;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 6px 8px;
  margin-top: 8px;
  color: #334155;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.rack-summary-strip span,
.rack-summary-strip strong,
.rack-summary-strip em,
.rack-summary-strip i {
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.rack-summary-strip span {
  color: #075985;
  font-size: 13px;
}

.rack-summary-strip strong,
.rack-summary-strip em,
.rack-summary-strip i {
  color: #64748b;
  font-size: 12px;
}

.rack-empty {
  display: grid;
  min-height: 240px;
  place-items: center;
  color: #64748b;
  font-size: 13px;
  font-weight: 900;
  background: #fff;
  border: 1px dashed #a6b2c2;
}

.location-visual-scroll {
  display: grid;
  flex: 1 1 auto;
  align-content: start;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  padding-top: 8px;
  overflow: auto;
}

.location-layer-group {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  background: #eef6fb;
  border: 1px solid #b6c8da;
}

.location-layer-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 32px;
  padding: 2px 4px 8px;
  border-bottom: 1px solid #cbd5e1;
}

.location-layer-group-header div {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.location-layer-group-header :deep(.iconify) {
  flex: 0 0 auto;
  color: #075985;
  font-size: 16px;
}

.location-layer-group-header strong {
  color: #075985;
  font-size: 14px;
  font-weight: 950;
  white-space: nowrap;
}

.location-layer-group-header span,
.location-layer-group-header em {
  color: #475569;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.location-area-groups {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  align-content: start;
  gap: 10px;
  min-width: 0;
  min-height: 0;
}

.location-area-group {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  background: #fff;
  border: 1px solid #cbd5e1;
}

.location-area-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 30px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.location-area-group-header div {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.location-area-group-header strong {
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 13px;
  font-weight: 950;
}

.location-area-group-header span,
.location-area-group-header em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.location-area-empty {
  display: grid;
  min-height: 80px;
  place-items: center;
  gap: 4px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 900;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}

.location-pallet-waterfall {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(82px, 1fr));
  gap: 6px;
}

.pallet-card-wrap {
  position: relative;
  min-width: 0;
}

.pallet-card {
  position: relative;
  display: flex;
  width: 100%;
  min-height: 58px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
  padding: 16px 6px 8px;
  color: #334155;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    color 0.16s ease;
}

.pallet-card strong {
  position: absolute;
  top: 5px;
  right: 7px;
  color: #64748b;
  font-size: 11px;
  font-weight: 950;
  line-height: 14px;
}

.pallet-card span {
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pallet-card span {
  color: #075985;
  font-size: 12px;
  font-weight: 950;
}

.pallet-card .pallet-piece {
  font-size: 11px;
  line-height: 15px;
}

.pallet-card .pallet-empty {
  font-size: 12px;
  line-height: 18px;
}

.pallet-card-wrap.is-occupied .pallet-card {
  color: #7f1d1d;
  background: #fee2e2;
  border-color: #fecaca;
}

.pallet-card-wrap.is-occupied .pallet-card span,
.pallet-card-wrap.is-occupied .pallet-card strong {
  color: #7f1d1d;
}

.pallet-card-wrap.is-missing .pallet-card {
  color: #94a3b8;
  background: #f1f5f9;
  border-style: dashed;
  cursor: not-allowed;
}

.pallet-card-wrap.is-selected .pallet-card {
  background: #bae6fd;
  border-color: #0284c7;
  box-shadow: inset 0 0 0 1px #0284c7;
}

.pallet-card-wrap.is-print-selected .pallet-card {
  box-shadow: inset 0 0 0 2px #0ea5e9;
}

.pallet-card-wrap.is-selection-mode .pallet-card {
  cursor: copy;
}

.pallet-card:disabled {
  cursor: not-allowed;
}

.pallet-card-select {
  position: absolute;
  z-index: 2;
  top: 4px;
  left: 4px;
  display: inline-flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: #64748b;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #cbd5e1;
  cursor: pointer;
}

.pallet-card-select.is-checked {
  color: #0284c7;
  border-color: #38bdf8;
}

.pallet-card-select:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.excel-location-map {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  padding-top: 8px;
  overflow: auto;
}

.excel-location-table {
  display: grid;
  align-content: start;
  min-width: max-content;
  background: #fff;
  border: 1px solid #9aa8b8;
}

.excel-location-row {
  display: grid;
  grid-template-columns: 118px repeat(15, 92px);
  min-height: 62px;
  border-bottom: 1px solid #d8e0ea;
}

.excel-location-row:last-child {
  border-bottom: 0;
}

.excel-location-row.is-head {
  position: sticky;
  top: 0;
  z-index: 4;
  min-height: 30px;
}

.excel-area-head,
.excel-slot-head,
.excel-area-code,
.excel-slot-wrap {
  min-width: 0;
  border-right: 1px solid #d8e0ea;
}

.excel-area-head,
.excel-slot-head {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #334155;
  font-size: 11px;
  font-weight: 950;
  background: #e2e8f0;
}

.excel-area-head,
.excel-area-code {
  position: sticky;
  left: 0;
  z-index: 3;
  border-right-color: #8794a4;
}

.excel-area-code {
  display: grid;
  align-content: center;
  gap: 1px;
  padding: 4px 8px;
  color: #075985;
  background: #eef6fb;
}

.excel-area-code strong {
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 12px;
  font-weight: 950;
  line-height: 16px;
  white-space: nowrap;
}

.excel-area-code span {
  color: #64748b;
  font-size: 11px;
  font-weight: 900;
  line-height: 14px;
  white-space: nowrap;
}

.excel-slot-wrap {
  position: relative;
  min-height: 62px;
  background: #fff;
}

.excel-slot-cell {
  display: grid;
  width: 100%;
  height: 100%;
  min-height: 62px;
  grid-template-rows: 15px 18px 18px;
  gap: 1px;
  align-items: center;
  padding: 3px 5px;
  color: #334155;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  background: #fff;
  border: 0;
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    color 0.16s ease,
    opacity 0.16s ease;
}

.excel-slot-select {
  position: absolute;
  z-index: 2;
  top: 2px;
  right: 2px;
  display: inline-flex;
  width: 16px;
  height: 16px;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: #64748b;
  background: rgba(255, 255, 255, 0.88);
  border: 0;
  cursor: pointer;
}

.excel-slot-select.is-checked {
  color: #0284c7;
}

.excel-slot-select:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.excel-slot-cell strong,
.excel-slot-cell span,
.excel-slot-cell em {
  min-width: 0;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.excel-slot-cell strong {
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 11px;
  font-weight: 950;
  line-height: 14px;
}

.excel-slot-cell span {
  color: #64748b;
  font-size: 10px;
  font-weight: 900;
  line-height: 16px;
}

.excel-slot-cell em {
  color: #475569;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
  line-height: 16px;
}

.excel-slot-wrap.is-occupied .excel-slot-cell {
  color: #7f1d1d;
  background: #fee2e2;
}

.excel-slot-wrap.is-occupied .excel-slot-cell strong,
.excel-slot-wrap.is-occupied .excel-slot-cell span,
.excel-slot-wrap.is-occupied .excel-slot-cell em {
  color: #7f1d1d;
}

.excel-slot-wrap.is-selected .excel-slot-cell {
  color: #075985;
  background: #bae6fd;
  box-shadow: inset 0 0 0 1px #0284c7;
}

.excel-slot-wrap.is-print-selected {
  box-shadow: inset 0 0 0 2px #0ea5e9;
}

.excel-slot-wrap.is-selection-mode .excel-slot-cell {
  cursor: copy;
}

.excel-slot-wrap.is-missing .excel-slot-cell {
  color: #94a3b8;
  background: #f1f5f9;
  cursor: not-allowed;
}

.excel-slot-wrap.is-dimmed {
  opacity: 0.22;
}

.location-table-grid-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding-top: 8px;
  overflow: hidden;
}

.location-table-grid-panel :deep(.vben-vxe-grid),
.location-table-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.location-table-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.location-table-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.location-table-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.location-table-grid-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.location-table-select-btn {
  display: inline-flex;
  width: 28px;
  min-width: 28px;
  align-items: center;
  justify-content: center;
  padding-inline: 0;
}

.location-detail-table-panel {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.location-detail-table-panel.is-modal {
  height: 62vh;
  min-height: 460px;
  max-height: 680px;
}

.location-detail-header {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 8px;
  min-height: 56px;
  padding: 8px;
  margin-bottom: 8px;
  overflow: hidden;
  background: linear-gradient(180deg, #fff 0%, #eef3f8 100%);
  border: 1px solid #d8e0ea;
}

.location-detail-title {
  display: grid;
  min-width: 0;
  gap: 1px;
}

.location-detail-title span {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
  line-height: 16px;
}

.location-detail-title strong {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 17px;
  font-weight: 950;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-detail-title em {
  min-width: 0;
  overflow: hidden;
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-structure-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px;
  min-width: 0;
}

.location-structure-tags :deep(.ant-tag) {
  margin-inline-end: 0;
  font-weight: 900;
}

.location-detail-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  min-width: max-content;
}

.location-detail-actions .ant-btn {
  width: 30px;
  border-radius: 2px;
}

.location-info-panel {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: minmax(0, 1fr) max-content;
  gap: 8px;
  align-items: center;
  min-height: 34px;
  padding: 6px 8px;
  margin-bottom: 8px;
  color: #334155;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.location-info-panel span {
  min-width: 0;
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-info-panel strong {
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.location-detail-table-panel :deep(.vben-vxe-grid),
.location-detail-table-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.location-detail-table-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.location-detail-table-panel :deep(.vxe-grid--form-wrapper),
.location-detail-table-panel :deep(.vxe-grid--toolbar-wrapper) {
  flex: 0 0 auto;
}

.location-detail-table-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.location-detail-table-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.location-detail-table-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.location-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 14px;
}

.location-form-tip {
  margin-bottom: 12px;
  padding: 8px 10px;
  color: #315b85;
  font-size: 13px;
  line-height: 20px;
  background: #f0f7ff;
  border: 1px solid #cfe2f5;
  border-radius: 6px;
}

.warehouse-quality-summary {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.warehouse-quality-summary span {
  padding: 9px 10px;
  color: #475569;
  font-size: 12px;
  text-align: center;
  background: #f8fafc;
  border: 1px solid #d8e0ea;
  border-radius: 4px;
}

.warehouse-quality-summary strong {
  color: #0f4c81;
  font-size: 15px;
}

.warehouse-quality-warning {
  margin-top: 10px;
  padding: 8px 10px;
  color: #92400e;
  font-size: 12px;
  line-height: 20px;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 4px;
}

.location-form :deep(.ant-form-item) {
  margin-bottom: 8px;
}

.form-control {
  width: 100%;
}

.qr-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0 12px;
  color: #334155;
  font-size: 13px;
  font-weight: 900;
}

.qr-preview-grid {
  display: grid;
  max-height: 62vh;
  padding: 8px;
  overflow: auto;
  gap: 10px;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  background: #f8fafc;
  border: 1px solid #d8e0ea;
}

.qr-preview-card {
  display: grid;
  justify-items: center;
  gap: 6px;
  min-width: 0;
  padding: 10px;
  text-align: center;
  background: #fff;
  border: 1px solid #cbd5e1;
}

.qr-preview-card strong,
.qr-preview-card span {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qr-preview-card strong {
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 13px;
  font-weight: 950;
}

.qr-preview-card span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 1100px) {
  .fg-location-page .prototype-banner {
    max-height: none;
    flex-wrap: wrap;
  }

  .location-progress-card {
    flex-basis: 150px;
  }

  .location-workspace {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }

  .location-layer-tree-panel {
    min-height: 260px;
  }

  .excel-location-map {
    min-height: 520px;
  }

  .excel-location-row {
    grid-template-columns: 108px repeat(15, 82px);
  }

  .location-detail-table-panel {
    min-height: 420px;
  }
}

@media (max-width: 720px) {
  .location-progress-card {
    flex: 1 1 100%;
    padding: 4px 0;
    border-left: 0;
  }

  .location-workspace,
  .location-form {
    grid-template-columns: 1fr;
  }

  .warehouse-quality-summary {
    grid-template-columns: 1fr;
  }

  .location-map-toolbar {
    flex-wrap: wrap;
  }

  .location-view-switch {
    width: 100%;
    overflow-x: auto;
  }

  .qr-preview-toolbar {
    width: 100%;
  }

  .qr-preview-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .rack-summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .rack-summary-strip :deep(.ant-progress) {
    grid-column: span 2;
  }

  .location-detail-header,
  .location-info-panel {
    grid-template-columns: 1fr;
  }

  .excel-location-row {
    grid-template-columns: 96px repeat(15, 76px);
  }

  .excel-slot-cell {
    padding-inline: 3px;
  }

  .location-structure-tags,
  .location-detail-actions {
    justify-content: flex-start;
  }

}
</style>

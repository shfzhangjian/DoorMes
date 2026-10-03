import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { DICT_TYPE } from '@vben/constants';

import { getMeasureToolLedgerSelectOptions } from '#/api/mes/quality/measure-tool';
import { getSimpleDeptList } from '#/api/system/dept';
import { getSimpleDictDataList } from '#/api/system/dict/data';
import { getSimpleUserList } from '#/api/system/user';

export type LedgerOption = {
  label?: string;
  value?: number | string;
};

export type MeasureToolAreaOption = LedgerOption & {
  parentId?: number;
  parentName?: string;
};

export type MeasureToolLocationFilterOption = LedgerOption & {
  locationId?: number;
};

export function mapLocationOptions(
  rows: QmsMeasureToolApi.Category[] = [],
): LedgerOption[] {
  return rows
    .filter(
      (item) => item.status !== 0 && (!item.parentId || item.parentId <= 0),
    )
    .map((item) => ({
      label: item.categoryName || item.categoryCode || String(item.id),
      value: item.id,
    }))
    .toSorted(compareOptions);
}

export function mapLocationFilterOptions(
  rows: QmsMeasureToolApi.Category[] = [],
): MeasureToolLocationFilterOption[] {
  return rows
    .filter(
      (item) => item.status !== 0 && (!item.parentId || item.parentId <= 0),
    )
    .map((item) => ({
      label: item.categoryName || item.categoryCode || String(item.id),
      locationId: item.id,
      value: item.categoryName || item.categoryCode || String(item.id),
    }))
    .toSorted(compareOptions);
}

export function mapAreaOptions(
  rows: QmsMeasureToolApi.Category[] = [],
): MeasureToolAreaOption[] {
  const locationNameMap = new Map<number, string>();
  rows.forEach((item) => {
    if (
      item.id &&
      item.status !== 0 &&
      (!item.parentId || item.parentId <= 0)
    ) {
      locationNameMap.set(
        item.id,
        item.categoryName || item.categoryCode || String(item.id),
      );
    }
  });
  return rows
    .filter((item) => item.status !== 0 && item.parentId && item.parentId > 0)
    .map((item) => ({
      label: item.categoryName || item.categoryCode || String(item.id),
      parentId: item.parentId,
      parentName: locationNameMap.get(item.parentId),
      value: item.id,
    }))
    .toSorted(compareOptions);
}

function compareOptions(left: LedgerOption, right: LedgerOption) {
  return (left.label || '').localeCompare(right.label || '', 'zh-CN');
}

function toUniqueOptions(...valueGroups: Array<Array<string | undefined>>) {
  const optionMap = new Map<string, LedgerOption>();
  valueGroups.flat().forEach((rawValue) => {
    const value = rawValue?.trim();
    if (!value) {
      return;
    }
    const uniqueKey = value.toLocaleLowerCase();
    if (!optionMap.has(uniqueKey)) {
      optionMap.set(uniqueKey, { label: value, value });
    }
  });
  return [...optionMap.values()].toSorted(compareOptions);
}

async function loadDictOptions(dictType: string): Promise<LedgerOption[]> {
  const rows = await getSimpleDictDataList();
  // 精简字典接口只返回启用项，且其响应不包含 status 字段。
  return rows
    .filter((item) => item.dictType === dictType)
    .map((item) => ({ label: item.label, value: item.value }))
    .toSorted(compareOptions);
}

export async function loadCalibrationOrgOptions() {
  const [dictOptions, selectOptions] = await Promise.all([
    loadDictOptions(DICT_TYPE.MES_QMS_MEASURE_TOOL_CALIBRATION_ORG),
    getMeasureToolLedgerSelectOptions(),
  ]);
  return toUniqueOptions(
    dictOptions.map((item) => String(item.value || '')),
    selectOptions.calibrationOrgs || [],
  );
}

export function loadCalibrationMethodOptions() {
  return loadDictOptions(DICT_TYPE.MES_QMS_MEASURE_TOOL_CALIBRATION_METHOD);
}

export async function loadUsingDepartmentOptions() {
  const [deptRows, selectOptions] = await Promise.all([
    getSimpleDeptList(),
    getMeasureToolLedgerSelectOptions(),
  ]);
  return toUniqueOptions(
    deptRows.filter((item) => item.status === 0).map((item) => item.name),
    selectOptions.usingDepartments || [],
  );
}

export function normalizeEditableSelectValue(value: unknown) {
  const rawValue = Array.isArray(value) ? value[value.length - 1] : value;
  if (typeof rawValue !== 'string') {
    return undefined;
  }
  const normalizedValue = rawValue.trim();
  return normalizedValue || undefined;
}

export function toEditableSelectValue(value?: string) {
  return value?.trim() || undefined;
}

export async function loadPersonnelOptions(): Promise<LedgerOption[]> {
  const [userRows, selectOptions] = await Promise.all([
    getSimpleUserList(),
    getMeasureToolLedgerSelectOptions(),
  ]);
  return toUniqueOptions(
    userRows
      .filter((item) => item.status === 0)
      .map((item) => item.nickname || item.username),
    selectOptions.personnelNames || [],
  );
}

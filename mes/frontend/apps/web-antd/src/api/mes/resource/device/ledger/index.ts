import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesDeviceLedgerApi {
  export interface Device {
    id?: number;
    rowNo?: number;
    deviceCode?: string;
    deviceName?: string;
    categoryId?: number;
    categoryName?: string;
    deviceType?: string;
    model?: string;
    specification?: string;
    manufacturer?: string;
    deviceLength?: string;
    deviceWidth?: string;
    deviceHeight?: string;
    assetNo?: string;
    location?: string;
    usingDepartment?: string;
    responsiblePerson?: string;
    factoryDate?: string;
    purchaseDate?: string;
    installDate?: string;
    useDate?: string;
    commissioningDate?: string;
    status?: number;
    maintStatus?: string;
    remark?: string;
    version?: number;
    createTime?: string;
    parts?: DevicePart[];
    params?: DeviceParam[];
  }

  export interface DevicePart {
    id?: number;
    deviceId?: number;
    partCode?: string;
    partName?: string;
    spec?: string;
    quantity?: number;
    replaceCycle?: number;
    remark?: string;
    sort?: number;
  }

  export interface DeviceParam {
    id?: number;
    deviceId?: number;
    paramName?: string;
    paramValue?: string;
    unit?: string;
    remark?: string;
    sort?: number;
  }
}

export function getDevicePage(params: PageParam) {
  return requestClient.get<PageResult<MesDeviceLedgerApi.Device>>(
    '/mes/resource/device/ledger/page',
    { params },
  );
}

export function getDevice(id: number) {
  return requestClient.get<MesDeviceLedgerApi.Device>(
    `/mes/resource/device/ledger/get?id=${id}`,
  );
}

export function createDevice(data: MesDeviceLedgerApi.Device) {
  return requestClient.post('/mes/resource/device/ledger/create', data);
}

export function updateDevice(data: MesDeviceLedgerApi.Device) {
  return requestClient.put('/mes/resource/device/ledger/update', data);
}

export function deleteDeviceList(ids: number[]) {
  return requestClient.delete(
    `/mes/resource/device/ledger/delete-list?ids=${ids.join(',')}`,
  );
}

export function exportDevice(params: any) {
  return requestClient.download('/mes/resource/device/ledger/export-excel', {
    params,
  });
}

export function getDevicePartListById(deviceId: number) {
  return requestClient.get<MesDeviceLedgerApi.DevicePart[]>(
    '/mes/resource/device/ledger/part/list',
    { params: { deviceId } },
  );
}

export function getDeviceParamListById(deviceId: number) {
  return requestClient.get<MesDeviceLedgerApi.DeviceParam[]>(
    '/mes/resource/device/ledger/param/list',
    { params: { deviceId } },
  );
}

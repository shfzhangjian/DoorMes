import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import {
  FILE_TYPE_COMPLIANCE_AGREEMENT,
  FILE_TYPE_ENVIRONMENT_CERT,
  FILE_TYPE_SYSTEM_CERT,
} from './template';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'supplierInfo',
      label: '供应商',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '名称/代码模糊检索',
      },
    },
    {
      fieldName: 'providedProduct',
      label: '供应产品',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入供应产品',
      },
    },
    {
      fieldName: 'productModel',
      label: '产品型号',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入产品型号',
      },
    },
    {
      fieldName: 'expiryStatus',
      label: '过期状态',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '全部', value: '' },
          { label: '已过期', value: 'EXPIRED' },
          { label: '未过期', value: 'UNEXPIRED' },
        ],
      },
      defaultValue: '',
    },
  ];
}

export function useGridColumns(
  fileType?: unknown,
): VxeTableGridOptions['columns'] {
  const normalizedFileType = String(fileType || '');
  const fileNameTitle = getFileNameTitle(normalizedFileType);
  const effectDateTitle = getEffectDateTitle(normalizedFileType);
  const expiryDateTitle = getExpiryDateTitle(normalizedFileType);

  const columns: NonNullable<VxeTableGridOptions['columns']> = [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    {
      field: 'supplierName',
      title: '供应商名称',
      minWidth: 180,
      fixed: 'left',
    },
    {
      field: 'supplierCode',
      title: '供应商代码',
      width: 130,
      align: 'center',
    },
    {
      field: 'providedProduct',
      title: '供应产品',
      minWidth: 160,
    },
    {
      field: 'fileName',
      title: fileNameTitle,
      minWidth:
        normalizedFileType === FILE_TYPE_COMPLIANCE_AGREEMENT ? 260 : 160,
    },
  ];

  if (!normalizedFileType) {
    columns.splice(4, 0, {
      field: 'fileType',
      title: '档案类型',
      width: 120,
      align: 'center',
    });
  }

  const shouldShowEnvironmentColumns =
    !normalizedFileType || normalizedFileType === FILE_TYPE_ENVIRONMENT_CERT;

  if (shouldShowEnvironmentColumns) {
    columns.splice(
      4,
      0,
      {
        field: 'productModel',
        title: '产品型号',
        minWidth: 140,
      },
      {
        field: 'inspectionAgency',
        title: '检测机构',
        minWidth: 150,
      },
      {
        field: 'standardCompliant',
        title: '是否符合标准',
        width: 120,
        align: 'center',
      },
    );
  }

  if (normalizedFileType === FILE_TYPE_ENVIRONMENT_CERT) {
    columns.push({
      field: 'reportCode',
      title: '报告编码',
      minWidth: 150,
    });
  } else if (normalizedFileType === FILE_TYPE_SYSTEM_CERT) {
    columns.push({
      field: 'reportCode',
      title: '报告编码',
      minWidth: 150,
    });
  } else if (!normalizedFileType) {
    columns.push({
      field: 'reportCode',
      title: '报告编码',
      minWidth: 150,
    });
  }

  columns.push(
    {
      field: 'effectDate',
      title: effectDateTitle,
      width: 130,
      align: 'center',
    },
    {
      field: 'expiryDate',
      title: expiryDateTitle,
      width: 130,
      align: 'center',
    },
    {
      field: 'daysLeft',
      title: '剩余效期(天)',
      width: 120,
      align: 'right',
    },
    {
      field: 'remark',
      title: '备注',
      minWidth: 180,
    },
    {
      field: 'fileStatus',
      title: '当前状态',
      width: 100,
      align: 'center',
    },
    {
      title: '操作',
      width: 160,
      align: 'center',
      fixed: 'right',
      slots: { default: 'actions' },
    },
  );

  return columns;
}

function getFileNameTitle(fileType: string) {
  if (fileType === FILE_TYPE_SYSTEM_CERT) {
    return '证书类型';
  }
  if (fileType === FILE_TYPE_COMPLIANCE_AGREEMENT) {
    return '协议类型';
  }
  if (fileType === FILE_TYPE_ENVIRONMENT_CERT) {
    return '报告类型';
  }
  return '档案名称';
}

function getEffectDateTitle(fileType: string) {
  if (fileType === FILE_TYPE_COMPLIANCE_AGREEMENT) {
    return '协议生效日期';
  }
  if (
    fileType === FILE_TYPE_ENVIRONMENT_CERT ||
    fileType === FILE_TYPE_SYSTEM_CERT
  ) {
    return '报告生成日期';
  }
  return '生效/生成日期';
}

function getExpiryDateTitle(fileType: string) {
  if (fileType === FILE_TYPE_COMPLIANCE_AGREEMENT) {
    return '协议失效日期';
  }
  if (
    fileType === FILE_TYPE_ENVIRONMENT_CERT ||
    fileType === FILE_TYPE_SYSTEM_CERT
  ) {
    return '报告过期日期';
  }
  return '失效/过期日期';
}

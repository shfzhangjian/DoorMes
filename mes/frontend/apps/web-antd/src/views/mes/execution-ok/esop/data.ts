import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'docName', label: '文件名称', component: 'Input', componentProps: { placeholder: '支持模糊查询' } },
    { fieldName: 'productName', label: '适用产品', component: 'Input', componentProps: { placeholder: '支持模糊查询' } },
    { fieldName: 'processCode', label: '适用工序', component: 'Select',
      componentProps: {
        options: [
          { label: '配料 (MIXING)', value: 'MIXING' },
          { label: '涂布 (COATING)', value: 'COATING' },
          { label: '分切 (SLITTING)', value: 'SLITTING' },
          { label: '检验 (QC)', value: 'QC' },
        ]
      }
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center' },
    { field: 'docNo', title: '文件编号', width: 150 },
    { field: 'docName', title: '文件名称', minWidth: 200 },
    { field: 'version', title: '版本号', width: 100, align: 'center' },
    { field: 'productName', title: '适用产品', width: 180 },
    { field: 'processCode', title: '适用工序', width: 120, slots: { default: 'processCode' } },
    { field: 'fileUrl', title: '源文件', width: 100, align: 'center', slots: { default: 'fileUrl' } },
    { field: 'status', title: '状态', width: 90, align: 'center', slots: { default: 'status' } },
    { field: 'createTime', title: '上传时间', width: 160 },
    { title: '操作', field: 'action', fixed: 'right', width: 200, slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'docNo', label: '文件编号', component: 'Input', componentProps: { disabled: true, placeholder: '保存后系统自动生成' } },
    { fieldName: 'docName', label: '文件名称', component: 'Input', rules: 'required' },
    { fieldName: 'version', label: '版本号', component: 'Input', defaultValue: 'V1.0', rules: 'required' },
    { fieldName: 'productName', label: '适用产品', component: 'Input', rules: 'required', help: '该文件将在该产品生产时推送到看板' },
    { fieldName: 'processCode', label: '适用工序', component: 'Select', rules: 'required',
      componentProps: {
        options: [
          { label: 'MIXING - 配料', value: 'MIXING' },
          { label: 'COATING - 涂布', value: 'COATING' },
          { label: 'SLITTING - 分切', value: 'SLITTING' },
          { label: 'QC - 检验', value: 'QC' },
        ]
      }
    },
    // 注意：实际项目中这里通常使用 Upload 组件，此处为原型简化为 Input
    { fieldName: 'fileUrl', label: '附件地址', component: 'Input', rules: 'required', componentProps: { placeholder: '请填入PDF或图片链接' } },
    { fieldName: 'status', label: '状态', component: 'RadioGroup', defaultValue: 1,
      componentProps: { options: [{ label: '启用 (现行)', value: 1 }, { label: '作废 (历史)', value: 0 }] }
    },
    { fieldName: 'remark', label: '备注说明', component: 'Textarea' },
  ];
}

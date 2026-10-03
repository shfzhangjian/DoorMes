import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { baseMesMaterialApi } from '#/api/mes/base/material';
import { markRaw } from 'vue';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import SupplierComboSelect from './components/SupplierComboSelect.vue';
import UnitComboSelect from './components/UnitComboSelect.vue';
import { getRangePickerDefaultProps } from '#/utils';

// 🔥 模拟的工业级物料分类树结构
const categoryTreeData = [
  {
    label: '原材料类 (RAW)', value: 'RAW',
    children: [
      { label: '电子元器件', value: 'RAW-01' },
      { label: '塑胶结构件', value: 'RAW-02' },
      { label: '五金标准件', value: 'RAW-03' },
      { label: '化工辅料 (胶水/溶剂)', value: 'RAW-04' },
    ],
  },
  {
    label: '半成品类 (SMF)', value: 'SMF',
    children: [
      { label: 'PCBA主板', value: 'SMF-01' },
      { label: '线束组件', value: 'SMF-02' },
      { label: '光学膜材母卷', value: 'SMF-03' },
    ],
  },
  {
    label: '成品类 (FG)', value: 'FG',
    children: [
      { label: '标准设备/量产件', value: 'FG-01' },
      { label: '客制化定制产品', value: 'FG-02' },
    ],
  }
];

/** 新增/修改的表单 (已根据业务逻辑进行完美CP对组合，彻底消除留空列) */
export function useFormSchema(): VbenFormSchema[] {
  return [
    // 隐藏的冗余支撑字段
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'batchRuleName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'supplierName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },

    // --- 组1：基础标识CP ---
    { fieldName: 'code', label: '物料编码', rules: 'required', component: 'Input', formItemClass: 'col-span-1', componentProps: { placeholder: '唯一编码/ERP料号' } },
    { fieldName: 'name', label: '物料名称', rules: 'required', component: 'Input', formItemClass: 'col-span-1', componentProps: { placeholder: '标准物料名称' } },

    // --- 组2：分类属性CP ---
    { fieldName: 'categoryId', label: '所属类目', rules: 'required', component: 'TreeSelect', formItemClass: 'col-span-1', componentProps: { treeData: categoryTreeData, placeholder: '选择所属分类树层级', treeDefaultExpandAll: true } },
    { fieldName: 'category', label: '物料性质', rules: 'required', component: 'Select', formItemClass: 'col-span-1', componentProps: { options: getDictOptions(DICT_TYPE.MES_MATERIAL_CATEGORY, 'string'), placeholder: '物理形态定义' } },

    // --- 组3：工程规格CP ---
    { fieldName: 'spec', label: '规格型号', component: 'Input', formItemClass: 'col-span-1', componentProps: { placeholder: '尺寸/型号说明' } },
    { fieldName: 'materialGrade', label: '材质牌号', component: 'Input', formItemClass: 'col-span-1', componentProps: { placeholder: '如：SUS304, AL6061' } },

    // --- 组4：度量标准CP ---
    { fieldName: 'unit', label: '计量单位', rules: 'required', component: markRaw(UnitComboSelect), formItemClass: 'col-span-1' },
    { fieldName: 'unitWeight', label: '单重 (kg)', component: 'InputNumber', formItemClass: 'col-span-1', componentProps: { placeholder: '用于包装与物流核算', class: 'w-full' } },

    // --- 组5：防呆与追溯CP (核心) ---
    { fieldName: 'trackingMode', label: '车间追溯模式', rules: 'required', component: 'Select', formItemClass: 'col-span-1', componentProps: { options: [{ label: '按批次管控 (BATCH)', value: 'BATCH' }, { label: '单件序列号管控 (SN)', value: 'SN' }, { label: '不追溯 (NONE)', value: 'NONE' }], placeholder: '排产下发时的防呆依据' } },
    { fieldName: 'batchRuleId', label: '批次生成规则', component: 'Select', formItemClass: 'col-span-1', componentProps: (opt) => ({ placeholder: '选填：绑定默认发号规则', options: [ { label: '默认量产件规则 [编码-日期-流水]', value: 1 }, { label: '客户定制件规则 [客编-单号-流水]', value: 2 }, { label: '车间半成品规则 [车间-年月-流水]', value: 3 } ], onChange: (val: any, option: any) => { if (val && option) { opt.formApi?.setValues({ batchRuleName: option.label }); } else { opt.formApi?.setValues({ batchRuleName: undefined }); } } }) },

    // --- 组6：供应与生产CP ---
    { fieldName: 'materialSource', label: '主要来源', rules: 'required', component: 'Select', formItemClass: 'col-span-1', componentProps: { placeholder: '业务获取途径', options: [{ label: '采购 (BUY)', value: 'BUY' }, { label: '自制 (MAKE)', value: 'MAKE' }, { label: '委外加工 (OUTSOURCE)', value: 'OUTSOURCE' }] } },
    { fieldName: 'supplierId', label: '默认供应商', component: markRaw(SupplierComboSelect), formItemClass: 'col-span-1', componentProps: (opt) => ({ defaultLabel: opt.formModel?.supplierName, onChange: (row: any) => { if (row) { opt.formApi?.setValues({ supplierId: row.id, supplierName: row.supplierName }); } else { opt.formApi?.setValues({ supplierId: undefined, supplierName: undefined }); } } }) },

    // --- 组7：排产算力CP ---
    { fieldName: 'leadTime', label: '制程提前期(天)', component: 'InputNumber', formItemClass: 'col-span-1', componentProps: { placeholder: '采购或车间生产提前期', class: 'w-full' } },
    { fieldName: 'scrapRate', label: '理论废品率(%)', component: 'InputNumber', formItemClass: 'col-span-1', componentProps: { placeholder: '用于APS引擎逆向算料补偿', class: 'w-full' } },

    // --- 组8：跨列宏观信息 (占据整行，平铺显示，绝不留空) ---
    { fieldName: 'status', label: '可用状态', rules: 'required', component: 'RadioGroup', formItemClass: 'col-span-2', componentProps: { options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'), buttonStyle: 'solid', optionType: 'button' } },

    // 🔥 修改：正式变更为 Upload 上传控件风格！
    // 🔥 修改：使用框架封装的 FileUpload 组件，并配置严格的文件类型和大小限制
    {
      fieldName: 'drawingUrl',
      label: '图纸与SOP附件',
      component: 'FileUpload',
      formItemClass: 'col-span-2',
      componentProps: {
        maxNumber: 1,
        maxSize: 10, // 限制 10MB
        accept: ['pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png', 'zip'],
        helpText: '请点击上传工程图纸或指导书附件 (最大支持 10MB)',
      }
    },

    { fieldName: 'remark', label: '备注说明', component: 'Textarea', formItemClass: 'col-span-2', componentProps: { placeholder: '其他补充说明', rows: 2 } },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'code', label: '编码检索', component: 'Input', componentProps: { allowClear: true, placeholder: 'ERP料号/条码' } },
    { fieldName: 'name', label: '物料名称', component: 'Input', componentProps: { allowClear: true, placeholder: '支持模糊匹配' } },
    { fieldName: 'categoryId', label: '所属类目', component: 'TreeSelect', componentProps: { treeData: categoryTreeData, allowClear: true, placeholder: '选择树形类目' } },
    { fieldName: 'category', label: '物料性质', component: 'Select', componentProps: { allowClear: true, options: getDictOptions(DICT_TYPE.MES_MATERIAL_CATEGORY, 'string'), placeholder: '筛选性质' } },
    { fieldName: 'trackingMode', label: '追溯模式', component: 'Select', componentProps: { allowClear: true, options: [{ label: 'BATCH (批次)', value: 'BATCH' }, { label: 'SN (单件)', value: 'SN' }], placeholder: '筛选防呆模式' } },
    { fieldName: 'status', label: '可用状态', component: 'Select', componentProps: { allowClear: true, options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'), placeholder: '启用/停用' } },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions<baseMesMaterialApi.MesMaterial>['columns'] {
  const catMap:any = {'RAW': '原料类', 'RAW-01': '电子元器件', 'RAW-02': '塑胶结构件', 'RAW-03': '五金标准件', 'RAW-04': '化工辅料', 'SMF': '半成品类', 'SMF-01': 'PCBA主板', 'SMF-02': '线束组件', 'SMF-03': '光学膜材母卷', 'FG': '成品类', 'FG-01': '标准设备/量产件', 'FG-02': '客制化设备'};

  return [
    { type: 'checkbox', width: 40, fixed: 'left' },
    { field: 'code', title: '物料编码', minWidth: 140, fixed: 'left' },
    { field: 'name', title: '标准名称', minWidth: 180, fixed: 'left' },
    { field: 'spec', title: '规格型号', minWidth: 140 },
    { field: 'categoryId', title: '所属类目', minWidth: 140, formatter: ({ cellValue }) => catMap[cellValue] || cellValue || '-' },
    { field: 'category', title: '物料性质', minWidth: 100, cellRender: { name: 'CellDict', props: { type: DICT_TYPE.MES_MATERIAL_CATEGORY } } },
    { field: 'trackingMode', title: '追溯控制模式', minWidth: 130, align: 'center', formatter: ({ cellValue }) => cellValue === 'SN' ? '📌 SN单件管控' : (cellValue === 'BATCH' ? '📦 批次管控' : '无追溯') },
    { field: 'batchRuleName', title: '发号规则', minWidth: 160, formatter: ({ cellValue }) => cellValue ? `⚙️ ${cellValue}` : '-' },
    { field: 'unit', title: '基本单位', minWidth: 90, align: 'center' },
    { field: 'materialSource', title: '来源', minWidth: 100, formatter: ({ cellValue }) => cellValue === 'BUY' ? '采购' : (cellValue === 'MAKE' ? '自制' : (cellValue === 'OUTSOURCE' ? '委外' : cellValue)) },
    { field: 'scrapRate', title: '设定废品率(%)', minWidth: 120, align: 'right' },
    { field: 'status', title: '状态', minWidth: 90, align: 'center', cellRender: { name: 'CellDict', props: { type: DICT_TYPE.COMMON_STATUS } } },
    { title: '操作', width: 150, fixed: 'right', slots: { default: 'actions' } },
  ];
}

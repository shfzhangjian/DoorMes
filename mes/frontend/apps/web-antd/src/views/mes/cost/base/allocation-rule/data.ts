// 文件路径：src/views/mes/cost/base/allocation-rule/data.ts

import { h } from 'vue';
import { Alert, Tag } from 'ant-design-vue';
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostRuleApi } from '#/api/mes/cost/base/allocation-rule';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { handleTree } from '@vben/utils';

// 导入成本中心API以供下拉树使用
import { getCostCenterList } from '#/api/mes/cost/base/cost-center';

// 分摊方法选项常量
export const mockAllocationMethodOptions = [
  { label: '生产工时比例法', value: 1 },
  { label: '机器工时比例法', value: 2 },
  { label: '按投入套数计算', value: 3 },
  { label: '标准成本/计划分配率', value: 4 },
];

// 动态公式与说明常量
export const ALLOCATION_METHOD_DESC: Record<number, { title: string; desc: string; formula: string; scene: string }> = {
  1: {
    title: '生产工时比例法',
    scene: '适用于劳动密集型、手工操作为主的车间（如：人工组装、包装）。',
    desc: '将当期归集的总制造费用，按照工单实际消耗的【人工报工总工时】占比进行分摊。',
    formula: '工单分摊费用 = (该工单实际消耗人工工时 ÷ 中心总实际人工工时) × 中心当期总制费'
  },
  2: {
    title: '机器工时比例法',
    scene: '适用于高度自动化、设备折旧占比高的车间（如：CNC机加工、SMT贴片、注塑）。',
    desc: '将当期归集的总制造费用，按照工单实际占用的【设备运行时间】占比进行分摊。',
    formula: '工单分摊费用 = (该工单占用设备时间 ÷ 中心总设备运行时间) × 中心当期总制费'
  },
  3: {
    title: '按投入套数计算',
    scene: '适用于工序极短、在制品极少、物料高度标准化的场景（如：五金冲压）。',
    desc: '忽略加工时间的细微差异，直接按照工单当期实际投入领料的【标准套数】比例进行平摊。',
    formula: '工单分摊费用 = (该工单当期投入套数 ÷ 中心当期总投入套数) × 中心当期总制费'
  },
  4: {
    title: '标准成本/计划分配率',
    scene: '适用于生产极其稳定、有成熟标准工时和年度预算体系的企业（如：连续流制浆线）。',
    desc: '在工单报工时直接按预设的【固定费率】结转制费。实际费用与标准费用的差异记入差异科目。',
    formula: '工单分摊费用 = 该工单标准产出工时 × 预设的计划分配率 (元/小时)'
  }
};

/** 新增/修改的表单 Schema */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'ruleCode',
      label: '规则编号',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '请输入分摊规则编号 (如: R-MACH-01)' },
    },
    {
      fieldName: 'ruleName',
      label: '规则名称',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '请输入分摊规则名称' },
    },
    {
      fieldName: 'costCenterId',
      label: '成本中心',
      rules: 'required',
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        api: async () => {
          const data = await getCostCenterList({});
          return handleTree(data); // getCostCenterList 内部已扁平化，这里将其还原为树
        },
        labelField: 'name',
        valueField: 'id',
        childrenField: 'children',
        placeholder: '请选择要绑定此规则的成本中心',
        treeDefaultExpandAll: true,
      },
    },
    {
      fieldName: 'method',
      label: '分摊方法',
      rules: 'required',
      component: 'Select',
      componentProps: {
        options: mockAllocationMethodOptions,
        placeholder: '请选择制费分摊方法',
      },
    },
// ======== 动态渲染公式卡片 ========
    {
      fieldName: 'methodDesc',
      label: ' ',
      component: Alert, // 废弃 'Custom'，直接使用 Alert 组件
      formItemClass: 'col-span-2',
      dependencies: {
        triggerFields: ['method'], // 监听 method 触发重新计算
        show: (values) => !!values.method,
      },
      // 核心修复：使用函数式 componentProps，入参为表单最新 values
      componentProps: (values) => {
        const methodValue = Number(values.method);
        const config = ALLOCATION_METHOD_DESC[methodValue];

        // 容错处理
        if (!config) return {};

        return {
          type: 'info',
          showIcon: true,
          // Ant Design Vue 的 Alert 支持将 message 和 description 作为属性接收 VNode
          message: `${config.title} - 计算模型说明`,
          description: h('div', { class: 'text-sm mt-1 leading-relaxed' }, [
            h('div', [h('strong', '适用场景：'), config.scene]),
            h('div', { class: 'mt-1' }, [h('strong', '计算逻辑：'), config.desc]),
            h('div', { class: 'mt-2 p-2 bg-blue-50 text-blue-800 rounded border border-blue-200 font-mono' }, [
              h('strong', '🧮 公式：'),
              config.formula,
            ]),
          ]),
        };
      },
    },
    // ===================================
    {
      fieldName: 'isDefault',
      label: '默认规则',
      component: 'RadioGroup',
      defaultValue: true,
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      rules: 'required',
      component: 'RadioGroup',
      defaultValue: 0,
      componentProps: {
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
        buttonStyle: 'solid',
        optionType: 'button',
      },
    },
    {
      fieldName: 'remark',
      label: '备注说明',
      component: 'Textarea',
      formItemClass: 'col-span-2',
      componentProps: { rows: 3 },
    },
  ];
}

/** 列表搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'ruleCode', label: '规则编号', component: 'Input', componentProps: { allowClear: true } },
    { fieldName: 'ruleName', label: '规则名称', component: 'Input', componentProps: { allowClear: true } },
    {
      fieldName: 'costCenterId',
      label: '成本中心',
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        api: async () => handleTree(await getCostCenterList({})),
        labelField: 'name',
        valueField: 'id',
      },
    },
    {
      fieldName: 'method',
      label: '分摊方法',
      component: 'Select',
      componentProps: { allowClear: true, options: mockAllocationMethodOptions },
    },
  ];
}

/** 列表列定义 */
export function useGridColumns(): VxeTableGridOptions<MesCostRuleApi.Rule>['columns'] {
  return [
    { field: 'ruleCode', title: '规则编号', width: 150 },
    { field: 'ruleName', title: '规则名称', minWidth: 180 },
    { field: 'costCenterId', title: '关联成本中心ID', width: 150 }, // 真实开发中可通过联合查询返回中心名称
    {
      field: 'method',
      title: '分摊方法',
      width: 180,
      formatter: ({ cellValue }) => {
        return mockAllocationMethodOptions.find(opt => opt.value === cellValue)?.label || cellValue;
      }
    },
    {
      field: 'isDefault',
      title: '是否默认',
      width: 100,
      align: 'center',
      cellRender: ({ cellValue }) => {
        return h(Tag, { color: cellValue ? 'blue' : 'default' }, () => (cellValue ? '是' : '否'));
      }
    },
    {
      field: 'status',
      title: '状态',
      width: 100,
      cellRender: { name: 'CellDict', props: { type: DICT_TYPE.COMMON_STATUS } },
    },
    { field: 'createTime', title: '创建时间', width: 160 },
    {
      title: '操作',
      width: 150,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

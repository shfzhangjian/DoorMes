import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

export const columns: BasicColumn[] = [
  { title: '适用产品', dataIndex: 'productName', width: 220, align: 'left' },
  { title: '参数名称', dataIndex: 'paramName', width: 150 },
  { title: '判定范围', dataIndex: 'range', width: 180,
    customRender: ({ record }) => {
      return h('span', {}, [
        h('b', { style: { color: '#1890ff' } }, record.minValue),
        h('span', { class: 'mx-2' }, '~'),
        h('b', { style: { color: '#ff4d4f' } }, record.maxValue),
        h('small', { class: 'ml-1 text-gray-400' }, record.paramUnit)
      ]);
    }
  },
  { title: '必填', dataIndex: 'isRequired', width: 80,
    customRender: ({ text }) => h(Tag, { color: text === 1 ? 'red' : 'blue' }, () => text === 1 ? '是' : '否')
  },
  { title: '状态', dataIndex: 'status', width: 100,
    customRender: ({ text }) => h(Tag, { color: text === 0 ? 'success' : 'error' }, () => text === 0 ? '启用' : '禁用')
  },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
];

export const searchFormSchema: FormSchema[] = [
  { field: 'productName', label: '产品名称', component: 'Input', colProps: { span: 8 } },
  { field: 'status', label: '状态', component: 'Select',
    componentProps: { options: [{ label: '启用', value: 0 }, { label: '禁用', value: 1 }] },
    colProps: { span: 8 }
  },
];

export const formSchema: FormSchema[] = [
  { field: 'id', label: 'ID', component: 'Input', show: false },
  { field: 'productName', label: '适用产品', component: 'Input', required: true },
  { field: 'paramName', label: '参数名称', component: 'Input', required: true },
  { field: 'paramUnit', label: '单位', component: 'Input', required: true, colProps: { span: 12 } },
  { field: 'isRequired', label: '必填', component: 'RadioButtonGroup', defaultValue: 1,
    componentProps: { options: [{ label: '是', value: 1 }, { label: '否', value: 0 }] },
    colProps: { span: 12 }
  },
  { field: 'minValue', label: '控制下限(LSL)', component: 'InputNumber', required: true, colProps: { span: 12 } },
  { field: 'maxValue', label: '控制上限(USL)', component: 'InputNumber', required: true, colProps: { span: 12 } },
  { field: 'status', label: '状态', component: 'RadioButtonGroup', defaultValue: 0,
    componentProps: { options: [{ label: '启用', value: 0 }, { label: '禁用', value: 1 }] }
  },
];

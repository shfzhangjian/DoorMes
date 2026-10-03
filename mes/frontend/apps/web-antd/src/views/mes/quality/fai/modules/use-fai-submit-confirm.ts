import type { MesFaiApi } from '#/api/mes/quality/fai';
import type { PropType } from 'vue';

import { defineComponent, h, reactive } from 'vue';

import { DatePicker, message, Modal, Radio } from 'ant-design-vue';

type SubmitConfirmState = {
  inspectionTime: string;
  retentionStatus: MesFaiApi.RetentionStatus;
};

export type FaiSubmitConfirmResult = SubmitConfirmState;

function formatLocalDateTime(date = new Date()) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  const hours = `${date.getHours()}`.padStart(2, '0');
  const minutes = `${date.getMinutes()}`.padStart(2, '0');
  const seconds = `${date.getSeconds()}`.padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

const FaiSubmitConfirmContent = defineComponent({
  name: 'FaiSubmitConfirmContent',
  props: {
    description: {
      default: '',
      type: String,
    },
    state: {
      required: true,
      type: Object as PropType<SubmitConfirmState>,
    },
  },
  setup(props) {
    return () =>
      h('div', { class: 'space-y-3 text-sm' }, [
        props.description
          ? h('div', { class: 'text-slate-500' }, props.description)
          : null,
        h('div', { class: 'space-y-1.5' }, [
          h('div', { class: 'font-medium text-slate-700' }, '检验时间'),
          h(DatePicker, {
            allowClear: false,
            class: 'w-full',
            format: 'YYYY-MM-DD HH:mm:ss',
            placeholder: '请选择实际检验时间',
            showTime: true,
            value: props.state.inspectionTime,
            valueFormat: 'YYYY-MM-DD HH:mm:ss',
            'onUpdate:value': (value?: string) => {
              props.state.inspectionTime = value || '';
            },
          }),
        ]),
        h('div', { class: 'space-y-1.5' }, [
          h('div', { class: 'font-medium text-slate-700' }, '是否留样'),
          h(
            Radio.Group,
            {
              buttonStyle: 'solid',
              value: props.state.retentionStatus,
              'onUpdate:value': (value: MesFaiApi.RetentionStatus) => {
                props.state.retentionStatus = value;
              },
            },
            () => [
              h(Radio.Button, { value: 'RETAINED' }, () => '需要留样'),
              h(Radio.Button, { value: 'NOT_RETAINED' }, () => '不留样'),
            ],
          ),
        ]),
      ]);
  },
});

export function confirmFaiSubmitOptions(
  description = '请选择本次首件检验是否需要留样，并填写实际检验时间。',
): Promise<FaiSubmitConfirmResult | undefined> {
  const state = reactive<SubmitConfirmState>({
    inspectionTime: formatLocalDateTime(),
    retentionStatus: 'RETAINED',
  });

  return new Promise((resolve) => {
    Modal.confirm({
      cancelText: '取消',
      closable: false,
      content: h(FaiSubmitConfirmContent, { description, state }),
      keyboard: false,
      maskClosable: false,
      okText: '确认提交',
      onCancel: () => resolve(undefined),
      onOk: () => {
        if (!state.inspectionTime) {
          message.warning('请选择检验时间');
          return Promise.reject(new Error('inspectionTimeRequired'));
        }
        resolve({ ...state });
        return undefined;
      },
      title: '提交检验结果',
    });
  });
}

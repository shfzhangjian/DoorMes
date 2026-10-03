import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';
import type { PropType } from 'vue';

import { computed, defineComponent, h, reactive, ref } from 'vue';

import { Empty, message, Modal, Spin, Tag } from 'ant-design-vue';

import { getProcessFormRecordPage } from '#/api/mes/hc/processform';
import { getStationFormPage } from '#/api/mes/hc/stationform';

type SelfCheckFormGroupOptions = {
  emptyMessage: string;
  excludeFormCodes?: string[];
  formCode?: string;
  formCodes?: string[];
  processCode: string;
  selectTitle: string;
};

type SelfCheckDailyStatus = 'CONFIRMED' | 'UNCHECKED' | 'UNCONFIRMED';

export type SelfCheckDailyFilter =
  | 'ALL'
  | 'CHECKED'
  | 'CONFIRMED'
  | 'UNCHECKED'
  | 'UNCONFIRMED';

export type SelfCheckDailyRecordRow = {
  form: MesHcStationFormApi.StationForm;
  formCode: string;
  formName: string;
  key: string;
  record?: MesHcProcessFormApi.Record;
  recordDate: string;
  recordNo: string;
  status: SelfCheckDailyStatus;
  statusColor: string;
  statusText: string;
  timingName: string;
};

export type SelfCheckDailyChoice =
  | {
      form: MesHcStationFormApi.StationForm;
      mode: 'create';
    }
  | {
      form?: MesHcStationFormApi.StationForm;
      mode: 'record';
      record: MesHcProcessFormApi.Record;
    };

type SelfCheckDailySelectorOptions = SelfCheckFormGroupOptions & {
  defaultFilter?: SelfCheckDailyFilter;
  forceCreate?: boolean;
  showFilters?: boolean;
};

type SelfCheckDailySelectorState = {
  filter: SelfCheckDailyFilter;
  loading: boolean;
  rows: SelfCheckDailyRecordRow[];
};

const DAILY_FILTERS: Array<{ label: string; value: SelfCheckDailyFilter }> = [
  { label: '未检', value: 'UNCHECKED' },
  { label: '已检', value: 'CHECKED' },
  { label: '未确认', value: 'UNCONFIRMED' },
  { label: '已确认', value: 'CONFIRMED' },
];

const MAX_PAGE_SIZE = 200;
const SELECTOR_LIST_HEIGHT = '520px';

export function todayDateText() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text && text !== '-') return text;
  }
  return '';
}

function normalizeDateText(value: unknown, fallback = '') {
  const text = firstText(value);
  if (!text) return fallback;
  const match = text.match(/^(\d{4})[-/,.年](\d{1,2})[-/,.月](\d{1,2})/u);
  if (!match) return text;
  const [, year, month, day] = match;
  return `${year}-${String(Number(month)).padStart(2, '0')}-${String(Number(day)).padStart(2, '0')}`;
}

function normalizeRecordStatus(record?: MesHcProcessFormApi.Record) {
  if (!record?.id) {
    return {
      status: 'UNCHECKED' as const,
      statusColor: 'orange',
      statusText: '未检',
    };
  }
  if (record.recordStatus === 'CONFIRMED') {
    return {
      status: 'CONFIRMED' as const,
      statusColor: 'green',
      statusText: '已确认',
    };
  }
  return {
    status: 'UNCONFIRMED' as const,
    statusColor: 'blue',
    statusText: '未确认',
  };
}

function selectFormsByOptions(
  forms: MesHcStationFormApi.StationForm[],
  options: SelfCheckFormGroupOptions,
) {
  const formCodes = new Set(options.formCodes || []);
  const excludeFormCodes = new Set(options.excludeFormCodes || []);
  return forms.filter((form) => {
    const formCode = String(form.formCode || '').trim();
    if (!formCode || excludeFormCodes.has(formCode)) return false;
    if (options.formCode) return formCode === options.formCode;
    return formCodes.size === 0 || formCodes.has(formCode);
  });
}

function pickLatestRecord(
  previous: MesHcProcessFormApi.Record | undefined,
  next: MesHcProcessFormApi.Record,
) {
  if (!previous) return next;
  const previousId = Number(previous.id || 0);
  const nextId = Number(next.id || 0);
  if (nextId !== previousId) return nextId > previousId ? next : previous;
  return String(next.createTime || '') > String(previous.createTime || '')
    ? next
    : previous;
}

function buildRecordMap(records: MesHcProcessFormApi.Record[]) {
  const map = new Map<string, MesHcProcessFormApi.Record>();
  records.forEach((record) => {
    const templateCode = String(record.templateCode || '').trim();
    if (!templateCode) return;
    map.set(templateCode, pickLatestRecord(map.get(templateCode), record));
  });
  return map;
}

function filterRows(
  rows: SelfCheckDailyRecordRow[],
  filter: SelfCheckDailyFilter,
) {
  if (filter === 'ALL') return rows;
  if (filter === 'CHECKED') return rows.filter((row) => !!row.record?.id);
  return rows.filter((row) => row.status === filter);
}

function filterCount(
  rows: SelfCheckDailyRecordRow[],
  filter: SelfCheckDailyFilter,
) {
  return filterRows(rows, filter).length;
}

const SelfCheckTodaySelectorContent = defineComponent({
  name: 'SelfCheckTodaySelectorContent',
  props: {
    choose: {
      required: true,
      type: Function as PropType<(row: SelfCheckDailyRecordRow) => void>,
    },
    state: {
      required: true,
      type: Object as PropType<SelfCheckDailySelectorState>,
    },
    showFilters: {
      default: true,
      type: Boolean,
    },
    forceCreate: {
      default: false,
      type: Boolean,
    },
    today: {
      required: true,
      type: String,
    },
  },
  setup(props) {
    const visibleRows = computed(() =>
      filterRows(props.state.rows, props.state.filter),
    );

    return () =>
      h(Spin, { spinning: props.state.loading }, () =>
        h('div', { class: 'space-y-4' }, [
          h(
            'div',
            { class: 'text-sm text-slate-600' },
            `点检日期：${props.today}，请选择需要处理的自检记录。`,
          ),
          props.showFilters
            ? h(
                'div',
                { class: 'flex flex-wrap gap-2' },
                DAILY_FILTERS.map((item) =>
                  h(
                    'button',
                    {
                      class: [
                        'rounded border px-3 py-1 text-sm transition',
                        props.state.filter === item.value
                          ? 'border-blue-500 bg-blue-50 text-blue-700'
                          : 'border-slate-200 bg-white text-slate-600 hover:border-blue-300',
                      ],
                      onClick: () => {
                        props.state.filter = item.value;
                      },
                      type: 'button',
                    },
                    `${item.label} ${filterCount(props.state.rows, item.value)}`,
                  ),
                ),
              )
            : null,
          h(
            'div',
            {
              class: 'overflow-y-auto pr-1',
              style: { height: SELECTOR_LIST_HEIGHT },
            },
            visibleRows.value.length === 0
              ? [
                  h(
                    'div',
                    { class: 'flex h-full items-center justify-center' },
                    [h(Empty, { description: '暂无符合条件的自检模板' })],
                  ),
                ]
              : [
                  h(
                    'div',
                    { class: 'grid gap-3' },
                    visibleRows.value.map((row) =>
                      h(
                        'button',
                        {
                          class:
                            'flex w-full items-center gap-4 rounded border border-slate-200 bg-white p-4 text-left transition hover:border-blue-400 hover:bg-blue-50',
                          onClick: () => {
                            props.choose(row);
                          },
                          type: 'button',
                        },
                        [
                          h(
                            'div',
                            {
                              class:
                                'flex h-12 w-12 shrink-0 items-center justify-center rounded border border-blue-200 bg-blue-50 text-xl font-semibold text-blue-600',
                            },
                            row.status === 'UNCHECKED' ? '+' : '✓',
                          ),
                          h('div', { class: 'min-w-0 flex-1' }, [
                            h(
                              'div',
                              {
                                class:
                                  'flex flex-wrap items-center gap-2 text-base font-semibold text-slate-900',
                              },
                              [
                                h('span', { class: 'truncate' }, row.formName),
                                h(Tag, { color: row.statusColor }, () =>
                                  row.statusText,
                                ),
                              ],
                            ),
                            h(
                              'div',
                              {
                                class:
                                  'mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-500',
                              },
                              [
                                h('span', `时机：${row.timingName}`),
                                h('span', `记录：${row.recordNo}`),
                                h('span', `日期：${row.recordDate}`),
                              ],
                            ),
                          ]),
                          h(
                            'div',
                            {
                              class:
                                'shrink-0 text-sm font-medium text-blue-600',
                            },
                            props.forceCreate
                              ? '新建记录'
                              : row.record?.id
                                ? '查看详情'
                                : '新建填写',
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
          ),
        ]),
      );
  },
});

export function isEffectiveSelfCheckRecord(
  record: MesHcProcessFormApi.Record,
  processCode: string,
  excludeTemplateCodes: string[] = [],
) {
  const templateCode = String(record.templateCode || '').trim();
  return (
    !!templateCode &&
    templateCode !== processCode &&
    !excludeTemplateCodes.includes(templateCode)
  );
}

export async function loadTodaySelfCheckRecords(processCode: string) {
  const result = await getProcessFormRecordPage({
    pageNo: 1,
    pageSize: MAX_PAGE_SIZE,
    processCode,
    recordDate: todayDateText(),
  });
  return result.list || [];
}

export async function loadTodayConfirmedSelfCheckRecords(processCode: string) {
  const result = await getProcessFormRecordPage({
    pageNo: 1,
    pageSize: MAX_PAGE_SIZE,
    processCode,
    recordDate: todayDateText(),
    recordStatus: 'CONFIRMED',
  });
  return result.list || [];
}

export async function hasTodayConfirmedSelfCheckRecord(
  processCode: string,
  excludeTemplateCodes: string[] = [],
) {
  const records = await loadTodayConfirmedSelfCheckRecords(processCode);
  return records.some((record) =>
    isEffectiveSelfCheckRecord(record, processCode, excludeTemplateCodes),
  );
}

export async function loadEnabledSelfCheckStationForms(
  processCode: string,
  emptyMessage: string,
) {
  const page = await getStationFormPage({
    pageNo: 1,
    pageSize: MAX_PAGE_SIZE,
    processCode,
    status: 1,
  });
  const forms = (page.list || []).filter(
    (form) => !!form?.id && form.formCode !== processCode,
  );
  if (forms.length === 0) {
    message.error(emptyMessage);
  }
  return forms;
}

export async function loadTodaySelfCheckDailyRows(
  options: SelfCheckFormGroupOptions,
) {
  const forms = await loadEnabledSelfCheckStationForms(
    options.processCode,
    options.emptyMessage,
  );
  const selectableForms = selectFormsByOptions(forms, options);
  if (selectableForms.length === 0) {
    message.error(options.emptyMessage);
    return [];
  }
  const recordMap = buildRecordMap(
    (await loadTodaySelfCheckRecords(options.processCode)).filter((record) =>
      isEffectiveSelfCheckRecord(record, options.processCode),
    ),
  );
  return selectableForms.map<SelfCheckDailyRecordRow>((form) => {
    const formCode = String(form.formCode || '').trim();
    const record = recordMap.get(formCode);
    const status = normalizeRecordStatus(record);
    return {
      form,
      formCode,
      formName: firstText(form.formName, formCode, '-'),
      key: formCode,
      record,
      recordDate: normalizeDateText(record?.recordDate, todayDateText()),
      recordNo: firstText(record?.recordNo, '-'),
      timingName: firstText(form.triggerTimingName, form.formTypeName, '-'),
      ...status,
    };
  });
}

export function resolveTodaySelfCheckDefaultFilter(
  rows: SelfCheckDailyRecordRow[],
): SelfCheckDailyFilter {
  if (
    rows.length === 0 ||
    rows.some((row) => row.status === 'UNCHECKED')
  ) {
    return 'UNCHECKED';
  }
  if (rows.some((row) => row.status === 'UNCONFIRMED')) return 'UNCONFIRMED';
  return 'CONFIRMED';
}

export async function selectTodaySelfCheckRecord(
  options: SelfCheckDailySelectorOptions,
) {
  const today = todayDateText();
  const state = reactive<SelfCheckDailySelectorState>({
    filter:
      options.defaultFilter ||
      (options.showFilters === false ? 'ALL' : 'UNCHECKED'),
    loading: true,
    rows: [],
  });
  let modalRef: ReturnType<typeof Modal.confirm> | undefined;
  let settled = false;

  return new Promise<SelfCheckDailyChoice | null>((resolve) => {
    const resolveWith = (choice: SelfCheckDailyChoice | null) => {
      if (settled) return;
      settled = true;
      modalRef?.destroy();
      resolve(choice);
    };

    modalRef = Modal.confirm({
      cancelText: '关闭',
      content: h(SelfCheckTodaySelectorContent, {
        choose: (row: SelfCheckDailyRecordRow) => {
          resolveWith(
            !options.forceCreate && row.record?.id
              ? {
                  form: row.form,
                  mode: 'record',
                  record: row.record,
                }
              : { form: row.form, mode: 'create' },
          );
        },
        forceCreate: options.forceCreate === true,
        showFilters: options.showFilters !== false,
        state,
        today,
      }),
      okButtonProps: { style: { display: 'none' } },
      onCancel: () => resolveWith(null),
      title: options.selectTitle,
      width: 760,
    });

    loadTodaySelfCheckDailyRows(options)
      .then((loadedRows) => {
        state.rows = loadedRows;
      })
      .catch((error: any) => {
        if (error?.message) {
          message.error(error.message);
        }
      })
      .finally(() => {
        state.loading = false;
      });
  });
}

export async function selectSelfCheckStationForm(
  options: SelfCheckFormGroupOptions,
) {
  const forms = await loadEnabledSelfCheckStationForms(
    options.processCode,
    options.emptyMessage,
  );
  const selectableForms = selectFormsByOptions(forms, options);
  if (selectableForms.length === 0) {
    message.error(options.emptyMessage);
    return null;
  }
  if (selectableForms.length <= 1) {
    return selectableForms[0] || null;
  }
  const selectedId = ref<number | undefined>(selectableForms[0]?.id);
  return new Promise<MesHcStationFormApi.StationForm | null>((resolve) => {
    Modal.confirm({
      cancelText: '取消',
      content: () =>
        h('div', { class: 'space-y-3' }, [
          h(
            'div',
            { class: 'text-sm text-slate-600' },
            '请选择本次需要填写的自检记录表。',
          ),
          h(
            'div',
            { class: 'grid gap-3' },
            selectableForms.map((form) => {
              const selected = form.id === selectedId.value;
              return h(
                'button',
                {
                  class: [
                    'flex w-full items-center justify-between rounded border px-4 py-3 text-left transition',
                    selected
                      ? 'border-blue-500 bg-blue-50 text-blue-700'
                      : 'border-slate-200 bg-white text-slate-700 hover:border-blue-300 hover:bg-slate-50',
                  ],
                  onClick: () => {
                    selectedId.value = form.id;
                  },
                  type: 'button',
                },
                [
                  h('span', { class: 'min-w-0' }, [
                    h(
                      'strong',
                      { class: 'block truncate text-base' },
                      form.formName || form.formCode || '-',
                    ),
                    h(
                      'em',
                      { class: 'mt-1 block text-sm not-italic text-slate-500' },
                      `${form.formTypeName || '-'} / ${form.triggerTimingName || '-'}`,
                    ),
                  ]),
                  h(
                    'span',
                    {
                      class: [
                        'ml-4 shrink-0 rounded px-2 py-1 text-xs',
                        selected
                          ? 'bg-blue-100 text-blue-700'
                          : 'bg-slate-100 text-slate-500',
                      ],
                    },
                    selected ? '已选' : '选择',
                  ),
                ],
              );
            }),
          ),
        ]),
      okText: '开始填写',
      onCancel: () => resolve(null),
      onOk: () => {
        const selected = selectableForms.find(
          (form) => form.id === selectedId.value,
        );
        resolve(selected || selectableForms[0] || null);
      },
      title: options.selectTitle,
      width: 680,
    });
  });
}

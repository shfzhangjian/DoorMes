import { Comment, Fragment, defineComponent, getCurrentInstance, h, inject, provide, reactive, ref } from 'vue';

export const preferences = reactive({
  transition: { enable: true, name: 'fade' },
  tabbar: { enable: true },
});
export const layoutState = { keepAlive: true };
export const usePreferences = () => ({ keepAlive: layoutState.keepAlive });
const tabbar = {
  getCachedTabs: ref<string[]>([]),
  getExcludeCachedTabs: ref<string[]>([]),
  renderRouteView: ref(true),
};
export const useTabbarStore = () => tabbar;
export const storeToRefs = () => tabbar;
export const getTabKey = (route: { fullPath: string }) => route.fullPath;
const defaultAccessCodes = ['doormes:orders:create', 'doormes:orders:update', 'doormes:design:claim', 'doormes:design:drawing-create', 'doormes:design:drawing-save'];
export const accessState = reactive({ accessCodes: [...defaultAccessCodes] });
export const userState = reactive({ userRoles: ['factory_design'], userInfo: { id: 1003 } });
export const useAccessStore = () => accessState;
export const useUserStore = () => userState;
export function resetTestIdentity() {
  accessState.accessCodes = [...defaultAccessCodes];
  userState.userRoles = ['factory_design']; userState.userInfo.id = 1003;
}

export const modalState = { allowLeave: false, confirmations: 0 };
export const Modal = Object.assign(defineComponent({
  name: 'ControlledModal', props: ['open', 'title', 'footer', 'okText'], emits: ['cancel', 'ok'],
  setup(props, { emit, slots }) {
    return () => props.open ? h('section', { role: 'dialog', 'aria-label': props.title, 'data-mock-modal': '' }, [
      h('header', [h('h2', String(props.title || '')), h('button', { onClick: () => emit('cancel') }, '关闭弹窗')]),
      ...(slots.default?.() || []),
      slots.footer ? h('footer', slots.footer()) : props.footer===null ? null : h('footer', [
        h('button', {onClick:()=>emit('cancel')}, '取消'),
        h('button', {onClick:()=>emit('ok')}, props.okText||'确定'),
      ]),
    ]) : null;
  },
}), {
  confirm(options: { onCancel: () => void; onOk: () => void }) {
    modalState.confirmations++;
    (modalState.allowLeave ? options.onOk : options.onCancel)();
  },
});
export const message = { warning() {}, success() {}, error() {} };
function simple(tag: string) {
  return defineComponent({ inheritAttrs: false, setup(_, { attrs, slots }) {
    return () => h(tag, attrs, slots.default?.());
  } });
}
export const Button = simple('button');
export const Alert = simple('aside');
function hasCellContent(nodes: any[]): boolean {
  return nodes.some(node => node.type !== Comment && (node.type !== Fragment || hasCellContent(node.children || [])));
}
export const Table = defineComponent({
  name: 'ControlledTable', props: ['columns', 'dataSource', 'pagination', 'scroll', 'customRow'],
  setup(props, { attrs, slots }) {
    function cell(column: any, record: Record<string, unknown>) {
      const rendered = slots.bodyCell?.({ column, record });
      const keys = Array.isArray(column.dataIndex) ? column.dataIndex : [column.dataIndex];
      const value = keys.reduce((item, key) => item?.[key], record as any);
      return rendered && hasCellContent(rendered) ? rendered : String(value ?? '');
    }
    return () => h('section', { ...attrs, 'data-mock-table': '' }, [h('table', [
      h('thead', h('tr', (props.columns || []).map((column: { title: string }) => h('th', column.title)))),
      h('tbody', (props.dataSource || []).map((record: object) => h('tr', props.customRow?.(record),
        (props.columns || []).map((column: { dataIndex?: string; key?: string; fixed?: string }) => h('td', { 'data-column': column.key || column.dataIndex, 'data-fixed': column.fixed },
          cell(column, record)))))),
    ]), ...(props.dataSource?.length ? [] : slots.emptyText?.() || [])]);
  },
});
export const Tag = simple('span');
export const Select = defineComponent({
  name: 'ControlledSelect', props: ['value', 'options', 'disabled'], emits: ['update:value', 'change'],
  setup(props, { attrs, emit }) {
    return () => h('select', { ...attrs, value: props.value, disabled: props.disabled,
      onChange: (event: Event) => {
        const value = (event.target as HTMLSelectElement).value;
        const selected = props.options?.find((option: { value: string | number }) => String(option.value) === value);
        emit('update:value', selected?.value ?? value); emit('change', selected?.value ?? value);
      },
    }, (props.options || []).map((option: { value: string | number; label: string }) => h('option', { value: option.value }, option.label)));
  },
});
export const Input = Object.assign(defineComponent({
  props: ['value'], emits: ['update:value'],
  setup(props, { attrs, emit }) {
    return () => h('input', { ...attrs, value: props.value, onInput: (event: Event) => emit('update:value', (event.target as HTMLInputElement).value) });
  },
}), { TextArea: defineComponent({
  props: ['value'], emits: ['update:value'],
  setup(props, { attrs, emit }) {
    return () => h('textarea', { ...attrs, value: props.value, onInput: (event: Event) => emit('update:value', (event.target as HTMLTextAreaElement).value) });
  },
}) });
export const InputNumber = defineComponent({
  props: ['value'], emits: ['update:value'],
  setup(props, { attrs, emit }) {
    return () => h('input', { ...attrs, type: 'number', value: props.value, onInput: (event: Event) => emit('update:value', Number((event.target as HTMLInputElement).value)) });
  },
});
export const Checkbox = defineComponent({
  props: ['checked'], emits: ['update:checked', 'change'],
  setup(props, { emit, slots }) {
    return () => h('label', [h('input', { type: 'checkbox', checked: props.checked, onChange: (event: Event) => {
      emit('update:checked', (event.target as HTMLInputElement).checked); emit('change', event);
    } }), ...(slots.default?.() || [])]);
  },
});
export const Dropdown = defineComponent({
  name: 'ControlledDropdown', props: ['disabled'],
  setup(props, { slots }) {
    const opened = ref(false);
    return () => h('div', { 'data-mock-dropdown': '' }, [
      h('div', { onClick: () => { if (!props.disabled) opened.value = !opened.value; } }, slots.default?.()),
      opened.value ? h('div', { role: 'menu', onClick:()=>{opened.value=false;} }, slots.overlay?.()) : null,
    ]);
  },
});
export const Menu = Object.assign(defineComponent({
  name: 'ControlledMenu', emits: ['click'],
  setup(_, { emit, slots }) {
    provide('controlled-menu-click', (key: unknown) => emit('click', { key }));
    return () => h('div', slots.default?.());
  },
}), { Item: defineComponent({
  name: 'ControlledMenuItem',
  setup(_, { slots }) {
    const select = inject<(key: unknown) => void>('controlled-menu-click')!;
    const key = getCurrentInstance()!.vnode.key;
    return () => h('button', { role: 'menuitem', 'data-menu-key': key, onClick: () => select(key) }, slots.default?.());
  },
}) });
export const Pagination = defineComponent({
  name: 'ControlledPagination', props: ['current', 'pageSize', 'total', 'disabled'], emits: ['change'],
  setup(props, { emit }) {
    return () => h('nav', { 'data-mock-pagination': '', 'aria-label': '分页' }, [
      h('span', `${props.current} / ${props.total}`),
      h('button', { disabled: props.disabled || props.current * props.pageSize >= props.total, onClick: () => emit('change', props.current + 1, props.pageSize) }, '下一页'),
    ]);
  },
});
export const Spin = simple('span');

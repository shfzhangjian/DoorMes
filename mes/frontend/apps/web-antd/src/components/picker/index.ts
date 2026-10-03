// ─── 组件 ──────────────────────────────────────────────────────
export { default as PickerModal }    from './components/PickerModal.vue';
export { default as PickerInline }   from './components/PickerInline.vue';
export { default as PickerQueryBar } from './components/PickerQueryBar.vue';
export { default as PickerTable }    from './components/PickerTable.vue';

// ─── 类型 ──────────────────────────────────────────────────────
export type {
  PickerOption,
  PickerQueryField,
  PickerColumn,
  PickerPageParams,
  PickerPageResult,
  PickerEntityConfig,
} from './types';

// ─── 实体预设 ──────────────────────────────────────────────────
export { materialPickerConfig }   from './presets/material';
export { processPickerConfig }    from './presets/process';
export { productBomPickerConfig } from './presets/product-bom';
export { recipePickerConfig }     from './presets/recipe';
export { bomPickerConfig }        from './presets/bom';
export { routePickerConfig }      from './presets/route';
export { saleOrderPickerConfig }  from './presets/sale-order';
export { workcenterPickerConfig } from './presets/workcenter';
export { productModelPickerConfig } from './presets/product-model';

// ─── Composables（按需使用） ───────────────────────────────────
export { usePickerSearch }   from './composables/usePickerSearch';
export { usePickerPanel }    from './composables/usePickerPanel';
export { usePickerKeyboard } from './composables/usePickerKeyboard';

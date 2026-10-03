import { reactive, ref } from 'vue';

import type { PickerEntityConfig, PickerPageResult } from '../types';

/**
 * 搜索与分页逻辑
 * - 防抖（默认 300ms，可配置）
 * - 支持服务端分页
 * - 支持手动触发 / 关键字变化自动触发
 */
export function usePickerSearch<T>(
  config: PickerEntityConfig<T>,
  options?: { debounce?: number; pageSize?: number },
) {
  const loading = ref(false);
  const list = ref<T[]>([]) as { value: T[] };
  const total = ref(0);
  const pageNo = ref(1);
  const pageSize = ref(options?.pageSize ?? 10);
  const filters = reactive<Record<string, any>>({});

  let debounceTimer: ReturnType<typeof setTimeout> | null = null;

  async function doFetch(): Promise<void> {
    loading.value = true;
    try {
      const res: PickerPageResult<T> = await config.fetchPage({
        pageNo: pageNo.value,
        pageSize: pageSize.value,
        filters: { ...filters },
      });
      list.value = res.list;
      total.value = res.total;
    } finally {
      loading.value = false;
    }
  }

  /** 触发搜索，reset=true 时回到第1页 */
  function search(reset = true): void {
    if (reset) pageNo.value = 1;
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(doFetch, options?.debounce ?? 300);
  }

  /** 立即触发搜索（不防抖），用于打开弹窗时的首次加载 */
  function searchNow(reset = true): void {
    if (reset) pageNo.value = 1;
    if (debounceTimer) clearTimeout(debounceTimer);
    doFetch();
  }

  /** 重置所有条件并重新查询 */
  function reset(initialFilters?: Record<string, any>): void {
    Object.keys(filters).forEach((k) => delete filters[k]);
    if (initialFilters) Object.assign(filters, initialFilters);
    searchNow(true);
  }

  /** 切换页码 */
  function changePage(no: number): void {
    pageNo.value = no;
    doFetch();
  }

  return {
    loading,
    list,
    total,
    pageNo,
    pageSize,
    filters,
    search,
    searchNow,
    reset,
    changePage,
  };
}

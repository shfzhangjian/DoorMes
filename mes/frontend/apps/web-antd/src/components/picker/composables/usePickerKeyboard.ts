import { ref, watch } from 'vue';

import type { ComputedRef, Ref } from 'vue';

/**
 * 键盘导航：↑↓ 移动高亮 / Enter 选中 / Escape 关闭
 */
export function usePickerKeyboard<T>(
  isOpen: Ref<boolean>,
  rows: ComputedRef<T[]> | Ref<T[]>,
  onPick: (row: T) => void,
  onClose: () => void,
) {
  const activeIndex = ref(0);

  // rows 变化时修正越界
  watch(
    () => (rows as Ref<T[]>).value,
    (newRows) => {
      if (activeIndex.value >= newRows.length) {
        activeIndex.value = Math.max(0, newRows.length - 1);
      }
    },
  );

  function handleKeydown(e: KeyboardEvent): void {
    if (!isOpen.value) return;
    const list = (rows as Ref<T[]>).value;
    if (!list.length) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      activeIndex.value = Math.min(activeIndex.value + 1, list.length - 1);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      activeIndex.value = Math.max(activeIndex.value - 1, 0);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const row = list[activeIndex.value];
      if (row !== undefined) onPick(row);
    } else if (e.key === 'Escape') {
      e.preventDefault();
      onClose();
    }
  }

  function resetIndex(): void {
    activeIndex.value = 0;
  }

  return { activeIndex, handleKeydown, resetIndex };
}

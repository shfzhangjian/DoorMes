import { onBeforeUnmount, onMounted, ref } from 'vue';

/**
 * 内联面板：定位 / 显隐 / 防点透 / 点击外部关闭
 */
export function usePickerPanel(options?: { minWidth?: number }) {
  const PANEL_CLASS = 'hc-picker-inline-panel';

  const open = ref(false);
  const wrapRef = ref<HTMLElement>();
  const panelRef = ref<HTMLElement>();
  const panelStyle = ref<Record<string, string>>({});
  const panelMouseDown = ref(false);

  function updatePosition(): void {
    if (!wrapRef.value) return;
    const rect = wrapRef.value.getBoundingClientRect();
    const vh = window.innerHeight || document.documentElement.clientHeight;
    const panelH = panelRef.value?.offsetHeight ?? 320;
    const gap = 4;
    const spaceBelow = vh - rect.bottom;
    const top =
      spaceBelow >= panelH + gap
        ? rect.bottom + gap
        : Math.max(8, rect.top - panelH - gap);
    panelStyle.value = {
      position: 'fixed',
      top: `${top}px`,
      left: `${rect.left}px`,
      width: `${Math.max(rect.width, options?.minWidth ?? 520)}px`,
      maxHeight: `${Math.max(220, vh - 16)}px`,
      zIndex: '5000',
    };
  }

  function openPanel(): void {
    open.value = true;
    updatePosition();
    // 等 DOM 渲染后再算一次真实高度
    window.setTimeout(() => updatePosition(), 0);
  }

  function closePanel(): void {
    open.value = false;
  }

  function handlePanelMouseDown(): void {
    panelMouseDown.value = true;
    window.setTimeout(() => {
      panelMouseDown.value = false;
    }, 0);
  }

  function handleInputBlur(event: FocusEvent): void {
    window.setTimeout(() => {
      if (panelMouseDown.value) return;
      const nextTarget = event.relatedTarget || document.activeElement;
      const node = nextTarget as Node | null;
      if (!node) {
        closePanel();
        return;
      }
      const panel = document.querySelector(`.${PANEL_CLASS}`);
      if (wrapRef.value?.contains(node) || panel?.contains(node)) return;
      closePanel();
    }, 0);
  }

  function handleDocumentClick(e: MouseEvent): void {
    const target = e.target as Node | null;
    if (!target || !wrapRef.value) return;
    const panel = document.querySelector(`.${PANEL_CLASS}`);
    if (wrapRef.value.contains(target) || panel?.contains(target)) return;
    closePanel();
  }

  onMounted(() => {
    document.addEventListener('click', handleDocumentClick, true);
    window.addEventListener('resize', updatePosition);
    window.addEventListener('scroll', updatePosition, true);
  });

  onBeforeUnmount(() => {
    document.removeEventListener('click', handleDocumentClick, true);
    window.removeEventListener('resize', updatePosition);
    window.removeEventListener('scroll', updatePosition, true);
  });

  return {
    open,
    wrapRef,
    panelRef,
    panelStyle,
    panelMouseDown,
    PANEL_CLASS,
    openPanel,
    closePanel,
    updatePosition,
    handlePanelMouseDown,
    handleInputBlur,
  };
}

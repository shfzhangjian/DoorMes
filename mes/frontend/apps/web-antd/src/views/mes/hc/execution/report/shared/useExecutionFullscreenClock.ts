import { computed } from 'vue';

import { usePreferences } from '@vben/preferences';

import { useExecutionTokenKeepAlive } from './useExecutionTokenKeepAlive';

export function useExecutionFullscreenClock() {
  const { contentIsMaximize, isFullContent } = usePreferences();
  useExecutionTokenKeepAlive();

  const showExecutionClock = computed(
    () => contentIsMaximize.value || isFullContent.value,
  );

  return {
    showExecutionClock,
  };
}

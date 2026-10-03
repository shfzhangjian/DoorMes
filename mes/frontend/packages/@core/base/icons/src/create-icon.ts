import { defineComponent, h } from 'vue';

import { IconifyIcon } from './iconify-icon';

function createIconifyIcon(icon: string) {
  return defineComponent({
    name: `Icon-${icon}`,
    setup(props, { attrs }) {
      return () => h(IconifyIcon, { icon, ...props, ...attrs });
    },
  });
}

export { createIconifyIcon };

import type { Component, PropType } from 'vue';
import type { IconifyIcon as IconifyIconStructure } from '@iconify/vue';

import { defineComponent, h } from 'vue';

import { Icon as RemoteIcon } from '@iconify/vue';
import * as LucideIcons from 'lucide-vue-next';

type IconName = string | IconifyIconStructure;

const LUCIDE_PREFIX = 'lucide:';
const SVG_PREFIX = 'svg:';
const DEFAULT_LOCAL_ICON = 'SquareMenu';
const lucideIcons = LucideIcons as unknown as Record<string, Component>;

const exactIconAliases: Record<string, string> = {
  'ant-design:align-center-outlined': 'AlignCenter',
  'ant-design:align-left-outlined': 'AlignLeft',
  'ant-design:api-outlined': 'Unplug',
  'ant-design:appstore-outlined': 'Grid3X3',
  'ant-design:close-circle-filled': 'CircleX',
  'ant-design:delete-outlined': 'Trash2',
  'ant-design:download-outlined': 'Download',
  'ant-design:export-outlined': 'Download',
  'ant-design:eye-outlined': 'Eye',
  'ant-design:folder-open-outlined': 'FolderOpen',
  'ant-design:line-chart-outlined': 'ChartLine',
  'ant-design:login-outlined': 'LogIn',
  'ant-design:mail-outlined': 'Mail',
  'ant-design:menu-outlined': 'Menu',
  'ant-design:phone-outlined': 'Phone',
  'ant-design:plus-outlined': 'Plus',
  'ant-design:profile-outlined': 'FileText',
  'ant-design:redo-outlined': 'Redo2',
  'ant-design:reload-outlined': 'RefreshCw',
  'ant-design:search-outlined': 'Search',
  'ant-design:select-outlined': 'MousePointerClick',
  'ant-design:table-outlined': 'Table',
  'ant-design:team-outlined': 'Users',
  'ant-design:unordered-list-outlined': 'List',
  'ant-design:undo-outlined': 'Undo2',
  'ant-design:upload-outlined': 'Upload',
  'ant-design:user-outlined': 'User',
  'ant-design:warning-outlined': 'TriangleAlert',
  'ant-design:zoom-in-outlined': 'ZoomIn',
  'ant-design:zoom-out-outlined': 'ZoomOut',
  'carbon:arrow-down': 'ArrowDown',
  'carbon:arrow-up': 'ArrowUp',
  'carbon:close': 'X',
  'carbon:copy': 'Copy',
  'carbon:diamond-solid': 'Diamond',
  'carbon:edit': 'Pencil',
  'carbon:save': 'Save',
  'carbon:trash-can': 'Trash2',
  'ep:arrow-left': 'ArrowLeft',
  'ep:arrow-right': 'ArrowRight',
  'ep:avatar': 'CircleUserRound',
  'ep:calendar': 'Calendar',
  'ep:check': 'Check',
  'ep:close': 'X',
  'ep:collection-tag': 'Tags',
  'ep:cpu': 'Cpu',
  'ep:goods': 'Package',
  'ep:grid': 'Grid3X3',
  'ep:guide': 'Map',
  'ep:info-filled': 'Info',
  'ep:lightning': 'Zap',
  'ep:list': 'List',
  'ep:office-building': 'Building2',
  'ep:plus': 'Plus',
  'ep:position': 'MapPin',
  'ep:refresh': 'RefreshCw',
  'ep:search': 'Search',
  'ep:setting': 'Settings',
  'ep:sort': 'ArrowUpDown',
  'ep:warning': 'TriangleAlert',
  'fluent-mdl2:world-clock': 'Globe2',
  'ic:round-view-carousel': 'GalleryHorizontal',
  'iconoir:input-search': 'Search',
  'material-symbols:refresh-rounded': 'RefreshCw',
  'mdi:checkbox-marked-circle-outline': 'CircleCheck',
  'mdi:chip': 'Cpu',
  'mdi:github': 'Github',
  'mdi:google': 'Chrome',
  'mdi:keyboard-esc': 'Keyboard',
  'mdi:magnify': 'Search',
  'mdi:qqchat': 'MessageCircle',
  'mdi:wechat': 'MessageCircle',
  'ri:dingding-fill': 'MessageCircle',
  'system-uicons:carousel': 'GalleryHorizontal',
  'tabler:arrows-minimize': 'Minimize2',
  'tabler:input-search': 'Search',
  'tdesign:image': 'Image',
  'tdesign:qrcode': 'QrCode',
};

function toPascalCase(value: string) {
  return value
    .split('-')
    .filter(Boolean)
    .map((item) => item.charAt(0).toUpperCase() + item.slice(1))
    .join('');
}

function findLocalIcon(name: string) {
  return lucideIcons[name] || lucideIcons[`${name}Icon`];
}

function resolveLucideIcon(icon: string) {
  if (!icon.startsWith(LUCIDE_PREFIX)) {
    return undefined;
  }

  const pascalName = toPascalCase(icon.slice(LUCIDE_PREFIX.length));
  return findLocalIcon(pascalName);
}

function inferLocalIconName(icon: string) {
  const normalized = icon.toLowerCase();

  if (normalized.includes('search') || normalized.includes('magnify')) {
    return 'Search';
  }
  if (normalized.includes('refresh') || normalized.includes('reload')) {
    return 'RefreshCw';
  }
  if (normalized.includes('upload')) {
    return 'Upload';
  }
  if (normalized.includes('download') || normalized.includes('export')) {
    return 'Download';
  }
  if (normalized.includes('delete') || normalized.includes('trash')) {
    return 'Trash2';
  }
  if (normalized.includes('copy')) {
    return 'Copy';
  }
  if (normalized.includes('edit')) {
    return 'Pencil';
  }
  if (normalized.includes('plus') || normalized.includes('add')) {
    return 'Plus';
  }
  if (normalized.includes('close') || normalized.includes('cancel')) {
    return 'X';
  }
  if (normalized.includes('check')) {
    return 'Check';
  }
  if (normalized.includes('finish') || normalized.includes('success')) {
    return 'CircleCheck';
  }
  if (normalized.includes('failed') || normalized.includes('error')) {
    return 'CircleX';
  }
  if (normalized.includes('warning') || normalized.includes('alert')) {
    return 'TriangleAlert';
  }
  if (normalized.includes('info')) {
    return 'Info';
  }
  if (normalized.includes('user') || normalized.includes('avatar')) {
    return 'User';
  }
  if (normalized.includes('team') || normalized.includes('users')) {
    return 'Users';
  }
  if (normalized.includes('folder')) {
    return 'FolderOpen';
  }
  if (
    normalized.includes('document') ||
    normalized.includes('file') ||
    normalized.includes('notebook') ||
    normalized.includes('memo')
  ) {
    return 'FileText';
  }
  if (normalized.includes('ticket')) {
    return 'Ticket';
  }
  if (normalized.includes('image') || normalized.includes('picture')) {
    return 'Image';
  }
  if (
    normalized.includes('chart') ||
    normalized.includes('histogram') ||
    normalized.includes('analysis') ||
    normalized.includes('data-board') ||
    normalized.includes('data-line') ||
    normalized.includes('trend') ||
    normalized.includes('pie')
  ) {
    return 'ChartColumn';
  }
  if (normalized.includes('ai')) {
    return 'Bot';
  }
  if (normalized.includes('category') || normalized.includes('collection')) {
    return 'Tags';
  }
  if (normalized.includes('grid') || normalized.includes('appstore')) {
    return 'Grid3X3';
  }
  if (normalized.includes('list')) {
    return 'List';
  }
  if (normalized.includes('setting')) {
    return 'Settings';
  }
  if (
    normalized.includes('setting') ||
    normalized.includes('set-up') ||
    normalized.includes('management') ||
    normalized.includes('operation')
  ) {
    return 'Settings';
  }
  if (normalized.includes('tool')) {
    return 'Wrench';
  }
  if (normalized.includes('calendar') || normalized.includes('clock') || normalized.includes('timer')) {
    return 'Calendar';
  }
  if (normalized.includes('building') || normalized.includes('office')) {
    return 'Building2';
  }
  if (
    normalized.includes('goods') ||
    normalized.includes('package') ||
    normalized.includes('box') ||
    normalized.includes('bucket')
  ) {
    return 'Package';
  }
  if (normalized.includes('lock') || normalized.includes('key')) {
    return 'Lock';
  }
  if (normalized.includes('money') || normalized.includes('coin')) {
    return 'CircleDollarSign';
  }
  if (normalized.includes('ship') || normalized.includes('van') || normalized.includes('truck')) {
    return 'Truck';
  }
  if (normalized.includes('location') || normalized.includes('place')) {
    return 'MapPin';
  }
  if (normalized.includes('link') || normalized.includes('connection')) {
    return 'Link';
  }
  if (normalized.includes('monitor') || normalized.includes('cellphone')) {
    return 'Monitor';
  }
  if (normalized.includes('switch') || normalized.includes('split')) {
    return 'GitBranch';
  }
  if (normalized.includes('present')) {
    return 'Gift';
  }
  if (normalized.includes('question') || normalized.includes('help')) {
    return 'CircleHelp';
  }
  if (normalized.includes('expand')) {
    return 'Maximize2';
  }
  if (normalized.includes('fold') || normalized.includes('minimize')) {
    return 'Minimize2';
  }
  if (normalized.includes('menu')) {
    return 'Menu';
  }
  if (normalized.includes('message') || normalized.includes('chat')) {
    return 'MessageSquare';
  }

  return DEFAULT_LOCAL_ICON;
}

function resolveLocalIcon(icon: string) {
  const lucideIcon = resolveLucideIcon(icon);
  if (lucideIcon) {
    return lucideIcon;
  }

  if (icon.startsWith(SVG_PREFIX)) {
    return undefined;
  }

  const alias = exactIconAliases[icon] || inferLocalIconName(icon);
  return findLocalIcon(alias) || findLocalIcon(DEFAULT_LOCAL_ICON);
}

const IconifyIcon = defineComponent({
  name: 'IconifyIcon',
  inheritAttrs: false,
  props: {
    color: String,
    height: [Number, String],
    icon: {
      required: true,
      type: [String, Object] as PropType<IconName>,
    },
    size: [Number, String],
    strokeWidth: [Number, String],
    width: [Number, String],
  },
  setup(props, { attrs }) {
    return () => {
      const icon = props.icon;
      if (typeof icon === 'string') {
        const LocalIcon = resolveLocalIcon(icon);
        if (LocalIcon) {
          const size =
            props.size ?? props.width ?? props.height ?? attrs.size ?? '1em';

          return h(LocalIcon, {
            ...attrs,
            color: props.color ?? attrs.color ?? 'currentColor',
            size,
            strokeWidth:
              props.strokeWidth ?? attrs.strokeWidth ?? attrs['stroke-width'],
          });
        }
      }

      return h(RemoteIcon, { ...attrs, ...props });
    };
  },
});

export { IconifyIcon };

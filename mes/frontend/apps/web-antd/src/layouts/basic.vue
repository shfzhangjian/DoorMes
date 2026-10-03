<script lang="ts" setup>
import type { NotificationItem } from '@vben/layouts';

import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { AuthenticationLoginExpiredModal, useVbenModal } from '@vben/common-ui';
import { VBEN_DOC_URL, VBEN_GITHUB_URL } from '@vben/constants';
import { useWatermark } from '@vben/hooks';
import {
  AntdProfileOutlined,
  BookOpenText,
  CircleHelp,
  SvgGithubIcon,
} from '@vben/icons';
import {
  BasicLayout,
  Help,
  LockScreen,
  Notification,
  UserDropdown,
} from '@vben/layouts';
import { preferences, updatePreferences } from '@vben/preferences';
import { useAccessStore, useUserStore } from '@vben/stores';
import { formatDateTime, openWindow } from '@vben/utils';

import {
  getUnreadNotifyMessageCount,
  getUnreadNotifyMessageList,
  updateAllNotifyMessageRead,
  updateNotifyMessageRead,
} from '#/api/system/notify/message';
import { $t } from '#/locales';
import { router } from '#/router';
import { useAuthStore } from '#/store';
import LoginForm from '#/views/_core/authentication/login.vue';

updatePreferences({
  app: {
    enablePreferences: false,
  },
  theme: {
    mode: 'light',
  },
  widget: {
    themeToggle: false,
  },
});

const userStore = useUserStore();
const authStore = useAuthStore();
const accessStore = useAccessStore();
const { destroyWatermark, updateWatermark } = useWatermark();

const notifications = ref<NotificationItem[]>([]);
const unreadCount = ref(0);
const showDot = computed(() => unreadCount.value > 0);
const headerClock = ref(new Date());
const weekTextMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
let headerClockTimer: ReturnType<typeof setInterval> | undefined;

const headerDateText = computed(() => {
  const date = headerClock.value;
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
});

const headerWeekText = computed(
  () => weekTextMap[headerClock.value.getDay()] ?? '',
);

const headerTimeText = computed(() => {
  const date = headerClock.value;
  return [date.getHours(), date.getMinutes(), date.getSeconds()]
    .map((item) => `${item}`.padStart(2, '0'))
    .join(':');
});

const [HelpModal, helpModalApi] = useVbenModal({
  connectedComponent: Help,
});

const menus = computed(() => [
  {
    handler: () => {
      router.push({ name: 'Profile' });
    },
    icon: AntdProfileOutlined,
    text: $t('ui.widgets.profile'),
  },
  // {
  //   handler: () => {
  //     openWindow(VBEN_DOC_URL, {
  //       target: '_blank',
  //     });
  //   },
  //   icon: BookOpenText,
  //   text: $t('ui.widgets.document'),
  // },
  // {
  //   handler: () => {
  //     openWindow(VBEN_GITHUB_URL, {
  //       target: '_blank',
  //     });
  //   },
  //   icon: SvgGithubIcon,
  //   text: 'GitHub',
  // },
  // {
  //   handler: () => {
  //     helpModalApi.open();
  //   },
  //   icon: CircleHelp,
  //   text: $t('ui.widgets.qa'),
  // },
]);

const avatar = computed(() => {
  return userStore.userInfo?.avatar ?? preferences.app.defaultAvatar;
});

async function handleLogout() {
  await authStore.logout(false);
}

/** 获得未读消息数 */
async function handleNotificationGetUnreadCount() {
  unreadCount.value = await getUnreadNotifyMessageCount();
}

/** 获得消息列表 */
async function handleNotificationGetList() {
  const list = await getUnreadNotifyMessageList();
  notifications.value = list.map((item) => ({
    avatar: preferences.app.defaultAvatar,
    date: formatDateTime(item.createTime) as string,
    isRead: false,
    id: item.id,
    message: item.templateContent,
    title: item.templateNickname,
  }));
}

/** 跳转我的站内信 */
function handleNotificationViewAll() {
  router.push({
    name: 'MyNotifyMessage',
  });
}

/** 标记所有已读 */
async function handleNotificationMakeAll() {
  await updateAllNotifyMessageRead();
  unreadCount.value = 0;
  notifications.value = [];
}

/** 清空通知 */
async function handleNotificationClear() {
  await handleNotificationMakeAll();
}

/** 标记单个已读 */
async function handleNotificationRead(item: NotificationItem) {
  if (!item.id) {
    return;
  }
  await updateNotifyMessageRead([item.id]);
  await handleNotificationGetUnreadCount();
  notifications.value = notifications.value.filter((n) => n.id !== item.id);
}

/** 处理通知打开 */
function handleNotificationOpen(open: boolean) {
  if (!open) {
    return;
  }
  handleNotificationGetList();
  handleNotificationGetUnreadCount();
}

// ========== 初始化 ==========
onMounted(() => {
  headerClock.value = new Date();
  headerClockTimer = setInterval(() => {
    headerClock.value = new Date();
  }, 1000);

  // 首次加载未读数量
  handleNotificationGetUnreadCount();
  // 轮询刷新未读数量
  setInterval(
    () => {
      if (userStore.userInfo) {
        handleNotificationGetUnreadCount();
      }
    },
    1000 * 60 * 2,
  );
});

onBeforeUnmount(() => {
  if (headerClockTimer) {
    clearInterval(headerClockTimer);
  }
});

watch(
  () => ({
    enable: preferences.app.watermark,
    content: preferences.app.watermarkContent,
  }),
  async ({ enable, content }) => {
    if (enable) {
      await updateWatermark({
        content:
          content ||
          `${userStore.userInfo?.id} - ${userStore.userInfo?.nickname}`,
      });
    } else {
      destroyWatermark();
    }
  },
  {
    immediate: true,
  },
);
</script>

<template>
  <BasicLayout @clear-preferences-and-logout="handleLogout">
    <template #user-dropdown>
      <UserDropdown
        :avatar
        :menus
        :text="userStore.userInfo?.nickname"
        :description="userStore.userInfo?.email"
        :tag-text="userStore.userInfo?.username"
        @logout="handleLogout"
      />
    </template>
    <template #notification>
      <Notification
        :dot="showDot"
        :notifications="notifications"
        @clear="handleNotificationClear"
        @make-all="handleNotificationMakeAll"
        @view-all="handleNotificationViewAll"
        @open="handleNotificationOpen"
        @read="handleNotificationRead"
      />
    </template>
    <template #header-right-45>
      <div class="hc-header-clock">
        <span>{{ headerDateText }}</span>
        <span>{{ headerWeekText }}</span>
        <strong>{{ headerTimeText }}</strong>
      </div>
    </template>
    <template #extra>
      <AuthenticationLoginExpiredModal
        v-model:open="accessStore.loginExpired"
        :avatar
      >
        <LoginForm />
      </AuthenticationLoginExpiredModal>
    </template>
    <template #lock-screen>
      <LockScreen :avatar @to-login="handleLogout" />
    </template>
  </BasicLayout>
  <HelpModal />
</template>

<style lang="scss" scoped>
.hc-header-clock {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  height: 32px;
  padding: 0 10px;
  margin-right: 8px;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  color: hsl(var(--foreground));
  white-space: nowrap;
  background: hsl(var(--accent));
  border: 1px solid hsl(var(--border));
  border-radius: 999px;
  gap: 7px;
}

.hc-header-clock strong {
  min-width: 64px;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  color: #0875be;
  text-align: right;
}

@media (max-width: 900px) {
  .hc-header-clock {
    display: none;
  }
}
</style>

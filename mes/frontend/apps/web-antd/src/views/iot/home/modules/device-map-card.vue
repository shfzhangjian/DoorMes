<script lang="ts" setup>
import type { IotDeviceApi } from '#/api/iot/device/device';

import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Card, Empty, Spin } from 'ant-design-vue';

import { getDeviceLocationList } from '#/api/iot/device/device';
import { DeviceStateEnum } from '#/views/iot/utils/constants';

defineOptions({ name: 'DeviceMapCard' });

const router = useRouter();
const loading = ref(true);
const deviceList = ref<IotDeviceApi.Device[]>([]);

/** 是否有数据 */
const hasData = computed(() => deviceList.value.length > 0);

/** 设备状态颜色映射 */
const stateColorMap: Record<number, string> = {
  [DeviceStateEnum.INACTIVE]: '#EAB308', // 待激活 - 黄色
  [DeviceStateEnum.ONLINE]: '#22C55E', // 在线 - 绿色
  [DeviceStateEnum.OFFLINE]: '#9CA3AF', // 离线 - 灰色
};

/** 获取设备状态配置 */
function getStateConfig(state: number): { color: string; name: string } {
  const stateNames: Record<number, string> = {
    [DeviceStateEnum.INACTIVE]: '待激活',
    [DeviceStateEnum.ONLINE]: '在线',
    [DeviceStateEnum.OFFLINE]: '离线',
  };
  return {
    name: stateNames[state] || '未知',
    color: stateColorMap[state] || '#909399',
  };
}

/** 加载设备数据 */
async function loadDeviceData() {
  loading.value = true;
  try {
    deviceList.value = await getDeviceLocationList();
  } finally {
    loading.value = false;
  }
}

/** 初始化 */
async function init() {
  await loadDeviceData();
}

/** 组件挂载时初始化 */
onMounted(() => {
  init();
});
</script>

<template>
  <Card class="h-full" title="设备分布地图">
    <template #extra>
      <div class="flex items-center gap-4 text-sm">
        <span class="flex items-center gap-1">
          <span
            class="inline-block h-3 w-3 rounded-full"
            :style="{ backgroundColor: stateColorMap[DeviceStateEnum.ONLINE] }"
          ></span>
          <span class="text-gray-500">在线</span>
        </span>
        <span class="flex items-center gap-1">
          <span
            class="inline-block h-3 w-3 rounded-full"
            :style="{ backgroundColor: stateColorMap[DeviceStateEnum.OFFLINE] }"
          ></span>
          <span class="text-gray-500">离线</span>
        </span>
        <span class="flex items-center gap-1">
          <span
            class="inline-block h-3 w-3 rounded-full"
            :style="{
              backgroundColor: stateColorMap[DeviceStateEnum.INACTIVE],
            }"
          ></span>
          <span class="text-gray-500">待激活</span>
        </span>
      </div>
    </template>
    <Spin v-if="loading" class="flex h-[500px] items-center justify-center" />
    <Empty
      v-else-if="!hasData"
      class="h-[500px]"
      description="暂无设备位置数据"
    />
    <div v-show="hasData && !loading" class="grid h-[500px] gap-3 overflow-auto">
      <button
        v-for="device in deviceList"
        :key="device.id"
        class="rounded border border-border bg-card p-3 text-left transition hover:border-primary"
        type="button"
        @click="
          router.push({
            name: 'IoTDeviceDetail',
            params: { id: device.id },
          })
        "
      >
        <div class="flex items-center justify-between">
          <span class="font-medium">{{ device.nickname || device.deviceName }}</span>
          <span
            class="rounded px-2 py-0.5 text-xs"
            :style="{
              color: getStateConfig(device.state!).color,
              backgroundColor: `${getStateConfig(device.state!).color}18`,
            }"
          >
            {{ getStateConfig(device.state!).name }}
          </span>
        </div>
        <div class="mt-2 grid grid-cols-3 gap-2 text-xs text-muted-foreground">
          <span>产品：{{ device.productName || '-' }}</span>
          <span>经度：{{ device.longitude || '-' }}</span>
          <span>纬度：{{ device.latitude || '-' }}</span>
        </div>
      </button>
    </div>
  </Card>
</template>

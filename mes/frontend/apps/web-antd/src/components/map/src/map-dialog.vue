<script setup lang="ts">
import { reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, Form, Input, Space } from 'ant-design-vue';

const emit = defineEmits<{
  confirm: [
    data: {
      address: string;
      latitude: string;
      longitude: string;
    },
  ];
}>();

const state = reactive({
  address: '',
  latitude: '',
  longitude: '',
});

// 初始经纬度（打开弹窗时传入）
const initLongitude = ref<number | undefined>();
const initLatitude = ref<number | undefined>();

/** 确认选择 */
function handleConfirm() {
  if (state.longitude && state.latitude) {
    emit('confirm', {
      longitude: state.longitude,
      latitude: state.latitude,
      address: state.address,
    });
  }
  modalApi.close();
}

const [Modal, modalApi] = useVbenModal({
  onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      initLongitude.value = undefined;
      initLatitude.value = undefined;
    }
  },
});

/** 打开弹窗 */
function open(longitude?: number, latitude?: number) {
  initLongitude.value = longitude;
  initLatitude.value = latitude;
  state.longitude = longitude ? String(longitude) : '';
  state.latitude = latitude ? String(latitude) : '';
  state.address = '';
  modalApi.open();
}

defineExpose({ open });
</script>

<template>
  <Modal :footer="false" class="w-[700px]" title="位置坐标">
    <div class="w-full">
      <Form :label-col="{ span: 4 }">
        <Form.Item label="位置地址">
          <Input
            v-model:value="state.address"
            allow-clear
            placeholder="请输入位置地址"
          />
        </Form.Item>
        <Form.Item label="当前坐标">
          <Space>
            <Input
              v-model:value="state.longitude"
              addon-before="经度"
              placeholder="请输入经度"
              style="width: 180px"
            />
            <Input
              v-model:value="state.latitude"
              addon-before="纬度"
              placeholder="请输入纬度"
              style="width: 180px"
            />
          </Space>
        </Form.Item>
      </Form>
    </div>
    <div class="mt-4 flex justify-end gap-2">
      <Button type="primary" @click="handleConfirm">确 定</Button>
      <Button @click="modalApi.close()">取 消</Button>
    </div>
  </Modal>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'; // 🌟 修复：补充引入 computed
import { useVbenModal } from '@vben/common-ui';
import { Form, FormItem, Input, message } from 'ant-design-vue';

const emit = defineEmits(['success']);
const actionType = ref<'login' | 'logout'>('login');
const formState = ref({ cardNo: '', password: '' });

const [Modal, modalApi] = useVbenModal({
  title: computed(() => actionType.value === 'login' ? '👨‍🔧 操作员上线打卡' : '🚪 操作员下线打卡'),
  async onConfirm() {
    if (!formState.value.cardNo) return message.warning('请输入工号或刷卡！');
    modalApi.lock();
    setTimeout(() => {
      emit('success', { type: actionType.value, user: formState.value.cardNo });
      message.success(actionType.value === 'login' ? '上线成功！' : '下线成功！');
      modalApi.close();
      modalApi.unlock();
    }, 500);
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData<any>();
      actionType.value = data.type || 'login';
      formState.value.cardNo = '';
    }
  }
});
</script>

<template>
  <Modal class="w-[400px]">
    <div class="p-6">
      <Form layout="vertical">
        <FormItem label="工号/员工卡号">
          <Input v-model:value="formState.cardNo" size="large" placeholder="请刷卡或输入工号" auto-focus />
        </FormItem>
        <FormItem label="验证密码 (可选)">
          <Input type="password" v-model:value="formState.password" size="large" placeholder="请输入密码" />
        </FormItem>
      </Form>
    </div>
  </Modal>
</template>

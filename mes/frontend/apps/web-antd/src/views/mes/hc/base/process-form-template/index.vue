<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Result } from 'ant-design-vue';
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<template>
  <Page auto-content-height>
    <Result
      status="warning"
      title="旧各工序表单模板入口已停用"
      sub-title="工序表单配置统一在动态表单配置中维护；填写页只读取动态表单配置，不再使用旧模板表。"
    >
      <template #extra>
        <Button type="primary" @click="router.push('/mes/hc/base/station-form')">
          打开动态表单配置
        </Button>
      </template>
    </Result>
  </Page>
</template>

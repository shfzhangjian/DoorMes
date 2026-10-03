import Vue from '@vitejs/plugin-vue';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';

const local = (path: string) => fileURLToPath(new URL(path, import.meta.url));

// This focused DOM test does not change the application's Vite/Vitest settings.
export default defineConfig({
  plugins: [Vue()],
  resolve: {
    alias: {
      '#': local('../../apps/web-antd/src'),
      '@vben/stores': local('./mocks.ts'),
      '@vben/preferences': local('./mocks.ts'),
      'ant-design-vue': local('./mocks.ts'),
      'vue-router': local('../../apps/web-antd/node_modules/vue-router/dist/vue-router.mjs'),
    },
  },
  test: {
    environment: 'happy-dom',
    include: ['tests/doormes-tabs/*.test.ts'],
    fileParallelism: false,
  },
});

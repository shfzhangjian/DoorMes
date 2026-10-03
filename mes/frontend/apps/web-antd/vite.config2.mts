import { defineConfig } from '@vben/vite-config';
import { loadEnv } from 'vite';

export default defineConfig(async ({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  const baseUrl = env.VITE_BASE_URL || 'http://localhost:48081';

  return {
    application: {},
    vite: {
      server: {
        allowedHosts: true,
        proxy: {
          '/admin-api': {
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/admin-api/, ''),
            // mock代理目标地址
            target: `${baseUrl.replace(/\/$/, '')}/admin-api`,
            ws: true,
          },
        },
      },
    },
  };
});

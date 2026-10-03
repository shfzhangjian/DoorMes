import { initPreferences } from '@vben/preferences';
import { unmountGlobalLoading } from '@vben/utils';

import { overridesPreferences } from './preferences';

/**
 * 应用初始化完成之后再进行页面加载渲染
 */
async function initApplication() {
  // name用于指定项目唯一标识
  // 用于区分不同项目的偏好设置以及存储数据的key前缀以及其他一些需要隔离的数据
  const env = import.meta.env.PROD ? 'prod' : 'dev';
  const appVersion = import.meta.env.VITE_APP_VERSION;
  const namespace = `${import.meta.env.VITE_APP_NAMESPACE}-${appVersion}-${env}`;

  // app偏好设置初始化
  await initPreferences({
    namespace,
    overrides: overridesPreferences,
  });

  // 启动应用并挂载
  // vue应用主要逻辑及视图
  const { bootstrap } = await import('./bootstrap');
  await bootstrap(namespace);

  // 移除并销毁loading
  unmountGlobalLoading();
}

initApplication().catch(() => {
  unmountGlobalLoading();
  const host = document.querySelector('#app');
  if (!host) return;
  const panel = document.createElement('section');
  panel.setAttribute('role', 'alert');
  panel.style.cssText = 'min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px;padding:24px;color:#263849;background:#f3f6f9;text-align:center;font-family:Arial,sans-serif';
  const title = document.createElement('h1');
  title.textContent = '门窗工厂系统暂时无法启动';
  const description = document.createElement('p');
  description.textContent = '页面资源未能加载，请确认服务连接后重新加载。';
  const retry = document.createElement('button');
  retry.textContent = '重新加载';
  retry.style.cssText = 'padding:8px 20px;border:0;border-radius:4px;color:white;background:#1677ff;cursor:pointer';
  retry.addEventListener('click', () => window.location.reload());
  panel.append(title, description, retry);
  host.replaceChildren(panel);
});

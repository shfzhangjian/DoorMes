import type { MenuRecordRaw } from '@vben/types';

const WELCOME_HOME_PATH = '/welcome';
const LEGACY_WORKSPACE_HOME_PATH = '/workspace';
const DASHBOARD_MENU_PATH = '/dashboard';
const FACTORY_MENU_PATH = '/factory';
const LEGACY_DASHBOARD_ENTRY_PATHS = new Set([
  '/analytics',
  '/approval-workbench',
  LEGACY_WORKSPACE_HOME_PATH,
]);

function normalizePath(path?: string) {
  if (!path) {
    return '';
  }
  const basePath = path.split('?')[0]?.split('#')[0] || '';
  return basePath.length > 1 ? basePath.replace(/\/+$/, '') : basePath;
}

function isDefaultHomeRequest(path: string, configuredHomePath?: string) {
  const currentPath = normalizePath(path);
  const defaultPaths = [
    WELCOME_HOME_PATH,
    LEGACY_WORKSPACE_HOME_PATH,
    normalizePath(configuredHomePath),
  ].filter(Boolean);

  return defaultPaths.some((item) => normalizePath(item) === currentPath);
}

function isLegacyDashboardEntryPath(path?: string) {
  return LEGACY_DASHBOARD_ENTRY_PATHS.has(normalizePath(path));
}

function isWorkbenchRootMenu(menu: MenuRecordRaw) {
  const menuPath = normalizePath(menu.path);
  return (
    menuPath === DASHBOARD_MENU_PATH ||
    menu.name === '工作台' ||
    menu.name === '我的工作台'
  );
}

function findFirstAvailableMenuPath(menu?: MenuRecordRaw): string | undefined {
  if (!menu || menu.disabled || menu.show === false) {
    return undefined;
  }

  const firstChildPath = menu.children
    ?.map((child) => findFirstAvailableMenuPath(child))
    .find(Boolean);

  return firstChildPath || normalizePath(menu.path);
}

function resolveAuthorizedHomePath(menus: MenuRecordRaw[]) {
  // New factory roles must not land in the archived industry's workbench.
  const factoryRoot = menus.find((menu) => normalizePath(menu.path) === FACTORY_MENU_PATH);
  const factoryHome = factoryRoot && !factoryRoot.disabled && factoryRoot.show !== false
    ? factoryRoot.children?.map((child) => findFirstAvailableMenuPath(child)).find(Boolean)
    : undefined;
  if (factoryHome) return factoryHome;
  const workbenchRoot = menus.find(isWorkbenchRootMenu);
  const firstWorkbenchChild = workbenchRoot?.children
    ?.map((child) => findFirstAvailableMenuPath(child))
    .find(Boolean);

  return firstWorkbenchChild || WELCOME_HOME_PATH;
}

export {
  isDefaultHomeRequest,
  isLegacyDashboardEntryPath,
  resolveAuthorizedHomePath,
  WELCOME_HOME_PATH,
};

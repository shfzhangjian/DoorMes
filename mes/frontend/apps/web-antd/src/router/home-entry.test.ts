import { describe, expect, it } from 'vitest';
import { resolveAuthorizedHomePath } from './home-entry';

describe('factory home entry', () => {
  it('prefers authorized factory menus over old dashboards', () => {
    expect(resolveAuthorizedHomePath([
      { name: '工作台', path: '/dashboard', children: [{ name: '旧行业', path: '/dashboard/workspace' }] },
      { name: '门窗业务', path: '/factory', children: [{ name: '工厂工作台', path: '/factory/workbench' }] },
    ])).toBe('/factory/workbench');
  });
  it('does not choose disabled or hidden factory children', () => {
    expect(resolveAuthorizedHomePath([
      { name: '门窗业务', path: '/factory', children: [
        { name: '隐藏', path: '/factory/hidden', show: false },
        { name: '禁用', path: '/factory/disabled', disabled: true },
        { name: '设计研发', path: '/factory/design' },
      ] },
    ])).toBe('/factory/design');
  });
  it('does not treat backup business menus as a new home', () => {
    expect(resolveAuthorizedHomePath([
      { name: '备份菜单', path: '/backup', children: [{ name: '工作台', path: '/backup/dashboard' }] },
    ])).toBe('/welcome');
  });
  it('rejects disabled factory roots', () => {
    expect(resolveAuthorizedHomePath([
      { name: '门窗业务', path: '/factory', disabled: true, children: [{ name: '工作台', path: '/factory/workbench' }] },
    ])).toBe('/welcome');
  });
});

import { describe, expect, it } from 'vitest';
import { getAuthorizedFactoryModules } from './factory-navigation';

describe('factory menu access', () => {
  it('uses only server-granted factory menus', () => {
    expect(getAuthorizedFactoryModules([{ name: '门窗业务', path: '/factory', children: [
      { name: '设计研发', path: '/factory/design' },
      { name: '禁用订单', path: '/factory/orders', disabled: true },
      { name: '隐藏目录', path: '/factory/catalog', show: false },
    ] }]).map((item) => item.key)).toEqual(['design']);
  });
  it('ignores legacy business menus in backup', () => {
    expect(getAuthorizedFactoryModules([{ name: '备份菜单', path: '/backup', children: [
      { name: '设计研发', path: '/factory/design' },
    ] }])).toEqual([]);
  });
  it('does not invent privileges without a factory root', () => {
    expect(getAuthorizedFactoryModules([])).toEqual([]);
  });
});

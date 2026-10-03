import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { mount, flushPromises } from '@vue/test-utils';
import { compileTemplate, parse } from 'vue/compiler-sfc';
import { defineComponent, h, nextTick } from 'vue';
import { createMemoryHistory, createRouter, isNavigationFailure, NavigationFailureType } from 'vue-router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import LayoutContent from '../../packages/effects/layouts/src/basic/content/content.vue';
import Requirements from '../../apps/web-antd/src/views/doormes/requirements/index.vue';
import { layoutState, modalState, preferences } from './mocks';

vi.mock('../../packages/effects/layouts/src/iframe', () => ({ IFrameRouterView: defineComponent({ render: () => null }) }));
vi.mock('#/api/request', () => ({ requestClient: { get: async () => [] } }));
vi.mock('#/api/doormes/requirements', () => ({
  listRequirements: async () => ({ list: [], total: 0 }),
  getRequirement: vi.fn(), getRequirementVersions: vi.fn(),
  saveRequirement: vi.fn(), actOnRequirement: vi.fn(),
}));

const sourcePath = resolve('apps/web-antd/src/views/doormes/requirements/index.vue');
const openedTabs = ['workbench', 'orders', 'design', 'catalog', 'drawings'];
const mounted: ReturnType<typeof mount>[] = [];
afterEach(() => {
  mounted.splice(0).forEach((wrapper) => wrapper.unmount());
  document.body.innerHTML = '';
  modalState.allowLeave = false; modalState.confirmations = 0;
});

async function settled() {
  await flushPromises();
  await nextTick();
  // Real Vue CSS transitions use two animation frames. No browser is launched.
  await new Promise((resolve) => setTimeout(resolve, 80));
  await flushPromises();
}
async function setup(keepAlive: boolean) {
  layoutState.keepAlive = keepAlive;
  preferences.transition.enable = true;
  const routes = openedTabs.map((name) => ({
    path: `/factory/${name}`, name, meta: { keepAlive: false },
    component: ['orders', 'design'].includes(name) ? Requirements : defineComponent({
      name: `Factory${name}`, render: () => h('main', { 'data-page': name }, `${name} content`),
    }),
  }));
  const router = createRouter({ history: createMemoryHistory(), routes });
  await router.push('/factory/workbench');
  await router.isReady();
  const wrapper = mount(LayoutContent, {
    attachTo: document.body,
    global: { plugins: [router], stubs: { transition: false, keepAlive: false } },
  });
  mounted.push(wrapper);
  await settled();
  return { router, wrapper };
}

describe('factory opened tabs with the actual LayoutContent transition', () => {
  it('the actual requirement template has one stable root containing the page and drawing overlay', () => {
    const source = readFileSync(sourcePath, 'utf8');
    const descriptor = parse(source).descriptor;
    const result = compileTemplate({ source: descriptor.template!.content, filename: sourcePath, id: 'tabs-regression' });
    expect(result.errors).toEqual([]);
    const elements = descriptor.template!.ast!.children.filter((node) => node.type === 1);
    expect(elements).toHaveLength(1);
    expect(elements[0]!.tag).toBe('div');
    expect(elements[0]!.children.some((node) => node.type === 1 && node.tag === 'main')).toBe(true);
    expect(elements[0]!.children.some((node) => node.type === 1 && node.tag === 'DrawingWorkspace')).toBe(true);
  });

  for (const keepAlive of [true, false]) {
    it(`switches previously opened factory tabs in both directions (global KeepAlive ${keepAlive})`, async () => {
      const { router, wrapper } = await setup(keepAlive);
      // Explicitly revisit, not just first-load each page. Every route keeps its
      // existing keepAlive:false policy, while production out-in remains active.
      for (const name of [...openedTabs, ...openedTabs.toReversed(), 'design', 'catalog', 'orders', 'drawings']) {
        await router.push(`/factory/${name}`);
        await settled();
        expect(wrapper.find('main').exists(), `blank tab ${name}`).toBe(true);
        if (['orders', 'design'].includes(name)) {
          expect(wrapper.find('.requirement-page').exists()).toBe(true);
          expect(wrapper.find('h1').text()).toBe(name === 'design' ? '研发需求工作台' : '订单与设计需求');
        } else {
          expect(wrapper.find(`[data-page="${name}"]`).exists()).toBe(true);
        }
      }
    });
  }

  it('an actual unsaved requirement guard cancellation keeps the editor visible, then an accepted leave works', async () => {
    const { router, wrapper } = await setup(true);
    await router.push('/factory/orders'); await settled();
    const create = wrapper.findAll('button').find((button) => button.text() === '新建定制需求')!;
    await create.trigger('click'); await settled();
    const number = wrapper.find('input[placeholder="例如 REQ-20261002-001"]');
    await number.setValue('REQ-UNSAVED');
    expect(wrapper.text()).toContain('未保存');
    const cancelled = await router.push('/factory/catalog'); await settled();
    expect(isNavigationFailure(cancelled, NavigationFailureType.aborted)).toBe(true);
    expect(modalState.confirmations).toBe(1);
    expect(router.currentRoute.value.path).toBe('/factory/orders');
    expect(wrapper.find('.requirement-page').exists()).toBe(true);
    expect(number.element).toHaveProperty('value', 'REQ-UNSAVED');
    modalState.allowLeave = true;
    await router.push('/factory/catalog'); await settled();
    expect(wrapper.find('[data-page="catalog"]').exists()).toBe(true);
  });
});

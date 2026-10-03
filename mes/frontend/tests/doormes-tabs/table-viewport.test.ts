import { readFileSync } from 'node:fs';
import { mount, flushPromises } from '@vue/test-utils';
import { defineComponent, h, ref } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import TableViewport from '../../apps/web-antd/src/views/doormes/shared/table-viewport.vue';

afterEach(() => { vi.unstubAllGlobals(); document.body.innerHTML = ''; });

describe('allocated table viewport (row count must not move the horizontal scrollbar)', () => {
  it('keeps the same body height for one/empty/many rows and follows parent/header resizing', async () => {
    let resize = () => {};
    const observe = vi.fn(), disconnect = vi.fn();
    vi.stubGlobal('ResizeObserver', class {
      constructor(callback: () => void) { resize = callback; }
      observe = observe; unobserve = vi.fn(); disconnect = disconnect;
    });
    const rows = ref(1);
    const Host = defineComponent({ setup: () => () => h(TableViewport, {}, {
      default: ({ scrollY }: { scrollY: number }) => h('div', { class: 'ant-table-container', 'data-scroll-y': scrollY }, [
        h('div', { class: 'ant-table-header' }, 'Header'),
        h('div', { class: 'ant-table-body' }, Array.from({ length: rows.value }, (_, i) => h('div', `Row ${i}`))),
      ]),
    }) });
    const wrapper = mount(Host, { attachTo: document.body });
    await flushPromises();
    const viewport = wrapper.get('.doormes-table-viewport').element as HTMLElement;
    const header = wrapper.get('.ant-table-header').element as HTMLElement;
    let height = 480, headerHeight = 44;
    Object.defineProperty(viewport, 'clientHeight', { get: () => height });
    vi.spyOn(header, 'getBoundingClientRect').mockImplementation(() => ({ height: headerHeight }) as DOMRect);
    resize(); await flushPromises();
    const expectedHeight = () => Number(wrapper.get('[data-scroll-y]').attributes('data-scroll-y'));
    expect(expectedHeight()).toBe(436);
    expect(viewport.style.getPropertyValue('--doormes-table-body-height')).toBe('436px');
    for (const count of [0, 20, 1]) {
      rows.value = count; await flushPromises();
      expect(expectedHeight()).toBe(436);
    }
    height = 620; headerHeight = 60; resize(); await flushPromises();
    expect(expectedHeight()).toBe(560);
    height = 0; resize(); await flushPromises();
    expect(expectedHeight()).toBe(560); // A hidden tab must not overwrite the useful measurement.
    height = 25; resize(); await flushPromises();
    expect(expectedHeight()).toBe(0); // Small containers do not force content into the pagination.
    expect(observe).toHaveBeenCalledWith(header);
    wrapper.unmount(); expect(disconnect).toHaveBeenCalledOnce();
  });

  it('fixes body height without suppressing real overflow or forcing a scrollbar', () => {
    const source = readFileSync('apps/web-antd/src/views/doormes/shared/table-viewport.vue', 'utf8');
    const bodyRule = source.match(/:deep\(\.ant-table-body\)\s*\{([^}]+)\}/)![1]!;
    expect(bodyRule).toContain('height: var(--doormes-table-body-height)');
    expect(bodyRule).toContain('overflow: auto !important');
    expect(bodyRule).not.toMatch(/overflow[^;]*:\s*(hidden|scroll)/);
    for (const file of ['designs/index.vue', 'drawing-workspace/index.vue', 'requirements/index.vue', 'catalog/index.vue', 'catalog/assets-panel.vue']) {
      const view = readFileSync(`apps/web-antd/src/views/doormes/${file}`, 'utf8');
      expect(view).toContain('TableViewport');
      expect(view).toContain('y:scrollY');
      expect(view).toContain(':pagination="false"');
      expect(view).toMatch(/(?:history|list|assets)-pagination/);
      expect(view).toMatch(/fixed:\s*'right'/);
      expect(view).toContain('Dropdown');
    }
  });
});

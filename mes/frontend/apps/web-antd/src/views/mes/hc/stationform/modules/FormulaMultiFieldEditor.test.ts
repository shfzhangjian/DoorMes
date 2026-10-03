import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import Editor from './FormulaMultiFieldEditor.vue';

describe('多字段配置编辑', () => {
  it('新增、改名和排序保持已存在字段标识，删除只影响指定字段', async () => {
    const fields = Array.from({ length: 5 }, (_, i) => ({ key: `f${i}`, label: `字段${i}`, type: 'TEXT', unit: '', required: true }));
    const wrapper = mount(Editor, { props: { value: JSON.stringify(fields) } });
    expect(wrapper.findAll('.multi-editor-row')).toHaveLength(5);
    await wrapper.findAll('button').find((b) => b.text().startsWith('新增子字段'))!.trigger('click');
    const added = JSON.parse(wrapper.emitted('update:value')!.at(-1)![0] as string);
    expect(added).toHaveLength(6);
    expect(added[0].key).toBe('f0');
    expect(added[5].key).toMatch(/^f_/);
    await wrapper.setProps({ value: JSON.stringify(added) });
    await wrapper.findAll('.multi-editor-row')[1]!.findAll('button').find((b) => b.text().replace(/\s/g, '') === '上移')!.trigger('click');
    const moved = JSON.parse(wrapper.emitted('update:value')!.at(-1)![0] as string);
    expect(moved.slice(0, 2).map((f: any) => f.key)).toEqual(['f1', 'f0']);
    await wrapper.setProps({ value: JSON.stringify(moved) });
    await wrapper.findAll('.multi-editor-row')[0]!.find('input').setValue('改名');
    const renamed = JSON.parse(wrapper.emitted('update:value')!.at(-1)![0] as string);
    expect(renamed[0]).toMatchObject({ key: 'f1', label: '改名' });
    await wrapper.setProps({ value: JSON.stringify(renamed) });
    await wrapper.findAll('.multi-editor-row')[0]!.findAll('button').find((b) => b.text().replace(/\s/g, '') === '删除')!.trigger('click');
    const deleted = JSON.parse(wrapper.emitted('update:value')!.at(-1)![0] as string);
    expect(deleted).toHaveLength(5);
    expect(deleted.some((f: any) => f.key === 'f1')).toBe(false);
    wrapper.unmount();
  });
});

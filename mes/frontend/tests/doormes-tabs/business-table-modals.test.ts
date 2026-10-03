import { flushPromises, mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import Requirements from '../../apps/web-antd/src/views/doormes/requirements/index.vue';
import Catalog from '../../apps/web-antd/src/views/doormes/catalog/index.vue';
import { newCatalogInput } from '../../apps/web-antd/src/views/doormes/catalog/model';
import { newRequirementInput } from '../../apps/web-antd/src/views/doormes/requirements/model';
import { modalState, Table } from './mocks';

const api = vi.hoisted(() => ({ requirement: null as any, catalog: null as any, save: vi.fn(), getRequirement: vi.fn(), getCatalog: vi.fn() }));
vi.mock('#/api/request', () => ({ requestClient: { get: async () => [] } }));
vi.mock('#/api/doormes/requirements', () => ({
  listRequirements: async () => ({ list: [{ id:'REQ-TEST',number:'REQ-001',customer:'测试客户',status:'DRAFT',revision:2 }],total:1 }),
  getRequirement: api.getRequirement,
  getRequirementVersions: async () => [2,1].map(revision=>({revision,action:'REVISE',changeNote:`版本${revision}`,changedBy:1003,updatedAt:'2026-10-03T00:00:00Z'})),
  saveRequirement: api.save, actOnRequirement: vi.fn(),
}));
vi.mock('#/api/doormes/catalog', () => ({
  listCatalog: async () => ({ list: [api.catalog], total:1 }), getCatalog: api.getCatalog,
  catalogVersions: async () => [2,1].map(revision=>({revision,status:'DRAFT',changeNote:`版本${revision}`,changedBy:1003,updatedAt:'2026-10-03T00:00:00Z'})),
  saveCatalog: api.save,publishCatalog: vi.fn(),
}));
vi.mock('#/api/doormes/assets', () => ({ listAssets: async()=>({list:[],total:0}),uploadAsset:vi.fn(),downloadAsset:vi.fn() }));
const mounted: ReturnType<typeof mount>[] = [];
beforeEach(()=>{
  modalState.allowLeave=false;modalState.confirmations=0;api.save.mockReset();
  api.requirement={id:'REQ-TEST',revision:2,createdBy:1003,status:'DRAFT',changedBy:1003,updatedAt:'2026-10-03T00:00:00Z',demand:{...newRequirementInput(),number:'REQ-001',customer:'测试客户'}};
  api.catalog={id:'CAT-TEST',revision:2,status:'DRAFT',changedBy:1003,updatedAt:'2026-10-03T00:00:00Z',item:{...newCatalogInput(),code:'CAT-001',name:'测试型材',specification:'测试规格'}};
  api.getRequirement.mockImplementation(async(_id:string,revision?:number)=>({...structuredClone(api.requirement),revision:revision||2}));
  api.getCatalog.mockImplementation(async(_id:string,revision?:number)=>({...structuredClone(api.catalog),revision:revision||2}));
});
afterEach(()=>{mounted.splice(0).forEach(wrapper=>wrapper.unmount());document.body.innerHTML='';});

async function setup(component:typeof Requirements|typeof Catalog,path:string){
  const router=createRouter({history:createMemoryHistory(),routes:[{path,component}]});await router.push(path);await router.isReady();
  const wrapper=mount(RouterView,{attachTo:document.body,global:{plugins:[router]}});mounted.push(wrapper);await flushPromises();return wrapper;
}
describe('business lists reserve table space and open details/history only in dialogs',()=>{
  for(const [component,path,list,title] of [[Requirements,'/factory/requirements','.requirement-list','需求明细'],[Catalog,'/factory/catalog','.catalog-list','材料型号明细']] as const){
    it(`${path}: fixed-right dropdown -> modal detail -> modal old revision, without a write`,async()=>{
      const wrapper=await setup(component,path);
      expect(wrapper.find('[role="dialog"]').exists()).toBe(false);
      expect(wrapper.get(`${list} .doormes-table-viewport`).exists()).toBe(true);
      expect(wrapper.get(`${list} .list-pagination [data-mock-pagination]`).exists()).toBe(true);
      expect(wrapper.findComponent(Table).props('pagination')).toBe(false);
      const cell=wrapper.get(`${list} td[data-column="operation"]`);expect(cell.attributes('data-fixed')).toBe('right');
      await cell.get('button').trigger('click');await cell.get('[data-menu-key="detail"]').trigger('click');await flushPromises();
      const detail=wrapper.get(`[role="dialog"][aria-label="${title}"]`);expect(detail.find('fieldset').exists()).toBe(true);
      expect(wrapper.find(`${list} fieldset`).exists()).toBe(false);
      const historyButton=detail.findAll('button').find(button=>button.text().startsWith('版本历史'))!;
      await historyButton.trigger('click');await flushPromises();
      const history=wrapper.findAll('[role="dialog"]').find(dialog=>dialog.attributes('aria-label')?.includes('版本历史'))!;
      expect(history.get('.list-pagination [data-mock-pagination]').exists()).toBe(true);
      const oldRow=history.findAll('tr').find(row=>row.element.querySelector('[data-column="revision"]')?.textContent==='R1')!;
      await oldRow.get('[data-column="operation"] button').trigger('click');await oldRow.get('[data-menu-key="preview"]').trigger('click');await flushPromises();
      expect(wrapper.findAll('[role="dialog"]').some(dialog=>dialog.attributes('aria-label')?.includes('版本历史'))).toBe(false);
      expect(wrapper.get(`[role="dialog"][aria-label="${title}"] fieldset`).attributes('disabled')).toBeDefined();
      expect(api.save).not.toHaveBeenCalled();
    });
  }
});

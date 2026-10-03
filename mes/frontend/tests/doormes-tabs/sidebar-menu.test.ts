import { mount,flushPromises } from '@vue/test-utils';
import { describe,expect,it } from 'vitest';
import { defineComponent,h } from 'vue';
import { createMemoryHistory,createRouter } from 'vue-router';
import LayoutMenu from '../../packages/effects/layouts/src/basic/menu/menu.vue';
import { useNavigation } from '../../packages/effects/layouts/src/basic/menu/use-navigation';

describe('actual sidebar menu event forwarding',()=>{
  it('emits the selected leaf route for li and title clicks',async()=>{
    const wrapper=mount(LayoutMenu,{props:{mode:'vertical',defaultActive:'/factory/workbench',menus:[
      {name:'门窗工厂',path:'/factory',children:[
        {name:'门窗设计工作台',path:'/factory/design'},
        {name:'图纸中心',path:'/factory/drawings'},
      ]},
    ]}});
    await flushPromises();
    const leaves=wrapper.findAll('li[role="menuitem"]');expect(leaves).toHaveLength(2);
    await leaves[0]!.trigger('click');
    await leaves[1]!.find('span').trigger('click');
    expect(wrapper.emitted('select')).toEqual([['/factory/design','vertical'],['/factory/drawings','vertical']]);
    wrapper.unmount();
  });
  it('the actual menu navigation changes an already-opened route in both directions',async()=>{
    const paths=['/factory/workbench','/factory/design','/factory/drawings'];
    const router=createRouter({history:createMemoryHistory(),routes:paths.map(path=>({path,component:{render:()=>null}}))});
    await router.push(paths[0]!);await router.isReady();
    const Host=defineComponent({setup(){const {navigation}=useNavigation();return()=>h(LayoutMenu,{mode:'vertical',menus:paths.map(path=>({name:path,path})),onSelect:navigation});}});
    const wrapper=mount(Host,{global:{plugins:[router]}});
    const items=wrapper.findAll('li[role="menuitem"]');
    for(const index of [1,2,0,2,1]){await items[index]!.trigger('click');await flushPromises();expect(router.currentRoute.value.path).toBe(paths[index]);}
    wrapper.unmount();
  });
});

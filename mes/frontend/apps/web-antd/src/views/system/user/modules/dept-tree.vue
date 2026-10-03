<script lang="ts" setup>
import type { SystemDeptApi } from '#/api/system/dept';

import { onMounted, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { handleTree } from '@vben/utils';

import { Input, Spin, Tree } from 'ant-design-vue';

import { getSimpleDeptList } from '#/api/system/dept';

const emit = defineEmits(['select']);
const deptList = ref<SystemDeptApi.Dept[]>([]); // 部门列表
const deptTree = ref<any[]>([]); // 部门树
const expandedKeys = ref<Array<number | string>>([]); // 展开的节点
const loading = ref(false); // 加载状态
const searchValue = ref(''); // 搜索值

function collectExpandedKeys(nodes: any[]): Array<number | string> {
  return nodes.flatMap((node) => {
    const currentKey = node.id === undefined ? [] : [node.id];
    return [...currentKey, ...collectExpandedKeys(node.children ?? [])];
  });
}

/** 处理搜索逻辑 */
function handleSearch(e: any) {
  const value = e.target.value;
  searchValue.value = value;
  const filteredList = value
    ? deptList.value.filter((item) =>
        item.name.toLowerCase().includes(value.toLowerCase()),
      )
    : deptList.value;
  deptTree.value = handleTree(filteredList);
  // 展开所有节点
  expandedKeys.value = collectExpandedKeys(deptTree.value);
}

/** 选中部门 */
function handleSelect(_selectedKeys: any[], info: any) {
  emit('select', info.node.dataRef);
}

function handleExpand(keys: Array<number | string>) {
  expandedKeys.value = keys;
}

/** 初始化 */
onMounted(async () => {
  try {
    loading.value = true;
    const data = await getSimpleDeptList();
    deptList.value = data;
    deptTree.value = handleTree(data);
    expandedKeys.value = collectExpandedKeys(deptTree.value);
  } catch (error) {
    console.error('获取部门数据失败', error);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="system-user-dept-tree">
    <Input
      placeholder="搜索部门"
      allow-clear
      v-model:value="searchValue"
      @change="handleSearch"
      class="system-user-dept-tree__search"
    >
      <template #prefix>
        <IconifyIcon icon="lucide:search" class="size-4" />
      </template>
    </Input>
    <Spin :spinning="loading" wrapper-class-name="system-user-dept-tree__spin">
      <div class="system-user-dept-tree__scroll">
        <Tree
          @expand="handleExpand"
          @select="handleSelect"
          v-if="deptTree.length > 0"
          class="system-user-dept-tree__tree"
          :tree-data="deptTree"
          :expanded-keys="expandedKeys"
          :show-line="{ showLeafIcon: false }"
          :field-names="{ title: 'name', key: 'id', children: 'children' }"
        />
        <div v-else-if="!loading" class="py-4 text-center text-gray-500">
          暂无数据
        </div>
      </div>
    </Spin>
  </div>
</template>

<style scoped>
.system-user-dept-tree {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  row-gap: 8px;
  height: 100%;
  min-height: 0;
}

.system-user-dept-tree__search {
  width: 100%;
}

.system-user-dept-tree :deep(.system-user-dept-tree__spin),
.system-user-dept-tree :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.system-user-dept-tree__scroll {
  height: 100%;
  min-height: 0;
  padding-right: 4px;
  overflow: auto;
}

.system-user-dept-tree__tree {
  min-width: max-content;
}

.system-user-dept-tree__tree :deep(.ant-tree-indent-unit::before) {
  border-color: hsl(var(--border));
}
</style>

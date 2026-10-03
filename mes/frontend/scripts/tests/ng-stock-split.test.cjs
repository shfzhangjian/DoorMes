// Run from frontend: node scripts/tests/ng-stock-split.test.cjs
// Execute the actual setup script with API/grid mocks; no service or business writes.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { createRequire } = require('node:module');
const root = path.resolve(__dirname, '../..');
const appRequire = createRequire(path.join(root, 'apps/web-antd/package.json'));
const vue = appRequire('vue');
const compiler = appRequire('vue/compiler-sfc');
const ts = require('typescript');
const base = path.join(root, 'apps/web-antd/src/views/mes/hc/ng-inventory');
const board = path.join(base, 'shared/UnqualifiedStockBoard.vue');

for (const file of [
  'shared/UnqualifiedStockBoard.vue',
  ...[
    'unqualified-stock-query',
    'slitting-ng-stock',
    'press-slot-ng-stock',
    'finished-ng-stock',
  ].map((p) => `${p}/index.vue`),
]) {
  const filename = path.join(base, file);
  const { descriptor, errors } = compiler.parse(
    fs.readFileSync(filename, 'utf8'),
    { filename },
  );
  assert.deepEqual(errors, []);
  const script = compiler.compileScript(descriptor, { id: file });
  const template = compiler.compileTemplate({
    source: descriptor.template.content,
    filename,
    id: file,
    compilerOptions: { bindingMetadata: script.bindings },
  });
  assert.deepEqual(template.errors, []);
}
function setup(scope, data = {}) {
  const calls = [];
  let grid;
  const api = new Proxy(
    {},
    {
      get:
        (_, name) =>
        async (...args) => {
          calls.push([name, ...args]);
          return data[name] ? data[name](...args) : { list: [], total: 0 };
        },
    },
  );
  const adapter = {
    useVbenVxeGrid(options) {
      grid = options.gridOptions;
      return [
        {},
        {
          query: () =>
            grid.proxyConfig.ajax.query({
              page: { currentPage: 1, pageSize: 20 },
            }),
        },
      ];
    },
  };
  const source =
    compiler.parse(fs.readFileSync(board, 'utf8')).descriptor.scriptSetup
      .content +
    `
export { query, allRows, selectedRows, visibleRows, resultTotal, pageTitle, permissionPrefix, areaOptions, statusOptions, outboundMode, handleReset, toggleRowSelection, togglePageSelection, mapNgPiece };
`;
  const compiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
    },
  });
  const sandbox = {
    exports: {},
    defineOptions() {},
    defineProps: () => ({ scope }),
    withDefaults: (p) => p,
    require(name) {
      if (name === 'vue') return vue;
      if (name === '#/adapter/vxe-table') return adapter;
      if (name.startsWith('#/api/')) return api;
      if (name === 'dayjs') return appRequire('dayjs');
      return {};
    },
    console,
  };
  vm.runInNewContext(compiled.outputText, sandbox, { filename: board });
  return {
    state: sandbox.exports,
    calls,
    load: (page = 1, size = 20) =>
      grid.proxyConfig.ajax.query({
        page: { currentPage: page, pageSize: size },
      }),
  };
}
(async () => {
  for (const scope of ['SLITTING', 'PRESS_SLOT']) {
    const t = setup(scope, {
      getNgPiecePage: () => ({
        list: [{ id: 1, qualityResult: 'NG', pieceQty: 1 }],
        total: 47,
      }),
    });
    const page = await t.load(2, 20);
    assert.equal(page.total, 47);
    assert.equal(t.calls.length, 1);
    assert.equal(t.calls[0][0], 'getNgPiecePage');
    assert.equal(t.calls[0][1].processType, scope);
    assert.equal(t.calls[0][1].unqualifiedOnly, true);
    assert.equal(t.calls[0][1].pageNo, 2);
    assert.equal(t.state.resultTotal.value, 47);
    t.state.query.keyword = 'search';
    t.state.togglePageSelection(true);
    assert.equal(t.state.selectedRows.value.length, 1);
    await t.state.handleReset();
    assert.equal(t.state.query.keyword, '');
    assert.equal(t.state.selectedRows.value.length, 0);
    assert.equal(t.calls.at(-1)[1].processType, scope);
    assert.equal(
      t.state.mapNgPiece({ qualityResult: 'OK' }).qualityResult,
      'OK',
    );
    assert.equal(
      t.state.mapNgPiece({ qualityResult: 'NG' }).fqcInspectionResult,
      undefined,
    );
  }
  const finished = setup('FINISHED', {
    getFgInboundWaitPiecePage: () => ({
      list: [{ sliceBatchNo: 'WAIT', sourceType: 'MANUAL' }],
      total: 1,
    }),
    getFgInboundPackagePage: () => ({
      list: [
        {
          id: 1,
          status: 'PACKED',
          qualityStatus: 'NG',
          items: [
            { id: 11, qualityStatus: 'NG' },
            { id: 12, qualityStatus: 'NG' },
          ],
        },
        {
          id: 2,
          status: 'PACKED',
          qualityStatus: 'NG',
          items: [{ qualityStatus: 'NG' }, { qualityStatus: 'OK' }],
        },
      ],
      total: 2,
    }),
    getFgStockLedgerPage: () => ({ list: [{ id: 3, qty: 1 }], total: 1 }),
  });
  await finished.load(1, 2);
  assert.equal(
    finished.calls.some((c) => c[0] === 'getNgPiecePage'),
    false,
  );
  assert.equal(finished.state.allRows.value.length, 4);
  assert.equal(
    finished.state.areaOptions.value.some((a) => a.value === 'NG_WAREHOUSE'),
    false,
  );
  finished.state.toggleRowSelection(finished.state.allRows.value[0], true);
  assert.equal(finished.state.outboundMode.value, undefined);
  finished.state.toggleRowSelection(finished.state.allRows.value[0], false);
  finished.state.toggleRowSelection(finished.state.allRows.value[1], true);
  assert.equal(finished.state.selectedRows.value.length, 2);
  assert.equal(finished.state.outboundMode.value, 'PACKAGED_PENDING_SHELF');
  finished.state.toggleRowSelection(finished.state.allRows.value[3], true);
  assert.equal(finished.state.outboundMode.value, undefined);
  const large = setup('FINISHED', {
    getFgStockLedgerPage: ({ pageNo, pageSize }) => ({
      list: Array.from(
        { length: Math.min(pageSize, 20001 - (pageNo - 1) * pageSize) },
        (_, i) => ({ id: (pageNo - 1) * pageSize + i, qty: 1 }),
      ),
      total: 20001,
    }),
  });
  const page = await large.load();
  assert.equal(page.total, 20001);
  large.state.togglePageSelection(true);
  assert.equal(large.state.selectedRows.value.length, 20);
  console.log(
    'PASS: 5 Vue SFCs; process scope/pagination/reset; finished sources; whole-box/mixed-source selection; >20,000 rows; page selection.',
  );
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

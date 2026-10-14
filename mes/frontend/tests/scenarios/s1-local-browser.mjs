import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';

const mesRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const credentialFile = process.env.DOORMES_S1_CREDENTIAL_FILE;
const drawingNumber = process.env.DOORMES_S1_DRAWING_NUMBER;
if (!credentialFile || !drawingNumber) {
  throw new Error('Set DOORMES_S1_CREDENTIAL_FILE and DOORMES_S1_DRAWING_NUMBER.');
}

const credentials = JSON.parse(fs.readFileSync(credentialFile, 'utf8'));
const password = credentials.password || credentials.accounts?.find((account) => account.username === 'design')?.password;
if (!password) throw new Error('Credential file has no password.');
const savedRevision = Number(process.env.DOORMES_S1_SAVED_REVISION || 2);
const savedWidth = Number(process.env.DOORMES_S1_SAVED_WIDTH || 1350);
const historicalRevision = Number(process.env.DOORMES_S1_HISTORY_REVISION || 1);
const historicalWidth = Number(process.env.DOORMES_S1_HISTORY_WIDTH || 1200);
const unsavedWidth = Number(process.env.DOORMES_S1_UNSAVED_WIDTH || 1450);
if (![savedRevision, historicalRevision].every((value) => Number.isInteger(value) && value > 0)
  || ![savedWidth, historicalWidth, unsavedWidth].every((value) => Number.isFinite(value) && value > 0)
  || savedWidth === unsavedWidth) throw new Error('Invalid S1 version or width parameters.');
const origin = process.env.DOORMES_S1_ORIGIN || 'http://localhost:5180';
const runStamp = new Date().toISOString().replace(/[:.]/g, '-');
const evidenceDir = process.env.DOORMES_S1_EVIDENCE_DIR
  || path.join(mesRoot, 'runtime-local/scenarios', `S1-LOCAL-${runStamp}`);
fs.mkdirSync(evidenceDir, { recursive: true, mode: 0o700 });

const checks = [];
const issues = [];
const pauseMs = Number(process.env.DOORMES_S1_CAPTURE_PAUSE_MS || 2000);
const button = (scope, label) => scope.locator('button:visible').filter({ hasText: label }).first();
const preview = (page) => page.locator('.factory-preview');
const checkpoint = async (page, label) => {
  checks.push(label);
  await page.waitForTimeout(pauseMs);
};
const paperSvg = async (page) => {
  const source = await preview(page).locator('.paper img').getAttribute('src');
  assert(source?.startsWith('data:image/svg+xml'), 'Preview has no SVG image');
  return decodeURIComponent(source.slice(source.indexOf(',') + 1));
};
const snapshot = async (page, name) => {
  await page.waitForTimeout(350);
  await page.screenshot({ path: path.join(evidenceDir, name) });
};
const download = async (page, label, name) => {
  const [result] = await Promise.all([
    page.waitForEvent('download'),
    button(preview(page), label).click(),
  ]);
  const target = path.join(evidenceDir, name);
  await result.saveAs(target);
  assert(fs.statSync(target).size > 0, `${name} is empty`);
  return target;
};

let browser;
let authContext;
let videoContext;
let videoPath;
let outcome = 'failed';
try {
  browser = await chromium.launch({ channel: process.env.DOORMES_S1_BROWSER_CHANNEL || 'chrome', headless: true });
  authContext = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const login = await authContext.newPage();
  await login.goto(`${origin}/auth/login`, { waitUntil: 'domcontentloaded' });
  await button(login, '进入门窗工厂').waitFor();
  await login.getByPlaceholder('请输入用户名').fill('design');
  await login.getByPlaceholder('请输入密码').fill(password);
  await button(login, '进入门窗工厂').click();
  await login.waitForURL('**/factory/workbench', { timeout: 20_000 });
  checks.push('design account login');
  const storageState = await authContext.storageState();
  await authContext.close();
  authContext = undefined;

  videoContext = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    storageState,
    acceptDownloads: true,
    recordVideo: { dir: evidenceDir, size: { width: 1440, height: 900 } },
  });
  const page = await videoContext.newPage();
  videoPath = await page.video().path();
  page.on('pageerror', (error) => issues.push(`Page error: ${error.message}`));
  page.on('response', (response) => {
    if (response.status() >= 400 && response.url().includes('/admin-api/')) {
      issues.push(`HTTP ${response.status()}: ${new URL(response.url()).pathname}`);
    }
  });
  await page.goto(`${origin}/factory/workbench`, { waitUntil: 'domcontentloaded' });
  const row = page.locator('tr').filter({ hasText: drawingNumber });
  await row.waitFor();
  await page.waitForTimeout(pauseMs);
  await button(row, '操作').click();
  await page.getByRole('menuitem', { name: '打开设计' }).click();
  const workspace = page.locator('.mes-drawing-workspace');
  await workspace.locator('.drawing-engine-host svg').first().waitFor();
  assert.match(await workspace.innerText(), new RegExp(`图纸 R${savedRevision} · 已保存`));
  await checkpoint(page, `saved R${savedRevision} reopens from workbench`);

  await button(workspace, '工厂图预览').click();
  await preview(page).locator('.paper img').waitFor();
  assert.match(await preview(page).innerText(), new RegExp(`已保存版本 R${savedRevision}`));
  assert.match(await preview(page).innerText(), /2 页/);
  assert.match(await paperSvg(page), new RegExp(String(savedWidth)));
  await snapshot(page, `r${savedRevision}-drawing.png`);
  await download(page, '下载当前页 SVG', `r${savedRevision}-drawing.svg`);
  await button(preview(page), '下一页').click();
  assert.match(await preview(page).locator('.paper img').getAttribute('alt'), /组成件表/);
  await snapshot(page, `r${savedRevision}-components.png`);
  await download(page, '下载当前页 SVG', `r${savedRevision}-components.svg`);
  const archive = await download(page, '下载全部 SVG', `r${savedRevision}-all-pages.zip`);
  const zip = fs.readFileSync(archive);
  assert.equal(zip.readUInt32LE(0), 0x04034b50);
  assert.equal(zip.readUInt16LE(zip.length - 12), 2);
  await checkpoint(page, `R${savedRevision} drawing, component page, SVG and two-page ZIP`);

  await button(preview(page), '打印全部页').click();
  const printFrame = page.locator('iframe[title="工厂图打印"]');
  await printFrame.waitFor({ state: 'attached', timeout: 10_000 });
  const printHtml = await printFrame.getAttribute('srcdoc');
  assert.match(printHtml, new RegExp(`R${savedRevision} 工厂图`));
  assert.equal((printHtml.match(/class="sheet"/g) || []).length, 2);
  await checkpoint(page, `R${savedRevision} print document has both pages`);
  await button(preview(page), '返回图纸').click();

  await workspace.locator('.drawing-engine-host button:visible').filter({ hasText: `C1 · ${savedWidth}×1500` }).click();
  await workspace.locator('.drawing-engine-host input[name=widthMm]:visible').fill(String(unsavedWidth));
  await button(workspace.locator('.drawing-engine-host'), '应用窗体信息').click();
  assert.match(await workspace.innerText(), new RegExp(`图纸 R${savedRevision} · 未保存`));
  await button(workspace, '工厂图预览').click();
  await preview(page).locator('.paper img').waitFor();
  assert.match(await preview(page).innerText(), /不包含这些调整/);
  const savedSvg = await paperSvg(page);
  assert.match(savedSvg, new RegExp(String(savedWidth)));
  assert.doesNotMatch(savedSvg, new RegExp(String(unsavedWidth)));
  await snapshot(page, `r${savedRevision}-unsaved-isolation.png`);
  await checkpoint(page, `unsaved ${unsavedWidth} mm draft excluded from saved R${savedRevision} output`);
  await button(preview(page), '返回图纸').click();
  await button(workspace, '重新加载当前版').click();
  await button(page.locator('.ant-modal-confirm'), '放弃修改').click();
  await workspace.getByText(`图纸 R${savedRevision} · 已保存`).waitFor();

  await button(workspace, '版本历史').click();
  const historyRow = page.locator('.ant-modal:visible').filter({ hasText: '版本历史' })
    .locator('tr').filter({ hasText: `R${historicalRevision}` }).first();
  await button(historyRow, '操作').click();
  await page.getByRole('menuitem', { name: '预览历史' }).click();
  await workspace.getByText(`图纸 R${historicalRevision} 历史预览`).waitFor();
  await button(workspace, '工厂图预览').click();
  await preview(page).locator('.paper img').waitFor();
  assert.match(await preview(page).innerText(), new RegExp(`已保存版本 R${historicalRevision}`));
  assert.match(await paperSvg(page), new RegExp(String(historicalWidth)));
  await snapshot(page, `r${historicalRevision}-history.png`);
  await checkpoint(page, `historical R${historicalRevision} remains ${historicalWidth} mm and read-only`);
  await button(preview(page), '返回图纸').click();
  await button(workspace, '返回图纸中心').click();
  await workspace.waitFor({ state: 'detached' });
  await page.getByRole('menuitem', { name: '订单与设计需求' }).click();
  await page.getByRole('menuitem', { name: '工厂工作台' }).click();
  await page.locator('tr').filter({ hasText: drawingNumber }).waitFor();
  await snapshot(page, 'workbench-return.png');
  await checkpoint(page, 'factory tabs return to a usable workbench');
  assert.deepEqual(issues, []);
  outcome = 'passed';
} catch (error) {
  issues.push(`Acceptance failed: ${error.message}`);
  throw error;
} finally {
  await authContext?.close().catch(() => {});
  await videoContext?.close().catch(() => {});
  await browser?.close().catch(() => {});
  if (videoPath && fs.existsSync(videoPath)) {
    fs.renameSync(videoPath, path.join(evidenceDir, 's1-live-acceptance.webm'));
  }
  fs.writeFileSync(path.join(evidenceDir, 'result.json'), JSON.stringify({
    outcome, drawingNumber, origin, checks, issues, finishedAt: new Date().toISOString(),
  }, null, 2));
  fs.writeFileSync(path.join(evidenceDir, 'walkthrough.md'), [
    '# S1 本机浏览器验收',
    '',
    `状态：${outcome === 'passed' ? '通过' : '失败'}`,
    `样图：${drawingNumber}`,
    '录像从设计账号登录后开始；密码和令牌不写入证据。',
    '',
    ...checks.map((check, index) => `${index + 1}. ${check}`),
    '',
    ...(issues.length ? ['异常：', ...issues.map((issue) => `- ${issue}`), ''] : []),
    '限制：打印仅验证两页打印文档，不代表系统打印对话框或实体打印完成。',
    '',
  ].join('\n'));
  console.log(JSON.stringify({ outcome, checks, issues, evidenceDir }));
}

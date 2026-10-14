import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const mesRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const runtimeRoot = path.resolve(process.env.DOORMES_RUNTIME_ROOT || path.join(mesRoot, 'runtime-local'));
const credentials = JSON.parse(fs.readFileSync(path.join(runtimeRoot, 'db-credentials.json'), 'utf8'));
const query = `
SELECT JSON_ARRAYAGG(JSON_OBJECT('kind', kind, 'path', json_path, 'sha256', sha256))
FROM (
  SELECT 'drawings' AS kind, json_path, sha256 FROM dm_drawing_version
  UNION ALL
  SELECT 'catalog' AS kind, json_path, sha256 FROM dm_material_catalog_version
  UNION ALL
  SELECT 'requirements' AS kind, json_path, sha256 FROM dm_design_requirement_version
) AS version_files;
`;
const mysql = spawnSync(process.env.DOORMES_MYSQL_BIN || 'mysql', [
  '--protocol=TCP', `--host=${credentials.host}`, `--port=${credentials.port}`,
  `--user=${credentials.username}`, '--batch', '--skip-column-names', '--raw',
  credentials.database,
], {
  input: query,
  encoding: 'utf8',
  env: { ...process.env, MYSQL_PWD: credentials.password },
  maxBuffer: 32 * 1024 * 1024,
});
if (mysql.status !== 0) throw new Error(`Could not read version records: ${mysql.stderr.trim()}`);

const records = JSON.parse(mysql.stdout.trim() || 'null') || [];
const result = {
  checkedAt: new Date().toISOString(),
  database: `${credentials.host}:${credentials.port}/${credentials.database}`,
  versionFiles: { total: records.length, present: 0, missing: [], mismatch: [], invalid: [] },
  markers: {},
  uploadsDirectoryExists: fs.statSync(path.join(runtimeRoot, 'uploads'), { throwIfNoEntry: false })?.isDirectory() || false,
};

for (const record of records) {
  const relative = String(record.path || '').replaceAll('\\', '/');
  const folder = path.join(runtimeRoot, 'design-data', record.kind);
  const file = path.resolve(folder, relative);
  if (!['drawings', 'catalog', 'requirements'].includes(record.kind)
    || !file.startsWith(`${folder}${path.sep}`)
    || !/^[a-f0-9]{64}$/i.test(record.sha256)) {
    result.versionFiles.invalid.push({ kind: record.kind, path: record.path });
    continue;
  }
  const name = `${record.kind}/${relative}`;
  if (!fs.existsSync(file)) {
    result.versionFiles.missing.push(name);
    continue;
  }
  const actual = createHash('sha256').update(fs.readFileSync(file)).digest('hex');
  if (actual.toLowerCase() !== record.sha256.toLowerCase()) {
    result.versionFiles.mismatch.push({ path: name, expected: record.sha256, actual });
    continue;
  }
  result.versionFiles.present++;
}

for (let version = 1; version <= 6; version++) {
  const name = `schema-v${String(version).padStart(3, '0')}.json`;
  result.markers[name] = fs.existsSync(path.join(runtimeRoot, 'design-data', name));
}
result.markers['factory-bootstrap/complete.json'] = fs.existsSync(
  path.join(runtimeRoot, 'factory-bootstrap', 'complete.json'),
);

const reportDir = path.join(runtimeRoot, 'scenarios');
fs.mkdirSync(reportDir, { recursive: true, mode: 0o700 });
const stamp = result.checkedAt.replace(/[:.]/g, '-');
const reportPath = process.env.DOORMES_AUDIT_REPORT
  || path.join(reportDir, `design-data-audit-${stamp}.json`);
fs.writeFileSync(reportPath, `${JSON.stringify(result, null, 2)}\n`, { flag: 'wx', mode: 0o600 });
const markerMissing = Object.values(result.markers).filter((present) => !present).length;
console.log(JSON.stringify({
  total: result.versionFiles.total,
  present: result.versionFiles.present,
  missing: result.versionFiles.missing.length,
  mismatch: result.versionFiles.mismatch.length,
  invalid: result.versionFiles.invalid.length,
  markerMissing,
  uploadsDirectoryExists: result.uploadsDirectoryExists,
  reportPath,
}));
if (result.versionFiles.missing.length || result.versionFiles.mismatch.length
  || result.versionFiles.invalid.length || markerMissing || !result.uploadsDirectoryExists) {
  process.exitCode = 1;
}

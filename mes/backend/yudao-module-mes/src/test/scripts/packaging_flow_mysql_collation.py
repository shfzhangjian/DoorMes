"""Run against an isolated MySQL test server: --mysql PATH --socket PATH.
Creates a randomly named fixture database, then drops only that database.
Does not connect over TCP or use application credentials.
"""
import argparse
import ctypes
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import uuid

if os.name == 'nt':
    code_page = ctypes.windll.kernel32.GetConsoleOutputCP()
    sys.stdout.reconfigure(encoding=f'cp{code_page}' if code_page else 'utf-8', errors='replace')

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--mysql', default='mysql')
parser.add_argument('--socket', required=True, help='Socket of an isolated test server')
args = parser.parse_args()
module = Path(__file__).resolve().parents[3]
mapper = module / 'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/qms/QmsNcPackagingFlowMapper.java'
fixture = module / 'src/test/java/cn/iocoder/yudao/module/mes/dal/mysql/qms/QmsNcPackagingFlowMapperTest.java'
source = mapper.read_text(encoding='utf-8')
relations = source.split('String RELATIONS = """', 1)[1].split('""";', 1)[0]
annotation = source.split('@Select(', 1)[1].split('\n    List<', 1)[0]
# Decode Java string literals; RELATIONS is the only interpolated constant.
tokens = re.findall(r'"(?:[^"\\]|\\.)*"|\bRELATIONS\b', annotation)
query = ''.join(relations if token == 'RELATIONS' else json.loads(token) for token in tokens)
query = query.replace('#{id}', '1').replace('#{tenantId}', '1')
portable_pattern = r'CONVERT\(([^()]*) USING utf8mb4\) COLLATE utf8mb4_unicode_ci'
old_query = re.sub(portable_pattern, r'\1', query)
name = 'codex_packaging_' + uuid.uuid4().hex
cmd = [args.mysql, '--no-defaults', '--protocol=SOCKET', '--socket=' + args.socket,
       '--user=root', '--default-character-set=utf8mb4', '--batch', '--skip-column-names']

def run(sql, database=True, check=True):
    result = subprocess.run(cmd + ([name] if database else []), input=sql, text=True,
                            encoding='utf-8', capture_output=True, timeout=30)
    if check and result.returncode:
        raise AssertionError(result.stderr)
    return result

try:
    run(f'CREATE DATABASE `{name}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;', False)
    definitions = re.findall(r'sql\("(CREATE TABLE .*?)"\);', fixture.read_text(encoding='utf-8'))
    for index, ddl in enumerate(definitions):
        ddl = re.sub(r'\bVARCHAR\b', 'VARCHAR(255)', ddl)
        collation = ['utf8mb4_general_ci', 'utf8mb4_0900_ai_ci', 'utf8mb4_unicode_ci'][index % 3]
        run(ddl + ' CHARACTER SET utf8mb4 COLLATE ' + collation + ';')
    run("""
        INSERT INTO mes_qms_nc_record VALUES(1,1,100,'CUT_ROUND_FQC',0),(2,1,200,'CUT_ROUND_FQC',0),(3,1,300,'FAI',0);
        INSERT INTO mes_qms_nc_disposition_execution VALUES(1,1,0);
        INSERT INTO mes_qms_nc_disposition_scope(nc_record_id,tenant_id,scope_level,piece_no,remark,deleted)
          VALUES(1,1,'PIECE','片号A001','已确认',0);
        INSERT INTO mes_qms_fqc_submission_detail VALUES(20,200,1,'片号B001','NG',0);
        INSERT INTO mes_qms_fqc_item VALUES(200,1,20,NULL,'NG',NULL,NULL,NULL,0,0);
        INSERT INTO mes_sfc_press_slot_abnormal_lock_item VALUES(300,1,'片号C001','母批C',0);
        INSERT INTO mes_sfc_inner_pack_unit_item VALUES(10,1,'片号A001',0);
        INSERT INTO mes_sfc_inner_pack_unit VALUES(10,1,'PACKED',0);
    """)
    before = run(old_query, check=False)
    assert before.returncode and 'Illegal mix of collations' in before.stderr and 'UNION' in before.stderr, before.stderr
    print('PASS: reproduced original MySQL UNION collation error')
    after = run(query).stdout.strip()
    assert '片号A001' in after and after.endswith('\t1\t1'), after
    print('PASS: normalized UNION and cross-table packaging comparison')
    for nc_id, piece in [(2, '片号B001'), (3, '片号C001')]:
        result = run(query.replace('r.nc_record_id = 1 ', f'r.nc_record_id = {nc_id} ')).stdout.strip()
        assert piece in result and len(result.splitlines()) == 1, result
    print('PASS: unconfirmed FQC/FAI branches, COALESCE and deduplication')
    result = run('SELECT COUNT(*) FROM mes_qms_nc_record WHERE EXISTS (SELECT 1 FROM (' + relations
                 + ") packaging_scope WHERE packaging_scope.nc_record_id = mes_qms_nc_record.id"
                 + " AND packaging_scope.tenant_id = mes_qms_nc_record.tenant_id AND packaging_scope.piece_no = '片号B001');").stdout.strip()
    assert result == '1', result
    print('PASS: exact piece page predicate uses the same normalized relation')
finally:
    run(f'DROP DATABASE IF EXISTS `{name}`;', False)

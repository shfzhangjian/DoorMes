-- Only the isolated DoorMes database; never import this migration into the source MES.
USE doormes_local;
CREATE TABLE IF NOT EXISTS dm_design_requirement (
  id CHAR(36) NOT NULL,
  tenant_id BIGINT NOT NULL,
  number VARCHAR(40) NOT NULL,
  customer VARCHAR(200) NOT NULL,
  project VARCHAR(200) NOT NULL,
  revision INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL,
  assigned_to BIGINT NULL,
  line_count INT NOT NULL,
  quantity INT NOT NULL,
  created_at VARCHAR(40) NOT NULL,
  updated_at VARCHAR(40) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dm_requirement_number (tenant_id,number),
  UNIQUE KEY uq_dm_requirement_tenant_id (tenant_id,id),
  KEY ix_dm_requirement_queue (tenant_id,status,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS dm_design_requirement_version (
  tenant_id BIGINT NOT NULL,
  requirement_id CHAR(36) NOT NULL,
  revision INT NOT NULL,
  json_path VARCHAR(160) NOT NULL,
  sha256 CHAR(64) NOT NULL,
  action VARCHAR(20) NOT NULL,
  change_note VARCHAR(1000) NOT NULL,
  changed_by BIGINT NOT NULL,
  updated_at VARCHAR(40) NOT NULL,
  PRIMARY KEY (tenant_id,requirement_id,revision),
  CONSTRAINT fk_dm_requirement_version FOREIGN KEY (tenant_id,requirement_id)
    REFERENCES dm_design_requirement (tenant_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
START TRANSACTION;
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,status,visible,keep_alive,always_show,creator,updater)
VALUES (96000101,'新建门窗需求','doormes:orders:create',3,1,96000002,'','',0,0,0,0,'doormes-v001','doormes-v001'),
       (96000102,'修订门窗需求','doormes:orders:update',3,2,96000002,'','',0,0,0,0,'doormes-v001','doormes-v001'),
       (96000103,'提交研发需求','doormes:orders:submit',3,3,96000002,'','',0,0,0,0,'doormes-v001','doormes-v001'),
       (96000104,'研发领用需求','doormes:design:claim',3,1,96000003,'','',0,0,0,0,'doormes-v001','doormes-v001');
INSERT INTO system_role_menu (role_id,menu_id,creator,updater,tenant_id)
VALUES (1002,96000101,'doormes-v001','doormes-v001',1),
       (1002,96000102,'doormes-v001','doormes-v001',1),
       (1002,96000103,'doormes-v001','doormes-v001',1),
       (1003,96000104,'doormes-v001','doormes-v001',1);
UPDATE system_menu SET component='doormes/requirements/index',updater='doormes-v001'
WHERE id IN (96000002,96000003);
SET SESSION group_concat_max_len=1000000;
UPDATE system_tenant_package SET menu_ids=(SELECT CONCAT('[',GROUP_CONCAT(id ORDER BY id),']') FROM system_menu WHERE deleted=0),updater='doormes-v001' WHERE id=2000;
COMMIT;

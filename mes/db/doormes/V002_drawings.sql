-- Apply ONLY in the isolated doormes_local database.
USE doormes_local;
CREATE TABLE dm_drawing (
 id CHAR(36) NOT NULL, tenant_id BIGINT NOT NULL, requirement_id CHAR(36) NOT NULL,
 requirement_revision INT NOT NULL, line_id CHAR(36) NOT NULL, revision INT NOT NULL,
 status VARCHAR(20) NOT NULL, created_by BIGINT NOT NULL, updated_at VARCHAR(40) NOT NULL,
 PRIMARY KEY(id), UNIQUE KEY uq_dm_drawing_tenant(tenant_id,id),
 UNIQUE KEY uq_dm_drawing_line(tenant_id,requirement_id,line_id),
 CONSTRAINT fk_dm_drawing_requirement FOREIGN KEY(tenant_id,requirement_id) REFERENCES dm_design_requirement(tenant_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE dm_drawing_version (
 tenant_id BIGINT NOT NULL,drawing_id CHAR(36) NOT NULL,revision INT NOT NULL,
 json_path VARCHAR(160) NOT NULL,sha256 CHAR(64) NOT NULL,change_note VARCHAR(1000) NOT NULL,
 changed_by BIGINT NOT NULL,updated_at VARCHAR(40) NOT NULL,PRIMARY KEY(tenant_id,drawing_id,revision),
 CONSTRAINT fk_dm_drawing_version FOREIGN KEY(tenant_id,drawing_id) REFERENCES dm_drawing(tenant_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
START TRANSACTION;
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,status,visible,keep_alive,always_show,creator,updater)
 VALUES(96000105,'绘制与保存需求图纸','doormes:design:drawing-save',3,2,96000003,'','',0,0,0,0,'doormes-v002','doormes-v002');
INSERT INTO system_role_menu(role_id,menu_id,creator,updater,tenant_id)
 VALUES(1003,96000105,'doormes-v002','doormes-v002',1);
SET SESSION group_concat_max_len=1000000;
UPDATE system_tenant_package SET menu_ids=(SELECT CONCAT('[',GROUP_CONCAT(id ORDER BY id),']') FROM system_menu WHERE deleted=0),updater='doormes-v002' WHERE id=2000;
COMMIT;

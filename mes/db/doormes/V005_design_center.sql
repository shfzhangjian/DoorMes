-- Existing requirement drawings and immutable snapshots remain intact. New database ONLY.
USE doormes_local;
ALTER TABLE dm_drawing
 MODIFY requirement_id CHAR(36) NULL,
 MODIFY line_id CHAR(36) NULL,
 ADD drawing_number VARCHAR(60) NULL,
 ADD name VARCHAR(200) NOT NULL DEFAULT '',
 ADD note VARCHAR(1000) NOT NULL DEFAULT '',
 ADD source_type VARCHAR(20) NOT NULL DEFAULT 'REQUIREMENT',
 ADD created_at VARCHAR(40) NOT NULL DEFAULT '',
 ADD create_request_id CHAR(36) NULL,
 ADD create_payload_hash CHAR(64) NULL;
UPDATE dm_drawing SET drawing_number=CONCAT('DW-',UPPER(LEFT(REPLACE(id,'-',''),12))),
 name='门窗设计',created_at=updated_at;
ALTER TABLE dm_drawing
 MODIFY drawing_number VARCHAR(60) NOT NULL,
 ADD UNIQUE KEY uq_dm_drawing_number(tenant_id,drawing_number),
 ADD UNIQUE KEY uq_dm_drawing_create_request(tenant_id,created_by,create_request_id),
 ADD KEY ix_dm_drawing_list(tenant_id,updated_at,id),
 ADD KEY ix_dm_drawing_owner(tenant_id,created_by,source_type);
START TRANSACTION;
UPDATE system_menu SET name='门窗设计工作台',component='doormes/designs/index',component_name='DoorMesDesignCenter',updater='doormes-v005' WHERE id=96000003;
UPDATE system_menu SET name='图纸中心',component='doormes/designs/index',component_name='DoorMesDrawingCenter',updater='doormes-v005' WHERE id=96000008;
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,status,visible,keep_alive,always_show,creator,updater)
 VALUES(96000113,'新建独立设计','doormes:design:drawing-create',3,3,96000003,'','',0,0,0,0,'doormes-v005','doormes-v005');
INSERT INTO system_role_menu(role_id,menu_id,creator,updater,tenant_id)
 VALUES(1003,96000113,'doormes-v005','doormes-v005',1);
SET SESSION group_concat_max_len=1000000;
UPDATE system_tenant_package SET menu_ids=(SELECT CONCAT('[',GROUP_CONCAT(id ORDER BY id),']') FROM system_menu WHERE deleted=0),updater='doormes-v005' WHERE id=2000;
COMMIT;

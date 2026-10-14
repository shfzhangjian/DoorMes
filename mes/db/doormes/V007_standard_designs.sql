-- Apply only to the isolated doormes_local database after V006.
USE doormes_local;
CREATE TABLE dm_standard_design (
 tenant_id BIGINT NOT NULL,
 drawing_id CHAR(36) NOT NULL,
 published_revision INT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 disabled_by BIGINT NULL,
 disabled_at VARCHAR(40) NULL,
 disable_reason VARCHAR(1000) NULL,
 updated_at VARCHAR(40) NOT NULL,
 PRIMARY KEY (tenant_id,drawing_id),
 CONSTRAINT fk_dm_standard_drawing FOREIGN KEY (tenant_id,drawing_id) REFERENCES dm_drawing(tenant_id,id),
 KEY ix_dm_standard_list (tenant_id,status,updated_at,drawing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE dm_standard_design_version (
 tenant_id BIGINT NOT NULL,
 drawing_id CHAR(36) NOT NULL,
 revision INT NOT NULL,
 status VARCHAR(20) NOT NULL,
 number VARCHAR(60) NOT NULL,
 name VARCHAR(200) NOT NULL,
 note VARCHAR(1000) NOT NULL,
 review_note VARCHAR(1000) NULL,
 submitted_by BIGINT NOT NULL,
 reviewed_by BIGINT NULL,
 submitted_at VARCHAR(40) NOT NULL,
 reviewed_at VARCHAR(40) NULL,
 PRIMARY KEY (tenant_id,drawing_id,revision),
 CONSTRAINT fk_dm_standard_version_drawing FOREIGN KEY (tenant_id,drawing_id,revision) REFERENCES dm_drawing_version(tenant_id,drawing_id,revision),
 KEY ix_dm_standard_review (tenant_id,status,submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE dm_drawing
 ADD standard_source_id CHAR(36) NULL,
 ADD standard_source_revision INT NULL,
 ADD KEY ix_dm_drawing_standard_source (tenant_id,standard_source_id,standard_source_revision);
START TRANSACTION;
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,status,visible,keep_alive,always_show,creator,updater) VALUES
 (96000118,'提交标准设计','doormes:design:standard-submit',3,4,96000003,'','',0,0,0,0,'doormes-v007','doormes-v007'),
 (96000119,'审核及停用标准设计','doormes:design:standard-review',3,5,96000003,'','',0,0,0,0,'doormes-v007','doormes-v007'),
 (96000120,'复用标准设计','doormes:design:standard-reuse',3,6,96000003,'','',0,0,0,0,'doormes-v007','doormes-v007');
INSERT INTO system_role_menu(role_id,menu_id,creator,updater,tenant_id) VALUES
 (1003,96000118,'doormes-v007','doormes-v007',1),
 (1003,96000120,'doormes-v007','doormes-v007',1),
 (1007,96000119,'doormes-v007','doormes-v007',1);
SET SESSION group_concat_max_len=1000000;
UPDATE system_tenant_package SET menu_ids=(SELECT CONCAT('[',GROUP_CONCAT(id ORDER BY id),']') FROM system_menu WHERE deleted=0),updater='doormes-v007' WHERE id=2000;
COMMIT;

USE doormes_local;
CREATE TABLE dm_material_catalog (
 id CHAR(36) NOT NULL,tenant_id BIGINT NOT NULL,category VARCHAR(20) NOT NULL,code VARCHAR(60) NOT NULL,name VARCHAR(160) NOT NULL,
 revision INT NOT NULL,status VARCHAR(20) NOT NULL,published_revision INT NULL,updated_at VARCHAR(40) NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uq_dm_catalog_tenant(tenant_id,id),UNIQUE KEY uq_dm_catalog_code(tenant_id,code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE dm_material_catalog_version (
 tenant_id BIGINT NOT NULL,catalog_id CHAR(36) NOT NULL,revision INT NOT NULL,status VARCHAR(20) NOT NULL,
 json_path VARCHAR(160) NOT NULL,sha256 CHAR(64) NOT NULL,change_note VARCHAR(1000) NOT NULL,changed_by BIGINT NOT NULL,updated_at VARCHAR(40) NOT NULL,
 PRIMARY KEY(tenant_id,catalog_id,revision),CONSTRAINT fk_dm_catalog_version FOREIGN KEY(tenant_id,catalog_id) REFERENCES dm_material_catalog(tenant_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
START TRANSACTION;
UPDATE system_menu SET component='doormes/catalog/index',updater='doormes-v003' WHERE id=96000004 AND permission='doormes:catalog:query';
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,status,visible,keep_alive,always_show,creator,updater) VALUES
 (96000110,'维护材质选型目录','doormes:catalog:edit',3,1,96000004,'','',0,0,0,0,'doormes-v003','doormes-v003'),
 (96000111,'发布设计选型版本','doormes:catalog:publish',3,2,96000004,'','',0,0,0,0,'doormes-v003','doormes-v003');
INSERT INTO system_role_menu(role_id,menu_id,creator,updater,tenant_id) VALUES
 (1003,96000110,'doormes-v003','doormes-v003',1),(1005,96000110,'doormes-v003','doormes-v003',1);
SET SESSION group_concat_max_len=1000000;
UPDATE system_tenant_package SET menu_ids=(SELECT CONCAT('[',GROUP_CONCAT(id ORDER BY id),']') FROM system_menu WHERE deleted=0),updater='doormes-v003' WHERE id=2000;
COMMIT;

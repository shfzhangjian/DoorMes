USE doormes_local;
CREATE TABLE dm_visual_asset (
 tenant_id BIGINT NOT NULL,asset_id VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 blob_id CHAR(36) NOT NULL,kind VARCHAR(30) NOT NULL,media_type VARCHAR(40) NOT NULL,
 content_hash VARCHAR(71) NOT NULL,byte_length INT NOT NULL,metadata_hash CHAR(64) NOT NULL,
 uploaded_by BIGINT NOT NULL,stored_at VARCHAR(40) NOT NULL,
 PRIMARY KEY(tenant_id,asset_id),UNIQUE KEY uq_dm_asset_blob(tenant_id,blob_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE dm_visual_asset_upload (
 grant_id CHAR(36) NOT NULL,tenant_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,
 asset_id VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,content_hash VARCHAR(71) NOT NULL,
 media_type VARCHAR(40) NOT NULL,byte_length INT NOT NULL,expires_at BIGINT NOT NULL,
 PRIMARY KEY(grant_id),KEY ix_dm_grant_actor(tenant_id,actor_id,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
START TRANSACTION;
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,status,visible,keep_alive,always_show,creator,updater)
 VALUES(96000112,'上传本地模型与纹理','doormes:catalog:asset-upload',3,3,96000004,'','',0,0,0,0,'doormes-v004','doormes-v004');
INSERT INTO system_role_menu(role_id,menu_id,creator,updater,tenant_id) VALUES
 (1003,96000112,'doormes-v004','doormes-v004',1),(1005,96000112,'doormes-v004','doormes-v004',1);
SET SESSION group_concat_max_len=1000000;
UPDATE system_tenant_package SET menu_ids=(SELECT CONCAT('[',GROUP_CONCAT(id ORDER BY id),']') FROM system_menu WHERE deleted=0),updater='doormes-v004' WHERE id=2000;
COMMIT;

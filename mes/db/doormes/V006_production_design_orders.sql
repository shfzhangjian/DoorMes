-- New DoorMes database only. Existing MES records and all accounts/organizations remain untouched.
USE doormes_local;
CREATE TABLE dm_production_order (
 id CHAR(36) NOT NULL PRIMARY KEY,
 tenant_id BIGINT NOT NULL,
 number VARCHAR(40) NOT NULL,
 customer VARCHAR(200) NOT NULL,
 project VARCHAR(200) NOT NULL,
 order_type VARCHAR(20) NOT NULL,
 status VARCHAR(20) NOT NULL,
 revision INT NOT NULL,
 created_by BIGINT NOT NULL,
 assigned_to BIGINT NULL,
 requirement_id CHAR(36) NULL,
 created_at VARCHAR(40) NOT NULL,
 updated_at VARCHAR(40) NOT NULL,
 UNIQUE KEY uq_dm_order_number(tenant_id,number),
 UNIQUE KEY uq_dm_order_requirement(tenant_id,requirement_id),
 KEY ix_dm_order_list(tenant_id,status,updated_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE dm_production_order_version (
 tenant_id BIGINT NOT NULL,
 order_id CHAR(36) NOT NULL,
 revision INT NOT NULL,
 payload_json LONGTEXT NOT NULL,
 sha256 CHAR(64) NOT NULL,
 action VARCHAR(40) NOT NULL,
 change_note VARCHAR(1000) NOT NULL,
 changed_by BIGINT NOT NULL,
 updated_at VARCHAR(40) NOT NULL,
 PRIMARY KEY(tenant_id,order_id,revision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
START TRANSACTION;
UPDATE system_menu SET name='生产设计订单',component='doormes/production-orders/index',component_name='DoorMesProductionOrders',updater='doormes-v006' WHERE id=96000006 AND path='production';
UPDATE system_menu SET name='订单图纸变更',component='doormes/production-orders/index',component_name='DoorMesOrderChanges',updater='doormes-v006' WHERE id=96000007 AND path='changes';
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,status,visible,keep_alive,always_show,creator,updater) VALUES
 (96000114,'绑定订单图纸与组成件','doormes:orders:bind',3,1,96000006,'','',0,0,0,0,'doormes-v006','doormes-v006'),
 (96000115,'申请订单图纸变更','doormes:orders:change-request',3,1,96000007,'','',0,0,0,0,'doormes-v006','doormes-v006'),
 (96000116,'审核订单图纸变更','doormes:orders:change-review',3,2,96000007,'','',0,0,0,0,'doormes-v006','doormes-v006'),
 (96000117,'生产确认采用新图版本','doormes:orders:change-apply',3,3,96000007,'','',0,0,0,0,'doormes-v006','doormes-v006');
INSERT INTO system_role_menu(role_id,menu_id,creator,updater,tenant_id) VALUES
 (1003,96000114,'doormes-v006','doormes-v006',1),
 (1003,96000115,'doormes-v006','doormes-v006',1),
 (1007,96000116,'doormes-v006','doormes-v006',1),
 (1006,96000117,'doormes-v006','doormes-v006',1);
-- Grant list/view access only; this does not grant sales drawing edits or designers sales creation.
INSERT INTO system_role_menu(role_id,menu_id,creator,updater,tenant_id)
 SELECT r.id,m.id,'doormes-v006','doormes-v006',1 FROM system_role r CROSS JOIN system_menu m
 WHERE r.id IN(1002,1003,1006,1007) AND r.tenant_id=1 AND r.deleted=0
 AND m.id IN(96000006,96000007,96000008)
 AND NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id AND rm.tenant_id=1 AND rm.deleted=0);
SET SESSION group_concat_max_len=1000000;
UPDATE system_tenant_package SET menu_ids=(SELECT CONCAT('[',GROUP_CONCAT(id ORDER BY id),']') FROM system_menu WHERE deleted=0),updater='doormes-v006' WHERE id=2000;
COMMIT;

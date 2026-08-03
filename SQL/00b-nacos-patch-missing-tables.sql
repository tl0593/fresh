-- 补齐 Nacos 2.1.2 缺失表（已有库可手动执行；新库仍看 00-nacos-init.sql）
USE nacos_config;

CREATE TABLE IF NOT EXISTS config_info_aggr (
  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  data_id varchar(255) NOT NULL COMMENT 'data_id',
  group_id varchar(255) NOT NULL COMMENT 'group_id',
  datum_id varchar(255) NOT NULL COMMENT 'datum_id',
  content longtext NOT NULL COMMENT '内容',
  gmt_modified datetime NOT NULL COMMENT '修改时间',
  app_name varchar(128) DEFAULT NULL,
  tenant_id varchar(128) DEFAULT '' COMMENT '租户字段',
  PRIMARY KEY (id),
  UNIQUE KEY uk_configinfoaggr_datagrouptenantdatum (data_id,group_id,tenant_id,datum_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='增加租户字段';

CREATE TABLE IF NOT EXISTS config_info_beta (
  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  data_id varchar(255) NOT NULL COMMENT 'data_id',
  group_id varchar(128) NOT NULL COMMENT 'group_id',
  app_name varchar(128) DEFAULT NULL COMMENT 'app_name',
  content longtext NOT NULL COMMENT 'content',
  beta_ips varchar(1024) DEFAULT NULL COMMENT 'betaIps',
  md5 varchar(32) DEFAULT NULL COMMENT 'md5',
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  src_user text COMMENT 'source user',
  src_ip varchar(50) DEFAULT NULL COMMENT 'source ip',
  tenant_id varchar(128) DEFAULT '' COMMENT '租户字段',
  encrypted_data_key text NOT NULL COMMENT '秘钥',
  PRIMARY KEY (id),
  UNIQUE KEY uk_configinfobeta_datagrouptenant (data_id,group_id,tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='config_info_beta';

CREATE TABLE IF NOT EXISTS config_info_tag (
  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  data_id varchar(255) NOT NULL COMMENT 'data_id',
  group_id varchar(128) NOT NULL COMMENT 'group_id',
  tenant_id varchar(128) DEFAULT '' COMMENT 'tenant_id',
  tag_id varchar(128) NOT NULL COMMENT 'tag_id',
  app_name varchar(128) DEFAULT NULL COMMENT 'app_name',
  content longtext NOT NULL COMMENT 'content',
  md5 varchar(32) DEFAULT NULL COMMENT 'md5',
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  src_user text COMMENT 'source user',
  src_ip varchar(50) DEFAULT NULL COMMENT 'source ip',
  PRIMARY KEY (id),
  UNIQUE KEY uk_configinfotag_datagrouptenanttag (data_id,group_id,tenant_id,tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='config_info_tag';

CREATE TABLE IF NOT EXISTS config_tags_relation (
  id bigint(20) NOT NULL COMMENT 'id',
  tag_name varchar(128) NOT NULL COMMENT 'tag_name',
  tag_type varchar(64) DEFAULT NULL COMMENT 'tag_type',
  data_id varchar(255) NOT NULL COMMENT 'data_id',
  group_id varchar(128) NOT NULL COMMENT 'group_id',
  tenant_id varchar(128) DEFAULT '' COMMENT 'tenant_id',
  nid bigint(20) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (nid),
  UNIQUE KEY uk_configtagrelation_configidtag (id,tag_name,tag_type),
  KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='config_tag_relation';

CREATE TABLE IF NOT EXISTS group_capacity (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  group_id varchar(128) NOT NULL DEFAULT '' COMMENT 'Group ID',
  quota int(10) unsigned NOT NULL DEFAULT '0' COMMENT '配额',
  `usage` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '使用量',
  max_size int(10) unsigned NOT NULL DEFAULT '0',
  max_aggr_count int(10) unsigned NOT NULL DEFAULT '0',
  max_aggr_size int(10) unsigned NOT NULL DEFAULT '0',
  max_history_count int(10) unsigned NOT NULL DEFAULT '0',
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tenant_capacity (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  tenant_id varchar(128) NOT NULL DEFAULT '' COMMENT 'Tenant ID',
  quota int(10) unsigned NOT NULL DEFAULT '0',
  `usage` int(10) unsigned NOT NULL DEFAULT '0',
  max_size int(10) unsigned NOT NULL DEFAULT '0',
  max_aggr_count int(10) unsigned NOT NULL DEFAULT '0',
  max_aggr_size int(10) unsigned NOT NULL DEFAULT '0',
  max_history_count int(10) unsigned NOT NULL DEFAULT '0',
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
  username varchar(50) NOT NULL PRIMARY KEY,
  password varchar(500) NOT NULL,
  enabled boolean NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
  username varchar(50) NOT NULL,
  role varchar(50) NOT NULL,
  UNIQUE INDEX idx_user_role (username ASC, role ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS permissions (
  role varchar(50) NOT NULL,
  resource varchar(255) NOT NULL,
  action varchar(8) NOT NULL,
  UNIQUE INDEX uk_role_permission (role,resource,action) USING BTREE
);

INSERT INTO users (username, password, enabled)
VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', TRUE)
ON DUPLICATE KEY UPDATE enabled = TRUE;

INSERT INTO roles (username, role)
VALUES ('nacos', 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE role = role;

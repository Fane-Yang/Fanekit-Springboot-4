-- 1. 字典类别表
CREATE TABLE tb_sys_dict (
                             id          BIGINT AUTO_INCREMENT COMMENT '主键，唯一标识一个字典项分类',
                             dict_type   VARCHAR(50) NOT NULL UNIQUE COMMENT '字典项分类的唯一标识符，例如 user_status, order_type',
                             dict_name   VARCHAR(100) NOT NULL COMMENT '字典项分类的名称，例如 "用户状态", "订单类型"，用于在界面上显示',
                             description VARCHAR(255) COMMENT '字典分类的详细描述，方便理解和维护',
                             sort_order  INT DEFAULT 0 COMMENT '排序字段，用于定义字典项分类在列表中的显示顺序',
                             status      TINYINT DEFAULT 1 COMMENT '状态字段，1表示启用，0表示禁用',
                             created_by  BIGINT COMMENT '创建人ID',
                             created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             updated_by  BIGINT COMMENT '最后更新人ID',
                             updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
                             editable        TINYINT DEFAULT 0 COMMENT '数据可编辑性状态，0:不允许修改和删除(系统内置)，1:允许修改不允许删除，2:允许修改和删除(用户自定义)',
                             version     INT DEFAULT 1 COMMENT '乐观锁版本号',
                             deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除标识，0:未删除, 1:已删除',

    -- 设置主键
                             PRIMARY KEY (id),

    -- 为 status 字段添加检查约束 (MySQL 8.0+ 支持)
                             CONSTRAINT chk_tb_sys_dict_status CHECK (status IN (0, 1)),

    -- 为 deleted 字段添加检查约束 (MySQL 8.0+ 支持)
                             CONSTRAINT chk_tb_sys_dict_deleted CHECK (deleted IN (0, 1)),
    -- 为 chk_editable 字段添加检查约束 (MySQL 8.0+ 支持)
                             CONSTRAINT chk_tb_sys_dict_editable CHECK (editable IN (0, 1, 2)) -- 新增检查约束，限定值为 0, 1, 2

) COMMENT='系统字典分类表';

-- 2. 字典项目表
CREATE TABLE tb_sys_dict_item (
                                  id           BIGINT AUTO_INCREMENT COMMENT '主键，唯一标识一个具体的字典项',
                                  dict_type    VARCHAR(50) NOT NULL COMMENT '外键，关联到 tb_sys_dict 表的 dict_type 字段，标识所属的字典分类',
                                  item_value   VARCHAR(100) NOT NULL COMMENT '字典项的值（键），即实际存储在业务表中的代码，例如 "ACTIVE", "PENDING"',
                                  item_label   VARCHAR(255) NOT NULL COMMENT '字典项的标签（值），即给人看的描述性文字，例如 "激活", "待处理"',
                                  description  VARCHAR(255) COMMENT '该字典项的详细描述',
                                  color        VARCHAR(7) COMMENT '可选字段，为前端展示提供颜色支持，如 #FF0000',
                                  parent_value VARCHAR(100) COMMENT '可选字段，用于支持树形结构的字典项，指向其父级的 item_value',
                                  sort_order   INT DEFAULT 0 COMMENT '排序字段，定义该字典项在其所属分类下的显示顺序',
                                  status       TINYINT DEFAULT 1 COMMENT '状态字段，1表示启用，0表示禁用',
                                  created_by   BIGINT COMMENT '创建人ID',
                                  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  updated_by   BIGINT COMMENT '最后更新人ID',
                                  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
                                  editable        TINYINT DEFAULT 0 COMMENT '数据可编辑性状态，0:不允许修改和删除(系统内置)，1:允许修改不允许删除，2:允许修改和删除(用户自定义)',
                                  version      INT DEFAULT 1 COMMENT '乐观锁版本号',
                                  deleted      TINYINT DEFAULT 0 COMMENT '逻辑删除标识，0:未删除, 1:已删除',

    -- 设置主键
                                  PRIMARY KEY (id),

    -- 设置联合唯一索引，确保在一个字典类型下，item_value 是唯一的
                                  UNIQUE KEY uk_dict_type_value (dict_type, item_value),

    -- 设置外键约束
                                  CONSTRAINT fk_dict_item_type FOREIGN KEY (dict_type) REFERENCES tb_sys_dict(dict_type),

    -- 为 status 字段添加检查约束 (MySQL 8.0+ 支持)
                                  CONSTRAINT chk_tb_sys_dict_item_status_item CHECK (status IN (0, 1)),

    -- 为 deleted 字段添加检查约束 (MySQL 8.0+ 支持)
                                  CONSTRAINT chk_tb_sys_dict_item_deleted_item CHECK (deleted IN (0, 1)),
    -- 为 chk_editable 字段添加检查约束 (MySQL 8.0+ 支持)
                                  CONSTRAINT chk_tb_sys_dict_item_editable CHECK (editable IN (0, 1, 2)) -- 新增检查约束，限定值为 0, 1, 2
) COMMENT='系统字典项明细表';

-- 3. 系统组织机构表
CREATE TABLE tb_sys_org (
                            id              BIGINT AUTO_INCREMENT COMMENT '主键，唯一标识一个组织机构',
                            org_code        VARCHAR(50) NOT NULL UNIQUE COMMENT '组织机构的唯一编码，例如 IT_DEPT, SH_BRANCH',
                            org_name        VARCHAR(100) NOT NULL COMMENT '组织机构的名称，例如 "技术部", "上海分公司"',
                            org_name_tree   VARCHAR(255) COMMENT '组织机构的全路径名称，例如 "XX集团/总部/技术中心/后端开发部"',
                            org_level       INT COMMENT '组织层级，例如 1表示集团，2表示公司，3表示部门',
                            org_type        VARCHAR(50) COMMENT '组织类型，关联字典表，如 COMPANY, DEPARTMENT',
                            parent_id       BIGINT COMMENT '父级组织ID，顶级组织为 NULL',
                            org_id_tree   VARCHAR(500) NOT NULL COMMENT '组织机构的祖先ID列表，用逗号分隔，例如 "0,1,2"',
                            province_code   VARCHAR(20) COMMENT '所属省份编码，关联 REGION_CODE 字典项',
                            city_code       VARCHAR(20) COMMENT '所属地市编码，关联 REGION_CODE 字典项',
                            sort_order      INT DEFAULT 0 COMMENT '排序字段',
                            status          TINYINT DEFAULT 1 COMMENT '状态，1启用，0禁用',
                            description     VARCHAR(255) COMMENT '描述',
                            created_by      BIGINT COMMENT '创建人ID',
                            created_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            updated_by      BIGINT COMMENT '更新人ID',
                            updated_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            version         INT DEFAULT 1 COMMENT '乐观锁版本号',
                            deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除标识，0:未删除, 1:已删除',

                            PRIMARY KEY (id),
    -- 外键约束 (指向自身，用于层级)
    CONSTRAINT fk_org_parent FOREIGN KEY (parent_id) REFERENCES tb_sys_org(id),
                            CONSTRAINT chk_tb_sys_org_status_org CHECK (status IN (0, 1)),
                            CONSTRAINT chk_tb_sys_org_deleted_org CHECK (deleted IN (0, 1))
) COMMENT='系统组织机构表';


CREATE TABLE tb_sys_user (
                             id              BIGINT AUTO_INCREMENT COMMENT '主键，唯一标识一个用户',
                             login_name      VARCHAR(50) NOT NULL COMMENT '用户登录名，用于登录，必须唯一',
                             password        VARCHAR(255) NOT NULL COMMENT '加密后的用户密码',
                             real_name       VARCHAR(100) NOT NULL COMMENT '用户真实姓名',
                             gender          TINYINT COMMENT '用户性别，关联 GENDER 字典',
                             org_id          BIGINT COMMENT '所属组织ID，关联组织表',
                             product_id      VARCHAR(50) COMMENT '产品线ID',
                             status          TINYINT DEFAULT 1 COMMENT '用户状态，0-默认(正常), 1-锁定, 2-注销, 3-待审核等等等等',
                             data_source     VARCHAR(50) COMMENT '数据来源，如 LOCAL, SYNC, 等',
                             description     VARCHAR(255) COMMENT '描述',
                             created_by      BIGINT COMMENT '创建人ID',
                             created_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             updated_by      BIGINT COMMENT '更新人ID',
                             updated_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             version         INT DEFAULT 1 COMMENT '乐观锁版本号',
                             deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除标识，0:未删除, 1:已删除',

    -- 设置 id 为单一主键
                             PRIMARY KEY (id),
    -- 为 login_name 创建唯一索引，确保其唯一性
                             UNIQUE KEY uk_login_name (login_name),
    -- 外键约束 (假设存在 tb_sys_org 表)
    -- CONSTRAINT fk_user_org FOREIGN KEY (org_id) REFERENCES tb_sys_org(id),
    -- 用户状态字段先不进行检测
#                              CONSTRAINT chk_tb_sys_user_status_user CHECK (status IN (0, 1, 2, 3, 4, 5)), -- 根据实际状态值调整
                             CONSTRAINT chk_tb_sys_user_deleted_user CHECK (deleted IN (0, 1))
) COMMENT='系统用户信息表';
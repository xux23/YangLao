-- ============================================================
-- 养老机构管理系统 初始化脚本
-- 数据库：MySQL 8.0
-- 说明：
--   1. 先执行 CREATE DATABASE elder_care DEFAULT CHARACTER SET utf8mb4;
--   2. 再导入本脚本：mysql -u root -p elder_care < init.sql
--   3. 演示账号密码均为 123456（BCrypt 加密存储）
--      管理员 admin / 护理 nure01,nurse02 / 家属 family01,family02
--   4. 若登录提示密码错误，请用 BCrypt 工具重新生成 123456 的密文替换 password 字段
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(50)  NOT NULL COMMENT '用户名',
  password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
  real_name   VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
  role        VARCHAR(20)  NOT NULL COMMENT '角色 admin/nurse/family',
  phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 老人信息表（业务主表，房间床位用字段）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS elder_info;
CREATE TABLE elder_info (
  id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name              VARCHAR(50)  NOT NULL COMMENT '姓名',
  gender            TINYINT      NOT NULL COMMENT '性别 1男 2女',
  birthday          DATE         DEFAULT NULL COMMENT '出生日期',
  id_card           VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
  phone             VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  emergency_contact VARCHAR(50)  DEFAULT NULL COMMENT '紧急联系人',
  emergency_phone   VARCHAR(20)  DEFAULT NULL COMMENT '紧急联系电话',
  room_no           VARCHAR(20)  DEFAULT NULL COMMENT '房间号',
  bed_no            VARCHAR(20)  DEFAULT NULL COMMENT '床位号',
  health_summary    VARCHAR(500) DEFAULT NULL COMMENT '健康概况',
  status            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1在住 0已退住',
  checkin_time      DATE         DEFAULT NULL COMMENT '入住日期',
  checkout_time     DATE         DEFAULT NULL COMMENT '退住日期',
  family_id         BIGINT       DEFAULT NULL COMMENT '关联家属账号 sys_user.id',
  create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_id_card (id_card),
  KEY idx_family_id (family_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人信息表';

-- ------------------------------------------------------------
-- 3. 护理记录表（护理计划与执行记录合一）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS care_record;
CREATE TABLE care_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id       BIGINT       NOT NULL COMMENT '老人ID',
  plan_name      VARCHAR(50)  DEFAULT NULL COMMENT '护理项目（如翻身、喂饭）',
  plan_frequency VARCHAR(50)  DEFAULT NULL COMMENT '频次说明',
  care_content   VARCHAR(500) DEFAULT NULL COMMENT '护理内容',
  nurse_id       BIGINT       DEFAULT NULL COMMENT '执行护理人员ID',
  care_time      DATETIME     DEFAULT NULL COMMENT '执行时间',
  remark         VARCHAR(200) DEFAULT NULL COMMENT '交接备注',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_elder_time (elder_id, care_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理记录表';

-- ------------------------------------------------------------
-- 4. 健康体征记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS health_record;
CREATE TABLE health_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id       BIGINT       NOT NULL COMMENT '老人ID',
  blood_pressure VARCHAR(20)  DEFAULT NULL COMMENT '血压（如128/82）',
  heart_rate     INT          DEFAULT NULL COMMENT '心率（次/分）',
  temperature    DECIMAL(3,1) DEFAULT NULL COMMENT '体温（℃）',
  blood_sugar    DECIMAL(4,1) DEFAULT NULL COMMENT '血糖（mmol/L）',
  record_time    DATETIME     DEFAULT NULL COMMENT '测量时间',
  recorder_id    BIGINT       DEFAULT NULL COMMENT '录入人ID',
  remark         VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_elder_time (elder_id, record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康体征记录表';

-- ------------------------------------------------------------
-- 5. 用药计划表（一行 = 某老人某天某时间点一次用药任务）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS medicine_plan;
CREATE TABLE medicine_plan (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id     BIGINT       NOT NULL COMMENT '老人ID',
  medicine_name VARCHAR(50) NOT NULL COMMENT '药名',
  dosage       VARCHAR(50)  DEFAULT NULL COMMENT '剂量（如每次1片）',
  plan_date    DATE         NOT NULL COMMENT '服药日期',
  plan_time    TIME         NOT NULL COMMENT '服药时间点（如08:00）',
  status       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待执行 1已执行 2已逾期',
  confirm_time DATETIME     DEFAULT NULL COMMENT '确认执行时间',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_elder_date (elder_id, plan_date),
  KEY idx_date_status (plan_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用药计划表';

-- ------------------------------------------------------------
-- 6. 探访预约表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS visit_appointment;
CREATE TABLE visit_appointment (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id     BIGINT       NOT NULL COMMENT '老人ID',
  family_id    BIGINT       NOT NULL COMMENT '预约家属用户ID',
  visit_date   DATE         NOT NULL COMMENT '探访日期',
  visit_time   VARCHAR(50)  DEFAULT NULL COMMENT '探访时段（如上午9:00-11:00）',
  persons      INT          NOT NULL DEFAULT 1 COMMENT '探访人数',
  status       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待审核 1已通过 2已驳回 3已完成',
  audit_remark VARCHAR(200) DEFAULT NULL COMMENT '审核意见（驳回必填）',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_status (status),
  KEY idx_elder (elder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探访预约表';

-- ------------------------------------------------------------
-- 7. 留言反馈表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS message;
CREATE TABLE message (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id    BIGINT       NOT NULL COMMENT '老人ID',
  family_id   BIGINT       NOT NULL COMMENT '家属用户ID',
  content     VARCHAR(500) NOT NULL COMMENT '留言内容',
  reply       VARCHAR(500) DEFAULT NULL COMMENT '回复内容',
  reply_time  DATETIME     DEFAULT NULL COMMENT '回复时间',
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0未回复 1已回复',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言反馈表';

-- ------------------------------------------------------------
-- 8. 操作日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_log;
CREATE TABLE sys_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       DEFAULT NULL COMMENT '操作人用户ID',
  username    VARCHAR(50)  DEFAULT NULL COMMENT '操作人用户名',
  operation   VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
  method      VARCHAR(100) DEFAULT NULL COMMENT '请求方法+路径',
  params      VARCHAR(500) DEFAULT NULL COMMENT '请求参数（截断）',
  ip          VARCHAR(50)  DEFAULT NULL COMMENT '客户端IP',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================================
-- 演示数据（密码均为 123456）
-- ============================================================
INSERT INTO sys_user (id, username, password, real_name, role, phone) VALUES
(1, 'admin',    '$2b$12$NQx.GzuzGDmzH8GWxSW4H.ufiXLDuNui1lawhKAfXaM/PsllRJbh2', '系统管理员', 'admin',  '13800000000'),
(2, 'nurse01',  '$2b$12$NQx.GzuzGDmzH8GWxSW4H.ufiXLDuNui1lawhKAfXaM/PsllRJbh2', '王丽华',     'nurse',  '13800000001'),
(3, 'nurse02',  '$2b$12$NQx.GzuzGDmzH8GWxSW4H.ufiXLDuNui1lawhKAfXaM/PsllRJbh2', '李建国',     'nurse',  '13800000002'),
(4, 'family01', '$2b$12$NQx.GzuzGDmzH8GWxSW4H.ufiXLDuNui1lawhKAfXaM/PsllRJbh2', '张小明',     'family', '13900000001'),
(5, 'family02', '$2b$12$NQx.GzuzGDmzH8GWxSW4H.ufiXLDuNui1lawhKAfXaM/PsllRJbh2', '陈晓芳',     'family', '13900000002');

INSERT INTO elder_info (id, name, gender, birthday, id_card, phone, emergency_contact, emergency_phone,
                        room_no, bed_no, health_summary, status, checkin_time, family_id) VALUES
(1, '张三',   1, '1940-05-12', '420000194005120011', '13900000010', '张小明', '13900000001', '201', 'A', '高血压，需低盐饮食，行动不便需轮椅辅助', 1, '2025-03-01', 4),
(2, '李秀英', 2, '1943-08-20', '420000194308200022', '13900000020', '陈晓芳', '13900000002', '202', 'B', '糖尿病，每日测血糖，忌甜食', 1, '2025-06-15', 5),
(3, '王德福', 1, '1935-01-05', '420000193501050033', '13900000030', '王强',   '13900000003', '203', 'A', '冠心病史，2026-07-30 已退住', 0, '2024-09-01', NULL);

INSERT INTO care_record (elder_id, plan_name, plan_frequency, care_content, nurse_id, care_time, remark) VALUES
(1, '翻身',     '每2小时一次',   '协助翻身并检查皮肤状况，无压疮', 2, '2026-08-16 08:30:00', '夜间交接：腿部保暖'),
(1, '喂饭',     '每日三餐',      '早餐粥类，食欲良好，进食量正常', 2, '2026-08-16 07:30:00', NULL),
(2, '血糖监测', '每日2次',       '早餐前空腹血糖 6.1，正常范围',  3, '2026-08-16 07:00:00', '午后复测'),
(2, '洗澡',     '每周2次',       '协助淋浴，注意防滑，状态良好',  3, '2026-08-15 15:00:00', NULL);

INSERT INTO health_record (elder_id, blood_pressure, heart_rate, temperature, blood_sugar, record_time, recorder_id) VALUES
(1, '128/82', 76, 36.5, NULL, '2026-08-14 08:00:00', 2),
(1, '130/85', 78, 36.4, NULL, '2026-08-15 08:00:00', 2),
(1, '126/80', 74, 36.6, NULL, '2026-08-16 08:00:00', 2),
(2, '118/75', 70, 36.4, 6.1,  '2026-08-16 07:00:00', 3);

-- 张三：硝苯地平缓释片 每日2次（今天2行待执行、昨天1行已执行、前天1行待执行用于演示逾期扫描）
INSERT INTO medicine_plan (elder_id, medicine_name, dosage, plan_date, plan_time, status, confirm_time) VALUES
(1, '硝苯地平缓释片', '每次1片', '2026-08-14', '08:00:00', 0, NULL),
(1, '硝苯地平缓释片', '每次1片', '2026-08-15', '08:00:00', 1, '2026-08-15 08:06:00'),
(1, '硝苯地平缓释片', '每次1片', '2026-08-16', '08:00:00', 0, NULL),
(1, '硝苯地平缓释片', '每次1片', '2026-08-16', '14:00:00', 0, NULL),
(2, '二甲双胍片',     '每次1片', '2026-08-16', '12:00:00', 0, NULL);

INSERT INTO visit_appointment (elder_id, family_id, visit_date, visit_time, persons, status, audit_remark) VALUES
(1, 4, '2026-08-20', '上午 9:00-11:00', 2, 0, NULL),
(2, 5, '2026-08-18', '下午 14:00-16:00', 1, 1, '同意探望'),
(1, 4, '2026-08-10', '上午 9:00-11:00', 2, 3, NULL);

INSERT INTO message (elder_id, family_id, content, reply, reply_time, status) VALUES
(1, 4, '父亲最近睡得好吗？', '睡得不错，白天精神也很好，请放心', '2026-08-15 10:00:00', 1),
(2, 5, '今天血糖测了吗？', NULL, NULL, 0);

INSERT INTO sys_log (user_id, username, operation, method, params, ip) VALUES
(1, 'admin',    '新增老人',   'POST /api/elders',  '{"name":"张三"}',   '127.0.0.1'),
(2, 'nurse01',  '新增护理记录', 'POST /api/care-records', '{"elderId":1,"planName":"翻身"}', '127.0.0.1'),
(1, 'admin',    '探访审核',   'PUT /api/visits/2/audit', '{"status":1}', '127.0.0.1');
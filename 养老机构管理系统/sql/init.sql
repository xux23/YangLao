-- ============================================================
-- 养老机构管理系统 数据库初始化脚本
-- 数据库：MySQL 8.0
-- 使用方式：mysql -u root -p < init.sql
-- 演示账号密码均为 123456（BCrypt 加密存储）：
--   管理员  admin    护理人员 nurse01 / nurse02  家属 family01 / family02
-- 说明：演示数据中的日期均相对系统当前日期生成（CURDATE），
--       因此任何时候导入都能演示"今日任务、趋势图、逾期"等效果。
-- ============================================================

CREATE DATABASE IF NOT EXISTS elder_care DEFAULT CHARACTER SET utf8mb4;
USE elder_care;

-- ------------------------------------------------------------
-- 1. 用户表（admin / nurse / family 三类角色共用）
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
-- 2. 老人信息表（业务主表，房间床位用字段，不单独建表）
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
-- 5. 用药计划表（一行 = 某老人某天某个时间点的一次用药任务）
--    disabled 字段为设计补充：计划停用后置 1，防止次日查询时
--    被"按需生成"逻辑重新复制出新任务（历史行保留作档案）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS medicine_plan;
CREATE TABLE medicine_plan (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id      BIGINT       NOT NULL COMMENT '老人ID',
  medicine_name VARCHAR(50)  NOT NULL COMMENT '药名',
  dosage        VARCHAR(50)  DEFAULT NULL COMMENT '剂量（如每次1片）',
  plan_date     DATE         NOT NULL COMMENT '服药日期',
  plan_time     TIME         NOT NULL COMMENT '服药时间点（如08:00）',
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待执行 1已执行 2已逾期',
  confirm_time  DATETIME     DEFAULT NULL COMMENT '确认执行时间',
  disabled      TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1已停用',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
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
-- 演示数据（密码均为 123456，BCrypt 密文）
-- ============================================================

INSERT INTO sys_user (id, username, password, real_name, role, phone) VALUES
(1, 'admin',    '$2b$12$NQx.GzuzGDmzH8GWxSW4H.ufiXLDuNui1lawhKAfXaM/PsllRJbh2', '系统管理员', 'admin',  '13800000000'),
(2, 'nurse01',  '$2b$12$NQx.GzuzGDmzH8GWxSW4H.ufiXLDuNui1lawhKAfXaM/PsllRJbh2', '王丽华',     'nurse',  '13800000001'),
(3, 'nurse02',  '$2b$12$NQx.GzuzGDmzH8GWxSW4H.ufiXLDuNui1lawhKAfXaM/PsllRJbh2', '李建国',     'nurse',  '13800000002'),
(4, 'family01', '$2b$12$NQx.GzuzGDmzH8GWxSW4H.ufiXLDuNui1lawhKAfXaM/PsllRJbh2', '张小明',     'family', '13900000001'),
(5, 'family02', '$2b$12$NQx.GzuzGDmzH8GWxSW4H.ufiXLDuNui1lawhKAfXaM/PsllRJbh2', '陈晓芳',     'family', '13900000002');

-- 老人档案（1、2、4、5 在住；3 已退住）
INSERT INTO elder_info (id, name, gender, birthday, id_card, phone, emergency_contact, emergency_phone,
                        room_no, bed_no, health_summary, status, checkin_time, checkout_time, family_id) VALUES
(1, '张三',   1, '1940-05-12', '420000194005120011', '13900000010', '张小明', '13900000001', '201', 'A', '高血压，需低盐饮食，行动不便需轮椅辅助',                        1, '2025-03-01', NULL, 4),
(2, '李秀英', 2, '1943-08-20', '420000194308200022', '13900000020', '陈晓芳', '13900000002', '202', 'B', '糖尿病，每日测血糖，忌甜食',                                  1, '2025-06-15', NULL, 5),
(3, '王德福', 1, '1935-01-05', '420000193501050033', '13900000030', '王强',   '13900000003', NULL, NULL, '冠心病史，已于 2026-07-30 退住',          0, '2024-09-01', '2026-07-30', NULL),
(4, '张桂花', 2, '1947-03-18', '420000194703180044', '13900000040', '张伟',   '13900000004', '204', 'A', '轻度认知障碍，需定期照看',                                    1, '2026-01-10', NULL, NULL),
(5, '刘建国', 1, '1959-11-02', '420000195911020055', '13900000050', '刘芳',   '13900000005', '205', 'B', '腰椎术后恢复中，避免剧烈活动',                                1, '2026-04-20', NULL, NULL);

-- 护理记录（时间相对当前日期生成，保障趋势图演示效果）
INSERT INTO care_record (elder_id, plan_name, plan_frequency, care_content, nurse_id, care_time, remark) VALUES
(1, '翻身', '每2小时一次', '协助翻身并检查皮肤状况，无压疮', 2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 0 DAY), ' 08:30:00'), '夜间交接：注意腿部保暖'),
(1, '喂饭', '每日三餐',    '早餐粥类，食欲良好，进食量正常',   2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 0 DAY), ' 07:30:00'), NULL),
(2, '血糖监测', '每日2次', '早餐前空腹血糖 6.1，正常范围',     3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 0 DAY), ' 07:00:00'), '午后复测'),
(1, '翻身', '每2小时一次', '协助翻身，皮肤状况良好',           2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 10:00:00'), NULL),
(1, '喂饭', '每日三餐',    '晚餐粥类，进食量正常',             2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 18:00:00'), NULL),
(2, '血糖监测', '每日2次', '午餐前血糖 5.9，正常范围',         3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 11:30:00'), NULL),
(2, '洗澡', '每周2次',     '协助淋浴，注意防滑，状态良好',     3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 15:00:00'), NULL),
(4, '康复训练', '每日1次', '上肢抬举训练 20 分钟，配合良好',   2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 09:30:00'), NULL),
(1, '翻身', '每2小时一次', '协助翻身，检查皮肤无异常',         2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), ' 08:30:00'), NULL),
(2, '血糖监测', '每日2次', '早餐前血糖 6.0',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), ' 07:00:00'), NULL),
(5, '康复训练', '每日1次', '腰背肌功能锻炼 15 分钟',           3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), ' 10:00:00'), NULL),
(1, '喂饭', '每日三餐',    '早餐粥类，食欲良好',               2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 3 DAY), ' 07:30:00'), NULL),
(2, '血糖监测', '每日2次', '晚餐后血糖 6.8',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 3 DAY), ' 19:00:00'), NULL),
(4, '康复训练', '每日1次', '下肢肌力训练 15 分钟',             2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 3 DAY), ' 09:30:00'), NULL),
(1, '翻身', '每2小时一次', '协助翻身，皮肤状况良好',           2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), ' 08:30:00'), NULL),
(2, '洗澡', '每周2次',     '协助淋浴，状态良好',               3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), ' 15:00:00'), NULL),
(1, '喂饭', '每日三餐',    '晚餐粥类，进食量正常',             2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 5 DAY), ' 18:00:00'), NULL),
(2, '血糖监测', '每日2次', '早餐前血糖 6.2',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 5 DAY), ' 07:00:00'), NULL),
(5, '康复训练', '每日1次', '腰背肌功能锻炼 20 分钟',           3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 5 DAY), ' 10:00:00'), NULL),
(1, '翻身', '每2小时一次', '协助翻身并检查皮肤状况',           2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 6 DAY), ' 08:30:00'), NULL),
(4, '康复训练', '每日1次', '上肢抬举训练 15 分钟',             2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 6 DAY), ' 09:30:00'), NULL),
(2, '血糖监测', '每日2次', '午餐前血糖 5.8',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 7 DAY), ' 11:30:00'), NULL),
(1, '喂饭', '每日三餐',    '早餐粥类，食欲良好',               2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 7 DAY), ' 07:30:00'), NULL),
(5, '康复训练', '每日1次', '腰背肌功能锻炼 15 分钟',           3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 8 DAY), ' 10:00:00'), NULL),
(1, '翻身', '每2小时一次', '协助翻身，皮肤状况良好',           2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 8 DAY), ' 08:30:00'), NULL),
(2, '血糖监测', '每日2次', '晚餐后血糖 6.5',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 9 DAY), ' 19:00:00'), NULL),
(4, '康复训练', '每日1次', '下肢肌力训练 20 分钟',             2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 9 DAY), ' 09:30:00'), NULL),
(1, '喂饭', '每日三餐',    '晚餐粥类，进食量正常',             2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 10 DAY), ' 18:00:00'), NULL),
(2, '血糖监测', '每日2次', '早餐前血糖 6.1',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 11 DAY), ' 07:00:00'), NULL),
(1, '翻身', '每2小时一次', '协助翻身并检查皮肤状况',           2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 12 DAY), ' 08:30:00'), NULL),
(5, '康复训练', '每日1次', '腰背肌功能锻炼 20 分钟',           3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 12 DAY), ' 10:00:00'), NULL),
(2, '洗澡', '每周2次',     '协助淋浴，状态良好',               3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 13 DAY), ' 15:00:00'), NULL),
(4, '康复训练', '每日1次', '上肢抬举训练 20 分钟',             2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 14 DAY), ' 09:30:00'), NULL),
(1, '喂饭', '每日三餐',    '早餐粥类，食欲良好',               2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 14 DAY), ' 07:30:00'), NULL),
(2, '血糖监测', '每日2次', '午餐前血糖 6.3',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 15 DAY), ' 11:30:00'), NULL),
(1, '翻身', '每2小时一次', '协助翻身，皮肤状况良好',           2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 16 DAY), ' 08:30:00'), NULL),
(5, '康复训练', '每日1次', '腰背肌功能锻炼 15 分钟',           3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 17 DAY), ' 10:00:00'), NULL),
(2, '血糖监测', '每日2次', '早餐前血糖 6.0',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 18 DAY), ' 07:00:00'), NULL),
(1, '喂饭', '每日三餐',    '晚餐粥类，进食量正常',             2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 19 DAY), ' 18:00:00'), NULL),
(4, '康复训练', '每日1次', '下肢肌力训练 15 分钟',             2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 20 DAY), ' 09:30:00'), NULL),
(2, '血糖监测', '每日2次', '晚餐后血糖 6.6',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 21 DAY), ' 19:00:00'), NULL),
(1, '翻身', '每2小时一次', '协助翻身并检查皮肤状况',           2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 22 DAY), ' 08:30:00'), NULL),
(5, '康复训练', '每日1次', '腰背肌功能锻炼 20 分钟',           3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 23 DAY), ' 10:00:00'), NULL),
(2, '血糖监测', '每日2次', '午餐前血糖 6.4',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 24 DAY), ' 11:30:00'), NULL),
(1, '喂饭', '每日三餐',    '早餐粥类，食欲良好',               2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 26 DAY), ' 07:30:00'), NULL),
(2, '血糖监测', '每日2次', '早餐前血糖 6.2',                   3, CONCAT(DATE_SUB(CURDATE(), INTERVAL 28 DAY), ' 07:00:00'), NULL),
(1, '翻身', '每2小时一次', '协助翻身，皮肤状况良好',           2, CONCAT(DATE_SUB(CURDATE(), INTERVAL 29 DAY), ' 08:30:00'), NULL);

-- 健康体征记录（近 10 天，用于体征趋势图）
INSERT INTO health_record (elder_id, blood_pressure, heart_rate, temperature, blood_sugar, record_time, recorder_id, remark) VALUES
(1, '118/78', 72, 36.4, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 9 DAY), ' 08:00:00'), 2, NULL),
(1, '126/80', 75, 36.5, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 8 DAY), ' 08:00:00'), 2, NULL),
(1, '130/84', 78, 36.4, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 7 DAY), ' 08:00:00'), 2, NULL),
(1, '122/79', 74, 36.6, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 6 DAY), ' 08:00:00'), 2, NULL),
(1, '128/82', 76, 36.5, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 5 DAY), ' 08:00:00'), 2, '晨起测量'),
(1, '124/78', 73, 36.4, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), ' 08:00:00'), 2, NULL),
(1, '120/76', 71, 36.5, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 3 DAY), ' 08:00:00'), 2, NULL),
(1, '126/80', 74, 36.4, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), ' 08:00:00'), 2, NULL),
(1, '130/85', 78, 36.5, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 08:00:00'), 2, NULL),
(1, '128/82', 76, 36.6, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 0 DAY), ' 08:00:00'), 2, NULL),
(2, '116/74', 70, 36.4, 6.0,  CONCAT(DATE_SUB(CURDATE(), INTERVAL 8 DAY), ' 07:00:00'), 3, NULL),
(2, '120/76', 72, 36.5, 5.9,  CONCAT(DATE_SUB(CURDATE(), INTERVAL 6 DAY), ' 07:00:00'), 3, NULL),
(2, '118/75', 71, 36.4, 6.1,  CONCAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), ' 07:00:00'), 3, NULL),
(2, '122/77', 73, 36.5, 6.0,  CONCAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), ' 07:00:00'), 3, NULL),
(2, '118/75', 70, 36.4, 6.1,  CONCAT(DATE_SUB(CURDATE(), INTERVAL 0 DAY), ' 07:00:00'), 3, '空腹血糖'),
(4, '135/88', 80, 36.5, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 7 DAY), ' 09:00:00'), 2, NULL),
(4, '132/86', 79, 36.4, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), ' 09:00:00'), 2, NULL),
(4, '130/85', 78, 36.5, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 09:00:00'), 2, NULL),
(5, '115/72', 65, 36.3, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 6 DAY), ' 10:00:00'), 3, NULL),
(5, '118/75', 67, 36.4, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 3 DAY), ' 10:00:00'), 3, NULL),
(5, '116/73', 66, 36.4, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 0 DAY), ' 10:00:00'), 3, NULL);

-- 用药计划/任务（相对日期：有今日待执行、昨日已执行、前天待执行（将逾期）三种状态，便于演示）
INSERT INTO medicine_plan (elder_id, medicine_name, dosage, plan_date, plan_time, status, confirm_time) VALUES
-- 张三：硝苯地平缓释片 每日 2 次
(1, '硝苯地平缓释片', '每次1片', DATE_SUB(CURDATE(), INTERVAL 2 DAY), '08:00:00', 0, NULL),
(1, '硝苯地平缓释片', '每次1片', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:00:00', 1, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 07:54:00')),
(1, '硝苯地平缓释片', '每次1片', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:00:00', 0, NULL),
(1, '硝苯地平缓释片', '每次1片', CURDATE(), '08:00:00', 0, NULL),
(1, '硝苯地平缓释片', '每次1片', CURDATE(), '14:00:00', 0, NULL),
-- 李秀英：二甲双胍片 每日 2 次；阿司匹林肠溶片 每日 1 次
(2, '二甲双胍片', '每次1片', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '12:00:00', 1, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 12:00:00')),
(2, '二甲双胍片', '每次1片', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '20:00:00', 1, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 19:50:00')),
(2, '二甲双胍片', '每次1片', CURDATE(), '12:00:00', 0, NULL),
(2, '二甲双胍片', '每次1片', CURDATE(), '20:00:00', 0, NULL),
(2, '阿司匹林肠溶片', '每次1片', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:00:00', 1, CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 09:00:00')),
(2, '阿司匹林肠溶片', '每次1片', CURDATE(), '09:00:00', 0, NULL);

-- 探访预约（待审核 / 已通过 / 已驳回 / 已完成 各状态均有）
INSERT INTO visit_appointment (elder_id, family_id, visit_date, visit_time, persons, status, audit_remark) VALUES
(1, 4, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '上午 9:00-11:00',  2, 0, NULL),
(1, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '下午 14:00-16:00', 5, 2, '探访人数过多，请改约其它时段'),
(2, 5, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '下午 14:00-16:00', 1, 1, '同意探望'),
(1, 4, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '上午 9:00-11:00',  2, 3, NULL),
(2, 5, DATE_SUB(CURDATE(), INTERVAL 8 DAY), '上午 9:00-11:00',  1, 3, NULL);

-- 家属留言（未回复 / 已回复）
INSERT INTO message (elder_id, family_id, content, reply, reply_time, status) VALUES
(1, 4, '父亲最近睡得好吗？', '睡得不错，白天精神也很好，请放心', DATE_SUB(NOW(), INTERVAL 1 DAY), 1),
(1, 4, '本周六想带父亲外出散步，可以吗？', NULL, NULL, 0),
(2, 5, '今天血糖测了吗？', NULL, NULL, 0),
(2, 5, '老妈说要换个软一点的枕头，麻烦安排一下', '已安排新的软枕，请放心', DATE_SUB(NOW(), INTERVAL 2 DAY), 1);

-- 操作日志（演示）
INSERT INTO sys_log (user_id, username, operation, method, params, ip, create_time) VALUES
(1, 'admin',   '新增老人',     'POST /api/elders',          '{"name":"张三"}',              '127.0.0.1', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'nurse01', '新增护理记录', 'POST /api/care-records',    '{"elderId":1,"planName":"翻身"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(1, 'admin',   '探访审核',     'PUT /api/visits/2/audit',   '{"status":1}',                 '127.0.0.1', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(3, 'nurse02', '新增体征记录', 'POST /api/health-records',  '{"elderId":2,"bloodPressure":"118/75"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(1, 'admin',   '重置密码',     'PUT /api/users/5/password', '{}',                           '127.0.0.1', DATE_SUB(NOW(), INTERVAL 1 DAY));
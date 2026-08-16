# 养老机构管理系统 —— API 接口文档

> 版本：v1.0　｜　日期：2026-08-16　｜　配套：《需求分析文档.md》《系统设计文档.md》

---

## 1. 通用约定

### 1.1 基础信息

- 后端服务地址（开发）：`http://localhost:8080/api`
- 数据格式：JSON（UTF-8）
- 认证方式：除登录外，所有接口需携带请求头 `Authorization: Bearer <token>`
- 角色标注：admin=管理员、nurse=护理人员、family=家属

### 1.2 统一响应体

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

错误响应示例：

```json
{ "code": 401, "message": "未登录或令牌已过期", "data": null }
```

### 1.3 错误码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录或令牌无效/过期 |
| 403 | 无权限（角色不允许或访问他人数据） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 1.4 分页约定

分页接口请求参数：`page`（页码，从 1 开始）、`size`（每页条数，默认 10）。
分页响应 data 结构：

```json
{
  "records": [],
  "total": 100,
  "current": 1,
  "size": 10
}
```

### 1.5 状态枚举

| 枚举 | 值 |
|------|-----|
| 用户状态 | 1 启用 / 0 禁用 |
| 角色 | admin / nurse / family |
| 老人状态 | 1 在住 / 0 已退住 |
| 用药任务状态 | 0 待执行 / 1 已执行 / 2 已逾期 |
| 探访状态 | 0 待审核 / 1 已通过 / 2 已驳回 / 3 已完成 |
| 留言状态 | 0 未回复 / 1 已回复 |

---

## 2. 认证接口

### 2.1 登录

`POST /auth/login`　权限：无

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码（明文传输，登录使用 HTTPS 部署） |

响应 data：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": { "id": 1, "username": "admin", "realName": "系统管理员", "role": "admin" }
}
```

### 2.2 获取当前用户信息

`GET /auth/me`　权限：登录用户

响应 data：`{ "id": 1, "username": "admin", "realName": "系统管理员", "role": "admin", "phone": "13800000000" }`

### 2.3 修改密码

`PUT /auth/password`　权限：登录用户

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldPassword | string | 是 | 原密码 |
| newPassword | string | 是 | 新密码（6~20 位） |

---

## 3. 用户管理（admin）

### 3.1 用户分页列表

`GET /users?page=1&size=10&username=&role=`　权限：admin

| 参数 | 类型 | 说明 |
|------|------|------|
| username | string | 可选，用户名模糊查询 |
| role | string | 可选，角色筛选 |

### 3.2 新增用户

`POST /users`　权限：admin

```json
{ "username": "nurse01", "password": "123456", "realName": "王护士", "role": "nurse", "phone": "13800000001" }
```

### 3.3 修改用户

`PUT /users/{id}`　权限：admin

```json
{ "realName": "王护士", "role": "nurse", "phone": "13800000001", "status": 1 }
```

### 3.4 删除用户

`DELETE /users/{id}`　权限：admin（不能删除自己）

### 3.5 重置密码

`PUT /users/{id}/password`　权限：admin　将密码重置为 `123456`

---

## 4. 操作日志（admin）

### 4.1 日志分页查询

`GET /logs?page=1&size=10&username=&startTime=&endTime=`　权限：admin

| 参数 | 类型 | 说明 |
|------|------|------|
| username | string | 可选，操作人筛选 |
| startTime / endTime | string | 可选，时间段筛选（yyyy-MM-dd HH:mm:ss） |

响应 data（分页）：records 中每条为

```json
{
  "id": 1, "username": "admin", "operation": "新增老人", "method": "POST /api/elders",
  "params": "{\"name\":\"张三\"}", "ip": "127.0.0.1", "createTime": "2026-08-16 10:00:00"
}
```

---

## 5. 老人档案（admin、nurse）

### 5.1 老人分页列表

`GET /elders?page=1&size=10&name=&roomNo=&status=`　权限：admin、nurse

| 参数 | 类型 | 说明 |
|------|------|------|
| name | string | 可选，姓名模糊查询 |
| roomNo | string | 可选，房间号 |
| status | int | 可选，1 在住 / 0 已退住 |

### 5.2 老人详情

`GET /elders/{id}`　权限：admin、nurse、family（仅关联老人）

### 5.3 家属查看自己关联的老人

`GET /elders/my`　权限：family

说明：家属账号登录后需先获取自己关联的老人 ID，才能访问健康档案、体征、探访、留言等业务；若账号未关联老人返回 400。

响应 data：老人档案对象（含 name、roomNo、bedNo、healthSummary 等），字段同老人详情。

### 5.4 新增老人

`POST /elders`　权限：admin、nurse

```json
{
  "name": "张三", "gender": 1, "birthday": "1940-05-12", "idCard": "420000194005120011",
  "phone": "13900000000", "emergencyContact": "张小明", "emergencyPhone": "13900000001",
  "healthSummary": "高血压，需低盐饮食", "familyId": 3
}
```

### 5.5 修改老人

`PUT /elders/{id}`　权限：admin、nurse（参数同新增，不含 idCard 时可保持不变）

### 5.6 删除老人

`DELETE /elders/{id}`　权限：admin　仅允许删除无业务数据关联的记录

### 5.7 入住登记

`POST /elders/{id}/checkin`　权限：admin、nurse

```json
{ "roomNo": "201", "bedNo": "A", "checkinTime": "2026-08-01" }
```

### 5.8 退住登记

`POST /elders/{id}/checkout`　权限：admin、nurse

```json
{ "checkoutTime": "2026-08-20" }
```

### 5.9 老人名单导出

`GET /elders/export?name=&status=`　权限：admin、nurse　响应为 .xlsx 文件流，前端以 Blob 下载

---

## 6. 护理记录（admin、nurse）

### 6.1 护理记录分页列表

`GET /care-records?page=1&size=10&elderId=&planName=&startTime=&endTime=`　权限：admin、nurse、family（仅关联老人）

### 6.2 新增护理记录

`POST /care-records`　权限：admin、nurse

```json
{ "elderId": 1, "planName": "翻身", "planFrequency": "每2小时一次", "careContent": "协助翻身并检查皮肤状况", "remark": "" }
```

### 6.3 修改护理记录

`PUT /care-records/{id}`　权限：admin、nurse（仅当天记录）

### 6.4 删除护理记录

`DELETE /care-records/{id}`　权限：admin、nurse（仅当天记录）

### 6.5 护理记录导出

`GET /care-records/export?elderId=&startTime=&endTime=`　权限：admin、nurse　返回 .xlsx

---

## 7. 健康管理

### 7.1 体征记录（admin、nurse；family 限关联老人）

`GET /health-records?page=1&size=10&elderId=&startTime=&endTime=`　分页列表

`POST /health-records`　新增，权限：admin、nurse

```json
{ "elderId": 1, "bloodPressure": "128/82", "heartRate": 76, "temperature": 36.5, "bloodSugar": 6.1, "remark": "晨起测量" }
```

`PUT /health-records/{id}`　修改，权限：admin、nurse

`DELETE /health-records/{id}`　删除，权限：admin、nurse

### 7.2 健康档案

`GET /elders/{id}/health`　权限：admin、nurse、family（家属仅限关联老人）
说明：健康档案存储在 `elder_info.health_summary` 字段，包含病史、过敏史、用药禁忌等概要。
响应 data：`{ "elderId": 1, "healthSummary": "高血压病史5年；青霉素过敏；忌服阿司匹林类" }`

`PUT /elders/{id}/health`　修改，权限：admin、nurse

```json
{ "healthSummary": "高血压病史5年；青霉素过敏；忌服阿司匹林类" }
```

### 7.3 用药计划（admin、nurse）

`POST /medicine-plans`　新增（录入后即为当天生成任务）　权限：nurse

```json
{ "elderId": 1, "medicineName": "硝苯地平缓释片", "dosage": "每次1片", "times": ["08:00", "14:00"] }
```

`GET /medicine-plans?elderId=`　在用药计划列表　权限：admin、nurse

`PUT /medicine-plans/{id}/disable`　停用计划　权限：nurse
说明：停用 = 删除该老人 `plan_date >= 今天` 的任务行（历史行保留作为档案），次日查询不再自动延续。

### 7.4 用药任务（admin、nurse）

`GET /medicine-tasks?date=2026-08-16&elderId=&status=`　当日/指定日任务列表
说明：查询时按需生成当日任务并执行逾期扫描（见《需求分析文档》4.2 节）。

`PUT /medicine-tasks/{id}/complete`　确认执行　权限：nurse
响应 data：`{ "id": 1, "status": 1, "confirmTime": "2026-08-16 08:05:00" }`

`GET /medicine-tasks/overdue?elderId=`　逾期任务列表（供提醒）　权限：admin、nurse

---

## 8. 探访管理

### 8.1 提交探访预约

`POST /visits`　权限：family（自动绑定当前用户为 familyId）

```json
{ "elderId": 1, "visitDate": "2026-08-20", "visitTime": "上午 9:00-11:00", "persons": 2, "remark": "带水果探望" }
```

### 8.2 预约列表

`GET /visits?page=1&size=10&status=&elderId=`　权限：全部
说明：family 角色仅返回自己提交的预约；admin、nurse 返回全部。

响应 data（分页）records 示例：

```json
{ "id": 1, "elderId": 1, "elderName": "张三", "familyId": 3, "familyName": "张小明",
  "visitDate": "2026-08-20", "visitTime": "上午 9:00-11:00", "persons": 2,
  "status": 0, "auditRemark": "", "createTime": "2026-08-16 09:00:00" }
```

### 8.3 预约审核

`PUT /visits/{id}/audit`　权限：admin、nurse

```json
{ "status": 1, "auditRemark": "同意探望" }
```

status 取值：1 通过 / 2 驳回（驳回时 auditRemark 必填）。

### 8.4 标记完成

`PUT /visits/{id}/finish`　权限：admin、nurse

---

## 9. 留言管理

### 9.1 发表留言

`POST /messages`　权限：family

```json
{ "elderId": 1, "content": "父亲最近睡得好吗？" }
```

### 9.2 留言列表

`GET /messages?page=1&size=10&status=&elderId=`　权限：全部
family 仅返回关联老人的留言；admin、nurse 返回全部。

### 9.3 回复留言

`PUT /messages/{id}/reply`　权限：admin、nurse

```json
{ "reply": "睡得不错，请放心" }
```

---

## 10. 统计接口（admin）

### 10.1 看板总览

`GET /stats/overview`

```json
{
  "elderTotal": 86, "inHouse": 78, "roomTotal": 100, "checkInRate": 78.0,
  "todayCareCount": 42, "todayVisitCount": 5, "overdueTaskCount": 2
}
```

### 10.2 老人年龄分布

`GET /stats/age-distribution`

```json
{ "categories": ["60-69", "70-79", "80-89", "90+"], "counts": [12, 34, 36, 4] }
```

### 10.3 近 30 天护理/探访趋势

`GET /stats/activity-trend?days=30`

```json
{ "dates": ["2026-07-18", "..."], "careCounts": [38, 41, ...], "visitCounts": [2, 3, ...] }
```

### 10.4 老人体征趋势

`GET /stats/elder/{id}/health-trend?days=30&metric=bloodPressure`　权限：admin、nurse、family（家属仅限关联老人）

```json
{ "dates": ["2026-07-18"], "values": ["128/82", "130/85"] }
```

metric 取值：bloodPressure / heartRate / temperature / bloodSugar（非血压类型 values 为数值）。
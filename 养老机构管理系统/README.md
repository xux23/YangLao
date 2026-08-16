# 基于 Spring Boot 的养老机构管理系统

毕业设计项目：面向中小型养老机构的信息化管理系统，覆盖**老人档案、护理管理、健康管理、探访管理、系统管理**五大功能模块，支持管理员、护理人员、家属三类角色协同使用。

## 一、技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2、MyBatis-Plus、MySQL 8.0、JWT（jjwt）、EasyExcel、Lombok、Maven |
| 前端 | Vue 3、Vite、Element Plus、Vue Router、Pinia、Axios、ECharts |
| 环境 | JDK 17+、Node 16+ |

## 二、项目结构

```
养老机构管理系统/
├── sql/init.sql              # 数据库初始化脚本（含演示数据，可直接导入）
├── backend/                  # 后端 Spring Boot 工程（端口 8080）
│   └── src/main/java/com/eldercare/
│       ├── common/           # 统一响应 Result、业务异常、全局异常处理
│       ├── config/           # Web 配置（拦截器、跨域）、MyBatis-Plus 分页
│       ├── security/         # JwtUtil、JwtInterceptor、@RequireRole、UserContext
│       ├── aspect/           # @OperLog + LogAspect（AOP 操作日志）
│       ├── entity/ mapper/ dto/ vo/      # 实体、数据访问、请求参数、视图对象
│       ├── service/ impl/    # 业务接口与实现
│       └── controller/       # 10 个 REST 控制器
└── frontend/                 # 前端 Vue 3 工程（端口 8081）
    └── src/
        ├── api/              # 接口封装（每个模块一个文件）
        ├── utils/request.js  # Axios 实例（自动附加令牌、统一错误处理）
        ├── router/           # 路由与登录守卫
        ├── store/user.js     # Pinia 登录态
        └── views/            # 登录、布局、看板及各业务页面
```

## 三、快速启动

### 1. 初始化数据库

```bash
# 需要本机已安装 MySQL 8.0，root 密码如与 yml 不一致请修改
mysql -u root -p < sql/init.sql
```

脚本会创建数据库 `elder_care`、8 张业务表，并写入演示数据。演示数据中的日期均相对系统当前日期生成（CURDATE），因此任何时候启动都能演示"今日用药任务、趋势图、逾期提醒"等效果。

### 2. 启动后端

```bash
cd backend
# 用 IDEA 打开并运行 EldercareApplication，或命令行：
mvn spring-boot:run
```

- 默认端口 8080，默认数据库账号 `root/123456`（在 `application.yml` 中修改）
- 启动前请确认已安装 JDK 17 和 Maven

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 开发服务器 `http://localhost:8081`，`/api` 请求已代理到后端 8080
- 也可以执行 `npm run build` 后将 `dist` 静态部署

## 四、演示账号（密码均为 123456）

| 账号 | 角色 | 功能范围 |
|------|------|----------|
| admin | 管理员 | 全部功能：看板、用户管理、操作日志、业务数据 |
| nurse01 / nurse02 | 护理人员 | 老人档案、护理、健康维护、用药、探访审核、留言回复 |
| family01 / family02 | 家属 | 查看关联老人健康数据与护理记录、探访预约、留言（只读） |

- family01 关联老人"张三"，family02 关联老人"李秀英"

## 五、核心功能演示路径（答辩建议）

1. **登录鉴权**：admin 登录后看板展示入住率、年龄分布饼图、近 30 天护理/探访趋势；用 family01 登录只能看到自己的功能菜单（菜单按角色动态生成）。
2. **用药任务闭环**（重点）：nurse01 登录 → 用药任务 → 查询今日任务（生成任务 + 逾期扫描）→ 确认执行；切日期查看昨日/前天任务，前天未执行的任务自动变"已逾期"，逾期列表可补服确认；用药计划页可停用方案。
3. **AOP 操作日志**：任意做一次新增/修改操作，用 admin 登录 → 操作日志查看自动记录（含操作人、接口、参数、IP）。
4. **Excel 导出**：老人档案列表 → 名单导出；护理记录 → 记录导出。
5. **家属视角**：family01 登录 → 提交探访预约 → nurse01 登录 → 探访审核通过/驳回（驳回需填原因）→ family01 查看状态。
6. **数据隔离**：family01 通过接口直接访问 family02 老人的 ID 会返回 403（服务端校验 elder_info.family_id）。

## 六、对设计文档的少量补充说明

代码严格按《需求分析文档》《系统设计文档》《API接口文档》实现，以下为落到代码时的补充/微调：

1. **medicine_plan 表增加 `disabled` 字段**（0正常/1已停用）：文档中"停用计划 = 删除未来任务行、次日不再自动延续"与"任务按需生成"两个设计点之间存在冲突——若只删行，次日查询时按需生成逻辑会从历史行重新复制出新任务。增加该标志位后：停用时删除今天及以后的任务行，并把历史行标记为停用，生成逻辑见到停用行即不再延续。答辩可主动讲解这个设计取舍（新增字段也符合文档"小数据用字段"的表设计原则）。
2. **新增接口 `GET /api/elders/my`**：家属登录后需要先知道自己关联的老人 ID，才能访问健康/探访/留言功能（原文档未提供此入口）。
3. **体征趋势接口 `GET /api/stats/elder/{id}/health-trend` 开放给家属**：需求文档 FR-4-3 要求家属也能查看体征表格和趋势图，故该接口权限为 admin/nurse/family，家属角色在服务端校验老人归属。
4. **看板"客房数"指标**：系统未单独建房间表，故以"在住老人占用的去重房间数"统计（`COUNT(DISTINCT room_no)`），住宿率 = 在住数 / 老人总数。
5. 护理记录的修改/删除限制为"仅当天记录"（需求 FR-3-3 的中等优先级约束）。

## 七、常见问题

| 问题 | 处理 |
|------|------|
| 登录提示密码错误 | 确认导入的是 `sql/init.sql` 中的账号；密码均为 123456 |
| 后端启动报数据库连接失败 | 检查 MySQL 是否启动、`application.yml` 中账号密码 |
| 前端请求 404/502 | 先启动后端；代理配置见 `frontend/vite.config.js` |
| createTime 返回格式异常 | 实体字段已配置 @JsonFormat，确认未改过 |
| 护理记录删除报"仅当天" | 接口限制当天可改删，属业务约束（演示时新增一条当天记录验证） |

## 八、默认配置速查

- 后端端口：8080；前端端口：8081
- JWT 密钥与有效期：`backend/src/main/resources/application.yml` 的 `app.jwt` 配置（默认 24 小时）
- 数据库连接：`backend/src/main/resources/application.yml` 的 `spring.datasource`
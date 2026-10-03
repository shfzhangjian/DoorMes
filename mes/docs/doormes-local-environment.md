# DoorMes 本地 MES 开发环境

## 工作目录与访问

- 新的实际 MES 副本：`E:\doorMES_v1\LUCK-MES-HC-0`。
- 新前端：`http://localhost:5180/`；同一局域网可使用 `http://192.168.50.193:5180/`。
- 前端 `/admin-api` 代理本机 `http://127.0.0.1:48082`，后端只监听回环地址；局域网客户端通过前端代理访问 API。
- 旧绘图原型的 5173 与新前端 5180 是两套不同入口。门窗业务和绘图引擎将按集成计划迁入新 MES，不表示已经全部迁入。
- 实际 UI 验收由人工完成；自动验证不操作浏览器。

## 数据库、缓存与文件隔离

- 原库 `qdaq0923` 保持不变。完整克隆至 `doormes_local`，复制前后逐表精确行数相等：1008 张基表、2 个视图、3 个存储过程；原库没有 SQL 定时事件或触发器。
- 从新库删除了 314 张 `bak_` 前缀备份表。删除前已确认没有活动 Java/MyBatis、视图、存储过程或外键引用；其他 `tmp`、历史表和正常业务表保留。清理后为 694 张基表、2 个视图。
- 可恢复导出与核对证据：`runtime-local\db-clone\20261002-075035`；备份表删除清单：`runtime-local\backup-table-cleanup\cleanup-manifest.json`。导出含业务数据，禁止提交 Git 或公开分享。
- 使用匹配服务器的 MySQL 5.7.28 导出工具。旧 5.6 客户端不能正确导出 5.7 生成列；旧失败导出和诊断证据亦保留，恢复使用 `source-data-mysql57.sql`。
- 应用专用账号 `doormes_app` 只授权新库，已验证不能访问原库。随机密码仅保存在忽略 Git 的 `runtime-local\db-credentials.json`；启动脚本读取到子进程环境，不在命令行输出。不要公开该文件。
- Redis：专用回环地址 `127.0.0.1:6381`，数据库 0；不改动原 6379 服务。专用实例不开放 LAN，使用独立随机密码，保存在忽略 Git 的 `runtime-local\redis\redis-credentials.json`。
- 新附件目录：`runtime-local\uploads`。旧附件记录保留，但旧本地目录/FTP/SFTP/S3 操作由 `doormes` 护栏拒绝，不会自动拷贝或删除原附件。
- 当前文件公开 origin 配置为 `http://192.168.50.193:5180`。主机 IP 变化时，同步修改 `application-doormes.yaml` 的 `doormes.local.file-origin` 与新库的专用 `infra_file_config.config.domain`；不要修改历史文件配置来恢复外部访问。

## 启动说明

要求 Node 22+（本机24.16，绘图共享校验需要 Node）、pnpm 10.28.1、Java 17、PowerShell 7、MySQL 5.7。不要使用当前系统默认的 Java 8。

1. 保持本机 MySQL 服务运行。`doormes_local` 已初始化，不要重复运行克隆或初始化脚本覆盖它。
2. 启动专用 Redis（独立目录与端口，不是原服务）：

   ```powershell
   & 'E:\Novi\Redis-x64-5.0.14.1\redis-server.exe' 'E:\doorMES_v1\LUCK-MES-HC-0\runtime-local\redis\redis.local.conf'
   ```

3. 在新副本根目录启动后端：

   ```powershell
   pwsh -NoProfile -File .\scripts\start-doormes-backend.ps1
   ```

4. 在 `frontend` 目录启动前端：

   ```powershell
   pnpm dev:antd
   ```

前端隔离参数保存在 `frontend\apps\web-antd\.env.development.local`：端口 5180、API 48082、浏览器存储 namespace 为 `doormes_factory_v1`、清空自动填入账号密码。登录企业选择“门窗工厂”，使用下述新账号；原系统账号不作为新门窗系统入口，不使用原型模拟角色代替真实权限。

后端开发配置加载 `local,doormes`。`doormes` 覆盖专用数据库账号、Redis、日志目录、外发限制；启动脚本传入专用账号环境变量，缺失会启动失败，不会自动回退到原库账号。配置文件修改后需重新打包或通过明确外部配置启动，不以源文件修改等同运行服务已更新。

## 门窗工厂账号、组织及菜单（已初始化）

用户确认默认企业名“门窗工厂”和管理员账号 `admin`。2026-10-02 仅在新库完成以下调整：

- 清空复制过来的后台用户、组织、岗位及用户关联，初始化 7 个账号、8 个组织节点（工厂根节点及系统管理、销售、设计研发、工艺技术、采购、生产、审核与质量部门）、7 个岗位及 7 个有效角色。
- 新用户、部门、岗位使用 1001 起的新 ID，不重用历史人员 ID，历史订单不会被误挂到新测试人员。旧角色逻辑归档，保留原 `super_admin` 基础管理机制。
- 原 1350 条有效菜单记录全部保留；新增 10 条菜单记录。根菜单为“门窗业务”“基础管理”“备份菜单”。用户、角色、菜单、组织、岗位、字典、审计、租户、文件、OAuth、地区及配置等基础功能保留；其他原行业菜单移入备份，不自动启用原本已禁用的菜单或外部集成。
- 管理员可见三个根菜单。业务测试账号仅可见门窗业务及已分配入口，没有旧行业和基础管理操作权限。业务选人所需的原简单用户查询仍保留，不能把菜单隔离等同所有数据的生产级权限隔离。
- 仅企业 ID 1“门窗工厂”启用。其他复制租户禁用但记录保留，域名绑定 `localhost`、`127.0.0.1`、`192.168.50.193`。清理了新库旧后台登录令牌并重置专用 6381 缓存；未操作原 6379。
- 登录页、网页标题、Logo、默认首页已切换为 DoorMes 门窗工厂。后端启动脚本关闭原本会输出登录请求体的本地 INFO 跟踪，使用源文件中的新 DoorMes 启动标识。

| 账号 | 角色 | 门窗菜单（均有工厂工作台） |
| --- | --- | --- |
| admin | 系统管理员 | 全部门窗菜单、基础管理、备份菜单 |
| sales | 销售 | 订单与设计需求、产品与组件目录、图纸中心 |
| design | 设计研发 | 订单与设计需求、设计研发、产品与组件目录、设计与生产变更、图纸中心 |
| process | 工艺 | 设计研发、工艺与采购确认、生产协同、设计与生产变更、图纸中心 |
| purchase | 采购 | 产品与组件目录、工艺与采购确认、图纸中心 |
| production | 生产 | 生产协同、设计与生产变更、图纸中心 |
| reviewer | 审核 | 订单与设计需求、设计研发、工艺与采购确认、设计与生产变更、图纸中心 |

各账号使用独立随机 16 位密码，存储 BCrypt 哈希。明文仅在本地 `runtime-local\factory-bootstrap\accounts.local.json`，权限限制为本机 LENOVO、SYSTEM 和初始化执行账户，不在文档或日志中列出，`runtime-local/` 在忽略规则内。首次人工使用请在个人账户设置中修改密码。本副本未初始化新 Git 仓库，不应将整个目录直接打包上传。

变更前 14 张基础表的恢复备份为 `runtime-local\factory-bootstrap\20261002-091519\before-factory.sql`；`complete.json` 记录 SHA-256、路径和初始化结果。备份含旧用户资料和认证信息，同样不能公开。若需恢复，先停新后端，仅将该备份导入 `doormes_local`，再清理专用 6381 缓存；不得导入原库。恢复会撤销此次基础初始化，执行前另存当前新库备份。

`scripts\initialize-doormes-factory.ps1` 是一次性脚本：默认只预览，明确 `-Apply` 才修改，已有完成标记时拒绝重跑；后续通过基础管理维护账号、组织和菜单。MySQL 管道显式使用 UTF-8，避免 Windows GB2312 导致中文事务失败。正常维护不要再次清空用户。

业务角色目前使用全企业测试读取范围；需求更新/提交校验本人创建（管理员例外），图纸保存校验领用研发人员/管理员。项目隔离、审批分权等生产级权限随 M1–M4 实际业务落地。定制需求、研发领用、明细绘图及表面/玻璃设计选型目录已接通，完整制造目录、BOM、审核及生产变更不能据此认为已经实现。

## 安全测试边界

- 关闭 Quartz、Flowable 异步/自动建表、OA、第三方社交登录和 Spring Boot Admin 自动注册；数据库中的原短信渠道/模板、邮件模板只在新库禁用。
- `doormes` 专用代码阻断 Spring RestTemplate 外发、系统短信/邮件实际发送、历史文件客户端及越界文件路径。不是通用网络沙箱，不能据此宣称所有原生 HTTP/JDBC 出网都已隔离。
- 历史流程、原生外链及第三方报表/BI/数据源需要受控迁移。初始联测只检查密码登录、权限菜单和普通本地 MES 页面，不执行历史审批、通知、外部接口或原设备任务。
- Druid 监控页关闭；Actuator 仅 health/info。开发服务仅供可信局域网人工测试，不是生产部署。

## 计划与验收

2026-10-02 已重载新前后端：后端健康检查为 `UP`，5180 标题为“门窗工厂 · DoorMes”。本机及局域网代理分别验证全部 7 个账号真实密码登录、角色和精确菜单集合；管理员读取 7 个新用户及 8 个组织节点，六个业务角色访问受保护的完整用户详情均返回 `code=403`，测试令牌均退出。证据为 `runtime-local\factory-bootstrap\verification-localhost.json`、`verification-192.168.50.193.json`，报告无密码/令牌。

新增导航/首页共 7 项单元测试通过；5 个修改 Vue 页面完成 SFC 编译检查，新页面和 Logo 经 Vite HTTP 转换检查返回 200；4 个 PowerShell 文件语法检查通过。未使用浏览器，视觉与人工操作仍待用户验收。可用 `scripts\verify-doormes-factory.ps1` 重复验证，密码文件保留的是初始化密码；人工改密后需同步本地测试凭据或采用新的安全验证方式。

见 `doormes-integration-plan.md`。M0 自动验证通过；M1 定制需求、研发领用、明细绘图、图纸版本保存/加载、表面/玻璃目录映射及本地模型/纹理资源已接通。下一步后端设计复用与企业标准设计引用；设计 BOM、工艺/采购反馈及生产变更仍按后续里程碑改造，不把原 HC 行业功能当作门窗业务已经完成。

## M1：定制设计需求（已接通，待人工界面验收）

销售登录后进入“门窗业务 → 订单与设计需求”，新建定制需求，填写需求号、客户和门窗明细。草稿可暂缺材料信息；提交研发前，每项必须有型材、玻璃、五金、颜色和需求日期。输入尺寸单位 mm，范围 1–50000，数量 1–1000，明细最多 100 项。需求号为 1–40 位字母/数字/点/下划线/短横线，在企业内唯一；门窗短编号在需求内不区分大小写唯一，内部 UUID 由服务端生成。

- 保存/修订均记录原因并生成新 JSON，历史版本只读。未领用需求修订返回草稿，需要重新提交。
- 研发账号进入“设计研发”查看待领用列表，领用后基线锁定。当前已接入明细的真实绘图入口，使用方式见下方“明细绘图与后端图纸版本”；需求录入与图纸设计仍是两个独立步骤。
- “引用标准设计”暂不启用，必须在已发布标准图纸目录接入后引用确定版本，不允许凭名称伪造标准件关系。
- “订单 / 项目名称”当前是需求关联说明，不是已有生产订单的外键。实际订单关联、设计发布、审核与生产执行继续开发。
- 数据源仅新库 `doormes_local`，本轮新增两张表后为 696 张基表。V001 只运行一次：`scripts/migrate-doormes-requirements.ps1 -Apply`，完成标记在 `runtime-local/design-data/schema-v001.json`，不可再次运行初始化脚本。
- 后端需求文件根目录：`E:\doorMES_v1\LUCK-MES-HC-0\runtime-local\design-data\requirements`，路径为 `tenant-<租户ID>/<内部UUID>/r<修订>.json`。通过 `application-doormes.yaml` 的 `doormes.requirements.root` 配置，修改后需重新打包/启动。禁止链接或目录重定向。
- SQL 保存索引与版本 SHA-256；先写临时文件并原子转成新版本，再提交 SQL；事务回滚移除本次文件。历史文件不得覆盖；文件损坏或身份不符时禁止加载。进程异常遗留而未被 SQL 引用的文件不会作为有效版本公开，但尚无自动孤儿清理任务。

### 通讯格式

前端通过 `5180/admin-api/doormes/requirements`，使用原系统 Bearer 及 `tenant-id`；响应为 `{code,data,msg}`，成功 `code=0`。以下错误是响应中的业务码，不能仅看 HTTP 状态：400 输入错误、403 无权限、404 不属于本企业/版本不存在、409 并发冲突/重复号/基线锁定、501 标准引用暂未接入。

| 方法与路径 | 请求 | 结果 / 权限 |
| --- | --- | --- |
| GET `/page` | `keyword,status,pageNo,pageSize` | 分页 `{list,total}`；需求或设计查询权限 |
| GET `/get` | `id,revision?` | 当前或历史完整 JSON；同上 |
| GET `/versions` | `id` | 修订、动作、操作者、原因、时间列表；同上 |
| POST `/create` | `{expectedRevision:0,changeNote,demand}` | 服务端生成 UUID、R1 草稿；销售新建权限 |
| PUT `/update?id=...` | `{expectedRevision,changeNote,demand}` | 基于当前版本修订；销售修订权限及创建人检查 |
| POST `/submit?id=...` | `{expectedRevision,note}` | 提交研发；销售提交权限及创建人检查 |
| POST `/claim?id=...` | `{expectedRevision,note}` | 领用并绑定真实研发账号；研发领用权限 |

`demand.schemaVersion="doormes-design-demand.v1"`，字段为 `number,customer,project,note,lines[]`；每项 `lines` 为 `id?（服务器生成）,mark,kind="custom",quantity,requirement`，`requirement` 为 `widthMm,heightMm,material,glass,hardware,finish,dueDate（YYYY-MM-DD或草稿空串）,note`。所有文本除可选内部 ID 都传字符串，不传 null。

保存后的文档 `schemaVersion="doormes-design-requirement.v1"`，包含 `id,tenantId,revision,status,createdBy,assignedTo,createdAt,updatedAt,changedBy,action,changeNote,demand`。这些身份、状态、版本与时间由服务端决定，不能用客户端字段改操作者或租户。`expectedRevision` 是并发校验，不是客户端指定新版本；时间为 ISO UTC，页面按本机时区显示。前端类型见 `src/api/doormes/requirement-contract.ts`，Java 契约见 `RequirementModels.java`。

### 验证证据与清理

真实四角色接口场景 `M1-REQ-20261002-102911-82a9f7` 的 27 项断言通过：新建、重新加载、宽度修订、历史版本、重复/并发/权限拒绝、提交/领用、MySQL 元数据和 4 个版本文件校验。8 项新增后端测试及 15 项前端输入/导航/首页测试通过；后端安全护栏另 9 项，0 失败、1 项链接测试因系统权限跳过。门窗模块 `tsconfig.doormes.json` 类型检查通过；全前端检查仍有原复制代码的大量历史类型问题，没有宣称全仓通过。Vue 编译与 5 个新模块 Vite HTTP 转换返回 200。没有使用浏览器。

场景定义在 `tests/scenarios/M1-requirements.v1.json`（SOAP，人工验证留空）。证据和可恢复测试快照归档在 `runtime-local/scenarios/M1-REQ-20261002-102911-82a9f7`，包括 `manifest.json, database-evidence.txt, final-document.json, versions.json, archived-data, cleanup.sql, restore.sql, cleanup.json`。测试需求和四条版本索引已清理，正式需求列表保持空白，文件及元数据可恢复。

重复验证使用 `scripts/verify-doormes-requirements.ps1`；归档清理使用 `scripts/archive-doormes-requirement-test.ps1 -ScenarioDirectory <本次完整目录>`，默认只预览，`-Apply` 才精确清理本次 TEST 记录。不改用户、组织或人工需求。恢复只针对新库：先另存当前新库，核对短号/UUID 无冲突，把归档 JSON 放回原要求目录后再导入对应 `restore.sql`；不得向原库导入。首次运行的清理脚本编号检查发现错误并拒绝执行，修正后成功归档，不曾误删其他数据。

## M1：明细绘图与后端图纸版本（已接通，待人工验收）

实际入口仍为 5180。销售在“订单与设计需求”录入尺寸/材料并提交；研发在“研发需求工作台”领用后，选择状态“研发设计中”，打开需求下的 `C1 · 打开 2D / 3D 图纸`。首次按该明细尺寸/数量建图；重复打开加载同一图纸。工作区全屏，保留模板、构造、独立窗/墙体平面、2D/3D、构件树与属性。保存需填写修订原因，关闭未保存修改时确认；当前/历史图纸可重新加载、下载 JSON。销售/审核仅预览，不能保存，预览调整不写入后端。默认绘图材料不是已经确认的制造选型，需求自由文本在工作区上方独立展示。

引擎源副本在 `frontend/vendor/doormes-engine/packages`，维护来源和差异见 `provenance.json`。旧 `DoorMes` 源码保持原样。浏览器适配层和后端校验器均复用同一套几何/领域命令；运行不需要启动旧5173服务。后端另需本机 Node，校验失败或服务不可用时不保存、不改变旧版本。当前一次需求明细对应一个设计文档，文档内可含多窗/转角组合；不会按数量复制多份几何。

配置位于 `backend/yudao-server/src/main/resources/application-doormes.yaml` 的 `doormes.drawings`：

- `root`：`E:/doorMES_v1/LUCK-MES-HC-0/runtime-local/design-data/drawings`。版本文件格式 `tenant-N/<服务器UUID>/rN.json`，不能由客户端指定路径。
- `node`：`C:/Program Files/nodejs/node.exe`；`validator`：`frontend/vendor/doormes-engine/dist/validator.mjs` 的绝对路径。
- `temporary-root`：`runtime-local/design-data/validation-temp`，仅用于共享几何校验的临时JSON；完成后清理。
- 源码变化后先在 `frontend/vendor/doormes-engine` 运行 `node build.mjs`。生成浏览器ES模块和独立Node校验器；构建优先使用本包依赖，当前可只读复用旧原型已安装依赖缓存。服务配置更改需重新打包部署，不以改源文件代替运行生效。
- V002 已在新库应用，完成标记 `schema-v002.json`，不要重跑迁移。新增 `dm_drawing`、`dm_drawing_version`，当前新库698张基表；只有研发角色增加 `doormes:design:drawing-save`。

### 图纸接口与 JSON

前缀 `/admin-api/doormes/drawings`；继续使用真实 Bearer、tenant-id 和 `{code,data,msg}`。SQL与JSON身份/版本均由服务器决定。

| 方法 | 参数 | 含义 |
| --- | --- | --- |
| GET `/find` | `requirementId,lineId` | 按当前需求稳定明细找图纸，无图纸返回null |
| POST `/open` | `{requirementId,lineId,expectedRequirementRevision}` | 领用研发/管理员按需求创建或幂等打开图纸 |
| GET `/get` | `id,revision?` | 当前或不可覆盖的历史版本 |
| GET `/versions` | `id` | 修订原因、人员及时间 |
| PUT `/save?id=...` | `{expectedRevision,changeNote,document}` | 共享几何校验后保存新修订；并发冲突409 |

图纸快照 `schemaVersion="doormes-drawing-snapshot.v1"`，字段 `id,tenantId,requirementId,requirementRevision,lineId,revision,status,createdBy,changedBy,createdAt,updatedAt,changeNote,document`。`document` 为原正式 `doormes-domain.v1`：`designId,revision,windows,assemblies,drawingTextLabels,factoryDrawingElementOptions,factoryDrawingAnnotationLayouts,wallPlan`；完整几何/材料/连接结构沿用 `vendor/doormes-engine/packages/contracts/src/index.ts`，不复用旧 `cn-door-window-design.v2` 的不同格式。外层 revision 是业务图纸版本；内层 revision 是绘图引擎命令计数，不能混用。

共享校验最多5MB JSON、两项并行、等待3秒、执行15秒；坏几何/内部ID400，越权403，缺失404，版本冲突/损坏文件409，校验服务未部署/超时503。临时文件与快照目录拒绝链接/重定向。数据库事务失败会删除本次新文件；旧版保留。异常中断导致的孤立未索引文件不会作为有效版本读取，完整故障恢复机制后续完善。

### 验证与待办

`M1-DRAW-20261002-112225-7cb700` 的23项真实 HTTP/SQL/文件断言通过；5项新增后端测试使用H2、临时文件和真实Node几何校验，3项引擎适配/往返测试通过；门窗Vue模块与引擎TypeScript检查通过，页面/接口/脚本HTTP读取200。没有使用浏览器，真实2D/3D操作与视觉布局待人工确认。该次启动PID56740为历史记录；2026-10-02材料目录部署后运行PID51872（启动12:40:27，完成12:42:37），健康UP。PID只作记录，不作为今后启动/停止依据。

测试需求和关联图纸已归档清理，证据在 `runtime-local/scenarios/M1-DRAW-20261002-112225-7cb700`：完整需求3版/图纸2版JSON、数据库断言、恢复SQL及清理记录均保留。复验用 `verify-doormes-drawings.ps1`；通用归档清理脚本现在会先备份并清理该TEST需求的关联图纸，仍不动人工记录和账号组织。

待接入：完整制造/产品目录、后端标准模板复用与订单标准设计引用、原型工厂图预览/打印入口。模型/纹理资产网关见下节。已迁入的本机“我的模板”按企业/账号独立保存，不等于受审核的企业标准目录。图纸当前都是设计草稿；设计BOM、正式发布、版本差异、审核及生产变更仍在M2–M4。

## M1：材料型号、版本及绘图映射（已接通，待人工验收）

“门窗业务 → 产品与组件目录”已替换成真实材料目录页面。当前支持型材表面与玻璃：型号、名称、规格、用户备注、颜色；玻璃还有厚度及适用型材系统。金属度、粗糙度和不透明度收在高级外观参数中。研发/采购可新建和修订，销售只读；管理员目前发布设计选型。发布状态不等于制造批准：快照明确 `scope="design-only", productionReady=false`；截面、孔槽、切割规则及五金模型未因此变成正式目录，后续审核分权在M2实现。

目录R1草稿，发布为R2；修订为R3草稿时，画布仍可选原R2，再发布为R4。已发布JSON从不覆盖；型号和分类创建后不修改，另一型号应新建。每次保存/发布填写说明，历史版只读；重复型号和过期 `expectedRevision` 返回409，输入保留。

研发图纸顶部“材料选型”：先选窗编号，再选室外表面、室内表面或玻璃，选择已发布型号及明确版本后应用，填写修订说明保存图纸。表面选型同时应用到框、扇、挺和飞挺的对应面，不改另一面；玻璃选型在同一个可撤销编辑中修改业务型号、玻璃厚度和外观。需求自由文本仍是需求基线，不冒充已选型号。

图纸原生材质字段保存 `appearanceId="MES-CATALOG:<目录UUID>", appearanceVersion="<目录修订>", finishCode=<业务型号>`，以及完整外观值；玻璃另存原生 `defaultGlassSelection` 的目录ID/版本、名称、型号、规格、适用系统与厚度。不新增第二套图纸几何格式。后端对精确已发布版本及租户校验，不查“最新”替换旧选型，且在几何正规化前后检查引用；篡改引用参数、使用草稿目录或玻璃物理厚度不符均拒绝保存。无MES前缀的原生自定义材质仍是设计草稿，不是已受控制造选型。

### 目录通讯与存储

前缀 `/admin-api/doormes/catalog`，真实Bearer/tenant-id及 `{code,data,msg}`：

| 方法 | 参数 / 内容 | 含义 |
| --- | --- | --- |
| GET `/page` | `category?,keyword?,published?,pageNo,pageSize` | 当前目录或已发布版本分页 `{list,total}`，最多100项/页 |
| GET `/get` | `id,revision?` | 当前/精确历史版本 |
| GET `/versions` | `id` | 修订、状态、说明、人员及时间 |
| POST `/create` | `{expectedRevision:0,changeNote,item}` | 新型号R1草稿，目录维护权限 |
| PUT `/revise?id=...` | `{expectedRevision,changeNote,item}` | 新草稿，旧已发布版仍可选 |
| POST `/publish?id=...` | `{expectedRevision,note}` | 新设计选型发布版本，目录发布权限 |

`item`字段：`category="finish"|"glass",code,name,specification,note,materialFamily="metal"|"glass",baseColor="#RRGGBB",metalness,roughness,opacity,thicknessMm,compatibleProfileSystemIds[]`；V004后增加可选 `textureSetId,textureContentHash,textureRepeatX,textureRepeatY`。未选择纹理时ID/哈希为空；重复值支持0.001–1000的小数。表面厚度为null，玻璃厚度必须有效，适用系统不可为空；三个外观数值0–1，型号1–60位字母/数字/点/下划线/短横线，本企业内唯一。

服务端快照 `schemaVersion="doormes-material-catalog.v1"`，字段 `id,tenantId,revision,status,changedBy,updatedAt,changeNote,item,data`；`data`含规范化原生appearance、玻璃glassSelection（仅玻璃）、scope与productionReady。身份和发布状态由服务器决定。SQL为 `dm_material_catalog` / `dm_material_catalog_version`，V003已应用；现在新库700张基表。目录版本JSON根目录 `runtime-local/design-data/catalog`，配置 `doormes.catalog.root`，按 `tenant-N/UUID/rN.json` 保存，原子写入、SHA-256与事务回滚规则同图纸。不得重复运行V003迁移。

### 人工示例与验证

已通过真实管理员接口建立5种 `[示例]` 材料：`DEMO-FIN-RAL7016 / RAL9016 / RAL9005`（深灰/白/黑色彩参考），`DEMO-GL-CLEAR-24 / GREEN-27`（24mm透明/27mm浅绿，适用AL70）。全部为设计示例，非供应商确认型号。脚本 `scripts/initialize-doormes-catalog.ps1` 默认预览、`-Apply`仅补缺，不覆盖现有条目；遇人工修改或草稿停止，正常维护请用页面。初始化无密报告 `runtime-local/design-data/catalog-examples-20261002-124834.json`。

场景 `M1-CAT-20261002-124444-296801` 的31项局域网真实HTTP/MySQL/版本文件检查通过，包括权限、重复/并发拒绝、内外双色、玻璃业务/物理/外观联动、更新不影响历史、篡改/草稿拒绝以及6个目录JSON哈希。后端27项测试无失败（9项安全测试中的1项链接权限跳过），共享引擎6项测试通过，门窗Vue模块和引擎TypeScript检查通过，目录/绘图页面及接口模块HTTP转换200。未使用浏览器，人工验证记录留空；没有宣称全仓类型检查、浏览器交互或生产环境已验收。

SOAP场景在 `tests/scenarios/M1-catalog.v1.json`，证据在 `runtime-local/scenarios/M1-CAT-20261002-124444-296801`。仅本次测试需求、图纸和材料已精确清理，完整JSON、SQL恢复文件和两份清理记录均保留。复验用 `verify-doormes-catalog.ps1`，先用 `archive-doormes-requirement-test.ps1` 归档关联测试图纸，再用 `archive-doormes-catalog-test.ps1` 清理测试目录；两者默认预览、`-Apply`才执行，有任何保留历史图纸引用则拒绝删除目录。恢复先另存当前新库，检查UUID/型号无冲突，再放回目录JSON并导入 `catalog-restore.sql`，随后恢复需求/图纸JSON与 `restore.sql`。只恢复到新库，禁止操作原库。

## M1：本地模型与颜色纹理（已接通，待人工渲染验收）

2026-10-02 V004已应用到 `doormes_local`，新增 `dm_visual_asset` / `dm_visual_asset_upload` 两表；新库702张基表。管理员、研发、采购有上传权限；已有目录/图纸查询权限的角色可读取本企业资源。仍使用真实Bearer、tenant-id及MES通讯约定，不借用旧原型的模拟资源服务。七账号、八组织、五种示例材料保持原样，不再次初始化。

### 人工操作

1. 在“门窗业务 → 产品与组件目录”展开“本地模型与纹理资源”。选GLB、内嵌glTF或PNG/JPEG上传；资源编号可填写，留空以文件名/内容校验值初始化。上传后可以下载核对。资源编号表示文件身份，与门窗组成件短编号、业务型号不是同一字段。
2. 表面/玻璃目录的“高级外观参数”可选择本地颜色纹理，横向/纵向重复允许0.5等小数。保存草稿，由管理员发布设计选型；图纸顶部选择该明确版本的材料并保存修订。未选纹理的旧目录无需升级或改变哈希。
3. 自定义五金模型通过原生绘图属性里的“模型导入”向导操作。上传主GLB/glTF及可选LOD，设置源单位、朝向、模型外形包络和安装轴，应用模型后保存图纸。关闭重新打开后，工作区按图纸保存的资源ID/哈希加载。包络是仿真尺寸，不是切割/制造规则，也不自动生成正式生产BOM。
4. 人工可用本次自生成的基础测试文件：`runtime-local/scenarios/M1-AST-20261002-171021-e69085/fixtures/triangle.glb`、`triangle.gltf`、`checker.png`。三角形只用于检查上传/坐标/哈希和重新加载，不是供应商门窗模型；`external.gltf`专供外链拒绝检查，不能正常导入。

3D按精确资源加载；2D工程线稿保持结构/尺寸表达，不输出照片纹理。缺失或损坏资源显示诊断/保留基础符号，不替换为其他文件。实际纹理视觉、向导交互、开启动画和无闪烁必须人工确认；接口/哈希验证不等于完成WebGL验收。

### 配置与接口

本地根目录由 `application-doormes.yaml` 的 `doormes.assets.root` 设置，当前为 `E:/doorMES_v1/LUCK-MES-HC-0/runtime-local/design-data/assets`。文件形式 `tenant-N/<服务端blob UUID>/content.bin` 和 `descriptor.json`。客户端不能指定磁盘目录；路径/目录链拒绝符号链接。文件与索引同时保存内容SHA-256和元数据SHA-256，不覆盖历史资源；数据库事务回滚会清理本次新文件。崩溃孤立文件不会作为已提交资源读取，完整故障恢复后续加强。

前缀 `/admin-api/doormes/visual-assets`：

| 方法 | 请求 | 结果 |
| --- | --- | --- |
| POST `/authorize` | `{assetId,expectedContentHash,mediaType,byteLength}` | `{grantId,expiresAtIso}`，5分钟、本人/本企业一次性授权 |
| PUT `/uploads/{grantId}` | 原始二进制；Content-Type与授权一致 | `{asset,inspection,inspectedAtIso}`，外层为MES响应 |
| GET `/page` | `kind?,keyword?,pageNo,pageSize` | `{list,total}`，最多100项/页 |
| GET `/get` | `assetId` | 固定文件描述，无磁盘路径 |
| GET `/content` | `assetId,contentHash` | 哈希验证后的原始二进制；错误仍为MES JSON响应 |

描述包含 `assetId,kind="component-model"|"texture-bundle",mediaType,contentHash,byteLength,storedAtIso`。落盘元数据 `schemaVersion="doormes-visual-asset.v1"`，包含 `blobId,tenantId,uploadedBy,evidence`；身份由服务端决定。图纸沿用原 `geometry.kind="gltf"` 的 `assetId,contentHash,lodAssets,importConfiguration`，纹理沿用 `textureSetId,textureContentHash,uvScale`，不新增第二套几何文档。每次图纸/材料保存会检查原始及规范化引用，缺失资源404、分类/哈希不符400。同ID同内容重复上传幂等，同ID不同内容409。

限制：GLB/glTF25MiB；只允许glTF2.0，buffer/image与扩展uri需内嵌，禁止HTTP或磁盘路径；节点512、网格256、图元2048、三角形估计50万、材质128、图片64。此检查是安全预检，不等于完整glTF语义或制造审核。PNG/JPEG8MiB、最多4096×4096像素，服务器实际解码，不只看扩展名。纹理ID以 `MES-TEXTURE-` 开头，目前只接颜色贴图，不是完整PBR多贴图包。每人最多20个未过期授权，服务端同时处理最多2份上传。局域网HTTP没有SubtleCrypto时，使用已测试的本地SHA-256实现。

### 证据、部署与清理

场景 `M1-AST-20261002-171021-e69085` 的36项局域网HTTP/MySQL/文件断言全通过：上传权限、授权本人绑定、MIME/长度/哈希、下载一致、防覆盖/重复幂等、外链/伪造PNG拒绝、纹理0.5/0.75重复、图纸主模型与LOD精确引用、历史保留及非法引用拒绝。资源二进制/元数据与SQL逐一匹配。后端资源/目录/图纸/需求26项测试、共享引擎14项、前端21项及两个定向TypeScript检查通过；安全护栏此前9项无失败、1项链接权限跳过，本轮未重新宣称全仓测试通过。

新后端完成启动17:03:25，部署后健康UP、7账号局域网权限复验通过；运行PID38028仅作本次记录。停止服务前仍须重新检查端口、可执行文件、工作目录，不能把历史PID直接作为停止依据。旧可运行JAR备份在 `runtime-local/build-backups/before-assets-20261002/yudao-server.jar`。Windows占用导致首轮重新封装失败；核实并只停止隔离服务后，使用已通过测试的编译输出重新封装、启动成功。前端模块用真实Vue导入路径核对，`/vendor/...`的SPA HTML回退不是脚本成功，实际引擎为 `/@fs/E:/doorMES_v1/LUCK-MES-HC-0/frontend/vendor/doormes-engine/dist/engine.mjs`。

SOAP定义 `tests/scenarios/M1-assets.v1.json`；复验脚本 `verify-doormes-assets.ps1`。失败首轮 `M1-AST-20261002-170719-ad10f4` 也归档，修正测试脚本的字节展开后重测通过。成功场景的需求3版、图纸3版、目录2版和3份资源均已备份并精确清理；仍被保留版本引用时清理资源会被拒绝，拒绝条件实测通过。证据、恢复SQL、原始二进制及三份清理记录均在场景目录中，账号、组织、人工记录与5种示例材料不变。

按需求/图纸→目录→资源顺序使用 `archive-doormes-requirement-test.ps1`、`archive-doormes-catalog-test.ps1`、`archive-doormes-asset-test.ps1`，默认预览，`-Apply`仅允许本次精确TEST数据。恢复先备份新库并核对ID无冲突，再恢复对应JSON/二进制和 `assets-restore.sql`、`catalog-restore.sql`、`restore.sql`，只导入新库。上传授权不恢复，它们是短期能力而非有效业务资源。证据可能包含业务数据，整个 `runtime-local` 保持本地并忽略Git。

# 迁移门窗工厂开发环境

核验日期：2026-10-03。适用于把当前真实 MES 开发环境搬到另一台 Windows 机器，不是生产部署指南。本次只交接代码和说明，尚未生成新的数据库备份或在新机器恢复。

2026-10-04 补充：用户要求推送缺失的运行资料，已在同分支新增[图纸文件补齐包](runtime-transfer-20261004/README.md)。它包含 design-data 业务版本、迁移标记和 factory-bootstrap/complete.json，但不含账号密码或数据库。本篇其余恢复要求仍适用，不能把文件补齐等同数据库迁移成功。

## 当前数据库 IP 和名称

| 项目 | 当前实测值 |
| --- | --- |
| 旧机器名称 | `LAPTOP-TTHF62OK` |
| 旧机器局域网 IP | **`192.168.50.193`**（网段 `/24`） |
| 应用实际数据库地址 | **`127.0.0.1:3306`** |
| 当前业务数据库 | **`doormes_local`** |
| MySQL | `5.7.28-log`，字符集 `utf8mb4`，排序规则 `utf8mb4_unicode_ci` |
| 应用数据库账号 | `doormes_app`，仅新库权限 |
| 旧源数据库 | `qdaq0923`，保持不变；本次要迁移的是 `doormes_local`，不是这个旧库 |

MySQL 当前监听 `0.0.0.0:3306`，但专用账号仅有 `@localhost` 和 `@127.0.0.1`，**现有凭据不能直接用于另一台机器远程连接**。防火墙和跨机器 3306 连通性未验证。本次建议迁移备份到新机器本地库，不开放旧机器数据库。

## 代码、服务和工具

| 项目 | 当前路径 / 值 |
| --- | --- |
| 正在使用的 MES | `E:\doorMES_v1\LUCK-MES-HC-0` |
| Git 交接副本 | `E:\doorMES_v1\DoorMes\mes` |
| 原始行业系统 | `D:\Biz-SME-Source\HC-MES\LUCK-MES-HC-0`，禁止覆盖 |
| 前端 | Vue 3 / Vben / Ant Design Vue，`0.0.0.0:5180` |
| 后端 | Java 17 / Spring Boot，`127.0.0.1:48082` |
| 前端入口 | `http://192.168.50.193:5180/`；本机 `http://localhost:5180/` |
| 代理 / 健康检查 | 前端 `/admin-api` → 后端 48082；后端 `/actuator/health`，此次核验为 `UP` |
| Redis | `127.0.0.1:6381`，DB 0，独立认证；实际版本 **7.4.0** |
| Node / pnpm | `24.16.0 / 10.28.1` |
| Java | `D:\Java\jdk-17.0.12\bin\java.exe`；旧机器系统默认仍为 Java 8，不可误用 |
| PowerShell / Maven | PowerShell `7.6.5`；Maven 安装文件为 3.9.11，需在新机核验 `mvn -version` |

旧 Redis 目录虽然叫 `Redis-x64-5.0.14.1`，二进制及运行实例实测为 7.4.0，不按文件夹名判断版本。实际 MES 内嵌自己的绘图引擎，不需要同时启动旧 5173 原型。当前后端、Redis 保持回环绑定，通过前端代理提供局域网测试。

## 私下迁移哪些资料

以下路径相对于旧 MES 根目录。密码、数据库备份及录屏仍须私下迁移，不提交 Git；2026-10-04 用户授权的图纸补齐包是本次明确限定的例外，不代表后续业务数据自动纳入版本库。

| 内容 | 为什么必须保留 |
| --- | --- |
| 当前 `doormes_local` 完整备份 | 表、数据、视图、存储过程及业务关联；最初克隆备份不包含后续图纸和账号变更 |
| `runtime-local/design-data/requirements/` | 需求版本快照，当前核验 4 个文件 |
| `runtime-local/design-data/drawings/` | 图纸当前及历史快照，当前核验 9 个文件 |
| `runtime-local/design-data/catalog/` | 材料目录版本，当前核验 10 个文件 |
| `runtime-local/design-data/assets/` | 模型、纹理及描述；当前为空，目录仍保留 |
| `runtime-local/uploads/` | 本地附件；当前为空，目录仍保留 |
| `runtime-local/design-data/schema-v001.json` 至 `schema-v006.json` | 已执行迁移记录，不能据此重新执行初始化 |
| `runtime-local/factory-bootstrap/complete.json` 等标记 | 已初始化账号组织的状态 |
| `runtime-local/db-credentials.json` | 私有数据库凭据 |
| `runtime-local/redis/redis-credentials.json`、`redis.local.conf` | 私有 Redis 凭据及配置；配置本身也包含认证信息 |
| `runtime-local/factory-bootstrap/accounts.local.json` | 管理员及销售、设计、工艺、采购、生产、审核账号密码；安全传输 |
| `frontend/apps/web-antd/.env.development.local` | 5180、API、命名空间等本地前端配置 |
| 本机生效的后端配置 | 私下保留作为新机配置依据；Git 副本已脱敏，不能期待其带有原凭据 |
| `runtime-local/scenarios/` | 测试报告、截图、录屏和下载件；不是运行必需，但必须保留交付证据 |

SQL 与版本文件通过路径和 SHA-256 关联：**只恢复数据库会丢失实际图纸；只复制 JSON 会丢失版本索引与权限关系。** 二者须来自同一停写时点。订单头/历史在数据库；应一并复制整个 `design-data`，不要只按上述当前数量挑选文件。

`db-clone`、初始化恢复备份及 `build-backups` 可以加密归档留作恢复证据，不要混成最新业务备份。浏览器配置、认证缓存、node_modules、包缓存和日志不是必须迁移内容。Redis 当前关闭 RDB 自动保存及 AOF，已有 `dump.rdb` 不能代表当前内存；新机器应重新登录，不能依赖旧会话。

## 1. 在旧机器做一致性备份

1. 约定停写窗口，关闭设计保存和其他业务写入。记录当前代码提交、数据库版本和备份时间。
2. 使用匹配 MySQL 5.7 的工具导出 **`doormes_local`** 的结构与数据，包含视图、存储过程和触发器。检查表引擎；`--single-transaction` 只保证事务表快照，不能单独保证数据库与文件或非事务表一致。
3. 在同一停写窗口复制整个 `runtime-local/design-data`、uploads、标记及所需私有配置。密码通过受控文件传递，不放命令行、聊天或 Git。
4. 对导出文件及业务文件生成 SHA-256 清单，检查导出退出码及可恢复性后再允许继续写入。保留原文件，不删除旧库或旧运行目录。

本次交接**未执行这四步**。旧的 `runtime-local/db-clone/.../source-data-mysql57.sql` 是初始克隆材料，不能代替当前完整备份。

## 2. 在新机器恢复数据

1. 克隆交接分支。选定一个稳定根目录，例如 `E:\doorMES_v1\DoorMes\mes`，下文称 `MES_ROOT`。
2. 安装并核验上表工具，尤其 Java 17、MySQL 5.7 和专用 Redis。跨 MySQL 大版本升级应另设验证任务，不与本次迁移同时处理。
3. 恢复到独立的 `doormes_local`。不要导入覆盖已有同名有效库；有冲突时先停下确认。
4. 新建或恢复 `doormes_app` 本机来源账号，只授权该库。逻辑数据库备份通常不包含 MySQL 用户授权。安全保存对应本地凭据。
5. 将图纸、目录、资源、附件及迁移标记恢复到 `MES_ROOT/runtime-local/`，检查哈希及运行账号文件权限。

**不要重新执行** clone、initialize-local、initialize-factory、initialize-redis、initialize-catalog 或 V001–V006 migration 来“补齐环境”。这些步骤已执行；clone 的 ResumeData 会清空目标表，factory 初始化会重建人员组织权限。迁移是恢复当前状态，不是重建示例系统。

## 3. 核对新机器配置

配置变化须重新打包后端，或明确提供外置覆盖配置；只改源码 YAML 不会改变已打包 JAR。

| 配置位置 | 必须核对 |
| --- | --- |
| `backend/yudao-server/src/main/resources/application-local.yaml` | master、slave JDBC URL 都指向新机 `doormes_local`；安全副本支持 `DOORMES_DB_URL`，默认本机库 |
| `application-doormes.yaml` | 安全副本用 `DOORMES_RUNTIME_ROOT` 配置 assets/catalog/drawings/requirements、validation-temp 和日志目录；Node、校验器分别用 `DOORMES_NODE_EXECUTABLE`、`DOORMES_VALIDATOR_PATH` |
| `doormes.local.upload-root` | 安全副本显式指向 runtime 下 uploads；原运行代码缺省值仍固定在旧根目录；使用旧私有配置时必须核对 |
| 新库 `infra_file_config` 主配置 | 当前 ID 38 的 `config.basePath` 与 upload-root 一致，`config.domain` 与新机公开前端 origin 一致；核实记录身份再改 |
| 租户域名绑定 | 与新机 IP/域名一致，保留正确租户；当前默认企业“门窗工厂”，租户 ID 1 |
| `scripts/start-doormes-backend.ps1` | 安全副本通过 `JAVA_HOME` 或 PATH 找 Java，必须配置为 Java 17；启动 profile 必须 `local,doormes`，不可只启 local |
| `runtime-local/redis/redis.local.conf` | 6381、回环绑定、密码、数据目录；不复用旧系统 6379 |
| 前端 `.env.development.local` | 5180、`/admin-api` 代理、业务命名空间；默认自动登录用户名密码留空 |

只修改 `db-credentials.json` 的 host/port **不会**改变当前 JDBC URL，真实地址取自 Spring 配置。后端启动脚本从本地文件注入数据库和 Redis 密码，必须使用与新机器实际账户匹配的凭据。旧系统外部 OA、短信、邮件、流程通知和定时任务保持关闭，不因迁移恢复旧生产连接。

安全副本已去除旧开发、生产外部连接配置及敏感值。不要将整份旧 `application-local.yaml` 覆盖回来；逐项迁移必要参数。后端从 `MES_ROOT/backend` 启动时，默认 runtime 根为 `../runtime-local`；也可显式设为新机绝对路径。`DOORMES_PUBLIC_FILE_ORIGIN` 默认 `http://127.0.0.1:5180`，提供局域网访问时改为新机 IP 的 5180 地址，并同步数据库文件配置和租户域名。管理员数据库脚本另用 `DOORMES_ADMIN_DB_USERNAME/PASSWORD`，不是业务后端启动所需，不要授予应用账号全库管理员权限。

`DOORMES_NODE_EXECUTABLE` 必须是 Node 的绝对路径，默认 `C:/Program Files/nodejs/node.exe`。当前 Java 校验器会把配置转为绝对文件路径，不会搜索 PATH；不能只填写 `node`，否则新机器无法保存图纸。

## 4. 安装、构建与启动

在 `MES_ROOT/frontend` 安装锁定的工作区依赖：

```powershell
pnpm install --frozen-lockfile
```

`frontend/vendor/doormes-engine` 不在当前 pnpm workspace 包列表中。新机器必须准备它的 `three`、`ajv` 依赖，不能依赖旧机器原型缓存。在该目录执行：

```powershell
pnpm install --ignore-workspace --no-frozen-lockfile
node build.mjs
```

vendor 当前没有独立锁文件，第一轮安装会生成它，不能声称完全锁定可重现；后续应单独审查并纳入锁文件。构建生成 `dist/engine.mjs`、`dist/engine.d.mts`、`dist/validator.mjs`，dist 不进入 Git。其现有 `tsconfig.json` 仍引用旧原型类型路径，迁移后定向类型检查需要另行修正；上面的构建步骤不等于该检查已经通过。安全复制范围见 `mes/SAFE-IMPORT.md`。

在后端根目录核验 Java 17 和 Maven 后，使用 Maven reactor 构建 `yudao-server` 及依赖模块，例如 `mvn -pl yudao-server -am package -DskipTests`。跳过测试只用于生成开发启动包，不代表测试通过；依赖仓库、私有配置和旧模块构建问题须单独记录。使用生成的 `yudao-server/target/yudao-server.jar`，不要用来源不明的旧 JAR。

按 Redis → 后端 → 前端顺序启动。Redis 使用新机实际安装路径及上述私有配置；不要照搬旧二进制目录名。后端在 `MES_ROOT` 执行，前端在 `MES_ROOT/frontend` 执行：

```powershell
# MES_ROOT
pwsh -NoProfile -File .\scripts\start-doormes-backend.ps1

# MES_ROOT/frontend
pnpm dev:antd
```

只开放所需的前端 5180 局域网端口，后端 48082 和 Redis 6381 保持回环。首次恢复尽量使用相同版本，不升级 Axios 锁定版本或混用 npm/pnpm 改写主工作区锁文件。

## 5. 迁移验收

- 后端 `/actuator/health` 为 UP，前端登录及代理请求正常，无 Illegal invocation 或刷新认证后旧响应问题。
- 使用私有账号表中的 admin/sales/design/process/purchase/production/reviewer，核对角色菜单和写入权限，不使用旧行业账号。
- design 打开现有图纸 R3，再查看 R1/R2，尺寸、分格、短编号、材质和历史一致；历史无保存入口。
- 创建新的迁移验收测试图，完成六步闭环；不要修改用于对照的旧样图。
- 加载材料目录、模型纹理和附件；校验哈希、路径及新前端 origin。
- 工厂图当前版/历史版、独立表页、编号、下载、页签返回和未保存草稿隔离复验。
- 订单仍引用原明确版本，保存新图纸版本不会自动改订单；变更审批与生产采用分别验证。
- 保留测试断言、录屏和文字步骤；失败记录与通过记录分开。迁移成功不等于所有业务场景已完成。

如需恢复旧机器，原运行目录、原数据库和账号配置均保留；本次 Git 操作不替换它们。

## 原型不要当作正式账号入口

仓库根目录的 `apps/factory-api`、5173 原型及其演示默认账号仍用于保留开发历史，不是实际 MES 的身份系统。不要将原型演示默认密码或模拟数据用于正式环境，也不要把原型开发服务直接暴露到互联网。实际开发入口是 `mes/frontend` 的 5180 与真实后端 48082。

# MES 安全代码快照

本目录从本机 `LUCK-MES-HC-0` 工作副本导入，作为 DoorMes 的 MES 业务实现。导入时没有修改、启动或停止原副本，没有操作原数据库，也没有复制运行数据。原目录继续作为当前运行环境。

## 纳入范围

- `backend/`：原 MES 全部业务模块、框架、Maven 配置和测试源码；保留上游许可证。
- `frontend/`：管理端、共享包、构建配置、字体和页面素材；包含 `vendor/doormes-engine` 的完整源码和构建脚本，保留上游许可证。
- `scripts/`：DoorMes 命名的初始化、迁移、启动、验收、归档和恢复脚本。
- `tools/FactoryAccountPasswordTool.java`、`tools/create-asset-fixtures.mjs`：账号初始化和资源验收所需工具。
- `tests/scenarios/`、`docs/scenarios/`、`docs/doormes*.md`：DoorMes 场景定义和开发记录。
- `db/doormes/V001` 至 `V006`：DoorMes 增量结构与菜单/权限迁移，不包含旧客户业务数据。

## 未纳入的内容

- `runtime-local/` 中的凭据、数据库导出、账号文件、上传文件、缓存、日志、浏览器配置、录像和场景证据。
- `db/backup/`、旧 MES 根目录 SQL、旧业务文档、测试截图、测试结果和数据库关系导出。
- 依赖、编译输出、虚拟环境和发布包，包括 `node_modules/`、`target/`、`dist/`、`build/`、`.venv/`、`release/`。
- IDE/个人配置、嵌套 Git 元数据、临时文件和私钥文件。
- 旧打印、Modbus、SPC 工具目录及旧生产维护脚本；它们不属于本次 DoorMes 核心运行所需内容。
- `backend/script/` 下旧 Docker、IDE HTTP 客户端及部署示例；当前 Maven/前端构建不依赖这些文件。
- 原 `application-prod.yaml`、`application-dev.yaml`；其中原外部环境连接没有作为模板带入。

## 脱敏与默认连接

`application.yaml`、`application-local.yaml`、`application-doormes.yaml` 及 IoT 网关配置中的密码、密钥、令牌改为环境变量。注释中的旧密码和私钥示例已移除。旧集成测试中的厂商 API 密钥、支付私钥、对象存储凭据也已清理；需要真实外部环境的测试必须另行提供凭据。

前端提交的 `.env*` 仅含公共默认配置。默认登录用户名和密码为空；API 加密示例密钥为空，前后端默认关闭这层可选加密。客户端配置中的值均不能作为服务端秘密使用。

默认采用 `local,doormes` profile；后端只监听 `127.0.0.1:48082`。前端开发端口为 `5180`，通过 `/admin-api` 代理本地后端。OA 通知、调度、异步流程、管理服务注册和第三方登录沿用 DoorMes 隔离关闭配置。

| 配置 | 用途 |
| --- | --- |
| `DOORMES_DB_URL` | 默认仅连接本地 `doormes_local` |
| `DOORMES_DB_USERNAME` / `DOORMES_DB_PASSWORD` | DoorMes 专用数据库账号 |
| `DOORMES_REDIS_PASSWORD` | 本地隔离 Redis，端口 `6381` |
| `DOORMES_RUNTIME_ROOT` | 从 `mes/backend` 启动时默认为 `../runtime-local` |
| `DOORMES_NODE_EXECUTABLE` | Node 的绝对路径，默认 `C:/Program Files/nodejs/node.exe`；校验器不按 PATH 查找 |
| `DOORMES_VALIDATOR_PATH` | 默认 `../frontend/vendor/doormes-engine/dist/validator.mjs` |
| `DOORMES_PUBLIC_FILE_ORIGIN` | 默认 `http://127.0.0.1:5180` |
| `DOORMES_ADMIN_DB_USERNAME` / `DOORMES_ADMIN_DB_PASSWORD` | 本地管理脚本凭据，不再从源码 YAML 读取 |
| `DOORMES_SOURCE_DATABASE` | 仅克隆脚本使用；必须明确指定经授权的本地源库 |

`start-doormes-backend.ps1` 使用 `JAVA_HOME` 或 PATH 中的 Java，仍从被忽略的 `runtime-local` 中读取专用账号凭据。仓库不附带该目录。其他旧外部连接所需的 `MES_*` 环境变量默认不提供有效凭据。

## 当前限制与验证边界

- 这是一份源码快照，不是含数据库和运行资源的完整环境备份。六个迁移依赖已有 MES 基础表，不能单独初始化空数据库。
- 部分一次性初始化、清理和归档脚本仍保留历史工作目录、日期或数据库基线校验。它们未在此副本执行；重新使用前须按目标环境审查，不能绕过其保护条件。
- `docs/doormes*.md` 中的历史路径、端口、测试次数和数据库状态描述的是导入前环境，不代表本副本重新验收通过。
- 引擎构建输出不入库。先在 `frontend` 安装锁定依赖，再按引擎目录说明安装自身依赖并构建。部分历史 TypeScript 配置仍引用原型工作区，跨机器构建需要继续调整。
- 后端根 Maven 工程已启用现有 CRM、ERP 模块；`yudao-server` 原本已依赖它们，此调整让 `-pl yudao-server -am` 在新机器从源码构建依赖，不依赖旧机器缓存的这两个 SNAPSHOT。已静态核对全部活动模块的内部依赖均在构建列表中；未执行依赖安装或编译。
- 本次仅进行文件范围、秘密模式、配置语法和源码完整性检查；没有执行数据库迁移、启动服务或连接旧外部系统。
- `.gitignore` 是后续防护，不代替提交前秘密扫描。新增生产连接和私有材料须继续存放在环境变量或本地运行目录。

# 图纸运行文件补齐包

2026-10-04 按用户要求随交接分支提供。ZIP 已检查未包含账号、数据库或 Redis 密码；仍含设计业务数据，仅供授权开发迁移使用。

## 包含内容

`01-design-data-and-bootstrap.zip` 内含 30 个文件：9 份图纸版本、10 份材料目录版本、4 份需求版本、6 个已执行迁移标记，以及 `factory-bootstrap/complete.json`。另保留 assets、validation-temp 空目录。

压缩包 SHA-256：

```text
576e766d8935536e2f050bfad854f049c225ba207d9ed4c9125b121fee402fcd
```

**不包含** `factory-bootstrap/accounts.local.json`、数据库备份、数据库/Redis 凭据或旧回滚备份。账号文件含明文密码，仍须从旧机器私下传递，不能因为这个包有初始化标记就认为数据库用户已经恢复。

## 新机器恢复

1. 拉取 `handoff/mes-migration-20261003` 分支，进入本目录，执行 `pwsh -NoProfile -File .\verify-archive.ps1`。
2. 将 ZIP 解压到新建临时目录。它自带 `runtime-local/` 顶层，不能再套一层 runtime-local。
3. 确认新机数据库已恢复配套的 `doormes_local`，暂停应用写入后，将解压得到的 `design-data`、`factory-bootstrap` 放入 `DoorMes/mes/runtime-local/`。目标已有文件时先备份核对，不直接覆盖。
4. 从旧机器单独传递 `02-factory-accounts-PRIVATE.zip` 或 `accounts.local.json`，放到 `DoorMes/mes/runtime-local/factory-bootstrap/accounts.local.json`。登录用户及权限实际在数据库中，JSON 主要供验收脚本读取，不会自动创建用户或改密。
5. 核对 `DOORMES_RUNTIME_ROOT`、数据库/Redis 本地凭据和 Node 绝对路径，重新加载已有图纸及历史版本。

最终应为：

```text
DoorMes/mes/runtime-local/
├─ design-data/
│  ├─ drawings/tenant-1/...
│  ├─ catalog/tenant-1/...
│  ├─ requirements/tenant-1/...
│  ├─ assets/
│  ├─ validation-temp/
│  └─ schema-v001.json ... schema-v006.json
└─ factory-bootstrap/
   ├─ complete.json
   └─ accounts.local.json  # 私下传递，不在 Git 中
```

文件保持原始字节；不要重新格式化 JSON，以免破坏数据库关联的哈希。`complete.json` 中旧备份路径仅作为历史记录保存，包内没有相应回滚 SQL。

**不要重跑初始化、清库或 V001–V006 迁移来补目录。** 当前包只证明文件完整，未证明与新机数据库备份同一时点，也没有在新机做登录/设计验收。完整环境步骤见上级目录的迁移指南。

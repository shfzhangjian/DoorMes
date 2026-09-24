# 视觉资产上传HTTP协议与后置治理预留

文档版本：0.3.0  
状态：开发上传与本地目录已用于当前功能；正式身份、权限、审核发布、对象存储和目录数据库全部后置，当前不实施

## 1. 边界

PC与移动编辑器只依赖`ManagedGltfAssetGateway`。开发环境默认使用同源`/api`，经Vite开发路由把文件写到项目本地目录；正式部署设置`VITE_VISUAL_ASSET_API_BASE_URL`后仍使用同一个`HttpManagedGltfAssetGateway`，不改变文件选择、SHA-256、LOD、进度、证据或设计命令。未配置正式HTTP地址时才使用IndexedDB离线后备。

> 执行约束：当前只使用本协议的本地文件上传/读取能力。下面的正式会话身份、用户角色、审核发布和状态码设计是未来兼容预留，不在现阶段任务队列中，也不得阻塞模型导入、2D/3D展现、移动端或BOM功能。

任何HTTP请求都不得在JSON、查询参数或自定义请求头中声明`actorId`或角色。网关使用同源会话/Cookie，服务端路由从已经认证的请求上下文构造`ComponentAssetCatalogActor`。

## 2. 本地目录配置

当前工作区已经写入不入库的`E:\doorMES_v1\DoorMes\.env`：

```dotenv
VITE_VISUAL_ASSET_API_BASE_URL=/api
DOORMES_VISUAL_ASSET_DIRECTORY=E:/doorMES_v1/DoorMes/runtime-data/visual-assets
DOORMES_DEV_ACTOR_ID=local-worker
```

配置变更后必须停止并重新执行`npm run dev`，因为Vite只在启动时读取环境变量。未设置`DOORMES_VISUAL_ASSET_DIRECTORY`时，默认目录也是：

`E:\doorMES_v1\DoorMes\runtime-data\visual-assets`

目录在第一次成功上传时自动创建，内部结构如下：

```text
runtime-data/visual-assets/
├─ objects/       # 按SHA-256去重的GLB/glTF原始字节（*.blob）
├─ assets/        # 资产ID到哈希、MIME、长度和时间的不可变描述（*.json）
└─ retentions/    # 目录版本/历史订单的精确留存清单（*.json）
```

资产ID和owner ID先做SHA-256再作为文件名，不能使用`../`逃出配置目录；JSON使用同目录临时文件加原子重命名写入。同一个资产ID不允许换绑其他内容，相同内容只保存一份原始字节。`runtime-data/`已加入`.gitignore`，不能把工人上传的供应商资产误提交到源码库。

这套开发路由固定把`DOORMES_DEV_ACTOR_ID`注入为`asset-designer`，只用于本机联调，不代表正式认证。生产部署必须由登录会话/令牌提供身份与租户，并使用数据库或对象存储实现同一端口。

人工测试可从以下本地测试包选择文件：

`E:\doorMES_v1\DoorMes\test-assets\gltf-import`

- `01-box-basic.glb`、`02-box-vertex-colors.glb`、`03-simple-meshes-embedded.gltf`、`04-damaged-helmet-textured.glb`应成功。
- `05-external-uri-rejected.gltf`、`06-invalid-json-rejected.gltf`应被拒绝，且不能在本地目录留下资产描述。

## 3. 申请上传授权

`POST {baseUrl}/visual-assets/model-uploads/authorize`

请求：

```json
{
  "assetId": "ASSET-HANDLE-42-HIGH",
  "expectedContentHash": "sha256:<64 hex>",
  "mediaType": "model/gltf-binary",
  "byteLength": 3773916
}
```

服务端使用不可预测的`grantId`、当前服务端时间和不超过十五分钟的失效时间调用`registerManagedGltfUploadAuthorization`。响应只暴露：

```json
{
  "grantId": "opaque-random-identifier",
  "expiresAtIso": "2026-09-18T12:05:00.000Z"
}
```

授权记录在服务端绑定登录人、资产ID、哈希、MIME、长度和时间；同一`grantId`不能换绑。只有`asset-designer`、`catalog-approver`或`administrator`可以申请。

## 4. 上传原始字节

`PUT {baseUrl}/visual-assets/model-uploads/{grantId}`

- 请求体是完整GLB/glTF原始字节，不使用Base64或multipart二次包装。
- `Content-Type`必须与授权一致。
- 路由将会话中的登录人、路径grant ID、原始字节和当前服务端时间传给`inspectAndStoreAuthorizedManagedGltfAsset`。
- 登录人不符、授权未知/未生效/过期、长度或SHA-256不符、外部URI、格式错误或复杂度超限均不得留下资产描述。
- 相同grant在有效期内以相同字节重试是幂等的；相同资产ID不能绑定其他内容。

成功响应为`ManagedGltfUploadEvidence`，包含不可变资产描述、字节数、节点/网格/图元/估算三角面/材质/图片计数和服务端检查时间。浏览器会再次核对响应ID、哈希和长度后才提交设计命令。

## 5. 查询资产描述

`GET {baseUrl}/visual-assets/{urlEncodedAssetId}`

- `200`返回`VisualAssetBlobDescriptor`，不返回原始字节或对象存储URL。
- `404`表示当前身份/租户下不可用。
- 响应资产ID必须与路径请求一致；编辑器再按设计快照SHA-256判断可用、缺失或错版。

### 5.1 读取精确模型字节

`GET {baseUrl}/visual-assets/{urlEncodedAssetId}/content?contentHash={urlEncodedSha256}`

- ID和SHA-256必须同时匹配才返回原始GLB/glTF字节；不能按ID静默返回“最新版”。
- 开发路由从配置的本地目录读取，不向浏览器暴露磁盘路径。
- 客户端先读取描述，收到字节后再次核对长度和SHA-256，再交给共享GLB/glTF预检与Three加载器。
- `404`表示该精确版本不存在；3D保留参数化/包络降级体并显示加载状态，不使整个画布失败。
- 桌面默认读取高清资产；移动端按低清、其次中清、最后高清选择一个版本，避免同时下载全部LOD。

## 6. 审核发布

建议路由：`POST {baseUrl}/component-asset-catalog/{catalogItemId}/publish`

请求携带完整`in-review`草稿和用户打开页面时读取的`expectedRevision`，身份仍取服务端会话。路由调用`publishRetainedComponentAssetCatalogVersion`：

1. 校验审批角色、草稿revision、`catalog-approved`生产身份、物资编码和完整LOD哈希。
2. 比较目录当前revision；不一致返回`409 Conflict`。
3. 在数据库事务内核验所有ID+哈希字节描述、写入不可变留存清单、追加目录版本并把revision加一。
4. 对象存储只保存不可覆盖字节；数据库事务保存对象键和精确哈希，不保存临时URL。

## 7. 状态码

| 状态码 | 语义 |
| --- | --- |
| 400 | 请求字段、MIME、时间或长度非法 |
| 401 | 尚未登录 |
| 403 | 角色不足或grant属于另一登录人 |
| 404 | grant/资产/目录项不存在或当前租户不可见 |
| 409 | 资产ID换绑、grant ID换绑或目录revision冲突 |
| 410 | 上传授权已过期 |
| 413 | 文件超过25MB |
| 422 | GLB/glTF哈希、格式、外链或复杂度预检失败 |

## 8. 版本记录

| 日期 | 版本 | 内容 |
| --- | --- | --- |
| 2026-09-18 | 0.1.0 | 固化授权、原始字节上传、描述查询和审核发布CAS/留存事务协议；接入可配置HTTP客户端 |
| 2026-09-18 | 0.2.0 | 增加开发路由、可配置本地目录、哈希寻址文件存储、配置说明与本地工人测试路径 |
| 2026-09-18 | 0.2.1 | 明确功能优先：当前只使用本地上传能力，正式身份、权限、审计与审批发布全部后置 |
| 2026-09-18 | 0.3.0 | 增加ID+哈希内容读取端点、浏览器长度/哈希二次核对，以及桌面/移动LOD加载规则 |

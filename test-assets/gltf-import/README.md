# DoorMes GLB/glTF 导入测试包

测试包版本：0.10.12  
上游固定版本：Khronos `glTF-Sample-Assets@c6a6bd13ab2b3c685c7903d03561b8a9392f38b8`

## 工人测试步骤

1. 启动DoorMes，打开任一带五金的示例窗。
2. 在“材质与型号”中进入“3 模型导入”。
3. 五金角色先选“执手/锁具操作件”，选择本目录中的一个文件作为高清文件。
4. 对四个官方样例，源单位设置为`meter`、向上轴设置为`+y`、朝外轴设置为`+z`。这是glTF 2.0规定的标准单位和坐标方向。
5. 设置实际宽、高、深后点击“预检、保存并导入”。记录界面显示的字节数、三角面数、节点数和错误信息。
6. 刷新页面并重新选择同一窗体。资产诊断不应再报告刚导入的资产缺失。

## 预期结果

| 文件 | 预期 | 主要验证点 |
| --- | --- | --- |
| `01-box-basic.glb` | 成功 | 最小GLB、自动哈希、持久保存 |
| `02-box-vertex-colors.glb` | 成功 | 顶点颜色模型能够解析；DoorMes仍按项目外观覆盖最终材质 |
| `03-simple-meshes-embedded.gltf` | 成功 | JSON glTF和内嵌data URI，不需要同目录外部文件 |
| `04-damaged-helmet-textured.glb` | 成功 | 约3.6MB带贴图PBR模型、进度和复杂度证据 |
| `05-external-uri-rejected.gltf` | 拒绝 | 应提示禁止外部buffer或图片URI，不产生设计提交 |
| `06-invalid-json-rejected.gltf` | 拒绝 | 应提示格式/JSON错误，不产生设计提交 |

同一个资产ID不得用于另一份不同内容。要更换文件时保留系统自动生成的新ID，不能把旧ID手工复制给新文件。当前导入结果是`preview-only`，只供设计预览，未经过目录审核前不得作为生产物料或加工模板使用。

每个文件的固定SHA-256、长度和预期结果见`manifest.json`；第三方许可和来源见`THIRD_PARTY_NOTICES.md`。

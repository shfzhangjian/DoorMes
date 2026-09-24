# DoorMes 代码注释规范

文档版本：0.1.0

公开类型、类、函数、领域算法和复杂私有函数使用TSDoc/Javadoc说明：

1. 业务作用和所属层级。
2. 调用流程或关键算法。
3. 参数、返回值、不变量和异常条件。
4. 至少一个典型调用或业务示例。
5. `@since`首次引入版本。
6. `@modified`日期和变更摘要。
7. BOM及工艺规则额外记录规则编号、版本和来源。

示例：

```ts
/**
 * Calculates one profile cut from a normalized frame segment.
 *
 * Algorithm: resolve end joints, subtract allowances, then round according to
 * the active rule-set precision. The result retains source object and rule IDs.
 *
 * @param segment Stable frame-segment domain object.
 * @returns A traceable cutting requirement in millimetres.
 * @example `calculateFrameCut(segment)` returns one 45° cut requirement.
 * @since 0.1.0
 * @modified 2026-09-17 - Added initial frame cutting rule.
 */
```

注释解释业务约束和设计意图，不逐行翻译代码。完整版本历史仍以Git和变更日志为准。

## 修改日志

| 日期 | 版本 | 修改内容 |
| --- | --- | --- |
| 2026-09-17 | 0.1.0 | 建立正式项目TSDoc/Javadoc注释要求 |

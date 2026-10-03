package cn.iocoder.yudao.module.mes.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * MES 错误码枚举类
 * * 错误码段: 1-008-000-000
 */
public interface ErrorCodeConstants {

    // ========== 物料管理 1-008-001-000 ==========
    ErrorCode MES_MATERIAL_NOT_EXISTS = new ErrorCode(1_008_001_001, "物料不存在");
    ErrorCode MES_MATERIAL_CODE_EXISTS = new ErrorCode(1_008_001_002, "物料编码已存在");

    // ========== 车间管理 1-008-002-000 ==========
    ErrorCode MES_WORKSHOP_NOT_EXISTS = new ErrorCode(1_008_002_001, "车间/产线不存在");
    ErrorCode MES_WORKSHOP_PARENT_NOT_EXITS = new ErrorCode(1_008_002_002, "父节点不存在");
    ErrorCode MES_WORKSHOP_PARENT_ERROR = new ErrorCode(1_008_002_003, "不能设置自己为父节点");
    // 修正：Service 中使用的是 EXITS_CHILDREN，此处保持一致
    ErrorCode MES_WORKSHOP_EXITS_CHILDREN = new ErrorCode(1_008_002_004, "该节点下存在子节点，无法删除");

    // 补全 Service 中用到的其他错误码
    ErrorCode MES_WORKSHOP_PARENT_IS_CHILD = new ErrorCode(1_008_002_005, "父节点不能是自己的子节点");
    ErrorCode MES_WORKSHOP_ID_DUPLICATE = new ErrorCode(1_008_002_006, "已经存在该 ID 的车间/产线");

    ErrorCode BOM_NOT_EXISTS = new ErrorCode(1_008_002_007, "BOM不存在");
    ErrorCode BOM_ITEM_NOT_EXISTS = new ErrorCode(1_008_002_008, "BOM的项目不存在");


    // ========== 工序管理 1-008-004-000 ==========
    ErrorCode PROCESS_NOT_EXISTS = new ErrorCode(1_008_004_001, "工序不存在");
    ErrorCode PROCESS_CODE_EXISTS = new ErrorCode(1_008_004_002, "工序编码已存在");

    // ========== 工艺路线 1-008-005-000 ==========
    ErrorCode ROUTE_NOT_EXISTS = new ErrorCode(1_008_005_001, "工艺路线不存在");
    ErrorCode ROUTE_CODE_EXISTS = new ErrorCode(1_008_005_002, "工艺路线编号已存在");
    ErrorCode ROUTE_DEFAULT_EXIST = new ErrorCode(1_008_005_003, "该产品已存在默认工艺路线，请先取消默认");
    ErrorCode ROUTE_PROCESS_NOT_EXISTS = new ErrorCode(1_008_005_004, "工艺路线子项不存在");
    ErrorCode ROUTE_PROCESS_PARAM_NOT_EXISTS = new ErrorCode(1_008_005_005, "工艺路线参数不存在");


    // ========== 计量单位 1-008-006-000 ==========
    ErrorCode UNIT_NOT_EXISTS = new ErrorCode(1_008_006_001, "计量单位不存在");
    ErrorCode UNIT_CODE_EXISTS = new ErrorCode(1_008_006_002, "计量单位符号已存在");


    // ========== 销售订单管理 1-008-010-000 (新增) ==========
    ErrorCode MES_SALE_ORDER_NOT_EXISTS = new ErrorCode(1_008_010_001, "销售订单不存在");
    ErrorCode MES_SALE_ORDER_NUMBER_EXISTS = new ErrorCode(1_008_010_002, "销售订单编号已存在");


    // ========== 主生产计划管理 1-008-011-000 (新增) ==========
    ErrorCode MES_PLAN_NOT_EXISTS = new ErrorCode(1_008_011_001, "生产计划不存在");
    ErrorCode MES_PLAN_NUMBER_EXISTS = new ErrorCode(1_008_011_002, "计划单号已存在");
    ErrorCode MES_PLAN_STATUS_ERROR = new ErrorCode(1_008_011_003, "计划状态不支持该操作");
    ErrorCode MES_PLAN_RELEASE_ERROR = new ErrorCode(1_008_011_003, "计划状态不正确，无法下达");

    // ========== 生产工单管理 1-008-012-000 ==========
    ErrorCode MES_WORK_ORDER_NOT_EXISTS = new ErrorCode(1_008_012_001, "生产工单不存在");
    ErrorCode MES_WORK_ORDER_EXCEED_PLAN = new ErrorCode(1_008_012_002, "工单数量超过计划剩余可排数量");

    // ========== 排产管理 1-008-013-000 ==========
    ErrorCode MES_SCHEDULE_NOT_EXISTS = new ErrorCode(1_008_013_001, "排程任务不存在");
    ErrorCode MES_SCHEDULE_TIME_CONFLICT = new ErrorCode(1_008_013_002, "该时间段设备已被占用");

    // ========== 派工细单/生产执行 1-008-014-000  ==========
    ErrorCode MES_SUB_ORDER_NOT_EXISTS = new ErrorCode(1_008_014_001, "派工细单不存在");
    ErrorCode MES_SUB_ORDER_STATUS_ERROR = new ErrorCode(1_008_014_002, "派工细单状态流转异常");

    // ========== 供应商 1-008-015-000  ==========
    ErrorCode SUPPLIER_NOT_EXISTS = new ErrorCode(1_008_015_001, "供应商不存在");
    ErrorCode SUPPLIER_VERSION_CONFLICT = new ErrorCode(1_008_015_002, "供应商数据已被其他用户修改，请刷新后重试");


    ErrorCode MOCK_WORK_ORDER_NOT_EXISTS = new ErrorCode(1_008_020_001, "模拟生产工单表（用于AI大模型MCP调用测试）不存在");

    // ========== 质量任务中心 1-008-021-000 ==========
    ErrorCode QMS_DISPATCH_TASK_NOT_EXISTS = new ErrorCode(1_008_021_001, "质量任务不存在");
    ErrorCode QMS_DISPATCH_TYPE_NOT_SUPPORTED = new ErrorCode(1_008_021_002, "暂不支持检验类型：{}");
    ErrorCode QMS_DISPATCH_EXECUTION_NOT_EXISTS = new ErrorCode(1_008_021_003,
            "未找到{}检验记录（ID={}），请确认原单仍然存在");
    ErrorCode QMS_DISPATCH_TASK_CANCELLED = new ErrorCode(1_008_021_004, "任务已取消，不能再次分派");
    ErrorCode QMS_DISPATCH_ITEM_INVALID = new ErrorCode(1_008_021_005, "选中的检验项目不存在或不属于当前来源");
    ErrorCode QMS_DISPATCH_STANDARD_INVALID = new ErrorCode(1_008_021_006, "检验标准不存在、未启用或未审核");
    ErrorCode QMS_DISPATCH_BPM_NOT_PUBLISHED = new ErrorCode(1_008_021_007,
            "质量任务流程未发布，请先发布 SIMPLE 流程 qms_quality_dispatch_task");
    ErrorCode QMS_DISPATCH_EXECUTOR_ONLY = new ErrorCode(1_008_021_008, "只有质量任务执行人可以保存检验结果");
    ErrorCode QMS_DISPATCH_RESULT_NOT_EDITABLE = new ErrorCode(1_008_021_009, "当前质量任务状态不允许保存检验结果");
    ErrorCode QMS_DISPATCH_REQUEST_INVALID = new ErrorCode(1_008_021_010, "质量任务参数不完整：{}");
    ErrorCode QMS_DISPATCH_PROCESS_NOT_ACTIONABLE = new ErrorCode(1_008_021_011, "当前用户没有可办理的质量任务流程节点");
    ErrorCode QMS_DISPATCH_EXECUTION_NOT_SUBMITTED = new ErrorCode(1_008_021_012, "请先在对应检验单详情中完整填写并提交检验结果");
    ErrorCode QMS_DISPATCH_NATIVE_ENTRY_REQUIRED = new ErrorCode(1_008_021_013, "{}检验结果必须在对应检验单详情中填写，不能使用任务中心简化录入");

    // ========== 量检具管理 1-008-022-000 ==========
    ErrorCode QMS_MEASURE_TOOL_CATEGORY_NOT_EXISTS = new ErrorCode(1_008_022_001, "量检具分类不存在");
    ErrorCode QMS_MEASURE_TOOL_CATEGORY_CODE_EXISTS = new ErrorCode(1_008_022_002, "量检具分类编码已存在");
    ErrorCode QMS_MEASURE_TOOL_LEDGER_NOT_EXISTS = new ErrorCode(1_008_022_003, "量检具台账不存在");
    ErrorCode QMS_MEASURE_TOOL_LEDGER_CODE_EXISTS = new ErrorCode(1_008_022_004, "量检具编码已存在");
    ErrorCode QMS_MEASURE_TOOL_APPLY_NOT_EXISTS = new ErrorCode(1_008_022_005, "量检具新增申请不存在");
    ErrorCode QMS_MEASURE_TOOL_APPLY_STATUS_INVALID = new ErrorCode(1_008_022_006, "当前申请状态不允许该操作");
    ErrorCode QMS_MEASURE_TOOL_TASK_NOT_EXISTS = new ErrorCode(1_008_022_007, "量检具校准预警任务不存在");
    ErrorCode QMS_MEASURE_TOOL_TASK_STATUS_INVALID = new ErrorCode(1_008_022_008, "当前校准任务状态不允许该操作");
    ErrorCode QMS_MEASURE_TOOL_RECORD_NOT_EXISTS = new ErrorCode(1_008_022_009, "量检具校准记录不存在");
    ErrorCode QMS_MEASURE_TOOL_VERSION_CONFLICT = new ErrorCode(1_008_022_010, "量检具数据已被其他用户修改，请刷新后重试");
    ErrorCode QMS_MEASURE_TOOL_MSA_NOT_ENABLED = new ErrorCode(1_008_022_011, "当前量检具未纳入 MSA 分析");
    ErrorCode QMS_MEASURE_TOOL_CATEGORY_HIERARCHY_INVALID = new ErrorCode(1_008_022_012, "量检具位置区域层级不合法");
    ErrorCode QMS_MEASURE_TOOL_CATEGORY_IN_USE = new ErrorCode(1_008_022_013, "当前位置或区域已被下级区域或台账使用，不能删除");

    // ========== 设备资源管理 1-008-023-000 ==========
    ErrorCode RESOURCE_DEVICE_CATEGORY_NOT_EXISTS = new ErrorCode(1_008_023_001, "设备分类不存在");
    ErrorCode RESOURCE_DEVICE_CATEGORY_CODE_EXISTS = new ErrorCode(1_008_023_002, "设备分类编码已存在");
    ErrorCode RESOURCE_DEVICE_LEDGER_NOT_EXISTS = new ErrorCode(1_008_023_003, "设备台账不存在");
    ErrorCode RESOURCE_DEVICE_LEDGER_CODE_EXISTS = new ErrorCode(1_008_023_004, "设备编号已存在");
    ErrorCode RESOURCE_DEVICE_MAINT_STANDARD_NOT_EXISTS = new ErrorCode(1_008_023_005, "设备保养标准不存在");
    ErrorCode RESOURCE_DEVICE_MAINT_STANDARD_CODE_EXISTS = new ErrorCode(1_008_023_006, "设备保养标准编号已存在");
    ErrorCode RESOURCE_DEVICE_MAINT_ORDER_NOT_EXISTS = new ErrorCode(1_008_023_007, "设备保养工单不存在");
    ErrorCode RESOURCE_DEVICE_MAINT_ORDER_STATUS_INVALID = new ErrorCode(1_008_023_008, "当前保养工单状态不允许该操作");
    ErrorCode RESOURCE_DEVICE_MAINT_RECORD_NOT_EXISTS = new ErrorCode(1_008_023_009, "设备保养执行记录不存在");
    ErrorCode RESOURCE_DEVICE_VERSION_CONFLICT = new ErrorCode(1_008_023_010, "设备管理数据已被其他用户修改，请刷新后重试");
    ErrorCode RESOURCE_DEVICE_MAINT_PLAN_NOT_EXISTS = new ErrorCode(1_008_023_011, "设备年度保养计划不存在");
    ErrorCode RESOURCE_DEVICE_EXCEPTION_NOT_EXISTS = new ErrorCode(1_008_023_012, "设备异常记录不存在");

    // ========== 供应商管理 SRM 1-008-024-000 ==========
    ErrorCode SRM_SUPPLIER_FILE_NOT_EXISTS = new ErrorCode(1_008_024_001, "供应商协议资质档案不存在");
    ErrorCode SRM_ONBOARDING_APPLY_NOT_EXISTS = new ErrorCode(1_008_024_002, "供应商导入申请不存在");
    ErrorCode SRM_ONBOARDING_APPLY_NO_EXISTS = new ErrorCode(1_008_024_003, "供应商导入申请单号已存在");
    ErrorCode SRM_SURVEY_NOT_EXISTS = new ErrorCode(1_008_024_004, "供应商调查表不存在");
    ErrorCode SRM_SURVEY_NO_EXISTS = new ErrorCode(1_008_024_005, "供应商调查表单号已存在");
    ErrorCode SRM_VERSION_CONFLICT = new ErrorCode(1_008_024_006, "供应商管理数据已被其他用户修改，请刷新后重试");
    ErrorCode SRM_DOCUMENT_NOT_EXISTS = new ErrorCode(1_008_024_007, "供应商管理业务单据不存在");
    ErrorCode SRM_DOCUMENT_NO_EXISTS = new ErrorCode(1_008_024_008, "供应商管理业务单号已存在");
    ErrorCode SRM_SUPPLIER_RESOURCE_NOT_EXISTS = new ErrorCode(1_008_024_009, "供应商资源池记录不存在或已失效");
    ErrorCode SRM_SUPPLIER_SELECTION_REQUIRED = new ErrorCode(1_008_024_010, "请选择供应商资源，或将未入库标记设为是后填写新供应商名称");
    ErrorCode SRM_REGISTERED_SUPPLIER_NAME_DUPLICATE = new ErrorCode(1_008_024_011, "供应商主表已有同名记录（代码：{}），请使用已有供应商");
    ErrorCode SRM_SUPPLIER_RESOURCE_NAME_DUPLICATE = new ErrorCode(1_008_024_012, "供应商资源池已有同名记录（临时代码：{}），请使用已有编号");
    ErrorCode SRM_SUPPLIER_RESOURCE_CONFIRM_REQUIRED = new ErrorCode(1_008_024_013, "未找到同名供应商，请确认初始化为考察中供应商资源");
    ErrorCode SRM_ATTACHMENT_NOT_EXISTS = new ErrorCode(1_008_024_014, "附件不存在或已被删除");
    ErrorCode SRM_ATTACHMENT_VERSION_CONFLICT = new ErrorCode(1_008_024_015, "附件已产生新版本，请刷新附件列表后重试");
    ErrorCode SRM_EVALUATION_TEMPLATE_NOT_EXISTS = new ErrorCode(1_008_024_016, "供应商评估模板不存在");
    ErrorCode SRM_EVALUATION_TEMPLATE_CODE_EXISTS = new ErrorCode(1_008_024_017, "供应商评估模板编码已存在");
    ErrorCode SRM_EVALUATION_TEMPLATE_VERSION_NOT_EXISTS = new ErrorCode(1_008_024_018, "供应商评估模板版本不存在");
    ErrorCode SRM_EVALUATION_TEMPLATE_VERSION_EXISTS = new ErrorCode(1_008_024_019, "供应商评估模板版本号已存在");
    ErrorCode SRM_EVALUATION_TEMPLATE_STATUS_INVALID = new ErrorCode(1_008_024_020, "当前模板版本状态不允许该操作");
    ErrorCode SRM_EVALUATION_TEMPLATE_SCORE_INVALID = new ErrorCode(1_008_024_021, "模板指标分值校验失败：{}");
    ErrorCode SRM_PRELIMINARY_EVALUATION_NOT_EXISTS = new ErrorCode(1_008_024_022, "供应商选择初评单不存在");
    ErrorCode SRM_PRELIMINARY_EVALUATION_NO_EXISTS = new ErrorCode(1_008_024_023, "供应商选择初评单号已存在");
    ErrorCode SRM_PRELIMINARY_EVALUATION_STATUS_INVALID = new ErrorCode(1_008_024_024, "当前初评单状态不允许该操作");
    ErrorCode SRM_PRELIMINARY_EVALUATION_SCORER_REQUIRED = new ErrorCode(1_008_024_025, "请先为所有评估指标分配评分人");
    ErrorCode SRM_PRELIMINARY_EVALUATION_SCORER_ONLY = new ErrorCode(1_008_024_026, "只有当前指标的评分人可以提交得分");
    ErrorCode SRM_PRELIMINARY_EVALUATION_SCORE_INVALID = new ErrorCode(1_008_024_027, "实际得分不能超过指标满分：{}");
    ErrorCode SRM_PRELIMINARY_EVALUATION_SCORE_INCOMPLETE = new ErrorCode(1_008_024_028, "尚有评分人未完成评分");
    ErrorCode SRM_PRELIMINARY_EVALUATION_OPERATOR_ONLY = new ErrorCode(1_008_024_029, "当前用户无权办理该初评单节点");
    ErrorCode SRM_PRELIMINARY_EVALUATION_GM_ROLE_REQUIRED = new ErrorCode(1_008_024_030, "所选办理人不具有总经理（供应商评估）角色");
    ErrorCode SRM_PRELIMINARY_EVALUATION_BPM_NOT_PUBLISHED = new ErrorCode(1_008_024_031, "供应商选择初评流程未发布，请先发布 SIMPLE 流程 srm_preliminary_evaluation");
    ErrorCode SRM_EVALUATION_TEMPLATE_ADMIN_REQUIRED = new ErrorCode(1_008_024_032, "仅供应商评估管理员可以维护、审核和发布评估模板");
    ErrorCode SRM_PRELIMINARY_EVALUATION_BPM_TASK_MISSING = new ErrorCode(1_008_024_033, "当前流程节点任务不存在或办理人不匹配，请刷新后重试");
    ErrorCode SRM_PRELIMINARY_EVALUATION_SCORE_LINE_INVALID = new ErrorCode(1_008_024_034, "合格线不能高于满分");
    ErrorCode SRM_SUPPLIER_SCOPE_NOT_EXISTS = new ErrorCode(1_008_024_035, "供应商名录管理范围不存在");
    ErrorCode SRM_SUPPLIER_SCOPE_CODE_EXISTS = new ErrorCode(1_008_024_036, "供应商名录管理范围编号已存在");
    ErrorCode SRM_SUPPLIER_SCOPE_IN_USE = new ErrorCode(1_008_024_037, "该供应商名录管理范围已关联供应商档案，不能删除");
    ErrorCode SRM_SUPPLIER_SCOPE_ACCESS_DENIED = new ErrorCode(1_008_024_038, "当前账号无权访问或维护该供应商范围");
    ErrorCode SRM_SUPPLIER_SCOPE_ADMIN_REQUIRED = new ErrorCode(1_008_024_039, "仅供应商超级管理员可以维护供应商名录范围和脱敏字段");
    ErrorCode SRM_PRELIMINARY_EVALUATION_TOTAL_SCORE_INVALID = new ErrorCode(1_008_024_040, "最终得分必须在 0 到满分之间");
    ErrorCode SRM_PRELIMINARY_PROJECT_NOT_EXISTS = new ErrorCode(1_008_024_085, "初评项目不存在");
    ErrorCode SRM_PRELIMINARY_PROJECT_CODE_EXISTS = new ErrorCode(1_008_024_086, "初评项目编码已存在");
    ErrorCode SRM_PRELIMINARY_PROJECT_IN_USE = new ErrorCode(1_008_024_087, "初评项目已被选择初评引用，不能删除");
    ErrorCode SRM_PERFORMANCE_SUPPLIER_CONFIG_NOT_EXISTS = new ErrorCode(1_008_024_088, "供应商季度评分配置不存在");
    ErrorCode SRM_PERFORMANCE_SUPPLIER_CONFIG_NO_EXISTS = new ErrorCode(1_008_024_089, "供应商季度评分配置编号已存在");
    ErrorCode SRM_PERFORMANCE_SUPPLIER_CONFIG_DUPLICATE = new ErrorCode(1_008_024_090, "该供应商已存在当前季度评价模板的配置");
    ErrorCode SRM_PERFORMANCE_SUPPLIER_CONFIG_IN_USE = new ErrorCode(1_008_024_091, "供应商季度评分配置已被季度评价引用，不能删除");
    ErrorCode SRM_PERFORMANCE_ACTUAL_REPORT_NOT_EXISTS = new ErrorCode(1_008_024_092, "供应商绩效实际上报单不存在");
    ErrorCode SRM_PERFORMANCE_ACTUAL_REPORT_NO_EXISTS = new ErrorCode(1_008_024_093, "供应商绩效实际上报单号已存在");
    ErrorCode SRM_PERFORMANCE_ACTUAL_REPORT_STATUS_INVALID = new ErrorCode(1_008_024_094, "当前实际上报单状态不允许该操作");
    ErrorCode SRM_PERFORMANCE_ACTUAL_REPORT_CONFIRM_REQUIRED = new ErrorCode(1_008_024_095, "只有供应商评估管理员可以确认实际上报数据");
    ErrorCode SRM_PERFORMANCE_QUARTER_EVALUATION_NOT_EXISTS = new ErrorCode(1_008_024_096, "供应商季度评价单不存在");
    ErrorCode SRM_PERFORMANCE_QUARTER_EVALUATION_NO_EXISTS = new ErrorCode(1_008_024_097, "供应商季度评价单号已存在");
    ErrorCode SRM_PERFORMANCE_QUARTER_EVALUATION_DUPLICATE = new ErrorCode(1_008_024_098, "该供应商当前年度季度与模板已存在评价单");
    ErrorCode SRM_PERFORMANCE_QUARTER_EVALUATION_STATUS_INVALID = new ErrorCode(1_008_024_099, "当前季度评价单状态不允许该操作");
    ErrorCode SRM_PERFORMANCE_QUARTER_SCORER_REQUIRED = new ErrorCode(1_008_024_100, "请先为所有人工评分指标分配评分人");
    ErrorCode SRM_PERFORMANCE_QUARTER_SCORER_ONLY = new ErrorCode(1_008_024_101, "只有当前指标的评分人可以提交得分");
    ErrorCode SRM_PERFORMANCE_QUARTER_SCORE_INVALID = new ErrorCode(1_008_024_102, "季度评价指标得分不能超过指标满分：{}");
    ErrorCode SRM_PERFORMANCE_QUARTER_SCORE_INCOMPLETE = new ErrorCode(1_008_024_103, "尚有季度评价指标未完成评分");
    ErrorCode SRM_PERFORMANCE_QUARTER_OPERATOR_ONLY = new ErrorCode(1_008_024_104, "当前用户无权办理该季度评价单");
    ErrorCode SRM_PERFORMANCE_QUARTER_ACTUAL_MISSING = new ErrorCode(1_008_024_105, "季度评价计算来源数据缺失：{}");
    ErrorCode SRM_PERFORMANCE_QUARTER_FORMULA_INVALID = new ErrorCode(1_008_024_106, "季度评价计算公式执行失败：{}");
    ErrorCode SRM_PERFORMANCE_QUARTER_SIGN_REQUIRED = new ErrorCode(1_008_024_107, "请先选择季度评价会签人员");
    ErrorCode SRM_PERFORMANCE_QUARTER_SIGNER_ONLY = new ErrorCode(1_008_024_108, "只有当前会签人可以提交会签意见");
    ErrorCode SRM_PERFORMANCE_QUARTER_SIGN_RESULT_INVALID = new ErrorCode(1_008_024_109, "季度评价会签结果必须为同意或不同意");
    ErrorCode SRM_SAMPLE_REQUEST_NOT_EXISTS = new ErrorCode(1_008_024_041, "样品需求单不存在");
    ErrorCode SRM_SAMPLE_REQUEST_NO_EXISTS = new ErrorCode(1_008_024_042, "样品需求单号已存在");
    ErrorCode SRM_SAMPLE_REQUEST_STATUS_INVALID = new ErrorCode(1_008_024_043, "当前样品需求单状态不允许该操作");
    ErrorCode SRM_SAMPLE_REQUEST_OPERATOR_ONLY = new ErrorCode(1_008_024_044, "当前用户无权办理该样品需求单节点");
    ErrorCode SRM_SAMPLE_REQUEST_REVIEWER_REQUIRED = new ErrorCode(1_008_024_045, "请先选择项目负责人、采购负责人或最终批准人");
    ErrorCode SRM_SAMPLE_REQUEST_BPM_NOT_PUBLISHED = new ErrorCode(1_008_024_046, "样品需求流程未发布，请先发布 SIMPLE 流程 srm_sample_request");
    ErrorCode SRM_SAMPLE_REQUEST_BPM_TASK_MISSING = new ErrorCode(1_008_024_047, "当前样品需求流程节点任务不存在或办理人不匹配，请刷新后重试");
    ErrorCode SRM_SAMPLE_EVALUATION_NOT_EXISTS = new ErrorCode(1_008_024_048, "样品评价单不存在");
    ErrorCode SRM_SAMPLE_EVALUATION_NO_EXISTS = new ErrorCode(1_008_024_049, "样品评价单号已存在");
    ErrorCode SRM_SAMPLE_EVALUATION_STATUS_INVALID = new ErrorCode(1_008_024_050, "当前样品评价单状态不允许该操作");
    ErrorCode SRM_SAMPLE_EVALUATION_OPERATOR_ONLY = new ErrorCode(1_008_024_051, "当前用户无权办理该样品评价节点");
    ErrorCode SRM_SAMPLE_EVALUATION_REVIEWER_REQUIRED = new ErrorCode(1_008_024_052, "请先选择样品评价办理人");
    ErrorCode SRM_SAMPLE_EVALUATION_BPM_NOT_PUBLISHED = new ErrorCode(1_008_024_053, "样品评价流程未发布，请先发布 SIMPLE 流程 srm_sample_evaluation_project");
    ErrorCode SRM_SAMPLE_EVALUATION_BPM_TASK_MISSING = new ErrorCode(1_008_024_054, "当前样品评价流程节点任务不存在或办理人不匹配，请刷新后重试");
    ErrorCode SRM_SAMPLE_EVALUATION_ITEM_REQUIRED = new ErrorCode(1_008_024_055, "请至少填写一项检验项目和值");
    ErrorCode SRM_SAMPLE_EVALUATION_SIGN_REQUIRED = new ErrorCode(1_008_024_056, "请先选择需要会签的部门人员");
    ErrorCode SRM_SAMPLE_EVALUATION_PRODUCTION_ATTACHMENT_REQUIRED = new ErrorCode(1_008_024_057, "生产部确认意见必须上传附件并填写意见");
    ErrorCode SRM_SAMPLE_EVALUATION_PROJECT_REQUIRED = new ErrorCode(1_008_024_110, "请选择样品评价项目");
    ErrorCode SRM_SAMPLE_EVALUATION_PROJECT_NOT_CONFIGURED = new ErrorCode(1_008_024_111, "样品评价项目未配置或未启用");
    ErrorCode SRM_SAMPLE_EVALUATION_INITIATOR_NOT_CONFIGURED = new ErrorCode(1_008_024_112, "当前用户不是该项目允许发起的责任人");
    ErrorCode SRM_SAMPLE_EVALUATION_INSPECTOR_OUT_OF_SCOPE = new ErrorCode(1_008_024_113, "所选检测办理人不在当前部门下属人员范围内");
    ErrorCode SRM_SAMPLE_EVALUATION_WITHDRAW_NOT_ALLOWED = new ErrorCode(1_008_024_114, "当前会签已有人办理或流程状态不允许撤回");
    ErrorCode SRM_SAMPLE_EVALUATION_PROJECT_CODE_EXISTS = new ErrorCode(1_008_024_115, "样品评价项目编码已存在");
    ErrorCode SRM_SAMPLE_EVALUATION_PROJECT_IN_USE = new ErrorCode(1_008_024_116, "样品评价项目已被样品评价表引用，不能删除");
    ErrorCode SRM_SAMPLE_EVALUATION_PROJECT_USER_REQUIRED = new ErrorCode(1_008_024_117, "启用的样品评价项目至少需要配置一个部门责任人");
    ErrorCode SRM_SAMPLE_EVALUATION_PROJECT_INITIATOR_REQUIRED = new ErrorCode(1_008_024_118, "启用的样品评价项目至少需要配置一个允许发起的责任人");
    ErrorCode SRM_SAMPLE_EVALUATION_ASSIGNER_NOT_CONFIGURED = new ErrorCode(1_008_024_119, "当前用户不是该项目允许分配检测人的责任人");
    ErrorCode SRM_SAMPLE_EVALUATION_APPLY_DEPT_REQUIRED = new ErrorCode(1_008_024_120, "请选择或输入申请部门");
    ErrorCode SRM_SAMPLE_EVALUATION_SAVE_INITIATOR_ONLY = new ErrorCode(1_008_024_121, "项目 {}，只有 {} 用户才能保存发起！");
    ErrorCode SRM_SAMPLE_EVALUATION_SAVE_INITIATOR_NOT_CONFIGURED = new ErrorCode(1_008_024_122, "项目 {}，未配置允许保存发起的责任人，请先维护样品评价项目配置！");
    ErrorCode SRM_SAMPLE_EVALUATION_FINAL_APPROVER_NOT_CONFIGURED = new ErrorCode(1_008_024_123, "样品评价最终审批人未配置，请先维护角色“样品评价最终审批人”并给用户授权！");
    ErrorCode SRM_ONBOARDING_APPLY_STATUS_INVALID = new ErrorCode(1_008_024_058, "当前导入申请状态不允许该操作");
    ErrorCode SRM_ONBOARDING_APPLY_OPERATOR_ONLY = new ErrorCode(1_008_024_059, "当前用户无权办理该导入申请节点");
    ErrorCode SRM_ONBOARDING_APPLY_REVIEWER_REQUIRED = new ErrorCode(1_008_024_060, "请先选择导入申请各审批节点办理人");
    ErrorCode SRM_ONBOARDING_APPLY_BPM_NOT_PUBLISHED = new ErrorCode(1_008_024_061, "导入申请流程未发布，请先发布 SIMPLE 流程 srm_onboarding_apply");
    ErrorCode SRM_ONBOARDING_APPLY_BPM_TASK_MISSING = new ErrorCode(1_008_024_062, "当前导入申请流程节点任务不存在或办理人不匹配，请刷新后重试");
    ErrorCode SRM_ONBOARDING_APPLY_REVIEW_RESULT_INVALID = new ErrorCode(1_008_024_063, "导入申请审批结果必须为同意或不同意");
    ErrorCode SRM_ONBOARDING_APPLY_APPLY_NO_EXISTS = new ErrorCode(1_008_024_064, "导入申请单号已存在");
    ErrorCode SRM_ONBOARDING_APPLY_SIGN_REQUIRED = new ErrorCode(1_008_024_065, "请先选择导入申请会签人员");
    ErrorCode SRM_ONBOARDING_APPLY_ENTRY_REQUIRED = new ErrorCode(1_008_024_066, "请先完成物料编码和供方清单录入");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_NOT_EXISTS = new ErrorCode(1_008_024_070, "供方退出审批不存在");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_NO_EXISTS = new ErrorCode(1_008_024_071, "供方退出审批单号已存在");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_STATUS_INVALID = new ErrorCode(1_008_024_072, "当前供方退出审批状态不允许该操作");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_OPERATOR_ONLY = new ErrorCode(1_008_024_073, "当前用户无权办理该供方退出审批节点");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_REVIEWER_REQUIRED = new ErrorCode(1_008_024_074, "请先选择供方退出审批各节点办理人");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_BPM_NOT_PUBLISHED = new ErrorCode(1_008_024_075, "供方退出审批流程未发布，请先发布 SIMPLE 流程 srm_supplier_exit_approval");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_BPM_TASK_MISSING = new ErrorCode(1_008_024_076, "当前供方退出审批流程节点任务不存在或办理人不匹配，请刷新后重试");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_REVIEW_RESULT_INVALID = new ErrorCode(1_008_024_077, "供方退出审批结果必须为同意或不同意");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_SIGN_REQUIRED = new ErrorCode(1_008_024_078, "请先选择供方退出审批会签人员");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_ENTRY_REQUIRED = new ErrorCode(1_008_024_079, "请先完成退出闭环办理");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_QUALIFIED_REQUIRED = new ErrorCode(1_008_024_085, "供方退出审批只能选择合格供方范围内的供应商");
    ErrorCode SRM_SUPPLIER_EXIT_APPROVAL_MATERIAL_REQUIRED = new ErrorCode(1_008_024_086, "合格供方未维护物料编码或物料名称");
    ErrorCode SRM_SUPPLIER_RESOURCE_STATUS_INVALID = new ErrorCode(1_008_024_080, "资源状态不允许调整为：{}");
    ErrorCode SRM_SUPPLIER_REVIEW_PLAN_NOT_EXISTS = new ErrorCode(1_008_024_081, "供方评审计划不存在");
    ErrorCode SRM_SUPPLIER_REVIEW_MONTH_PLAN_NOT_EXISTS = new ErrorCode(1_008_024_082, "供方评审月份计划不存在");
    ErrorCode SRM_SUPPLIER_REVIEW_PLAN_SUPPLIER_REQUIRED = new ErrorCode(1_008_024_083, "请选择供应商后再追加年度计划");
    ErrorCode SRM_SUPPLIER_REVIEW_PLAN_STATUS_INVALID = new ErrorCode(1_008_024_084, "供方评审计划状态不允许调整为：{}");
    ErrorCode SRM_SUPPLIER_REVIEW_MONTH_PLAN_CLEAR_NOT_ALLOWED = new ErrorCode(1_008_024_087, "该供方评审月份计划已有执行事实，不允许清空，请走状态取消或归档流程");
    ErrorCode SRM_SUPPLIER_REVIEW_PLAN_DELETE_FORBIDDEN = new ErrorCode(1_008_024_110, "仅具备供方评审计划删除权限的角色可以删除年度计划");
    ErrorCode SRM_SUPPLIER_REVIEW_EXECUTION_VIEW_ALL_FORBIDDEN = new ErrorCode(1_008_024_111, "无权限查看全部供方评审计划，请联系管理员分配查看全部评审计划权限");
    ErrorCode SRM_SCAR_NOT_EXISTS = new ErrorCode(1_008_024_112, "供方异常与整改台账不存在");
    ErrorCode SRM_SCAR_NO_EXISTS = new ErrorCode(1_008_024_113, "供方异常与整改台账编号已存在");
    ErrorCode SRM_TRIAL_VALIDATION_NOT_EXISTS = new ErrorCode(1_008_024_114, "试产验证跟踪单不存在");
    ErrorCode SRM_TRIAL_VALIDATION_NO_EXISTS = new ErrorCode(1_008_024_115, "试产验证跟踪单号已存在");
    ErrorCode SRM_TRIAL_VALIDATION_STATUS_INVALID = new ErrorCode(1_008_024_116, "当前试产验证状态不允许该操作");
    ErrorCode SRM_TRIAL_VALIDATION_OPERATOR_ONLY = new ErrorCode(1_008_024_117, "当前用户无权办理该试产验证节点");
    ErrorCode SRM_TRIAL_VALIDATION_REVIEWER_REQUIRED = new ErrorCode(1_008_024_118, "请先维护试产验证流程办理角色及成员");
    ErrorCode SRM_TRIAL_VALIDATION_BPM_NOT_PUBLISHED = new ErrorCode(1_008_024_119, "试产验证流程未发布，请先发布 SIMPLE 流程 srm_trial_validation");
    ErrorCode SRM_TRIAL_VALIDATION_BPM_TASK_MISSING = new ErrorCode(1_008_024_120, "当前试产验证流程节点任务不存在或办理人不匹配，请刷新后重试");
    ErrorCode SRM_TRIAL_VALIDATION_SOURCE_REQUIRED = new ErrorCode(1_008_024_121, "样品评价单缺少供应商、物料或样品数量信息，不能下达试生产通知");
    ErrorCode SRM_TRIAL_VALIDATION_DUPLICATE = new ErrorCode(1_008_024_122, "该样品评价单已下达试产验证跟踪单，请勿重复下达");

    // ========== COA 报告管理 1-008-025-000 ==========
    ErrorCode QMS_COA_TEMPLATE_NOT_EXISTS = new ErrorCode(1_008_025_001, "COA模板不存在");
    ErrorCode QMS_COA_TEMPLATE_DUPLICATE = new ErrorCode(1_008_025_002, "COA模板编码和版本已存在");
    ErrorCode QMS_COA_TEMPLATE_STATUS_INVALID = new ErrorCode(1_008_025_003, "当前COA模板状态不允许该操作");
    ErrorCode QMS_COA_TEMPLATE_NOT_MATCHED = new ErrorCode(1_008_025_004, "没有匹配到已审核启用的COA模板");
    ErrorCode QMS_COA_REPORT_NOT_EXISTS = new ErrorCode(1_008_025_005, "COA报告不存在");
    ErrorCode QMS_COA_REPORT_STATUS_INVALID = new ErrorCode(1_008_025_006, "当前COA报告状态不允许该操作");
    ErrorCode QMS_COA_REPORT_ITEM_NOT_EXISTS = new ErrorCode(1_008_025_007, "COA报告项目不存在");
    ErrorCode QMS_COA_REPORT_ITEM_NOT_CORRECTABLE = new ErrorCode(1_008_025_008, "该COA项目不允许人工修正");
    ErrorCode QMS_COA_REPORT_INCOMPLETE = new ErrorCode(1_008_025_009, "COA报告必填项目存在缺失，请补充或修正后再提交");
    ErrorCode QMS_COA_FAI_NOT_FOUND = new ErrorCode(1_008_025_010, "未找到当前批次的有效FAI记录");
    ErrorCode QMS_COA_SHIPPING_NOTICE_NOT_FOUND = new ErrorCode(1_008_025_011, "出货通知不存在");
    ErrorCode QMS_COA_STANDARD_ITEM_INVALID = new ErrorCode(1_008_025_012, "COA模板关联的FAI标准项目无效");
    ErrorCode QMS_COA_SHIPPING_GATE_NOT_PASSED = new ErrorCode(1_008_025_013, "出货质量状态未放行，不能签发COA");
    ErrorCode QMS_COA_REPORT_REFRESH_AFTER_CORRECTION = new ErrorCode(1_008_025_014, "COA报告已有人工修正历史，禁止刷新来源；请保留当前报告或创建新版本");
    ErrorCode QMS_COA_REPORT_ITEM_VALUE_INVALID = new ErrorCode(1_008_025_015, "COA项目取值来源、附件或聚合规则无效");
    ErrorCode QMS_COA_MOTHER_BATCH_NOT_FOUND = new ErrorCode(1_008_025_016, "未找到与COA模板产品匹配的母批次检验记录");
}

package cn.iocoder.yudao.module.mes.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * 禾臣 MES 基础资料错误码
 */
public interface HcErrorCodeConstants {

    // ========== 物料分类 ==========
    ErrorCode HCMATERIALCATEGORY_NOT_EXISTS = new ErrorCode(1008100001, "物料分类不存在");
    ErrorCode HCMATERIALCATEGORY_CATEGORYCODE_EXISTS = new ErrorCode(1008100002, "分类编码已存在");

    // ========== 物料主数据 ==========
    ErrorCode HCMATERIAL_NOT_EXISTS = new ErrorCode(1008100003, "物料主数据不存在");
    ErrorCode HCMATERIAL_MATERIALCODE_EXISTS = new ErrorCode(1008100004, "物料编码已存在");
    ErrorCode HCMATERIAL_DEFAULT_ROUTE_INVALID = new ErrorCode(1008100038, "默认工艺路线必须是当前物料适配的工艺路线");

    // ========== 型号编码规则 ==========
    ErrorCode HCMODELRULE_NOT_EXISTS = new ErrorCode(1008100005, "型号编码规则不存在");
    ErrorCode HCMODELRULE_RULECODE_EXISTS = new ErrorCode(1008100006, "规则编码已存在");
    ErrorCode HCMODELRULE_GENERATE_ITEMS_EMPTY = new ErrorCode(1008100033, "生成编码失败，规则字段不能为空");
    ErrorCode HCMODELRULE_GENERATE_ITEM_CODE_EMPTY = new ErrorCode(1008100034, "生成编码失败，存在未维护字段编码的规则字段");
    ErrorCode HCMODELRULE_GENERATE_REQUIRED_VALUE_EMPTY = new ErrorCode(1008100035, "字段【{}】不能为空");
    ErrorCode HCMODELRULE_GENERATE_INVALID_DICT_VALUE = new ErrorCode(1008100036, "字段【{}】的值【{}】不在字典范围内");
    ErrorCode HCMODELRULE_GENERATE_LENGTH_MISMATCH = new ErrorCode(1008100037, "字段【{}】长度必须为 {} 位，当前为 {} 位");

    // ========== 产品型号字典 ==========
    ErrorCode HCPRODUCTMODEL_NOT_EXISTS = new ErrorCode(1008100042, "产品型号字典不存在");
    ErrorCode HCPRODUCTMODEL_MODELCODE_EXISTS = new ErrorCode(1008100043, "产品型号编码已存在");
    ErrorCode HCPRODUCTMODEL_REFERENCED_LOCKED = new ErrorCode(1008100044, "产品型号已被引用，关键字段不允许修改");
    ErrorCode HCPRODUCTMODEL_REFERENCED_DELETE_DENIED = new ErrorCode(1008100045, "产品型号已被业务引用，不允许删除，请改为停用");

    // ========== 成品胶板对照表 ==========
    ErrorCode HCFINISHEDGLUEBOARDMAP_NOT_EXISTS = new ErrorCode(1008100095, "成品胶板对照不存在");
    ErrorCode HCFINISHEDGLUEBOARDMAP_MODEL_SPEC_EXISTS = new ErrorCode(1008100096, "同一产品型号和成品规格的胶板对照已存在");
    ErrorCode HCFINISHEDGLUEBOARDMAP_ITEMS_EMPTY = new ErrorCode(1008100097, "请至少维护一条胶板对照明细");


    // ========== 配方 ==========
    ErrorCode HCRECIPE_NOT_EXISTS = new ErrorCode(1008100007, "配方不存在");
    ErrorCode HCRECIPE_RECIPECODE_EXISTS = new ErrorCode(1008100008, "配方编码已存在");

    // ========== BOM ==========
    ErrorCode HCBOM_NOT_EXISTS = new ErrorCode(1008100009, "BOM不存在");
    ErrorCode HCBOM_BOMCODE_EXISTS = new ErrorCode(1008100010, "BOM编码已存在");

    // ========== 工艺路线 ==========
    ErrorCode HCROUTE_NOT_EXISTS = new ErrorCode(1008100011, "工艺路线不存在");
    ErrorCode HCROUTE_ROUTECODE_EXISTS = new ErrorCode(1008100012, "路线编码已存在");

    // ========== 工作中心 ==========
    ErrorCode HCWORKCENTER_NOT_EXISTS = new ErrorCode(1008100013, "工作中心不存在");
    ErrorCode HCWORKCENTER_WCCODE_EXISTS = new ErrorCode(1008100014, "工作中心编码已存在");
    ErrorCode HCWORKCENTER_PROCESS_REQUIRED = new ErrorCode(1008100098, "工作中心必须选择标准工序");
    ErrorCode HCWORKCENTER_PROCESS_INVALID = new ErrorCode(1008100099, "选择的标准工序不存在或未启用");

    // ========== 班组 ==========
    ErrorCode HCTEAM_NOT_EXISTS = new ErrorCode(1008100015, "班组不存在");
    ErrorCode HCTEAM_TEAMCODE_EXISTS = new ErrorCode(1008100016, "班组编码已存在");

    // ========== 设备台账 ==========
    ErrorCode HCEQUIPMENT_NOT_EXISTS = new ErrorCode(1008100017, "设备台账不存在");
    ErrorCode HCEQUIPMENT_EQUIPMENTCODE_EXISTS = new ErrorCode(1008100018, "设备编码已存在");

    // ========== 批号规则 ==========
    ErrorCode HCLOTRULE_NOT_EXISTS = new ErrorCode(1008100019, "批号规则不存在");
    ErrorCode HCLOTRULE_RULECODE_EXISTS = new ErrorCode(1008100020, "规则编码已存在");

    // ========== 库位 ==========
    ErrorCode HCLOCATION_NOT_EXISTS = new ErrorCode(1008100021, "库位不存在");
    ErrorCode HCLOCATION_LOCATIONCODE_EXISTS = new ErrorCode(1008100022, "库位编码已存在");

    // ========== 货主 ==========
    ErrorCode HCOWNER_NOT_EXISTS = new ErrorCode(1008100023, "货主不存在");
    ErrorCode HCOWNER_OWNERCODE_EXISTS = new ErrorCode(1008100024, "货主编码已存在");

    // ========== 先进先出策略 ==========
    ErrorCode HCFIFOPOLICY_NOT_EXISTS = new ErrorCode(1008100025, "先进先出策略不存在");
    ErrorCode HCFIFOPOLICY_POLICYCODE_EXISTS = new ErrorCode(1008100026, "策略编码已存在");

    // ========== 终端工位 ==========
    ErrorCode HCTERMINAL_NOT_EXISTS = new ErrorCode(1008100027, "终端工位不存在");
    ErrorCode HCTERMINAL_TERMINALCODE_EXISTS = new ErrorCode(1008100028, "终端编码已存在");

    // ========== 表单模板 ==========
    ErrorCode HCFORMTEMPLATE_NOT_EXISTS = new ErrorCode(1008100029, "表单模板不存在");
    ErrorCode HCFORMTEMPLATE_TEMPLATECODE_EXISTS = new ErrorCode(1008100030, "模板编码已存在");

    // ========== 工位动态表单 ==========
    ErrorCode HCSTATIONFORM_NOT_EXISTS = new ErrorCode(1008100050, "工位动态表单不存在");
    ErrorCode HCSTATIONFORM_FORMCODE_EXISTS = new ErrorCode(1008100051, "工位动态表单编码已存在");
    ErrorCode HCSTATIONFORM_IMPORT_INVALID = new ErrorCode(1008100052, "工位动态表单导入数据无效");
    ErrorCode HCSTATIONFORM_DEV_CONTRACT_INVALID = new ErrorCode(1008100213,
            "DEV 表单编码必须以 _DEV 结尾，且 _DEV 表单必须标记 devOnly=true");
    ErrorCode HCSTATIONRECORD_NOT_EXISTS = new ErrorCode(1008100053, "工位记录不存在");

    // ========== OCAP模板 ==========
    ErrorCode HCOCAPTEMPLATE_NOT_EXISTS = new ErrorCode(1008100031, "OCAP模板不存在");
    ErrorCode HCOCAPTEMPLATE_OCAPCODE_EXISTS = new ErrorCode(1008100032, "OCAP编码已存在");

    ErrorCode HCPARAMRECORD_REPORT_NOT_EXISTS = new ErrorCode(1008100039, "报工单不存在");

    ErrorCode HCPLANORDER_NOT_EXISTS = new ErrorCode(1008100040, "Production plan does not exist");
    ErrorCode HCPLANORDER_PLANNO_EXISTS = new ErrorCode(1008100041, "Production plan number already exists");

    // ========== 缺陷代码库 ==========
    ErrorCode HCDEFECTCODE_NOT_EXISTS = new ErrorCode(1008100040, "缺陷代码不存在");
    ErrorCode HCDEFECTCODE_CODE_EXISTS = new ErrorCode(1008100041, "缺陷代码已存在");
    ErrorCode HCDEFECTCODE_HAS_CHILDREN = new ErrorCode(1008100042, "存在子节点，禁止直接删除");
    ErrorCode HCDEFECTCODE_PARENT_INVALID = new ErrorCode(1008100043, "父级缺陷分类不合法");
    ErrorCode HCDEFECTCAUSE_CODE_EXISTS = new ErrorCode(1008100047, "同一缺陷项下原因代码重复");
    ErrorCode HCDEFECTCAUSE_INVALID = new ErrorCode(1008100048, "发生原因不合法");
    ErrorCode HCQUALITYSTANDARD_NOT_EXISTS = new ErrorCode(1008100044, "检验标准不存在");
    ErrorCode HCQUALITYSTANDARD_BIZ_EXISTS = new ErrorCode(1008100045, "同物料、同工序、同环节、同版本下的检验标准已存在");
    ErrorCode HCQUALITYSTANDARD_AUDITED = new ErrorCode(1008100046, "检验标准已审核，请勿重复操作");
    ErrorCode HCQUALITYSTANDARD_NO_EXISTS = new ErrorCode(1008100049, "检验标准编号已存在");
    ErrorCode HCQUALITYSTANDARD_PROCESS_REQUIRED = new ErrorCode(1008100066, "关联工序信息不完整，请从候选列表中选择关联工序");
    ErrorCode HCQUALITYSTANDARD_SCOPE_REQUIRED = new ErrorCode(1008100067, "检验标准必须至少选择关联物料或关联工序");
    ErrorCode HCQUALITYSTANDARD_MATERIAL_REQUIRED = new ErrorCode(1008100068, "关联物料信息不完整，请从候选列表中选择关联物料");
    ErrorCode HCQUALITYSTANDARD_APPLY_TYPE_INVALID = new ErrorCode(1008100089, "检验标准适用环节不合法");
    ErrorCode HCQUALITYSTANDARD_APPLY_TYPE_MISMATCH = new ErrorCode(1008100090, "检验标准不属于当前菜单环节，禁止跨环节操作");
    ErrorCode HCQUALITYSTANDARD_REFERENCED = new ErrorCode(1008100091, "检验标准已被执行单引用，不允许删除，请改为停用或新建版本");
    ErrorCode HCQUALITYSTANDARD_ENTRY_RULE_INVALID = new ErrorCode(1008100094, "检验标准数据处理规则不合法，请检查输入字段、公式和判定指标");
    ErrorCode HCQUALITYSTANDARD_AUDIT_RESULT_INVALID = new ErrorCode(1008100101, "检验标准审核结果不正确");
    ErrorCode HCQUALITYSTANDARD_REJECT_REASON_REQUIRED = new ErrorCode(1008100102, "检验标准驳回原因不能为空");
    ErrorCode HCQUALITYSTANDARD_AUDIT_SNAPSHOT_NOT_EXISTS = new ErrorCode(1008100103, "未找到可恢复的检验标准修改前快照");
    ErrorCode HCQUALITYSTANDARD_AUDIT_SNAPSHOT_INVALID = new ErrorCode(1008100104, "检验标准修改前快照数据不完整，无法恢复");
    ErrorCode HCQUALITYSTANDARD_PRODUCT_MODEL_REQUIRED = new ErrorCode(1008100105, "产品型号信息不完整，请从候选列表中选择产品型号");
    ErrorCode HCQUALITYSTANDARD_GLUE_BOARD_MODEL_REQUIRED = new ErrorCode(1008100106, "胶板检验标准必须选择胶板型号");
    ErrorCode HCQUALITYSTANDARD_ENTRY_RULE_TEMPLATE_NOT_EXISTS = new ErrorCode(1008100107, "录入规则模板不存在或已删除");
    ErrorCode HCQUALITYSTANDARD_ROLE_SCOPE_ROLE_REQUIRED = new ErrorCode(1008100131, "请选择要维护范围的角色");
    ErrorCode HCQUALITYSTANDARD_ROLE_SCOPE_TYPE_INVALID = new ErrorCode(1008100132, "角色检验标准范围类型不合法");
    ErrorCode HCQUALITYSTANDARD_ROLE_SCOPE_STANDARD_REQUIRED = new ErrorCode(1008100133, "请选择要加入范围的检验标准");
    ErrorCode HCQUALITYSTANDARD_ROLE_SCOPE_STANDARD_INVALID = new ErrorCode(1008100134, "检验标准不存在或不属于当前范围");
    ErrorCode HCQUALITYSTANDARD_DATE_ITEM_INVALID = new ErrorCode(1008100135, "时间类型仅适用于IQC，且必须设置大于等于0的过期天数");

    // ========== IQC进料检验单 ==========
    ErrorCode HCIQC_NOT_EXISTS = new ErrorCode(1008100050, "IQC进料检验单不存在");
    ErrorCode HCIQC_NO_EXISTS = new ErrorCode(1008100051, "IQC检验单号已存在");
    ErrorCode HCIQC_FINISHED_LOCKED = new ErrorCode(1008100052, "已完成的IQC检验单不允许直接修改或删除");
    ErrorCode HCIQC_ITEMS_EMPTY = new ErrorCode(1008100053, "IQC检验项不能为空");
    ErrorCode HCIQC_ITEMS_NOT_COMPLETED = new ErrorCode(1008100054, "请确保所有检验项均已完成判定");
    ErrorCode HCIQC_STANDARD_NOT_EXISTS = new ErrorCode(1008100055, "未找到启用且已审核的 IQC 检验标准，请先维护检验标准定义");
    ErrorCode HCIQC_STANDARD_ITEMS_EMPTY = new ErrorCode(1008100056, "IQC检验标准未维护检测项目明细");
    ErrorCode HCIQC_ITEM_SOURCE_INVALID = new ErrorCode(1008100057, "IQC检验项必须来源于检验标准定义明细");
    ErrorCode HCIQC_WAIT_CONFIRM_REQUIRED = new ErrorCode(1008100092, "请先提交IQC检验结果并流转至待确认");
    ErrorCode HCIQC_AUDIT_RESULT_INVALID = new ErrorCode(1008100093, "IQC审核结果不正确");
    ErrorCode HCIQC_RETURN_REASON_REQUIRED = new ErrorCode(1008100098, "IQC退回原因不能为空");
    ErrorCode HCIQC_DATE_FUTURE_INVALID = new ErrorCode(1008100136, "检验项目【{}】选择的日期不能晚于当前日期");
    ErrorCode HCIQC_DATE_RULE_MISSING = new ErrorCode(1008100137, "检验项目【{}】缺少过期天数快照，请重新生成IQC检验单");
    ErrorCode HCIQC_ATTACHMENT_DISABLED = new ErrorCode(1008100138, "检验项目【{}】未启用附件上传");
    ErrorCode HCIQC_ATTACHMENT_TOO_MANY = new ErrorCode(1008100139, "检验项目【{}】最多上传10个附件");

    // ========== FAI首件检验单 ==========
    ErrorCode HCFAI_NOT_EXISTS = new ErrorCode(1008100056, "FAI首件检验单不存在");
    ErrorCode HCFAI_NO_EXISTS = new ErrorCode(1008100057, "FAI首检单号已存在");
    ErrorCode HCFAI_FINISHED_LOCKED = new ErrorCode(1008100058, "已完成或已驳回的FAI首件检验单不允许直接修改或删除");
    ErrorCode HCFAI_ITEMS_EMPTY = new ErrorCode(1008100059, "FAI检验项不能为空");
    ErrorCode HCFAI_OPERATOR_ITEMS_NOT_COMPLETED = new ErrorCode(1008100060, "请确保所有机长自检项均已完成判定");
    ErrorCode HCFAI_QA_ITEMS_NOT_COMPLETED = new ErrorCode(1008100061, "请确保所有品质复核项均已完成判定");
    ErrorCode HCFAI_STANDARD_NOT_EXISTS = new ErrorCode(1008100062, "未找到启用且已审核的FAI检验标准");
    ErrorCode HCFAI_WAIT_QA_REQUIRED = new ErrorCode(1008100063, "请先完成机长自检并流转至待品质复核");
    ErrorCode HCFAI_STANDARD_ITEMS_EMPTY = new ErrorCode(1008100064, "FAI检验标准未维护检测项目明细");
    ErrorCode HCFAI_ITEM_SOURCE_INVALID = new ErrorCode(1008100065, "FAI检验项必须来源于检验标准定义明细");
    ErrorCode HCFAI_VALUE_TEMPLATE_REQUIRED = new ErrorCode(1008100085, "FAI定量检验项必须维护录入值模板");
    ErrorCode HCFAI_VALUE_TEMPLATE_CALC_INVALID = new ErrorCode(1008100086, "FAI录入值模板计算失败，请检查原始录入值");
    ErrorCode HCFAI_SHEET_TEMPLATE_NOT_EXISTS = new ErrorCode(1008100087, "FAI原始记录表模板不存在或未启用");
    ErrorCode HCFAI_SHEET_IMPORT_INVALID = new ErrorCode(1008100088, "FAI原始记录表导入失败，请检查模板、样本量和单元格数据");
    ErrorCode HCFAI_AUDIT_NOT_COMPLETED = new ErrorCode(1008100095, "请先完成所有检测项录入和判定后再审核");
    ErrorCode HCFAI_RETURN_REASON_REQUIRED = new ErrorCode(1008100096, "驳回原因不能为空");
    ErrorCode HCFAI_AUDIT_RESULT_INVALID = new ErrorCode(1008100097, "FAI审核结果不正确");
    ErrorCode HCFAI_ITEM_AUDIT_NOT_COMPLETED = new ErrorCode(1008100181, "请先完成所有FAI样本组的审核确认");
    ErrorCode HCFAI_STANDARD_BIND_REQUIRED = new ErrorCode(1008100128, "请先为检验任务选择检验标准");
    ErrorCode HCFAI_STANDARD_ALREADY_BOUND = new ErrorCode(1008100129, "当前检验任务已绑定检验标准，不允许重复绑定");
    ErrorCode HCFAI_STANDARD_BIND_REASON_REQUIRED = new ErrorCode(1008100130, "所选标准与送检物料、型号或工段不完全匹配，请填写选择原因");
    ErrorCode HCFAI_ATTACHMENT_DISABLED = new ErrorCode(1008100210, "首检项目【{}】未启用附件上传");
    ErrorCode HCFAI_ATTACHMENT_TOO_MANY = new ErrorCode(1008100211, "首检项目【{}】最多上传10个附件");
    ErrorCode HCFAI_RECHECK_SOURCE_INVALID = new ErrorCode(1008100214, "只能对不合格的原检或加检FAI首件检验单申请复检");
    ErrorCode HCFAI_RECHECK_APPLY_PENDING = new ErrorCode(1008100215, "当前首件检验单已有待审核复检申请");
    ErrorCode HCFAI_RECHECK_APPLY_NOT_EXISTS = new ErrorCode(1008100216, "FAI复检申请不存在");
    ErrorCode HCFAI_RECHECK_APPLY_STATUS_INVALID = new ErrorCode(1008100217, "只有待审核复检申请允许审核");
    ErrorCode HCFAI_RECHECK_APPLY_GENERATED = new ErrorCode(1008100218, "当前首件检验单已审核生成复检单，不允许重复申请");

    // ========== IPQC过程抽检 ==========
    ErrorCode HCIPQC_NOT_EXISTS = new ErrorCode(1008100069, "IPQC过程抽检单不存在");
    ErrorCode HCIPQC_NO_EXISTS = new ErrorCode(1008100070, "IPQC过程抽检单号已存在");
    ErrorCode HCIPQC_FINISHED_LOCKED = new ErrorCode(1008100071, "已完成或已异常结案的IPQC过程抽检单不允许直接修改或删除");
    ErrorCode HCIPQC_ITEMS_EMPTY = new ErrorCode(1008100072, "IPQC检验项不能为空");
    ErrorCode HCIPQC_ITEMS_NOT_COMPLETED = new ErrorCode(1008100073, "请确保所有IPQC检验项均已完成判定");
    ErrorCode HCIPQC_STANDARD_NOT_EXISTS = new ErrorCode(1008100074, "未找到启用且已审核的IPQC检验标准");
    ErrorCode HCIPQC_STANDARD_ITEMS_EMPTY = new ErrorCode(1008100075, "IPQC检验标准未维护检测项目明细");
    ErrorCode HCIPQC_ITEM_SOURCE_INVALID = new ErrorCode(1008100076, "IPQC检验项必须来源于检验标准定义明细");

    // ========== FQC成品检验 ==========
    ErrorCode HCFQC_NOT_EXISTS = new ErrorCode(1008100077, "FQC成品检验单不存在");
    ErrorCode HCFQC_NO_EXISTS = new ErrorCode(1008100078, "FQC成品检验单号已存在");
    ErrorCode HCFQC_FINISHED_LOCKED = new ErrorCode(1008100079, "已完成或已拦截的FQC成品检验单不允许直接修改或删除");
    ErrorCode HCFQC_ITEMS_EMPTY = new ErrorCode(1008100080, "FQC检验项不能为空");
    ErrorCode HCFQC_ITEMS_NOT_COMPLETED = new ErrorCode(1008100081, "请确保所有FQC检验项均已完成判定");
    ErrorCode HCFQC_STANDARD_NOT_EXISTS = new ErrorCode(1008100082, "未找到启用且已审核的FQC检验标准");
    ErrorCode HCFQC_STANDARD_ITEMS_EMPTY = new ErrorCode(1008100083, "FQC检验标准未维护检测项目明细");
    ErrorCode HCFQC_ITEM_SOURCE_INVALID = new ErrorCode(1008100084, "FQC检验项必须来源于检验标准定义明细");
    ErrorCode HCFQC_SAMPLE_DEFECT_REQUIRED = new ErrorCode(1008100107, "FQC NG样本【{}】必须选择缺陷码");
    ErrorCode HCFQC_SAMPLE_DEFECT_INVALID = new ErrorCode(1008100108, "FQC缺陷码【{}】必须为启用的缺陷项");

    // ========== OQC出货检验 ==========
    ErrorCode HCOQC_NOT_EXISTS = new ErrorCode(1008100109, "OQC出货检验单不存在");
    ErrorCode HCOQC_NO_EXISTS = new ErrorCode(1008100110, "OQC出货检验单号已存在");
    ErrorCode HCOQC_FINISHED_LOCKED = new ErrorCode(1008100111, "已完成或已拦截的OQC出货检验单不允许直接修改或删除");
    ErrorCode HCOQC_ITEMS_EMPTY = new ErrorCode(1008100112, "OQC检验项不能为空");
    ErrorCode HCOQC_ITEMS_NOT_COMPLETED = new ErrorCode(1008100113, "请确保所有OQC检验项均已完成判定");
    ErrorCode HCOQC_STANDARD_NOT_EXISTS = new ErrorCode(1008100114, "未找到启用且已审核的OQC出货检验标准");
    ErrorCode HCOQC_STANDARD_ITEMS_EMPTY = new ErrorCode(1008100115, "OQC出货检验标准未维护检测项目明细");
    ErrorCode HCOQC_ITEM_SOURCE_INVALID = new ErrorCode(1008100116, "OQC检验项必须来源于出货检验标准定义明细");
    ErrorCode HCOQC_SHIPPING_NOTICE_NOT_EXISTS = new ErrorCode(1008100117, "发货通知单或明细不存在，无法生成OQC出货检验单");

    // ========== 打印程序版本 ==========
    ErrorCode HCPRINTAGENTPACKAGE_NOT_EXISTS = new ErrorCode(1008100120, "打印程序版本不存在");
    ErrorCode HCPRINTAGENTPACKAGE_VERSION_EXISTS = new ErrorCode(1008100121, "同一打印程序版本号已存在");
    ErrorCode HCPRINTAGENTPACKAGE_PACKAGE_URL_REQUIRED = new ErrorCode(1008100122, "请先上传打印程序附件");

    // ========== 打印字段模板 ==========
    ErrorCode HCPRINTFIELDTEMPLATE_NOT_EXISTS = new ErrorCode(1008100123, "打印字段模板不存在");
    ErrorCode HCPRINTFIELDTEMPLATE_CODE_EXISTS = new ErrorCode(1008100124, "打印字段模板编码已存在");

    // ========== 客户打印模板 ==========
    ErrorCode HCCUSTOMERPRINTTEMPLATE_NOT_EXISTS = new ErrorCode(1008100125, "客户打印模板不存在");
    ErrorCode HCCUSTOMERPRINTTEMPLATE_CODE_EXISTS = new ErrorCode(1008100126, "客户打印模板编码已存在");
    ErrorCode HCCUSTOMERPRINTTEMPLATE_VARIABLE_EMPTY = new ErrorCode(1008100127, "客户打印模板未解析到变量");

}

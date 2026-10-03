package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcPackagingFlowMapper;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcPackagingPieceRespVO;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceCopyService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDefectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcMrbReviewDelegateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcMrbReviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordBatchCreateFromProductEventReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordCreateFromProductEventReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordCreateFromProductEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyReplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordFinalApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordHandleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordLinkExceptionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordRestorePreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordStockDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordWithdrawReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRelationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductNcRecordRestoreReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcInspectionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordCreateFromInspectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordCreateFromInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordRestoreReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dFlowLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dRelationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dTeamMemberDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionRelationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDefectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionNotifyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcFlowLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcMrbReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRelationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcReportGateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRestoreLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcWorkstationCommandDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsProductEventRecheckDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsProductEventRecheckGroupDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dActionItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dFlowLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dRelationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dTeamMemberMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionRelationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcDefectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcDispositionExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcDispositionNotifyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcDispositionScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcFlowLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcMrbReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcInitial8dRestoreMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRelationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcReportGateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRestoreLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcWorkstationCommandMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsProductNcRestoreMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsRawMaterialNcRestoreMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsProductEventRecheckDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsProductEventRecheckGroupMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.flowable.task.api.Task;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class QmsNcRecordServiceImpl implements QmsNcRecordService {

    private static final ErrorCode QMS_NCR_NOT_EXISTS = new ErrorCode(1008100140, "NCR 不存在");
    private static final ErrorCode QMS_NCR_CLOSED = new ErrorCode(1008100141, "NCR 已关闭或已取消，不能继续流转");
    private static final ErrorCode QMS_NCR_SUBMIT_STATUS_INVALID = new ErrorCode(1008100143, "只有草稿或退回状态可以提交 MRB");
    private static final ErrorCode QMS_NCR_FINAL_REQUIRED = new ErrorCode(1008100144, "终审必须填写处置结论和终审意见");
    private static final ErrorCode QMS_NCR_RETURN_OPINION_REQUIRED = new ErrorCode(1008100145, "退回意见不能为空");
    private static final ErrorCode QMS_NCR_STOCK_STATUS_INVALID = new ErrorCode(1008100146, "只有待库存处置状态可以执行库存处置");
    private static final ErrorCode QMS_NCR_SOURCE_NOT_EXISTS = new ErrorCode(1008100147, "未找到可生成 NCR 的产品异常事件");
    private static final ErrorCode QMS_NCR_REVIEW_REQUIRED = new ErrorCode(1008100148, "提交 NCR 前必须选择会签单位");
    private static final ErrorCode QMS_NCR_REVIEW_HANDLER_NOT_CONFIG =
            new ErrorCode(1008100149, "会签单位【{}】未配置默认办理人，请先维护 NCR 评审会签配置");
    private static final ErrorCode QMS_NCR_BPM_NOT_PUBLISHED =
            new ErrorCode(1008100151, "NCR 正式流程未发布，请先在 SIMPLE 设计器发布流程 qms_ncr_disposition");
    private static final ErrorCode QMS_NCR_FINAL_STATUS_INVALID =
            new ErrorCode(1008100153, "只有终审节点可以填写终审意见");
    private static final ErrorCode QMS_NCR_CURRENT_HANDLER_INVALID =
            new ErrorCode(1008100154, "当前登录人不是该 NCR 的当前办理人");
    private static final ErrorCode QMS_NCR_NO_EXHAUSTED =
            new ErrorCode(1008100155, "NCR 单号流水已超过 999，请检查单号规则：{}");
    private static final ErrorCode QMS_NCR_NEXT_HANDLER_REQUIRED =
            new ErrorCode(1008100156, "请先选择下一节点办理人");
    private static final ErrorCode QMS_NCR_STOCK_RESULT_REQUIRED =
            new ErrorCode(1008100157, "不良品处置执行必须填写处置意见");
    private static final ErrorCode QMS_NCR_RETURN_STATUS_INVALID =
            new ErrorCode(1008100158, "只有处置完成复核节点可以退回");
    private static final ErrorCode QMS_NCR_DISPOSE_ASSIGN_REQUIRED =
            new ErrorCode(1008100159, "处置分派必须填写处置数量和最终处置说明");
    private static final ErrorCode QMS_NCR_DISPOSE_QTY_INVALID =
            new ErrorCode(1008100160, "处置数量必须大于 0 且不能大于不合格数量");
    private static final ErrorCode QMS_NCR_FINAL_APPROVER_INVALID =
            new ErrorCode(1008100161, "终审办理人必须来自启用的 NCR 默认终审人配置");
    private static final ErrorCode QMS_NCR_PROCESS_NOT_PUBLISHED =
            new ErrorCode(1008100162, "NCR 正式流程未发布，请先在 SIMPLE 设计器发布流程 {}");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_SOURCE_NOT_EXISTS =
            new ErrorCode(1008100163, "未找到可生成原物料不合格处置单的检验记录");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_INSPECTION_TYPE_MISMATCH =
            new ErrorCode(1008100164, "批量生成原物料不合格处置单时不能选择不同检验类型");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_INSPECTION_TYPE_UNSUPPORTED =
            new ErrorCode(1008100165, "暂不支持该检验类型生成原物料不合格处置单：{}");
    private static final ErrorCode QMS_NCR_MRB_REVIEW_NOT_EXISTS =
            new ErrorCode(1008100166, "NCR 会签记录不存在");
    private static final ErrorCode QMS_NCR_MRB_REVIEW_NO_PERMISSION =
            new ErrorCode(1008100167, "当前用户无权委托该会签记录");
    private static final ErrorCode QMS_NCR_MRB_REVIEW_DELEGATE_REQUIRED =
            new ErrorCode(1008100168, "请选择被委托代办人");
    private static final ErrorCode QMS_NCR_MRB_REVIEW_DELEGATE_SELF_INVALID =
            new ErrorCode(1008100169, "被委托代办人不能是本人");
    private static final ErrorCode QMS_NCR_MRB_REVIEW_STATUS_INVALID =
            new ErrorCode(1008100170, "当前会签状态不允许委托代办");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_SOURCE_ALREADY_GENERATED =
            new ErrorCode(1008100171, "所选检验单已生成原物料不合格处置单：{}");
    private static final ErrorCode QMS_NCR_WORKSTATION_COMMAND_NOT_APPLIED =
            new ErrorCode(1008100180, "工作台指令尚未应用，不能完成处置执行");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_QUALITY_ROLE_REQUIRED =
            new ErrorCode(1008100181, "当前用户没有原材料不合格确认角色，不能办理品质部确认/分派");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_TRANSFER_ROUTE_REQUIRED =
            new ErrorCode(1008100182, "请选择提交终审、处置执行分派或直接关闭");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_FINAL_REPEAT_INVALID =
            new ErrorCode(1008100183, "该不合格处置单已完成终审，不能再次提交终审");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_TRANSFER_DECISION_REQUIRED =
            new ErrorCode(1008100184, "处置执行分派或直接关闭时，必须填写处置选项、终审意见和具体措施");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_QUALITY_CONFIRM_REQUIRED =
            new ErrorCode(1008100185, "品质部确认/分派必须填写缺陷名称、不合格等级并选择会签单位和会签人");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_REGISTER_REQUIRED =
            new ErrorCode(1008100186, "发起登记必须填写批次、数量和不合格说明");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_RESTORE_CONFIRM_INVALID =
            new ErrorCode(1008100190, "还原确认失败，请输入完整且一致的NCR单号");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_RESTORE_SOURCE_INVALID =
            new ErrorCode(1008100191, "仅允许还原来源为IQC且检验结论仍为NG的原材料不合格处置单");
    private static final ErrorCode QMS_RAW_MATERIAL_NCR_RESTORE_DELETE_FAILED =
            new ErrorCode(1008100194, "原材料不合格处置单还原失败，主单未被删除");
    private static final ErrorCode QMS_PRODUCT_NCR_QUALITY_ROLE_REQUIRED =
            new ErrorCode(1008100196, "当前用户没有产品不合格确认角色，不能办理品质部确认/分派");
    private static final ErrorCode QMS_PRODUCT_NCR_RESTORE_CONFIRM_INVALID =
            new ErrorCode(1008100197, "还原确认失败，请输入完整且一致的产品NCR单号");
    private static final ErrorCode QMS_PRODUCT_NCR_RESTORE_SOURCE_INVALID =
            new ErrorCode(1008100198, "仅允许还原具有独立产品异常来源的产品不合格处置单");
    private static final ErrorCode QMS_PRODUCT_NCR_RESTORE_DELETE_FAILED =
            new ErrorCode(1008100201, "产品不合格处置单还原失败，主单未被删除");
    private static final ErrorCode QMS_PRODUCT_NCR_REGISTER_REQUIRED =
            new ErrorCode(1008100203, "发起登记必须填写批次、数量和不合格说明");
    private static final ErrorCode QMS_PRODUCT_NCR_QUALITY_CONFIRM_REQUIRED =
            new ErrorCode(1008100204, "品质部确认/分派必须填写缺陷名称、不合格等级并选择会签单位和会签人");
    private static final ErrorCode QMS_NCR_RESTORE_8D_DELETE_FAILED =
            new ErrorCode(1008100206, "NCR还原失败，初始化8D未能安全删除：{}");
    private static final ErrorCode QMS_NCR_DISPOSITION_NOTIFY_NOT_EXISTS =
            new ErrorCode(1008100207, "当前没有待回复的处置执行通知");
    private static final ErrorCode QMS_NCR_DISPOSITION_NOTIFY_REPLY_REQUIRED =
            new ErrorCode(1008100208, "请填写通知人办理结论");
    private static final ErrorCode QMS_NCR_CONTENT_CONFIRM_USER_REQUIRED =
            new ErrorCode(1008100209, "提交不合格处置单前必须选择再次确认人");
    private static final ErrorCode QMS_NCR_CONTENT_CONFIRM_DESCRIPTION_REQUIRED =
            new ErrorCode(1008100210, "再次确认内容必须填写不合格说明");
    private static final ErrorCode QMS_NCR_WITHDRAW_NOT_AVAILABLE =
            new ErrorCode(1008100211, "当前不合格处置单没有可撤回修改的未办理下一环节");
    private static final ErrorCode QMS_NCR_WITHDRAW_TARGET_INVALID =
            new ErrorCode(1008100212, "撤回后未能定位到可编辑的上一环节");
    private static final ErrorCode QMS_NCR_EXCEPTION_NOT_EXISTS =
            new ErrorCode(1008100213, "异常事件不存在");
    private static final ErrorCode QMS_NCR_EXCEPTION_ALREADY_LINKED =
            new ErrorCode(1008100214, "该异常事件已关联其他不合格处置单：{}");

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONTENT_CONFIRM = "CONTENT_CONFIRM";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_MRB_REVIEW = "MRB_REVIEW";
    private static final String STATUS_REVIEW_ASSIGN = "REVIEW_ASSIGN";
    private static final String STATUS_FINAL_APPROVAL = "FINAL_APPROVAL";
    private static final String STATUS_EXECUTION_ASSIGN = "EXECUTION_ASSIGN";
    private static final String STATUS_PENDING_STOCK_DISPOSE = "PENDING_STOCK_DISPOSE";
    private static final String STATUS_CLOSE_CONFIRM = "CLOSE_CONFIRM";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String NODE_DRAFT = "DRAFT";
    private static final String NODE_CONTENT_CONFIRM = "CONTENT_CONFIRM";
    private static final String NODE_SUBMITTED = "QUALITY_CONFIRM";
    private static final String NODE_MRB_REVIEW = "MRB_REVIEW";
    private static final String NODE_REVIEW_ASSIGN = "REVIEW_ASSIGN";
    private static final String NODE_FINAL_APPROVAL = "FINAL_APPROVAL";
    private static final String NODE_EXECUTION_ASSIGN = "EXECUTION_ASSIGN";
    private static final String NODE_STOCK_DISPOSE = "STOCK_DISPOSE";
    private static final String NODE_CLOSE_CONFIRM = "CLOSE_CONFIRM";
    private static final String NODE_CLOSED = "CLOSED";
    private static final String NODE_CANCELLED = "CANCELLED";
    private static final String NODE_RETURNED = "RETURNED";

    private static final String NCR_TYPE_FINISHED_PRODUCT = "FINISHED_PRODUCT";
    private static final String NCR_TYPE_SEMI_FINISHED = "SEMI_FINISHED";
    private static final String NCR_TYPE_CUSTOMER_RETURN = "CUSTOMER_RETURN";
    private static final String NCR_TYPE_RAW_MATERIAL = "RAW_MATERIAL";
    private static final String SOURCE_BIZ_CUT_ROUND_FQC = "CUT_ROUND_FQC";
    private static final String SOURCE_BIZ_FG_SHIPPING_FQC = "FG_SHIPPING_FQC";
    private static final String SOURCE_BIZ_OQC = "OQC";
    private static final String SOURCE_BIZ_IQC = "IQC";
    private static final String RAW_MATERIAL_CATEGORY_INCOMING = "INCOMING";
    private static final String RAW_MATERIAL_CATEGORY_STOCK = "STOCK";
    private static final String NCR_STATUS_ALL = "ALL";
    private static final String NCR_STATUS_GENERATED = "GENERATED";
    private static final String NCR_STATUS_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String RECHECK_STATUS_NONE = "NONE";
    private static final String RECHECK_STATUS_RECHECKING = "RECHECKING";
    private static final String RECHECK_STATUS_OK = "RECHECK_OK";
    private static final String RECHECK_STATUS_NG = "RECHECK_NG";
    private static final String DEFECT_CODE_DRAFT_PENDING = " ";
    private static final String BPM_NCR_PROCESS_KEY = "qms_ncr_disposition";
    private static final String BPM_NCR_MODEL_ID = "qms-ncr-disposition-model";
    private static final String BPM_RAW_MATERIAL_NCR_PROCESS_KEY = "qms_raw_material_ncr_disposition";
    private static final String BPM_RAW_MATERIAL_NCR_MODEL_ID = "qms-raw-material-ncr-disposition-model";
    private static final String BPM_NODE_START_USER = "StartUserNode";
    private static final String BPM_NODE_CONTENT_CONFIRM = "content_confirm";
    private static final String BPM_NODE_QUALITY_CONFIRM = "quality_confirm";
    private static final String BPM_NODE_MRB_REVIEW = "mrb_review";
    private static final String BPM_NODE_REVIEW_ASSIGN = "review_assign";
    private static final String BPM_NODE_FINAL_DISPOSE = "final_dispose";
    private static final String BPM_NODE_EXECUTION_ASSIGN = "execution_assign";
    private static final String BPM_NODE_EXECUTION_UPLOAD = "execution_upload";
    private static final String BPM_NODE_REVIEW_CLOSE = "review_close";
    private static final String ATTACHMENT_RELATION_TYPE = "ATTACHMENT";
    private static final String DISPOSITION_ATTACHMENT_RELATION_TYPE = "DISPOSITION_ATTACHMENT";
    private static final String RAW_MATERIAL_QUALITY_CONFIRM_ROLE_CODE = "qms_raw_material_ncr_confirm";
    private static final String PRODUCT_NCR_QUALITY_CONFIRM_ROLE_CODE = "qms_product_ncr_confirm";
    private static final String RAW_TRANSFER_ROUTE_FINAL = "FINAL_APPROVAL";
    private static final String RAW_TRANSFER_ROUTE_EXECUTION = "DISPOSITION_EXECUTION";
    private static final String RAW_TRANSFER_ROUTE_CLOSE = "DIRECT_CLOSE";
    private static final String RAW_TRANSFER_ROUTE_VARIABLE = "rawMaterialTransferRoute";
    private static final String PRODUCT_TRANSFER_ROUTE_VARIABLE = "productTransferRoute";
    private static final String TRANSFER_ROUTE_UNSELECTED = "";
    private static final String RELATION_TYPE_EXCEPTION = "EXCEPTION";
    private static final String RELATION_TYPE_NCR = "NCR";
    private static final String RELATION_TYPE_RAW_MATERIAL_NCR = "RAW_MATERIAL_NCR";
    private static final String BPM_ADJUST_CLEAR_BUSINESS_DATA = "bpmAdjustClearBusinessData";
    private static final Integer BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE = 1009003002;
    private static final Long BPM_NCR_BUILT_IN_MANAGER_USER_ID = 144L;
    private static final String DISPOSITION_NOTIFY_STATUS_PENDING = "PENDING";
    private static final String DISPOSITION_NOTIFY_STATUS_REPLIED = "REPLIED";
    private static final String DISPOSITION_NOTIFY_STATUS_CANCELLED = "CANCELLED";
    private static final String DISPOSITION_SCRAP = "SCRAP";
    private static final String SCOPE_LEVEL_PIECE = "PIECE";
    private static final String SCOPE_ROLE_PICK_OUTSIDE_SCRAP = "PICK_OUTSIDE_SCRAP";
    private static final Set<String> PACKAGING_EXECUTED_UNIT_STATUSES =
            Set.of("PACKED", "INBOUND_LOCKED", "INBOUNDED");

    @Resource
    private QmsNcRecordMapper qmsNcRecordMapper;
    @Resource
    private QmsNcMrbReviewMapper qmsNcMrbReviewMapper;
    @Resource
    private QmsNcDefectMapper qmsNcDefectMapper;
    @Resource
    private QmsNcRelationMapper qmsNcRelationMapper;
    @Resource
    private QmsNcDispositionExecutionMapper qmsNcDispositionExecutionMapper;
    @Resource
    private QmsNcDispositionNotifyMapper qmsNcDispositionNotifyMapper;
    @Resource
    private QmsNcDispositionScopeMapper qmsNcDispositionScopeMapper;
    @Resource
    private QmsNcWorkstationCommandMapper qmsNcWorkstationCommandMapper;
    @Resource
    private QmsNcReportGateMapper qmsNcReportGateMapper;
    @Resource
    private QmsNcPickQualificationService pickQualificationService;
    @Resource
    private HcInnerPackUnitItemMapper hcInnerPackUnitItemMapper;
    @Resource
    private HcInnerPackUnitMapper hcInnerPackUnitMapper;
    @Resource
    private QmsNcFlowLogMapper qmsNcFlowLogMapper;
    @Resource
    private QmsNcRestoreLogMapper qmsNcRestoreLogMapper;
    @Resource
    private QmsNcInitial8dRestoreMapper qmsNcInitial8dRestoreMapper;
    @Resource
    private Qms8dActionItemMapper qms8dActionItemMapper;
    @Resource
    private Qms8dFlowLogMapper qms8dFlowLogMapper;
    @Resource
    private Qms8dRelationMapper qms8dRelationMapper;
    @Resource
    private Qms8dTeamMemberMapper qms8dTeamMemberMapper;
    @Resource
    private QmsRawMaterialNcRestoreMapper qmsRawMaterialNcRestoreMapper;
    @Resource
    private QmsProductNcRestoreMapper qmsProductNcRestoreMapper;
    @Resource
    private QmsIqcOrderMapper qmsIqcOrderMapper;
    @Resource
    private QmsIqcAbnormalMapper qmsIqcAbnormalMapper;
    @Resource
    private QmsProductEventRecheckGroupMapper qmsProductEventRecheckGroupMapper;
    @Resource
    private QmsProductEventRecheckDetailMapper qmsProductEventRecheckDetailMapper;
    @Resource
    private QmsExceptionEventMapper qmsExceptionEventMapper;
    @Resource
    private QmsExceptionRelationMapper qmsExceptionRelationMapper;
    @Resource
    private QmsProductAbnormalEventService qmsProductAbnormalEventService;
    @Resource
    private QmsIqcService qmsIqcService;
    @Resource
    private QmsNcReviewConfigService qmsNcReviewConfigService;
    @Resource
    @Lazy
    private QmsExceptionEventService qmsExceptionEventService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private BpmModelService bpmModelService;
    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private BpmProcessInstanceCopyService bpmProcessInstanceCopyService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private HistoryService historyService;

    @Override
    public PageResult<QmsNcRecordRespVO> getNcRecordPage(QmsNcRecordPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        Set<Long> participatedIds = buildParticipatedNcRecordIds(loginUserId);
        Set<Long> pendingReviewIds = buildPendingReviewNcRecordIds(loginUserId);
        Set<Long> pendingNotifyIds = buildPendingDispositionNotifyNcRecordIds(loginUserId);
        boolean rawMaterialQualityConfirmUser = isRawMaterialQualityConfirmUser(loginUserId);
        boolean productQualityConfirmUser = isProductQualityConfirmUser(loginUserId);
        PageResult<QmsNcRecordDO> pageResult = qmsNcRecordMapper.selectPage(pageReqVO, loginUserId, participatedIds,
                pendingReviewIds, pendingNotifyIds, rawMaterialQualityConfirmUser, productQualityConfirmUser);
        PageResult<QmsNcRecordRespVO> respPage = BeanUtils.toBean(pageResult, QmsNcRecordRespVO.class);
        respPage.getList().forEach(respVO -> {
            clearDraftPendingDefect(respVO);
            fillCurrentUserListState(respVO, loginUserId, participatedIds, pendingReviewIds, pendingNotifyIds,
                    rawMaterialQualityConfirmUser, productQualityConfirmUser);
        });
        return respPage;
    }

    @Override
    public PageResult<QmsNcDispositionNotifyRespVO> getDispositionNotifyPage(
            QmsNcDispositionNotifyPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        List<Long> lotFilteredNcRecordIds = StrUtil.isBlank(pageReqVO.getLotNo())
                ? List.of()
                : qmsNcRecordMapper.selectIdsByLotNoLike(pageReqVO.getLotNo());
        if (StrUtil.isNotBlank(pageReqVO.getLotNo()) && CollUtil.isEmpty(lotFilteredNcRecordIds)) {
            return PageResult.empty();
        }
        PageResult<QmsNcDispositionNotifyDO> notifyPage =
                qmsNcDispositionNotifyMapper.selectWorkbenchPage(pageReqVO, loginUserId, lotFilteredNcRecordIds);
        if (CollUtil.isEmpty(notifyPage.getList())) {
            return PageResult.empty(notifyPage.getTotal());
        }
        Set<Long> ncRecordIds = notifyPage.getList().stream()
                .map(QmsNcDispositionNotifyDO::getNcRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, QmsNcRecordDO> recordMap = CollUtil.isEmpty(ncRecordIds)
                ? Map.of()
                : qmsNcRecordMapper.selectBatchIds(ncRecordIds).stream()
                        .collect(Collectors.toMap(QmsNcRecordDO::getId, record -> record, (left, right) -> left));
        List<QmsNcDispositionNotifyRespVO> list = notifyPage.getList().stream()
                .map(notify -> buildDispositionNotifyWorkbenchResp(notify, recordMap.get(notify.getNcRecordId())))
                .toList();
        return new PageResult<>(list, notifyPage.getTotal());
    }

    private QmsNcDispositionNotifyRespVO buildDispositionNotifyWorkbenchResp(QmsNcDispositionNotifyDO notify,
                                                                             QmsNcRecordDO record) {
        QmsNcDispositionNotifyRespVO respVO = BeanUtils.toBean(notify, QmsNcDispositionNotifyRespVO.class);
        if (record != null) {
            respVO.setSourceType(record.getSourceType());
            respVO.setSourceTypeName(record.getSourceTypeName());
            respVO.setSourceBizType(record.getSourceBizType());
            respVO.setSourceBizTypeName(record.getSourceBizTypeName());
            respVO.setSourceNo(record.getSourceNo());
            respVO.setHappenTime(record.getHappenTime());
            respVO.setProcessName(record.getProcessName());
            respVO.setHappenDeptName(record.getHappenDeptName());
            respVO.setMaterialCode(record.getMaterialCode());
            respVO.setMaterialName(record.getMaterialName());
            respVO.setSpecification(record.getSpecification());
            respVO.setLotNo(record.getLotNo());
            respVO.setDefectName(record.getDefectName());
            respVO.setDefectQty(record.getDefectQty());
            respVO.setNcLevelName(record.getNcLevelName());
            respVO.setStatus(record.getStatus());
            respVO.setCurrentNodeName(record.getCurrentNodeName());
            respVO.setCurrentHandlerUserName(record.getCurrentHandlerUserName());
            respVO.setFinalDisposition(record.getFinalDisposition());
        }
        boolean canReply = DISPOSITION_NOTIFY_STATUS_PENDING.equals(respVO.getNotifyStatus());
        respVO.setCanReply(canReply);
        respVO.setListActionName(canReply ? "回复" : "查看");
        return respVO;
    }

    @Resource
    private QmsNcPackagingFlowMapper packagingFlowMapper;

    /** 包装入口严格只读：不能调用会修复历史状态、同步 BPM 的 getNcRecord。 */
    @Override
    public QmsNcRecordRespVO getPackagingFlowRecord(Long id) {
        QmsNcRecordDO entity = validatePackagingFlowRecord(id);
        QmsNcRecordRespVO result = buildNcResp(entity, true);
        result.setCanHandle(false);
        return result;
    }

    @Override
    public List<QmsNcPackagingPieceRespVO>
            getPackagingFlowPieces(Long id) {
        validatePackagingFlowRecord(id);
        return packagingFlowMapper.selectPieces(id, TenantContextHolder.getRequiredTenantId());
    }

    private QmsNcRecordDO validatePackagingFlowRecord(Long id) {
        QmsNcRecordDO entity = validateNcExists(id);
        if (isRawMaterialNcr(entity)
                || !Objects.equals(entity.getTenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw exception(QMS_NCR_NOT_EXISTS);
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsNcRecordRespVO getNcRecord(Long id) {
        QmsNcRecordDO entity = validateNcExists(id);
        entity = repairLegacyReviewAssignStatus(entity);
        entity = syncCancelledFromBpm(entity);
        ensureNcrTransferRouteVariable(entity);
        if (!syncBusinessStatusFromRunningBpm(entity)) {
            syncBpmToBusinessStatus(entity, "NCR业务状态同步");
        }
        QmsNcRecordRespVO respVO = buildNcResp(qmsNcRecordMapper.selectById(id), true);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        fillCurrentUserListState(respVO, loginUserId, buildParticipatedNcRecordIds(loginUserId),
                buildPendingReviewNcRecordIds(loginUserId), buildPendingDispositionNotifyNcRecordIds(loginUserId),
                isRawMaterialQualityConfirmUser(loginUserId),
                isProductQualityConfirmUser(loginUserId));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNcRecord(QmsNcRecordSaveReqVO createReqVO) {
        LocalDateTime now = LocalDateTime.now();
        QmsNcRecordDO entity = BeanUtils.toBean(createReqVO, QmsNcRecordDO.class);
        entity.setNcNo(StrUtil.blankToDefault(createReqVO.getNcNo(),
                generateNcNo(now, entity.getSourceType(), entity.getSourceTypeName())));
        entity.setStatus(StrUtil.blankToDefault(entity.getStatus(), STATUS_DRAFT));
        applyNode(entity, entity.getStatus());
        entity.setHappenTime(entity.getHappenTime() != null ? entity.getHappenTime() : now);
        entity.setApplicantUserId(firstNonNull(entity.getApplicantUserId(), SecurityFrameworkUtils.getLoginUserId()));
        entity.setApplicantUserName(firstNotBlank(entity.getApplicantUserName(), currentUserName()));
        entity.setApplicantDeptId(firstNonNull(entity.getApplicantDeptId(), SecurityFrameworkUtils.getLoginUserDeptId()));
        entity.setApplicantDeptName(firstNotBlank(entity.getApplicantDeptName(), "当前部门"));
        entity.setCurrentHandlerUserId(firstNonNull(entity.getCurrentHandlerUserId(), SecurityFrameworkUtils.getLoginUserId()));
        entity.setCurrentHandlerUserName(firstNotBlank(entity.getCurrentHandlerUserName(), currentUserName()));
        entity.setMrbDecision(StrUtil.blankToDefault(entity.getMrbDecision(), "PENDING"));
        applyDraftDefaults(entity);
        applyDictSnapshots(entity);
        qmsNcRecordMapper.insert(entity);
        saveReviews(entity, createReqVO.getReviews(), false);
        saveDefects(entity, createReqVO.getDefects());
        saveRelations(entity, createReqVO.getRelations());
        writeFlowLog(null, entity, "CREATE", "开立", createReqVO.getRemark(),
                snapshot("sourceType", entity.getSourceType(), "sourceNo", entity.getSourceNo(),
                        "lotNo", entity.getLotNo(), "defectQty", entity.getDefectQty()));
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long relaunchNcRecord(Long sourceId) {
        QmsNcRecordDO source = validateNcExists(sourceId);
        LocalDateTime now = LocalDateTime.now();
        QmsNcRecordDO entity = BeanUtils.toBean(source, QmsNcRecordDO.class);
        entity.setId(null);
        entity.clean();
        entity.setDeleted(null);
        entity.setNcNo(generateNcNo(now, entity.getSourceType(), entity.getSourceTypeName()));
        entity.setSourceNcRecordId(source.getId());
        entity.setStatus(STATUS_DRAFT);
        entity.setProcessInstanceId(null);
        entity.setCurrentHandlerUserId(SecurityFrameworkUtils.getLoginUserId());
        entity.setCurrentHandlerUserName(currentUserName());
        entity.setApplicantUserId(SecurityFrameworkUtils.getLoginUserId());
        entity.setApplicantUserName(currentUserName());
        entity.setApplicantDeptId(SecurityFrameworkUtils.getLoginUserDeptId());
        entity.setApplicantDeptName(firstNotBlank(entity.getApplicantDeptName(), "当前部门"));
        entity.setQualityConfirmUserId(null);
        entity.setQualityConfirmUserName(null);
        entity.setQualityConfirmTime(null);
        entity.setMrbDecision("PENDING");
        entity.setFinalDisposition(null);
        entity.setFinalOpinion(null);
        entity.setFinalDisposeDescription(null);
        entity.setFinalApproverId(null);
        entity.setFinalApproverName(null);
        entity.setFinalApproveTime(null);
        entity.setStockDisposeStatus(null);
        entity.setStockDisposeQty(null);
        entity.setStockDisposeUserId(null);
        entity.setStockDisposeUserName(null);
        entity.setStockDisposeResult(null);
        entity.setStockDisposeTime(null);
        entity.setRelated8dNo(null);
        entity.setCloseTime(null);
        entity.setCloseUserId(null);
        entity.setCloseUserName(null);
        entity.setRemark(StrUtil.format("基于旧 NCR {} 重新发起", source.getNcNo()));
        applyNode(entity, STATUS_DRAFT);
        applyDictSnapshots(entity);
        applyDraftDefaults(entity);
        qmsNcRecordMapper.insert(entity);
        saveReviews(entity, buildRelaunchReviews(source.getId()), false);
        saveDefects(entity, buildRelaunchDefects(source.getId()));
        saveRelations(entity, buildRelaunchRelations(source.getId()));
        writeFlowLog(null, entity, "RELAUNCH", "重新发起", entity.getRemark(),
                snapshot("sourceNcRecordId", source.getId(), "sourceNcNo", source.getNcNo(),
                        "newNcNo", entity.getNcNo()));
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrGetFromProductEvent(QmsNcRecordCreateFromProductEventReqVO createReqVO) {
        String sourceBizType = normalizeSourceType(createReqVO.getSourceType());
        QmsNcRecordDO existing = findExistingProductEventNcr(sourceBizType, createReqVO.getInspectionId());
        if (existing != null) {
            return existing.getId();
        }
        QmsProductAbnormalEventDetailRespVO detail = getProductEventDetail(sourceBizType, createReqVO.getInspectionId());
        return createNcRecord(buildProductEventNcr(sourceBizType, detail));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<QmsNcRecordCreateFromProductEventRespVO> batchCreateFromProductEvent(
            QmsNcRecordBatchCreateFromProductEventReqVO createReqVO) {
        List<QmsNcRecordCreateFromProductEventRespVO> result = new ArrayList<>();
        for (QmsNcRecordBatchCreateFromProductEventReqVO.Item item : createReqVO.getItems()) {
            String sourceBizType = normalizeSourceType(item.getSourceType());
            QmsNcRecordDO existing = findExistingProductEventNcr(sourceBizType, item.getInspectionId());
            if (existing != null) {
                result.add(buildProductEventCreateResp(sourceBizType, item.getInspectionId(), existing, true));
                continue;
            }
            QmsProductAbnormalEventDetailRespVO detail = getProductEventDetail(sourceBizType, item.getInspectionId());
            QmsNcRecordSaveReqVO ncReqVO = buildProductEventNcr(sourceBizType, detail);
            if (item.getDefectQty() != null && item.getDefectQty().compareTo(BigDecimal.ZERO) > 0) {
                ncReqVO.setDefectQty(item.getDefectQty());
            }
            if (StrUtil.isNotBlank(item.getDefectCode())) {
                ncReqVO.setDefectCode(StrUtil.trim(item.getDefectCode()));
            }
            if (StrUtil.isNotBlank(item.getDefectName())) {
                ncReqVO.setDefectName(StrUtil.trim(item.getDefectName()));
            }
            ncReqVO.setNcDescription(firstNotBlank(item.getNcDescription(), ncReqVO.getNcDescription()));
            ncReqVO.setNcLevel(normalizeUpper(createReqVO.getNcLevel()));
            ncReqVO.setReviews(buildResponsibleReviews(createReqVO.getResponsibleDeptNames()));
            ncReqVO.setRemark(firstNotBlank(createReqVO.getRemark(), "从产品异常事件批量生成NCR"));
            Long ncRecordId = createNcRecord(ncReqVO);
            QmsNcRecordDO created = qmsNcRecordMapper.selectLightById(ncRecordId);
            if (Boolean.TRUE.equals(createReqVO.getDirectSubmit())) {
                QmsNcRecordSubmitReqVO submitReqVO = new QmsNcRecordSubmitReqVO();
                submitReqVO.setId(ncRecordId);
                submitReqVO.setContentConfirmUserId(createReqVO.getContentConfirmUserId());
                submitReqVO.setContentConfirmUserName(createReqVO.getContentConfirmUserName());
                submitReqVO.setOpinion("从产品异常事件生成 NCR 后直接提交");
                submitNcRecord(submitReqVO);
                created = qmsNcRecordMapper.selectLightById(ncRecordId);
            }
            result.add(buildProductEventCreateResp(sourceBizType, item.getInspectionId(), created, false));
        }
        return result;
    }

    @Override
    public PageResult<QmsRawMaterialNcInspectionRespVO> getRawMaterialInspectionPage(
            QmsRawMaterialNcInspectionPageReqVO pageReqVO) {
        String inspectionType = normalizeRawMaterialInspectionType(pageReqVO.getInspectionType());
        if (!SOURCE_BIZ_IQC.equals(inspectionType)) {
            return new PageResult<>(List.of(), 0L);
        }
        QmsIqcPageReqVO iqcReqVO = new QmsIqcPageReqVO();
        iqcReqVO.setPageNo(1);
        iqcReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        iqcReqVO.setIqcNo(pageReqVO.getInspectionNo());
        iqcReqVO.setSupplierName(pageReqVO.getSupplierName());
        iqcReqVO.setMaterialCode(pageReqVO.getMaterialCode());
        iqcReqVO.setMaterialName(pageReqVO.getMaterialName());
        iqcReqVO.setBatchNo(pageReqVO.getLotNo());
        iqcReqVO.setJudgment(JUDGMENT_NG);
        iqcReqVO.setInspectionTime(pageReqVO.getInspectionTime());
        PageResult<QmsIqcOrderDO> pageResult = qmsIqcOrderMapper.selectPage(iqcReqVO);
        List<QmsRawMaterialNcInspectionRespVO> rows = pageResult.getList().stream()
                .map(this::buildRawMaterialIqcInspectionRow)
                .collect(Collectors.toList());
        rows = mergeRawMaterialIqcRecheckRows(pageReqVO, rows);
        fillRawMaterialNcrStatus(rows);
        List<QmsRawMaterialNcInspectionRespVO> filteredRows = rows.stream()
                .filter(row -> matchesNcrStatus(row, pageReqVO.getNcrStatus()))
                .collect(Collectors.toList());
        filteredRows.sort(this::compareRawMaterialInspectionTimeDesc);
        return new PageResult<>(pageRawMaterialRows(filteredRows, pageReqVO), (long) filteredRows.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrGetRawMaterialFromInspection(QmsRawMaterialNcRecordCreateFromInspectionReqVO createReqVO) {
        String inspectionType = normalizeRawMaterialInspectionType(createReqVO.getInspectionType());
        validateRawMaterialInspectionType(inspectionType);
        QmsNcRecordDO existing = findExistingSourceNcr(inspectionType, createReqVO.getInspectionId());
        if (existing != null) {
            return existing.getId();
        }
        QmsIqcRespVO detail = getRawMaterialIqcDetail(createReqVO.getInspectionId());
        Long ncRecordId = createNcRecord(buildRawMaterialIqcNcr(detail));
        qmsIqcAbnormalMapper.updateNcRecordIdByIqcId(detail.getId(), ncRecordId);
        return ncRecordId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<QmsRawMaterialNcRecordCreateFromInspectionRespVO> batchCreateRawMaterialFromInspection(
            QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO createReqVO) {
        String batchInspectionType = normalizeRawMaterialInspectionType(createReqVO.getInspectionType());
        validateRawMaterialInspectionType(batchInspectionType);
        List<QmsRawMaterialNcRecordCreateFromInspectionRespVO> result = new ArrayList<>();
        for (QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item item : createReqVO.getItems()) {
            String itemInspectionType = normalizeRawMaterialInspectionType(
                    firstNotBlank(item.getInspectionType(), batchInspectionType));
            if (!Objects.equals(batchInspectionType, itemInspectionType)) {
                throw exception(QMS_RAW_MATERIAL_NCR_INSPECTION_TYPE_MISMATCH);
            }
            QmsNcRecordDO existing = findExistingSourceNcr(itemInspectionType, item.getInspectionId());
            if (existing != null) {
                result.add(buildRawMaterialCreateResp(itemInspectionType, item.getInspectionId(), existing, true));
                continue;
            }
            QmsIqcRespVO detail = getRawMaterialIqcDetail(item.getInspectionId());
            QmsNcRecordSaveReqVO ncReqVO = buildRawMaterialIqcNcr(detail);
            if (item.getDefectQty() != null && item.getDefectQty().compareTo(BigDecimal.ZERO) > 0) {
                ncReqVO.setDefectQty(item.getDefectQty());
            }
            if (StrUtil.isNotBlank(item.getDefectCode())) {
                ncReqVO.setDefectCode(StrUtil.trim(item.getDefectCode()));
            }
            if (StrUtil.isNotBlank(item.getDefectName())) {
                ncReqVO.setDefectName(StrUtil.trim(item.getDefectName()));
            }
            String manualDescription = StrUtil.trim(item.getNcDescription());
            if (StrUtil.isNotBlank(manualDescription)
                    && !Objects.equals(manualDescription, buildRawMaterialIqcInspectionSummary(detail))) {
                ncReqVO.setNcDescription(manualDescription);
            }
            ncReqVO.setNcLevel(firstNotBlank(normalizeUpper(createReqVO.getNcLevel()), ncReqVO.getNcLevel()));
            ncReqVO.setReviews(buildResponsibleReviews(createReqVO.getResponsibleDeptNames()));
            ncReqVO.setRemark(firstNotBlank(createReqVO.getRemark(), "从IQC检验单批量生成原物料不合格处置单"));
            Long ncRecordId = createNcRecord(ncReqVO);
            qmsIqcAbnormalMapper.updateNcRecordIdByIqcId(detail.getId(), ncRecordId);
            QmsNcRecordDO created = qmsNcRecordMapper.selectLightById(ncRecordId);
            if (Boolean.TRUE.equals(createReqVO.getDirectSubmit())) {
                QmsNcRecordSubmitReqVO submitReqVO = new QmsNcRecordSubmitReqVO();
                submitReqVO.setId(ncRecordId);
                submitReqVO.setContentConfirmUserId(createReqVO.getContentConfirmUserId());
                submitReqVO.setContentConfirmUserName(createReqVO.getContentConfirmUserName());
                submitReqVO.setOpinion("从IQC检验单生成原物料不合格处置单后直接提交");
                submitNcRecord(submitReqVO);
                created = qmsNcRecordMapper.selectLightById(ncRecordId);
            }
            result.add(buildRawMaterialCreateResp(itemInspectionType, item.getInspectionId(), created, false));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long mergeCreateRawMaterialFromInspection(QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO createReqVO) {
        String batchInspectionType = normalizeRawMaterialInspectionType(createReqVO.getInspectionType());
        validateRawMaterialInspectionType(batchInspectionType);
        Map<Long, QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item> itemMap = new LinkedHashMap<>();
        for (QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item item : createReqVO.getItems()) {
            String itemInspectionType = normalizeRawMaterialInspectionType(
                    firstNotBlank(item.getInspectionType(), batchInspectionType));
            if (!Objects.equals(batchInspectionType, itemInspectionType)) {
                throw exception(QMS_RAW_MATERIAL_NCR_INSPECTION_TYPE_MISMATCH);
            }
            itemMap.putIfAbsent(item.getInspectionId(), item);
        }
        List<QmsIqcRespVO> details = new ArrayList<>();
        for (QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item item : itemMap.values()) {
            QmsNcRecordDO existing = findExistingSourceNcr(batchInspectionType, item.getInspectionId());
            if (existing != null) {
                throw exception(QMS_RAW_MATERIAL_NCR_SOURCE_ALREADY_GENERATED,
                        firstNotBlank(existing.getNcNo(), String.valueOf(existing.getId())));
            }
            details.add(getRawMaterialIqcDetail(item.getInspectionId()));
        }
        if (CollUtil.isEmpty(details)) {
            throw exception(QMS_RAW_MATERIAL_NCR_SOURCE_NOT_EXISTS);
        }

        QmsNcRecordSaveReqVO ncReqVO = buildMergedRawMaterialIqcNcr(details, itemMap, createReqVO);
        Long ncRecordId = createNcRecord(ncReqVO);
        for (QmsIqcRespVO detail : details) {
            qmsIqcAbnormalMapper.updateNcRecordIdByIqcId(detail.getId(), ncRecordId);
        }
        if (Boolean.TRUE.equals(createReqVO.getDirectSubmit())) {
            QmsNcRecordSubmitReqVO submitReqVO = new QmsNcRecordSubmitReqVO();
            submitReqVO.setId(ncRecordId);
            submitReqVO.setContentConfirmUserId(createReqVO.getContentConfirmUserId());
            submitReqVO.setContentConfirmUserName(createReqVO.getContentConfirmUserName());
            submitReqVO.setOpinion("合并IQC检验单生成原物料不合格处置单后直接提交");
            submitNcRecord(submitReqVO);
        }
        return ncRecordId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreRawMaterialNcRecord(QmsRawMaterialNcRecordRestoreReqVO restoreReqVO) {
        QmsNcRecordDO ncRecord = qmsNcRecordMapper.selectByIdForUpdate(restoreReqVO.getId());
        if (ncRecord == null) {
            throw exception(QMS_NCR_NOT_EXISTS);
        }
        if (!Objects.equals(ncRecord.getNcNo(), StrUtil.trim(restoreReqVO.getConfirmNcNo()))) {
            throw exception(QMS_RAW_MATERIAL_NCR_RESTORE_CONFIRM_INVALID);
        }
        if (!NCR_TYPE_RAW_MATERIAL.equals(ncRecord.getSourceType())
                || !SOURCE_BIZ_IQC.equals(ncRecord.getSourceBizType())
                || ncRecord.getSourceId() == null) {
            throw exception(QMS_RAW_MATERIAL_NCR_RESTORE_SOURCE_INVALID);
        }

        Long tenantId = Objects.requireNonNullElse(TenantContextHolder.getTenantId(), 0L);
        QmsIqcOrderDO sourceIqc = qmsIqcOrderMapper.selectById(ncRecord.getSourceId());
        List<String> restoreWarnings = buildRawMaterialNcRestoreWarnings(ncRecord, sourceIqc, tenantId);
        List<Qms8dReportDO> linked8dReports = selectLinked8dReportsForRestore(ncRecord, tenantId, true);
        List<Qms8dReportDO> initial8dReports = linked8dReports.stream()
                .filter(report -> isInitial8dReport(report, ncRecord))
                .toList();
        List<Qms8dReportDO> retained8dReports = linked8dReports.stream()
                .filter(report -> !isInitial8dReport(report, ncRecord))
                .toList();

        Set<String> processInstanceIds = findRawMaterialNcProcessInstanceIds(ncRecord);
        Map<String, Object> restoreSummary = buildRawMaterialNcRestoreSummary(ncRecord, sourceIqc,
                processInstanceIds);
        appendForcedRestoreWarningSummary(restoreSummary, restoreWarnings);
        appendInitial8dRestoreSummary(restoreSummary, initial8dReports);
        appendDetached8dRestoreSummary(restoreSummary, retained8dReports);
        qmsNcRestoreLogMapper.insert(QmsNcRestoreLogDO.builder()
                .ncRecordId(ncRecord.getId())
                .ncNo(ncRecord.getNcNo())
                .documentType(NCR_TYPE_RAW_MATERIAL)
                .sourceBizType(ncRecord.getSourceBizType())
                .sourceObjectId(ncRecord.getSourceId())
                .sourceObjectNo(sourceIqc == null ? ncRecord.getSourceNo() : sourceIqc.getIqcNo())
                .sourceIqcId(ncRecord.getSourceId())
                .sourceIqcNo(sourceIqc == null ? ncRecord.getSourceNo() : sourceIqc.getIqcNo())
                .originalStatus(ncRecord.getStatus())
                .processInstanceIds(String.join(",", processInstanceIds))
                .restoreReason(StrUtil.trim(restoreReqVO.getReason()))
                .restoreUserId(SecurityFrameworkUtils.getLoginUserId())
                .restoreUserName(currentUserName())
                .restoreSummary(restoreSummary)
                .tenantId(tenantId)
                .build());

        deleteRawMaterialNcProcessInstances(processInstanceIds);
        deleteInitial8dReports(initial8dReports, tenantId);
        detachRetained8dReports(ncRecord, retained8dReports, tenantId);
        clearRawMaterialNcExternalLinks(ncRecord, tenantId);
        qmsRawMaterialNcRestoreMapper.deleteCommands(ncRecord.getId(), tenantId);
        qmsRawMaterialNcRestoreMapper.deleteReportGates(ncRecord.getId(), tenantId);
        qmsRawMaterialNcRestoreMapper.deleteDispositionScopes(ncRecord.getId(), tenantId);
        qmsRawMaterialNcRestoreMapper.deleteDispositionExecutions(ncRecord.getId(), tenantId);
        qmsRawMaterialNcRestoreMapper.deleteDefects(ncRecord.getId(), tenantId);
        qmsRawMaterialNcRestoreMapper.deleteReviews(ncRecord.getId(), tenantId);
        qmsRawMaterialNcRestoreMapper.deleteRelations(ncRecord.getId(), tenantId);
        qmsRawMaterialNcRestoreMapper.deleteFlowLogs(ncRecord.getId(), tenantId);
        qmsRawMaterialNcRestoreMapper.restoreIqcAbnormalLink(ncRecord.getId(), ncRecord.getSourceId(), tenantId,
                String.valueOf(SecurityFrameworkUtils.getLoginUserId()));
        if (qmsRawMaterialNcRestoreMapper.deleteNcRecord(ncRecord.getId(), tenantId) != 1) {
            throw exception(QMS_RAW_MATERIAL_NCR_RESTORE_DELETE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreProductNcRecord(QmsProductNcRecordRestoreReqVO restoreReqVO) {
        QmsNcRecordDO ncRecord = qmsNcRecordMapper.selectByIdForUpdate(restoreReqVO.getId());
        if (ncRecord == null) {
            throw exception(QMS_NCR_NOT_EXISTS);
        }
        if (!Objects.equals(ncRecord.getNcNo(), StrUtil.trim(restoreReqVO.getConfirmNcNo()))) {
            throw exception(QMS_PRODUCT_NCR_RESTORE_CONFIRM_INVALID);
        }
        if (isRawMaterialNcr(ncRecord) || ncRecord.getSourceId() == null
                || StrUtil.isBlank(ncRecord.getSourceBizType())) {
            throw exception(QMS_PRODUCT_NCR_RESTORE_SOURCE_INVALID);
        }

        Long tenantId = Objects.requireNonNullElse(TenantContextHolder.getTenantId(), 0L);
        List<String> restoreWarnings = buildProductNcRestoreWarnings(ncRecord, tenantId);
        List<Qms8dReportDO> linked8dReports = selectLinked8dReportsForRestore(ncRecord, tenantId, true);
        List<Qms8dReportDO> initial8dReports = linked8dReports.stream()
                .filter(report -> isInitial8dReport(report, ncRecord))
                .toList();
        List<Qms8dReportDO> retained8dReports = linked8dReports.stream()
                .filter(report -> !isInitial8dReport(report, ncRecord))
                .toList();
        Set<String> processInstanceIds = findNcProcessInstanceIds(ncRecord, BPM_NCR_PROCESS_KEY);
        Map<String, Object> restoreSummary = buildNcRestoreSummary(ncRecord, processInstanceIds);
        appendForcedRestoreWarningSummary(restoreSummary, restoreWarnings);
        appendInitial8dRestoreSummary(restoreSummary, initial8dReports);
        appendDetached8dRestoreSummary(restoreSummary, retained8dReports);
        qmsNcRestoreLogMapper.insert(QmsNcRestoreLogDO.builder()
                .ncRecordId(ncRecord.getId())
                .ncNo(ncRecord.getNcNo())
                .documentType("PRODUCT")
                .sourceBizType(ncRecord.getSourceBizType())
                .sourceObjectId(ncRecord.getSourceId())
                .sourceObjectNo(ncRecord.getSourceNo())
                .originalStatus(ncRecord.getStatus())
                .processInstanceIds(String.join(",", processInstanceIds))
                .restoreReason(StrUtil.trim(restoreReqVO.getReason()))
                .restoreUserId(SecurityFrameworkUtils.getLoginUserId())
                .restoreUserName(currentUserName())
                .restoreSummary(restoreSummary)
                .tenantId(tenantId)
                .build());

        deleteNcProcessInstances(processInstanceIds, "管理员还原产品NCR");
        deleteInitial8dReports(initial8dReports, tenantId);
        detachRetained8dReports(ncRecord, retained8dReports, tenantId);
        clearProductNcExternalLinks(ncRecord, tenantId);
        qmsProductNcRestoreMapper.deleteCommands(ncRecord.getId(), tenantId);
        qmsProductNcRestoreMapper.deleteReportGates(ncRecord.getId(), tenantId);
        qmsProductNcRestoreMapper.deleteDispositionScopes(ncRecord.getId(), tenantId);
        qmsProductNcRestoreMapper.deleteDispositionExecutions(ncRecord.getId(), tenantId);
        qmsProductNcRestoreMapper.deleteDefects(ncRecord.getId(), tenantId);
        qmsProductNcRestoreMapper.deleteReviews(ncRecord.getId(), tenantId);
        qmsProductNcRestoreMapper.deleteRelations(ncRecord.getId(), tenantId);
        qmsProductNcRestoreMapper.deleteFlowLogs(ncRecord.getId(), tenantId);
        clearProductNcSourceLinks(ncRecord, tenantId);
        if (qmsProductNcRestoreMapper.deleteNcRecord(ncRecord.getId(), tenantId) != 1) {
            throw exception(QMS_PRODUCT_NCR_RESTORE_DELETE_FAILED);
        }
    }

    @Override
    public QmsNcRecordRestorePreviewRespVO getProductNcRestorePreview(Long id) {
        QmsNcRecordDO ncRecord = qmsNcRecordMapper.selectById(id);
        if (ncRecord == null) {
            throw exception(QMS_NCR_NOT_EXISTS);
        }
        if (isRawMaterialNcr(ncRecord) || ncRecord.getSourceId() == null
                || StrUtil.isBlank(ncRecord.getSourceBizType())) {
            throw exception(QMS_PRODUCT_NCR_RESTORE_SOURCE_INVALID);
        }
        Long tenantId = Objects.requireNonNullElse(TenantContextHolder.getTenantId(), 0L);
        return buildNcRestorePreview(ncRecord, "PRODUCT", buildProductNcRestoreWarnings(ncRecord, tenantId));
    }

    private List<String> buildProductNcRestoreWarnings(QmsNcRecordDO ncRecord, Long tenantId) {
        List<String> warnings = new ArrayList<>();
        long otherNcrCount = qmsProductNcRestoreMapper.selectOtherActiveNcrCount(ncRecord.getId(),
                ncRecord.getSourceBizType(), ncRecord.getSourceId(), tenantId);
        if (otherNcrCount > 0) {
            warnings.add("同一产品异常来源已存在其他有效 NCR：" + otherNcrCount + " 单；本次仅删除当前 NCR，并释放当前来源关联。");
        }
        appendCommonRestoreWarnings(warnings, ncRecord,
                qmsProductNcRestoreMapper.selectChildNcrCount(ncRecord.getId(), tenantId),
                qmsProductNcRestoreMapper.selectExceptionLinkCount(ncRecord.getNcNo(), tenantId),
                qmsProductNcRestoreMapper.selectAppliedCommandCount(ncRecord.getId(), tenantId),
                qmsProductNcRestoreMapper.selectEffectiveGateCount(ncRecord.getId(), tenantId),
                tenantId);
        return warnings;
    }

    private void clearProductNcSourceLinks(QmsNcRecordDO ncRecord, Long tenantId) {
        String operatorId = String.valueOf(SecurityFrameworkUtils.getLoginUserId());
        qmsProductNcRestoreMapper.clearFaiAbnormalLink(ncRecord.getId(), tenantId, operatorId);
        qmsProductNcRestoreMapper.clearIpqcAbnormalLink(ncRecord.getId(), tenantId, operatorId);
        qmsProductNcRestoreMapper.clearIpqcOrderLink(ncRecord.getId(), tenantId, operatorId);
        qmsProductNcRestoreMapper.clearFqcAbnormalLink(ncRecord.getId(), ncRecord.getNcNo(), tenantId, operatorId);
        qmsProductNcRestoreMapper.clearFqcOrderLink(ncRecord.getNcNo(), tenantId, operatorId);
        qmsProductNcRestoreMapper.clearOqcAbnormalLink(ncRecord.getId(), ncRecord.getNcNo(), tenantId, operatorId);
        qmsProductNcRestoreMapper.clearOqcOrderLink(ncRecord.getNcNo(), tenantId, operatorId);
    }

    private void clearProductNcExternalLinks(QmsNcRecordDO ncRecord, Long tenantId) {
        String operatorId = String.valueOf(SecurityFrameworkUtils.getLoginUserId());
        qmsProductNcRestoreMapper.clearChildNcrSourceLink(ncRecord.getId(), tenantId, operatorId);
        qmsProductNcRestoreMapper.clearExceptionEventLink(ncRecord.getNcNo(), ncRecord.getRelatedExceptionId(),
                tenantId, operatorId);
    }

    private void clearRawMaterialNcExternalLinks(QmsNcRecordDO ncRecord, Long tenantId) {
        String operatorId = String.valueOf(SecurityFrameworkUtils.getLoginUserId());
        qmsRawMaterialNcRestoreMapper.clearChildNcrSourceLink(ncRecord.getId(), tenantId, operatorId);
        qmsRawMaterialNcRestoreMapper.clearExceptionEventLink(ncRecord.getNcNo(), ncRecord.getRelatedExceptionId(),
                tenantId, operatorId);
    }

    @Override
    public QmsNcRecordRestorePreviewRespVO getRawMaterialNcRestorePreview(Long id) {
        QmsNcRecordDO ncRecord = qmsNcRecordMapper.selectById(id);
        if (ncRecord == null) {
            throw exception(QMS_NCR_NOT_EXISTS);
        }
        if (!NCR_TYPE_RAW_MATERIAL.equals(ncRecord.getSourceType())
                || !SOURCE_BIZ_IQC.equals(ncRecord.getSourceBizType())
                || ncRecord.getSourceId() == null) {
            throw exception(QMS_RAW_MATERIAL_NCR_RESTORE_SOURCE_INVALID);
        }
        Long tenantId = Objects.requireNonNullElse(TenantContextHolder.getTenantId(), 0L);
        return buildNcRestorePreview(ncRecord, NCR_TYPE_RAW_MATERIAL,
                buildRawMaterialNcRestoreWarnings(ncRecord, qmsIqcOrderMapper.selectById(ncRecord.getSourceId()), tenantId));
    }

    private List<String> buildRawMaterialNcRestoreWarnings(QmsNcRecordDO ncRecord, QmsIqcOrderDO sourceIqc,
                                                           Long tenantId) {
        List<String> warnings = new ArrayList<>();
        if (sourceIqc == null) {
            warnings.add("来源 IQC 单据已无法读取；确认后仅删除当前 NCR 并尝试释放来源异常关联。");
        } else if (!JUDGMENT_NG.equalsIgnoreCase(sourceIqc.getJudgment())) {
            warnings.add("来源 IQC 当前判定已变更为 " + StrUtil.blankToDefault(sourceIqc.getJudgment(), "-")
                    + "；确认后仍会删除当前 NCR，IQC 检验数据不删除。");
        }
        long otherNcrCount = qmsRawMaterialNcRestoreMapper.selectOtherActiveNcrCount(ncRecord.getId(),
                ncRecord.getSourceId(), tenantId);
        if (otherNcrCount > 0) {
            warnings.add("同一来源 IQC 已存在其他有效 NCR：" + otherNcrCount + " 单；本次仅删除当前 NCR，并释放当前来源关联。");
        }
        appendCommonRestoreWarnings(warnings, ncRecord,
                qmsRawMaterialNcRestoreMapper.selectChildNcrCount(ncRecord.getId(), tenantId),
                qmsRawMaterialNcRestoreMapper.selectExceptionLinkCount(ncRecord.getNcNo(), tenantId),
                qmsRawMaterialNcRestoreMapper.selectAppliedCommandCount(ncRecord.getId(), tenantId),
                qmsRawMaterialNcRestoreMapper.selectEffectiveGateCount(ncRecord.getId(), tenantId),
                tenantId);
        return warnings;
    }

    private QmsNcRecordRestorePreviewRespVO buildNcRestorePreview(QmsNcRecordDO ncRecord, String documentType,
                                                                  List<String> warnings) {
        return QmsNcRecordRestorePreviewRespVO.builder()
                .id(ncRecord.getId())
                .ncNo(ncRecord.getNcNo())
                .documentType(documentType)
                .status(ncRecord.getStatus())
                .sourceBizType(ncRecord.getSourceBizType())
                .sourceObjectNo(ncRecord.getSourceNo())
                .warnings(warnings)
                .hasWarnings(CollUtil.isNotEmpty(warnings))
                .build();
    }

    private void appendCommonRestoreWarnings(List<String> warnings, QmsNcRecordDO ncRecord, long childNcrCount,
                                             long exceptionLinkCount, long appliedCommandCount,
                                             long effectiveGateCount, Long tenantId) {
        if (StrUtil.isNotBlank(ncRecord.getStatus())
                && !List.of(STATUS_DRAFT, STATUS_RETURNED, STATUS_CANCELLED).contains(ncRecord.getStatus())) {
            warnings.add("当前单据状态为 " + ncRecord.getStatus() + "，可能已经办理或流程已完成；确认后仍会删除当前 NCR 及流程记录。");
        }
        if (childNcrCount > 0) {
            warnings.add("存在基于本单重新发起的 NCR：" + childNcrCount + " 单；确认后会解除这些单据与当前 NCR 的父子来源关联。");
        }
        if (exceptionLinkCount > 0 || ncRecord.getRelatedExceptionId() != null
                || StrUtil.isNotBlank(ncRecord.getRelatedExceptionNo())) {
            warnings.add("已关联异常事件；确认后会解除异常事件与当前 NCR 的关联，不删除异常事件本身。");
        }
        if (appliedCommandCount > 0) {
            warnings.add("存在已下发/已执行的工作台指令：" + appliedCommandCount + " 条；确认后会删除当前 NCR 下的指令记录。");
        }
        if (effectiveGateCount > 0) {
            warnings.add("存在已生效的报工门禁：" + effectiveGateCount + " 条；确认后会删除当前 NCR 下的门禁记录。");
        }
        List<Qms8dReportDO> linkedReports = selectLinked8dReportsForRestore(ncRecord, tenantId, false);
        if (CollUtil.isNotEmpty(linkedReports)) {
            long initialCount = linkedReports.stream().filter(report -> isInitial8dReport(report, ncRecord)).count();
            long retainedCount = linkedReports.size() - initialCount;
            if (initialCount > 0) {
                warnings.add("存在初始化且尚未办理的直连 8D：" + initialCount + " 单；确认后会随当前 NCR 一并删除。");
            }
            if (retainedCount > 0) {
                warnings.add("存在已办理或非初始化 8D：" + retainedCount + " 单；确认后仅解除与当前 NCR 的关联，不删除 8D。");
            }
        }
    }

    private List<Qms8dReportDO> selectLinked8dReportsForRestore(QmsNcRecordDO ncRecord, Long tenantId,
                                                                boolean forUpdate) {
        if (forUpdate) {
            return qmsNcInitial8dRestoreMapper.selectLinkedReportsForUpdate(
                    ncRecord.getId(), ncRecord.getNcNo(), StrUtil.trim(ncRecord.getRelated8dNo()), tenantId);
        }
        return qmsNcInitial8dRestoreMapper.selectLinkedReports(
                ncRecord.getId(), ncRecord.getNcNo(), StrUtil.trim(ncRecord.getRelated8dNo()), tenantId);
    }

    private boolean isInitial8dReport(Qms8dReportDO report, QmsNcRecordDO ncRecord) {
        if (report == null || !"NCR".equalsIgnoreCase(StrUtil.blankToDefault(report.getSourceType(), ""))) {
            return false;
        }
        boolean sourceMatches = Objects.equals(report.getSourceId(), ncRecord.getId())
                && Objects.equals(StrUtil.trim(report.getSourceNo()), StrUtil.trim(ncRecord.getNcNo()));
        if (!sourceMatches
                || !"APPROVING".equalsIgnoreCase(StrUtil.blankToDefault(report.getStatus(), ""))
                || !"D1_D2".equalsIgnoreCase(StrUtil.blankToDefault(report.getCurrentStep(), ""))
                || !"D1_D2_DEFINE".equalsIgnoreCase(StrUtil.blankToDefault(report.getCurrentNodeCode(), ""))
                || !Objects.equals(report.getCreateTime(), report.getUpdateTime())) {
            return false;
        }
        if (StrUtil.isNotBlank(report.getContainmentAction())
                || report.getContainmentOwnerId() != null || StrUtil.isNotBlank(report.getContainmentOwnerName())
                || report.getContainmentDate() != null
                || StrUtil.isNotBlank(report.getRootCauseCategory())
                || StrUtil.isNotBlank(report.getRootCauseAnalysis())
                || StrUtil.isNotBlank(report.getCorrectiveAction())
                || report.getActionOwnerId() != null || StrUtil.isNotBlank(report.getActionOwnerName())
                || report.getActionPlanDate() != null
                || StrUtil.isNotBlank(report.getValidationResult()) || report.getValidationDate() != null
                || Boolean.TRUE.equals(report.getUpdateSop()) || Boolean.TRUE.equals(report.getUpdateFmea())
                || Boolean.TRUE.equals(report.getUpdateControlPlan())
                || StrUtil.isNotBlank(report.getStandardizeDesc())
                || report.getCloseTime() != null || report.getCloseUserId() != null
                || StrUtil.isNotBlank(report.getCloseUserName())) {
            return false;
        }
        if (CollUtil.isNotEmpty(qms8dActionItemMapper.selectListByReportId(report.getId()))) {
            return false;
        }
        List<Qms8dFlowLogDO> flowLogs = qms8dFlowLogMapper.selectListByReportId(report.getId());
        if (flowLogs.size() != 1 || !"CREATE".equalsIgnoreCase(flowLogs.get(0).getActionCode())
                || flowLogs.get(0).getFromStep() != null
                || !"D1_D2".equalsIgnoreCase(StrUtil.blankToDefault(flowLogs.get(0).getToStep(), ""))) {
            return false;
        }
        List<Qms8dRelationDO> relations = qms8dRelationMapper.selectListByReportId(report.getId());
        if (relations.size() != 1 || !isInitial8dNcrRelation(relations.get(0), ncRecord)) {
            return false;
        }
        List<Qms8dTeamMemberDO> members = qms8dTeamMemberMapper.selectListByReportId(report.getId());
        return members.size() == 1
                && "LEADER".equalsIgnoreCase(StrUtil.blankToDefault(members.get(0).getMemberRole(), ""))
                && StrUtil.isBlank(members.get(0).getResponsibility())
                && Objects.equals(members.get(0).getUserId(), report.getInitiatorUserId());
    }

    private boolean isInitial8dNcrRelation(Qms8dRelationDO relation, QmsNcRecordDO ncRecord) {
        return relation != null
                && "NCR".equalsIgnoreCase(StrUtil.blankToDefault(relation.getRelationType(), ""))
                && Boolean.TRUE.equals(relation.getPrimaryFlag())
                && Objects.equals(relation.getRelatedObjectId(), ncRecord.getId())
                && Objects.equals(StrUtil.trim(relation.getRelatedObjectNo()), StrUtil.trim(ncRecord.getNcNo()));
    }

    private void appendInitial8dRestoreSummary(Map<String, Object> summary, List<Qms8dReportDO> reports) {
        summary.put("cascadeDeletedInitial8dCount", reports.size());
        summary.put("cascadeDeletedInitial8dIds", reports.stream().map(Qms8dReportDO::getId).toList());
        summary.put("cascadeDeletedInitial8dNos", reports.stream().map(Qms8dReportDO::getReportNo).toList());
    }

    private void appendForcedRestoreWarningSummary(Map<String, Object> summary, List<String> warnings) {
        summary.put("forcedRestoreWarningCount", warnings.size());
        summary.put("forcedRestoreWarnings", warnings);
    }

    private void appendDetached8dRestoreSummary(Map<String, Object> summary, List<Qms8dReportDO> reports) {
        summary.put("detached8dCount", reports.size());
        summary.put("detached8dIds", reports.stream().map(Qms8dReportDO::getId).toList());
        summary.put("detached8dNos", reports.stream().map(Qms8dReportDO::getReportNo).toList());
    }

    private void deleteInitial8dReports(List<Qms8dReportDO> reports, Long tenantId) {
        for (Qms8dReportDO report : reports) {
            qmsNcInitial8dRestoreMapper.deleteActionItems(report.getId(), tenantId);
            qmsNcInitial8dRestoreMapper.deleteFlowLogs(report.getId(), tenantId);
            qmsNcInitial8dRestoreMapper.deleteRelations(report.getId(), tenantId);
            qmsNcInitial8dRestoreMapper.deleteTeamMembers(report.getId(), tenantId);
            if (qmsNcInitial8dRestoreMapper.deleteReport(report.getId(), tenantId) != 1) {
                throw exception(QMS_NCR_RESTORE_8D_DELETE_FAILED, report.getReportNo());
            }
        }
    }

    private void detachRetained8dReports(QmsNcRecordDO ncRecord, List<Qms8dReportDO> reports, Long tenantId) {
        if (CollUtil.isEmpty(reports)) {
            return;
        }
        String operatorId = String.valueOf(SecurityFrameworkUtils.getLoginUserId());
        for (Qms8dReportDO report : reports) {
            qmsNcInitial8dRestoreMapper.clearReportSourceLink(report.getId(), ncRecord.getId(), ncRecord.getNcNo(),
                    tenantId, operatorId);
            qmsNcInitial8dRestoreMapper.deleteNcrRelations(report.getId(), ncRecord.getId(), ncRecord.getNcNo(),
                    tenantId);
        }
    }

    private Set<String> findRawMaterialNcProcessInstanceIds(QmsNcRecordDO ncRecord) {
        Set<String> processInstanceIds = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(ncRecord.getProcessInstanceId())) {
            processInstanceIds.add(ncRecord.getProcessInstanceId());
        }
        String businessKey = String.valueOf(ncRecord.getId());
        runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(BPM_RAW_MATERIAL_NCR_PROCESS_KEY)
                .processInstanceBusinessKey(businessKey)
                .list()
                .stream()
                .map(ProcessInstance::getId)
                .forEach(processInstanceIds::add);
        historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(BPM_RAW_MATERIAL_NCR_PROCESS_KEY)
                .processInstanceBusinessKey(businessKey)
                .list()
                .stream()
                .map(HistoricProcessInstance::getId)
                .forEach(processInstanceIds::add);
        return processInstanceIds;
    }

    private Map<String, Object> buildRawMaterialNcRestoreSummary(QmsNcRecordDO ncRecord, QmsIqcOrderDO sourceIqc,
                                                                  Set<String> processInstanceIds) {
        Map<String, Object> summary = buildNcRestoreSummary(ncRecord, processInstanceIds);
        summary.put("sourceType", ncRecord.getSourceType());
        summary.put("sourceBizType", ncRecord.getSourceBizType());
        summary.put("sourceIqcExists", sourceIqc != null);
        summary.put("sourceIqcJudgment", sourceIqc == null ? null : sourceIqc.getJudgment());
        summary.put("sourceIqcStatus", sourceIqc == null ? null : sourceIqc.getStatus());
        summary.put("sourceIqcDisposalType", sourceIqc == null ? null : sourceIqc.getDisposalType());
        return summary;
    }

    private Map<String, Object> buildNcRestoreSummary(QmsNcRecordDO ncRecord,
                                                       Set<String> processInstanceIds) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("documentType", isRawMaterialNcr(ncRecord) ? NCR_TYPE_RAW_MATERIAL : "PRODUCT");
        summary.put("sourceType", ncRecord.getSourceType());
        summary.put("sourceBizType", ncRecord.getSourceBizType());
        summary.put("sourceObjectId", ncRecord.getSourceId());
        summary.put("sourceObjectNo", ncRecord.getSourceNo());
        summary.put("defectCount", qmsNcDefectMapper.selectCount(QmsNcDefectDO::getNcRecordId, ncRecord.getId()));
        summary.put("reviewCount", qmsNcMrbReviewMapper.selectCount(QmsNcMrbReviewDO::getNcRecordId, ncRecord.getId()));
        summary.put("relationCount", qmsNcRelationMapper.selectCount(QmsNcRelationDO::getNcRecordId, ncRecord.getId()));
        summary.put("flowLogCount", qmsNcFlowLogMapper.selectCount(QmsNcFlowLogDO::getNcRecordId, ncRecord.getId()));
        summary.put("executionCount", qmsNcDispositionExecutionMapper.selectCount(
                QmsNcDispositionExecutionDO::getNcRecordId, ncRecord.getId()));
        summary.put("scopeCount", qmsNcDispositionScopeMapper.selectCount(
                QmsNcDispositionScopeDO::getNcRecordId, ncRecord.getId()));
        summary.put("reportGateCount", qmsNcReportGateMapper.selectCount(
                QmsNcReportGateDO::getNcRecordId, ncRecord.getId()));
        summary.put("commandCount", qmsNcWorkstationCommandMapper.selectCount(
                QmsNcWorkstationCommandDO::getNcRecordId, ncRecord.getId()));
        summary.put("processInstanceCount", processInstanceIds.size());
        return summary;
    }

    private Set<String> findNcProcessInstanceIds(QmsNcRecordDO ncRecord, String processDefinitionKey) {
        Set<String> processInstanceIds = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(ncRecord.getProcessInstanceId())) {
            processInstanceIds.add(ncRecord.getProcessInstanceId());
        }
        String businessKey = String.valueOf(ncRecord.getId());
        runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .processInstanceBusinessKey(businessKey)
                .list()
                .stream()
                .map(ProcessInstance::getId)
                .forEach(processInstanceIds::add);
        historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .processInstanceBusinessKey(businessKey)
                .list()
                .stream()
                .map(HistoricProcessInstance::getId)
                .forEach(processInstanceIds::add);
        return processInstanceIds;
    }

    private void deleteNcProcessInstances(Set<String> processInstanceIds, String reason) {
        for (String processInstanceId : processInstanceIds) {
            ProcessInstance runningInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (runningInstance != null) {
                runtimeService.deleteProcessInstance(processInstanceId, reason);
            }
            if (historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .count() > 0) {
                historyService.deleteHistoricProcessInstance(processInstanceId);
            }
            bpmProcessInstanceCopyService.deleteProcessInstanceCopy(processInstanceId);
        }
    }

    private void deleteRawMaterialNcProcessInstances(Set<String> processInstanceIds) {
        deleteNcProcessInstances(processInstanceIds, "管理员还原原材料NCR");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNcRecord(QmsNcRecordSaveReqVO updateReqVO) {
        QmsNcRecordDO before = validateNcCanEdit(updateReqVO.getId());
        List<QmsNcMrbReviewDO> beforeReviews = qmsNcMrbReviewMapper.selectListByNcRecordId(before.getId());
        List<QmsNcRelationDO> beforeRelations = qmsNcRelationMapper.selectListByNcRecordId(before.getId());
        QmsNcRecordDO updateObj = BeanUtils.toBean(updateReqVO, QmsNcRecordDO.class);
        updateObj.setId(before.getId());
        if (StrUtil.isNotBlank(updateObj.getFinalDisposition())) {
            updateObj.setMrbDecision(updateObj.getFinalDisposition());
        }
        applyDictSnapshots(updateObj);
        qmsNcRecordMapper.updateById(updateObj);
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        saveReviews(after, updateReqVO.getReviews(), false);
        saveDefects(after, updateReqVO.getDefects());
        saveRelations(after, updateReqVO.getRelations());
        List<QmsNcMrbReviewDO> afterReviews = qmsNcMrbReviewMapper.selectListByNcRecordId(after.getId());
        List<QmsNcRelationDO> afterRelations = qmsNcRelationMapper.selectListByNcRecordId(after.getId());
        writeFlowLog(before, after, "UPDATE", "保存修改", updateReqVO.getRemark(),
                buildUpdateSnapshot(before, after, beforeReviews, afterReviews, beforeRelations, afterRelations));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitNcRecord(QmsNcRecordSubmitReqVO submitReqVO) {
        QmsNcRecordDO before = validateNcCanFlow(submitReqVO.getId());
        if (!STATUS_DRAFT.equals(before.getStatus()) && !STATUS_RETURNED.equals(before.getStatus())) {
            throw exception(QMS_NCR_SUBMIT_STATUS_INVALID);
        }
        validateSubmitRequired(before);
        submitNcRecordToContentConfirm(before, submitReqVO);
    }

    private void submitNcRecordToContentConfirm(QmsNcRecordDO before, QmsNcRecordSubmitReqVO submitReqVO) {
        Long contentConfirmUserId = submitReqVO.getContentConfirmUserId();
        if (contentConfirmUserId == null) {
            throw exception(QMS_NCR_CONTENT_CONFIRM_USER_REQUIRED);
        }
        String contentConfirmUserName = resolveUserDisplayName(contentConfirmUserId,
                submitReqVO.getContentConfirmUserName());
        // 会签单位和会签人在后续“品质部确认/分派”选择，发起登记不预置。
        saveReviews(before, List.of(), false);
        List<QmsNcMrbReviewDO> reviewHandlers = List.of();
        String processInstanceId = startNcrBpmProcess(before, reviewHandlers, contentConfirmUserId);
        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(before.getId());
        updateObj.setStatus(STATUS_CONTENT_CONFIRM);
        updateObj.setProcessInstanceId(processInstanceId);
        updateObj.setCurrentNodeCode(NODE_CONTENT_CONFIRM);
        updateObj.setCurrentNodeName("再次确认内容");
        updateObj.setCurrentHandlerUserId(contentConfirmUserId);
        updateObj.setCurrentHandlerUserName(contentConfirmUserName);
        updateObj.setContentConfirmUserId(contentConfirmUserId);
        updateObj.setContentConfirmUserName(contentConfirmUserName);
        qmsNcRecordMapper.updateById(updateObj);
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        Map<String, List<Long>> nextAssignees = singleNextAssignees(BPM_NODE_CONTENT_CONFIRM, contentConfirmUserId);
        refreshStartUserSelectAssignees(after.getProcessInstanceId(), nextAssignees);
        approveRunningBpmTasks(after, BPM_NODE_START_USER, "NCR提交同步发起登记",
                nextAssignees);
        writeFlowLog(before, after, "SUBMIT", "提交", submitReqVO.getOpinion(),
                snapshot("status", after.getStatus(), "currentNodeName", after.getCurrentNodeName(),
                        "contentConfirmUserName", after.getContentConfirmUserName()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawNcRecord(QmsNcRecordWithdrawReqVO withdrawReqVO) {
        QmsNcRecordDO before = validateNcCanFlow(withdrawReqVO.getId());
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        HistoricTaskInstance withdrawTask = findWithdrawableHistoricTask(before, loginUserId);
        if (withdrawTask == null) {
            throw exception(QMS_NCR_WITHDRAW_NOT_AVAILABLE);
        }
        disableStartUserNodeAutoApproveWhenWithdrawToDraft(before, withdrawTask);
        bpmTaskService.withdrawTask(loginUserId, withdrawTask.getId());

        List<Task> runningTasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                before.getProcessInstanceId(), true, null);
        if (CollUtil.isEmpty(runningTasks)) {
            throw exception(QMS_NCR_WITHDRAW_TARGET_INVALID);
        }
        Task targetTask = runningTasks.get(0);
        String targetStatus = resolveWithdrawStatusByBpmTaskKey(before, targetTask.getTaskDefinitionKey());
        if (StrUtil.isBlank(targetStatus)) {
            throw exception(QMS_NCR_WITHDRAW_TARGET_INVALID);
        }

        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(before.getId());
        updateObj.setStatus(targetStatus);
        applyNode(updateObj, targetStatus);
        applyWithdrawCurrentHandler(before, updateObj, targetStatus, targetTask);
        clearBusinessDataAfterWithdraw(before.getId(), updateObj, targetStatus);
        qmsNcRecordMapper.updateById(updateObj);
        persistClearedBusinessDataAfterWithdraw(before.getId(), targetStatus);
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        writeFlowLog(before, after, "WITHDRAW", "撤回修改", withdrawReqVO.getReason(),
                snapshot("withdrawTaskId", withdrawTask.getId(),
                        "withdrawTaskName", withdrawTask.getName(),
                        "targetTaskKey", targetTask.getTaskDefinitionKey(),
                        "targetTaskName", targetTask.getName(),
                        "targetStatus", targetStatus));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleNcRecord(QmsNcRecordHandleReqVO handleReqVO) {
        QmsNcRecordDO before = validateNcCanFlow(handleReqVO.getId());
        ensureNcrTransferRouteVariable(before);
        validateCurrentHandlerForHandle(before);
        if (STATUS_CONTENT_CONFIRM.equals(before.getStatus())) {
            confirmNcContent(before, handleReqVO);
            return;
        }
        boolean markCurrentReviewHandled = STATUS_MRB_REVIEW.equals(before.getStatus());
        saveReviews(before, handleReqVO.getReviews(), markCurrentReviewHandled);
        if (STATUS_SUBMITTED.equals(before.getStatus())) {
            validateQualityConfirmation(before);
            validateSubmitReviewHandlers(before.getId());
        }
        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(before.getId());
        applyNextNode(before, updateObj, handleReqVO);
        applyQualityTransferDecision(before, updateObj, handleReqVO);
        if (STATUS_SUBMITTED.equals(before.getStatus()) || STATUS_RETURNED.equals(before.getStatus())) {
            updateObj.setQualityConfirmUserId(SecurityFrameworkUtils.getLoginUserId());
            updateObj.setQualityConfirmUserName(currentUserName());
            updateObj.setQualityConfirmTime(LocalDateTime.now());
        }
        if (STATUS_CLOSE_CONFIRM.equals(before.getStatus())) {
            updateObj.setEffectConfirmResult(handleReqVO.getOpinion());
            updateObj.setEffectConfirmUserId(SecurityFrameworkUtils.getLoginUserId());
            updateObj.setEffectConfirmUserName(currentUserName());
            updateObj.setEffectConfirmTime(LocalDateTime.now());
        }
        applyDisposeAssignment(before, updateObj, handleReqVO);
        applyNextHandler(before, updateObj, handleReqVO);
        qmsNcRecordMapper.updateById(updateObj);
        boolean clearCurrentHandler = STATUS_SUBMITTED.equals(before.getStatus())
                || (isQualityTransferStage(before)
                && RAW_TRANSFER_ROUTE_CLOSE.equals(handleReqVO.getTransferRoute()));
        if (clearCurrentHandler) {
            qmsNcRecordMapper.clearCurrentHandler(before.getId());
        }
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        syncDispositionNotifies(before, after, handleReqVO);
        approveCurrentUserMrbReviewTask(before, after, "NCR会签办理同步审批");
        syncQualityTransferBpmTransition(before, after, handleReqVO,
                isRawMaterialNcr(before) ? "原材料NCR办理同步审批" : "产品NCR办理同步审批");
        boolean closeAction = STATUS_CLOSE_CONFIRM.equals(before.getStatus())
                || (isQualityTransferStage(before)
                && RAW_TRANSFER_ROUTE_CLOSE.equals(handleReqVO.getTransferRoute()));
        String actionCode = closeAction ? "CLOSE" : "HANDLE";
        String actionName = closeAction ? "关闭" : "办理";
        writeFlowLog(before, after, actionCode, actionName, handleReqVO.getOpinion(),
                snapshot("status", after.getStatus(), "currentNodeName", after.getCurrentNodeName(),
                        "stockDisposeQty", after.getStockDisposeQty(), "finalDisposeDescription",
                        after.getFinalDisposeDescription()));
        if (isQualityTransferStage(before)
                && !RAW_TRANSFER_ROUTE_FINAL.equals(handleReqVO.getTransferRoute())) {
            createExceptionEventFromNcrIfNecessary(after);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkException(QmsNcRecordLinkExceptionReqVO linkReqVO) {
        QmsNcRecordDO before = validateNcExists(linkReqVO.getId());
        QmsExceptionEventDO exceptionEvent = resolveLinkException(linkReqVO);
        validateExceptionSelectableForNcr(exceptionEvent, before);
        clearOldExceptionLinks(before, exceptionEvent);
        upsertNcrExceptionRelation(before, exceptionEvent, linkReqVO.getRemark());
        upsertExceptionNcrRelation(exceptionEvent, before, linkReqVO.getRemark());

        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(before.getId());
        updateObj.setRelatedExceptionId(exceptionEvent.getId());
        updateObj.setRelatedExceptionNo(exceptionEvent.getExceptionNo());
        qmsNcRecordMapper.updateById(updateObj);

        QmsExceptionEventDO exceptionUpdate = new QmsExceptionEventDO();
        exceptionUpdate.setId(exceptionEvent.getId());
        exceptionUpdate.setRelatedNcrNo(before.getNcNo());
        exceptionUpdate.setIsRelatedProduct(!isRawMaterialNcr(before));
        qmsExceptionEventMapper.updateById(exceptionUpdate);

        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        writeFlowLog(before, after, "LINK_EXCEPTION", "关联异常事件",
                firstNotBlank(linkReqVO.getRemark(), "处置单侧选择关联异常事件"),
                snapshot("exceptionId", exceptionEvent.getId(),
                        "exceptionNo", exceptionEvent.getExceptionNo()));
    }

    private QmsExceptionEventDO resolveLinkException(QmsNcRecordLinkExceptionReqVO linkReqVO) {
        QmsExceptionEventDO exceptionEvent = linkReqVO.getExceptionId() == null
                ? null : qmsExceptionEventMapper.selectById(linkReqVO.getExceptionId());
        if (exceptionEvent == null && StrUtil.isNotBlank(linkReqVO.getExceptionNo())) {
            exceptionEvent = qmsExceptionEventMapper.selectByExceptionNo(linkReqVO.getExceptionNo());
        }
        if (exceptionEvent == null) {
            throw exception(QMS_NCR_EXCEPTION_NOT_EXISTS);
        }
        return exceptionEvent;
    }

    private void validateExceptionSelectableForNcr(QmsExceptionEventDO exceptionEvent, QmsNcRecordDO ncr) {
        if (StrUtil.isNotBlank(exceptionEvent.getRelatedNcrNo())
                && !Objects.equals(exceptionEvent.getRelatedNcrNo(), ncr.getNcNo())) {
            throw exception(QMS_NCR_EXCEPTION_ALREADY_LINKED, exceptionEvent.getRelatedNcrNo());
        }
        for (QmsExceptionRelationDO relation : qmsExceptionRelationMapper.selectListByExceptionId(exceptionEvent.getId())) {
            if ((RELATION_TYPE_NCR.equals(relation.getRelationType())
                    || RELATION_TYPE_RAW_MATERIAL_NCR.equals(relation.getRelationType()))
                    && StrUtil.isNotBlank(relation.getRelatedObjectNo())
                    && !Objects.equals(relation.getRelatedObjectNo(), ncr.getNcNo())) {
                throw exception(QMS_NCR_EXCEPTION_ALREADY_LINKED, relation.getRelatedObjectNo());
            }
        }
    }

    private void clearOldExceptionLinks(QmsNcRecordDO ncr, QmsExceptionEventDO selectedException) {
        for (QmsNcRelationDO relation : qmsNcRelationMapper.selectListByNcRecordId(ncr.getId())) {
            if (!RELATION_TYPE_EXCEPTION.equals(relation.getRelationType())
                    || Objects.equals(relation.getRelatedObjectNo(), selectedException.getExceptionNo())) {
                continue;
            }
            qmsNcRelationMapper.deleteById(relation.getId());
            QmsExceptionEventDO oldException = relation.getRelatedObjectId() == null
                    ? qmsExceptionEventMapper.selectByExceptionNo(relation.getRelatedObjectNo())
                    : qmsExceptionEventMapper.selectById(relation.getRelatedObjectId());
            clearExceptionBackReference(oldException, ncr);
        }
        if (StrUtil.isNotBlank(ncr.getRelatedExceptionNo())
                && !Objects.equals(ncr.getRelatedExceptionNo(), selectedException.getExceptionNo())) {
            clearExceptionBackReference(qmsExceptionEventMapper.selectByExceptionNo(ncr.getRelatedExceptionNo()), ncr);
        }
    }

    private void clearExceptionBackReference(QmsExceptionEventDO exceptionEvent, QmsNcRecordDO ncr) {
        if (exceptionEvent == null) {
            return;
        }
        qmsExceptionRelationMapper.selectListByExceptionId(exceptionEvent.getId()).stream()
                .filter(relation -> (RELATION_TYPE_NCR.equals(relation.getRelationType())
                        || RELATION_TYPE_RAW_MATERIAL_NCR.equals(relation.getRelationType()))
                        && Objects.equals(relation.getRelatedObjectNo(), ncr.getNcNo()))
                .forEach(relation -> qmsExceptionRelationMapper.deleteById(relation.getId()));
        if (Objects.equals(exceptionEvent.getRelatedNcrNo(), ncr.getNcNo())) {
            qmsExceptionEventMapper.update(null, new LambdaUpdateWrapper<QmsExceptionEventDO>()
                    .eq(QmsExceptionEventDO::getId, exceptionEvent.getId())
                    .set(QmsExceptionEventDO::getRelatedNcrNo, null)
                    .set(QmsExceptionEventDO::getIsRelatedProduct, null));
        }
    }

    private void upsertNcrExceptionRelation(QmsNcRecordDO ncr, QmsExceptionEventDO exceptionEvent, String remark) {
        QmsNcRelationDO existing = qmsNcRelationMapper.selectByObjectNo(
                ncr.getId(), RELATION_TYPE_EXCEPTION, exceptionEvent.getExceptionNo());
        QmsNcRelationDO relation = QmsNcRelationDO.builder()
                .ncRecordId(ncr.getId())
                .ncNo(ncr.getNcNo())
                .relationType(RELATION_TYPE_EXCEPTION)
                .relatedObjectId(exceptionEvent.getId())
                .relatedObjectNo(exceptionEvent.getExceptionNo())
                .relatedObjectName(firstNotBlank(exceptionEvent.getDescription(), exceptionEvent.getExceptionNo()))
                .relationStatus(exceptionEvent.getStatus())
                .primaryFlag(true)
                .relationTime(LocalDateTime.now())
                .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                .relationUserName(currentUserName())
                .remark(firstNotBlank(remark, "处置单侧选择关联异常事件"))
                .build();
        if (existing == null) {
            qmsNcRelationMapper.insert(relation);
            return;
        }
        relation.setId(existing.getId());
        qmsNcRelationMapper.updateById(relation);
    }

    private void upsertExceptionNcrRelation(QmsExceptionEventDO exceptionEvent, QmsNcRecordDO ncr, String remark) {
        String relationType = isRawMaterialNcr(ncr) ? RELATION_TYPE_RAW_MATERIAL_NCR : RELATION_TYPE_NCR;
        qmsExceptionRelationMapper.selectListByExceptionId(exceptionEvent.getId()).stream()
                .filter(relation -> (RELATION_TYPE_NCR.equals(relation.getRelationType())
                        || RELATION_TYPE_RAW_MATERIAL_NCR.equals(relation.getRelationType()))
                        && !Objects.equals(relation.getRelationType(), relationType)
                        && Objects.equals(relation.getRelatedObjectNo(), ncr.getNcNo()))
                .forEach(relation -> qmsExceptionRelationMapper.deleteById(relation.getId()));
        QmsExceptionRelationDO existing = qmsExceptionRelationMapper.selectByObjectNo(
                exceptionEvent.getId(), relationType, ncr.getNcNo());
        QmsExceptionRelationDO relation = QmsExceptionRelationDO.builder()
                .exceptionId(exceptionEvent.getId())
                .relationType(relationType)
                .relatedObjectId(ncr.getId())
                .relatedObjectNo(ncr.getNcNo())
                .relatedObjectName(firstNotBlank(ncr.getLotNo(), ncr.getDefectCode(), ncr.getRemark()))
                .relationStatus(ncr.getMrbDecision())
                .primaryFlag(true)
                .relationTime(LocalDateTime.now())
                .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                .relationUserName(currentUserName())
                .remark(firstNotBlank(remark, "处置单侧选择关联异常事件"))
                .build();
        if (existing == null) {
            qmsExceptionRelationMapper.insert(relation);
            return;
        }
        relation.setId(existing.getId());
        qmsExceptionRelationMapper.updateById(relation);
    }

    private void confirmNcContent(QmsNcRecordDO before, QmsNcRecordHandleReqVO handleReqVO) {
        String ncDescription = firstNotBlank(handleReqVO.getNcDescription(), before.getNcDescription());
        if (StrUtil.isBlank(ncDescription)) {
            throw exception(QMS_NCR_CONTENT_CONFIRM_DESCRIPTION_REQUIRED);
        }
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = currentUserName();
        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(before.getId());
        updateObj.setNcDescription(StrUtil.trim(ncDescription));
        updateObj.setStatus(STATUS_SUBMITTED);
        updateObj.setCurrentNodeCode(NODE_SUBMITTED);
        updateObj.setCurrentNodeName("品质部确认/分派");
        updateObj.setContentConfirmUserId(loginUserId);
        updateObj.setContentConfirmUserName(loginUserName);
        updateObj.setContentConfirmTime(now);
        qmsNcRecordMapper.updateById(updateObj);
        qmsNcRecordMapper.clearCurrentHandler(before.getId());
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        approveCurrentUserBpmTask(after, BPM_NODE_CONTENT_CONFIRM, "NCR再次确认内容同步审批", null, null);
        writeFlowLog(before, after, "CONTENT_CONFIRM", "再次确认内容", handleReqVO.getOpinion(),
                snapshot("status", after.getStatus(), "currentNodeName", after.getCurrentNodeName(),
                        "contentConfirmUserName", after.getContentConfirmUserName()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateMrbReview(QmsNcMrbReviewDelegateReqVO delegateReqVO) {
        QmsNcRecordDO ncRecord = validateNcCanFlow(delegateReqVO.getId());
        if (!STATUS_MRB_REVIEW.equals(ncRecord.getStatus())) {
            throw exception(QMS_NCR_MRB_REVIEW_STATUS_INVALID);
        }
        QmsNcMrbReviewDO review = qmsNcMrbReviewMapper.selectById(delegateReqVO.getReviewId());
        if (review == null || !Objects.equals(review.getNcRecordId(), ncRecord.getId())) {
            throw exception(QMS_NCR_MRB_REVIEW_NOT_EXISTS);
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!Objects.equals(review.getHandlerUserId(), loginUserId)) {
            throw exception(QMS_NCR_MRB_REVIEW_NO_PERMISSION);
        }
        if (!isPendingMrbReview(review)) {
            throw exception(QMS_NCR_MRB_REVIEW_STATUS_INVALID);
        }
        Long delegateUserId = delegateReqVO.getDelegateUserId();
        if (delegateUserId == null) {
            throw exception(QMS_NCR_MRB_REVIEW_DELEGATE_REQUIRED);
        }
        if (Objects.equals(delegateUserId, review.getHandlerUserId())) {
            throw exception(QMS_NCR_MRB_REVIEW_DELEGATE_SELF_INVALID);
        }
        AdminUserRespDTO delegateUser = adminUserApi.getUser(delegateUserId);
        String delegateUserName = firstNotBlank(delegateReqVO.getDelegateUserName(),
                delegateUser == null ? null : delegateUser.getNickname(), String.valueOf(delegateUserId));
        LocalDateTime now = LocalDateTime.now();
        QmsNcMrbReviewDO updateObj = new QmsNcMrbReviewDO();
        updateObj.setId(review.getId());
        updateObj.setDelegateUserId(delegateUserId);
        updateObj.setDelegateUserName(delegateUserName);
        updateObj.setDelegateTime(now);
        qmsNcMrbReviewMapper.updateById(updateObj);
        writeFlowLog(ncRecord, ncRecord, "MRB_REVIEW_DELEGATE", "会签办理委托",
                currentUserName() + "委托" + delegateUserName + "代办",
                snapshot("reviewId", review.getId(), "deptName", review.getDeptName(),
                        "handlerUserId", review.getHandlerUserId(), "handlerUserName", review.getHandlerUserName(),
                        "delegateUserId", delegateUserId, "delegateUserName", delegateUserName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnNcRecord(QmsNcRecordReturnReqVO returnReqVO) {
        if (StrUtil.isBlank(returnReqVO.getOpinion())) {
            throw exception(QMS_NCR_RETURN_OPINION_REQUIRED);
        }
        QmsNcRecordDO before = validateNcCanFlow(returnReqVO.getId());
        if (!STATUS_CLOSE_CONFIRM.equals(before.getStatus())) {
            throw exception(QMS_NCR_RETURN_STATUS_INVALID);
        }
        validateCurrentHandlerForHandle(before);
        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(before.getId());
        updateObj.setStatus(STATUS_EXECUTION_ASSIGN);
        updateObj.setCurrentNodeCode(NODE_EXECUTION_ASSIGN);
        updateObj.setCurrentNodeName(isRawMaterialNcr(before)
                ? "品质部转办" : "产品处置范围确认/重新选择执行人");
        updateObj.setCurrentHandlerUserId(resolveAssignmentHandlerUserId(before));
        updateObj.setCurrentHandlerUserName(resolveAssignmentHandlerUserName(before));
        updateObj.setStockDisposeStatus("REWORK");
        updateObj.setStockDisposeResult(null);
        updateObj.setStockDisposeTime(null);
        String returnTarget = !isRawMaterialNcr(before) || before.getFinalApproveTime() != null
                ? BPM_NODE_EXECUTION_ASSIGN : BPM_NODE_REVIEW_ASSIGN;
        returnRunningBpmTask(before, BPM_NODE_REVIEW_CLOSE, returnTarget, returnReqVO.getOpinion());
        qmsNcRecordMapper.updateById(updateObj);
        qmsNcRecordMapper.clearStockDisposeResult(before.getId());
        qmsNcRecordMapper.clearCloseFields(before.getId());
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        writeFlowLog(before, after, "RETURN", "退回", returnReqVO.getOpinion(),
                snapshot("targetNodeCode", after.getCurrentNodeCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalApproveNcRecord(QmsNcRecordFinalApproveReqVO approveReqVO) {
        if (StrUtil.isBlank(approveReqVO.getFinalDisposition()) || StrUtil.isBlank(approveReqVO.getFinalOpinion())) {
            throw exception(QMS_NCR_FINAL_REQUIRED);
        }
        QmsNcRecordDO before = validateNcCanFlow(approveReqVO.getId());
        if (!STATUS_FINAL_APPROVAL.equals(before.getStatus())) {
            throw exception(QMS_NCR_FINAL_STATUS_INVALID);
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (before.getCurrentHandlerUserId() != null && !Objects.equals(before.getCurrentHandlerUserId(), loginUserId)) {
            throw exception(QMS_NCR_CURRENT_HANDLER_INVALID);
        }
        if (before.getFinalApproveTime() != null) {
            throw exception(QMS_RAW_MATERIAL_NCR_FINAL_REPEAT_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(before.getId());
        updateObj.setFinalDisposition(approveReqVO.getFinalDisposition());
        updateObj.setMrbDecision(approveReqVO.getFinalDisposition());
        updateObj.setFinalOpinion(approveReqVO.getFinalOpinion());
        updateObj.setFinalDisposeDescription(null);
        updateObj.setFinalApproverId(loginUserId);
        updateObj.setFinalApproverName(currentUserName());
        updateObj.setFinalApproveTime(now);
        updateObj.setStockDisposeStatus(null);
        updateObj.setStockDisposeQty(null);
        updateObj.setStockDisposeUserId(null);
        updateObj.setStockDisposeUserName(null);
        updateObj.setStockDisposeResult(null);
        updateObj.setStockDisposeTime(null);
        updateObj.setStatus(STATUS_EXECUTION_ASSIGN);
        updateObj.setCurrentNodeCode(NODE_EXECUTION_ASSIGN);
        updateObj.setCurrentNodeName(NCR_TYPE_RAW_MATERIAL.equalsIgnoreCase(
                StrUtil.blankToDefault(before.getSourceType(), ""))
                ? "终审完成/处置分派" : "NCR确认范围/执行分派");
        updateObj.setCurrentHandlerUserId(resolveAssignmentHandlerUserId(before));
        updateObj.setCurrentHandlerUserName(resolveAssignmentHandlerUserName(before));
        qmsNcRecordMapper.updateById(updateObj);
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        approveCurrentUserBpmTask(after, BPM_NODE_FINAL_DISPOSE,
                isRawMaterialNcr(before) ? "原材料NCR终审同步审批" : "产品NCR终审同步审批",
                singleNextAssignees(BPM_NODE_EXECUTION_ASSIGN, resolveAssignmentHandlerUserId(after)), null);
        writeFlowLog(before, after, "FINAL_APPROVE", "终审", approveReqVO.getFinalOpinion(),
                snapshot("finalDisposition", after.getFinalDisposition(), "finalApproverName", after.getFinalApproverName()));
    }

    private void createExceptionEventFromNcrIfNecessary(QmsNcRecordDO ncr) {
        if (ncr == null || !Boolean.TRUE.equals(ncr.getCreateExceptionFlag())
                || ncr.getRelatedExceptionId() != null || StrUtil.isNotBlank(ncr.getRelatedExceptionNo())
                || qmsNcRelationMapper.selectPrimaryRelation(ncr.getId(), RELATION_TYPE_EXCEPTION) != null) {
            return;
        }
        boolean rawMaterialNcr = NCR_TYPE_RAW_MATERIAL.equalsIgnoreCase(StrUtil.blankToDefault(ncr.getSourceType(), ""));
        QmsExceptionEventCreateReqVO exceptionReqVO = new QmsExceptionEventCreateReqVO();
        exceptionReqVO.setSourceType(rawMaterialNcr ? RELATION_TYPE_RAW_MATERIAL_NCR : RELATION_TYPE_NCR);
        exceptionReqVO.setSourceId(ncr.getId());
        exceptionReqVO.setSourceNo(ncr.getNcNo());
        exceptionReqVO.setExceptionType(rawMaterialNcr ? "INSPECTION" : "PRODUCTION");
        exceptionReqVO.setExceptionLevel(firstNotBlank(ncr.getNcLevel(), "MINOR"));
        exceptionReqVO.setDiscoverDeptId(firstNonNull(ncr.getHappenDeptId(), ncr.getApplicantDeptId()));
        exceptionReqVO.setDiscoverDeptCode(defaultCode(exceptionReqVO.getDiscoverDeptId()));
        exceptionReqVO.setDiscoverDeptName(firstNotBlank(ncr.getHappenDeptName(), ncr.getApplicantDeptName(), "当前部门"));
        exceptionReqVO.setDiscovererId(firstNonNull(ncr.getApplicantUserId(), SecurityFrameworkUtils.getLoginUserId()));
        exceptionReqVO.setDiscovererCode(defaultCode(exceptionReqVO.getDiscovererId()));
        exceptionReqVO.setDiscovererName(firstNotBlank(ncr.getApplicantUserName(), currentUserName()));
        exceptionReqVO.setDiscoverTime(firstNonNull(ncr.getHappenTime(), ncr.getFinalApproveTime(), LocalDateTime.now()));
        exceptionReqVO.setIsRelatedProduct(!rawMaterialNcr);
        exceptionReqVO.setRelatedNcrNo(ncr.getNcNo());
        exceptionReqVO.setDescription(firstNotBlank(ncr.getNcDescription(), ncr.getFinalOpinion(), ncr.getRemark(), ncr.getNcNo()));
        exceptionReqVO.setInitialImpact(buildExceptionInitialImpact(ncr));
        exceptionReqVO.setRemark(rawMaterialNcr
                ? "由原材料不合格处置单品质转办自动生成并挂接"
                : "由产品不合格处置单品质转办自动生成并挂接");
        qmsExceptionEventService.createExceptionEvent(exceptionReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockDisposeNcRecord(QmsNcRecordStockDisposeReqVO disposeReqVO) {
        qmsNcRecordMapper.selectByIdForUpdate(disposeReqVO.getId());
        QmsNcRecordDO before = validateNcCanFlow(disposeReqVO.getId());
        if (!STATUS_PENDING_STOCK_DISPOSE.equals(before.getStatus())) {
            throw exception(QMS_NCR_STOCK_STATUS_INVALID);
        }
        boolean rawMaterialNcr = isRawMaterialNcr(before);
        if (rawMaterialNcr) {
            validateWorkstationCommandApplied(before);
        } else if (!pickQualificationService.requiresConfirmation(before)) {
            autoApplyProductWorkstationCommand(before);
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (before.getCurrentHandlerUserId() != null && !Objects.equals(before.getCurrentHandlerUserId(), loginUserId)) {
            throw exception(QMS_NCR_CURRENT_HANDLER_INVALID);
        }
        boolean rollPick = pickQualificationService.requiresConfirmation(before);
        if (rollPick) {
            pickQualificationService.confirm(before, disposeReqVO);
            QmsNcDispositionExecutionDO execution = qmsNcDispositionExecutionMapper.selectByNcRecordId(before.getId());
            QmsNcWorkstationCommandDO command = qmsNcWorkstationCommandMapper.selectByExecutionId(execution.getId());
            updateDispositionRelationStatus(before, "DISPOSITION_EXECUTION", execution.getExecutionNo(), "COMPLETED",
                    "执行人确认所选母卷/加工段挑选合格");
            updateDispositionRelationStatus(before, "WORKSTATION_COMMAND", command.getCommandNo(), "APPLIED",
                    "按已选对象改判合格，原始检验NG保留");
        }
        String uploadOpinion = StrUtil.trimToNull(disposeReqVO.getOpinion());
        String stockResult = rawMaterialNcr
                ? firstNotBlank(disposeReqVO.getStockResult(), "原材料不合格处置结果上传完成")
                : firstNotBlank(disposeReqVO.getStockResult(),
                        before.getFinalDisposeDescription(), "产品NCR处置指令已自动下达");
        if (StrUtil.isBlank(stockResult)) {
            throw exception(QMS_NCR_STOCK_RESULT_REQUIRED);
        }
        LocalDateTime now = LocalDateTime.now();
        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(before.getId());
        updateObj.setStockDisposeStatus(StrUtil.blankToDefault(disposeReqVO.getStockDisposeStatus(), "DONE"));
        updateObj.setStockDisposeUserId(loginUserId);
        updateObj.setStockDisposeUserName(currentUserName());
        updateObj.setStockDisposeResult(rollPick ? "挑选合格（原始检验NG保留）；" + stockResult : stockResult);
        updateObj.setStockDisposeTime(now);
        updateObj.setStatus(STATUS_CLOSE_CONFIRM);
        updateObj.setCurrentNodeCode(NODE_CLOSE_CONFIRM);
        updateObj.setCurrentNodeName("处置完成/复核关闭");
        updateObj.setCurrentHandlerUserId(resolveAssignmentHandlerUserId(before));
        updateObj.setCurrentHandlerUserName(resolveAssignmentHandlerUserName(before));
        qmsNcRecordMapper.updateById(updateObj);
        saveDispositionAttachments(before, disposeReqVO.getRelations());
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        approveCurrentUserBpmTask(after, BPM_NODE_EXECUTION_UPLOAD,
                isRawMaterialNcr(before) ? "原材料NCR处置结果上传同步审批" : "产品NCR处置结果上传同步审批",
                singleNextAssignees(BPM_NODE_REVIEW_CLOSE, resolveAssignmentHandlerUserId(after)), null);
        writeFlowLog(before, after, "STOCK_DISPOSE", "不良品处置执行",
                firstNotBlank(uploadOpinion, stockResult),
                snapshot("lotNo", after.getLotNo(), "defectQty", after.getDefectQty(),
                        "finalDisposition", after.getFinalDisposition(), "stockDisposeQty",
                        after.getStockDisposeQty(), "stockResult", stockResult,
                        "uploadOpinion", uploadOpinion));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void autoCompleteProductNcrAfterPackaging(Collection<String> pieceNos) {
        List<String> normalizedPieceNos = normalizePieceNos(pieceNos);
        if (CollUtil.isEmpty(normalizedPieceNos)) {
            return;
        }
        Set<Long> ncRecordIds = qmsNcDispositionScopeMapper
                .selectPackagingRelevantListByPieceNos(normalizedPieceNos)
                .stream()
                .map(QmsNcDispositionScopeDO::getNcRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long ncRecordId : ncRecordIds) {
            autoCompleteProductNcrAfterPackaging(ncRecordId);
        }
    }

    private void autoCompleteProductNcrAfterPackaging(Long ncRecordId) {
        QmsNcRecordDO before = qmsNcRecordMapper.selectByIdForUpdate(ncRecordId);
        if (!canAutoCompleteProductNcrAfterPackaging(before)) {
            return;
        }
        QmsNcDispositionExecutionDO execution = qmsNcDispositionExecutionMapper.selectByNcRecordId(before.getId());
        if (execution == null) {
            return;
        }
        List<String> requiredPieceNos = qmsNcDispositionScopeMapper.selectListByExecutionId(execution.getId()).stream()
                .filter(scope -> isPackagingRequiredScope(before, scope))
                .map(this::resolveScopePieceNo)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(requiredPieceNos) || !allPiecesPackaged(requiredPieceNos)) {
            return;
        }
        autoApplyProductWorkstationCommand(before);
        LocalDateTime now = LocalDateTime.now();
        String stockResult = buildAutoPackagingStockResult(before, requiredPieceNos);
        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(before.getId());
        updateObj.setStockDisposeStatus("DONE");
        updateObj.setStockDisposeUserId(firstNonNull(before.getStockDisposeUserId(), before.getCurrentHandlerUserId()));
        updateObj.setStockDisposeUserName(firstNotBlank(before.getStockDisposeUserName(),
                before.getCurrentHandlerUserName(), "系统自动"));
        updateObj.setStockDisposeResult(stockResult);
        updateObj.setStockDisposeTime(now);
        updateObj.setStatus(STATUS_CLOSE_CONFIRM);
        updateObj.setCurrentNodeCode(NODE_CLOSE_CONFIRM);
        updateObj.setCurrentNodeName("处置完成/复核关闭");
        updateObj.setCurrentHandlerUserId(resolveAssignmentHandlerUserId(before));
        updateObj.setCurrentHandlerUserName(resolveAssignmentHandlerUserName(before));
        qmsNcRecordMapper.updateById(updateObj);
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        approveRunningBpmTasks(after, BPM_NODE_EXECUTION_UPLOAD,
                "产品NCR处置范围片号已全部完成包装，系统自动完成处置结果上传",
                singleNextAssignees(BPM_NODE_REVIEW_CLOSE, resolveAssignmentHandlerUserId(after)));
        writeFlowLog(before, after, "STOCK_DISPOSE_AUTO_PACKAGING", "包装完成自动推进",
                stockResult,
                snapshot("executionNo", execution.getExecutionNo(), "packagedPieceCount", requiredPieceNos.size(),
                        "packagedPieceNos", requiredPieceNos, "finalDisposition", after.getFinalDisposition()));
    }

    private boolean canAutoCompleteProductNcrAfterPackaging(QmsNcRecordDO ncr) {
        return ncr != null
                && !isRawMaterialNcr(ncr)
                && STATUS_PENDING_STOCK_DISPOSE.equals(ncr.getStatus())
                && SOURCE_BIZ_CUT_ROUND_FQC.equals(normalizeUpper(ncr.getSourceBizType()))
                && !DISPOSITION_SCRAP.equals(normalizeUpper(ncr.getFinalDisposition()));
    }

    private boolean isPackagingRequiredScope(QmsNcRecordDO ncr, QmsNcDispositionScopeDO scope) {
        if (scope == null || !SCOPE_LEVEL_PIECE.equals(normalizeUpper(scope.getScopeLevel()))
                || SCOPE_ROLE_PICK_OUTSIDE_SCRAP.equals(normalizeUpper(scope.getScopeRole()))) {
            return false;
        }
        String dispositionType = normalizeUpper(firstNotBlank(scope.getDispositionType(), ncr.getFinalDisposition()));
        return !DISPOSITION_SCRAP.equals(dispositionType);
    }

    private boolean allPiecesPackaged(List<String> pieceNos) {
        for (String pieceNo : pieceNos) {
            if (!isPiecePackaged(pieceNo)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPiecePackaged(String pieceNo) {
        HcInnerPackUnitItemDO packItem = hcInnerPackUnitItemMapper.selectBySliceBatchNo(pieceNo);
        if (packItem == null || packItem.getInnerUnitId() == null) {
            return false;
        }
        HcInnerPackUnitDO packUnit = hcInnerPackUnitMapper.selectById(packItem.getInnerUnitId());
        return packUnit != null
                && !Boolean.TRUE.equals(packUnit.getDeleted())
                && PACKAGING_EXECUTED_UNIT_STATUSES.contains(normalizeUpper(packUnit.getUnitStatus()));
    }

    private String resolveScopePieceNo(QmsNcDispositionScopeDO scope) {
        return StrUtil.trim(firstNotBlank(scope.getPieceNo(), scope.getSourceObjectNo(), scope.getObjectKey()));
    }

    private List<String> normalizePieceNos(Collection<String> pieceNos) {
        if (pieceNos == null || pieceNos.isEmpty()) {
            return List.of();
        }
        return pieceNos.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private String buildAutoPackagingStockResult(QmsNcRecordDO ncr, List<String> requiredPieceNos) {
        String description = firstNotBlank(ncr.getFinalDisposeDescription(), ncr.getFinalOpinion());
        String result = "处置范围内需包装片号已全部完成包装，共" + requiredPieceNos.size()
                + "片，系统自动完成处置结果上传";
        return StrUtil.isBlank(description) ? result : description + "；" + result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyDispositionNotify(QmsNcDispositionNotifyReplyReqVO replyReqVO) {
        String replyConclusion = StrUtil.trim(replyReqVO.getReplyConclusion());
        if (StrUtil.isBlank(replyConclusion)) {
            throw exception(QMS_NCR_DISPOSITION_NOTIFY_REPLY_REQUIRED);
        }
        QmsNcRecordDO record = validateNcExists(replyReqVO.getId());
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        QmsNcDispositionNotifyDO notify =
                qmsNcDispositionNotifyMapper.selectPendingByNcRecordIdAndNotifyUserId(record.getId(), loginUserId);
        if (notify == null) {
            throw exception(QMS_NCR_DISPOSITION_NOTIFY_NOT_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now();
        QmsNcDispositionNotifyDO updateObj = new QmsNcDispositionNotifyDO();
        updateObj.setId(notify.getId());
        updateObj.setNotifyStatus(DISPOSITION_NOTIFY_STATUS_REPLIED);
        updateObj.setReplyConclusion(replyConclusion);
        updateObj.setReplyUserId(loginUserId);
        updateObj.setReplyUserName(currentUserName());
        updateObj.setReplyTime(now);
        qmsNcDispositionNotifyMapper.updateById(updateObj);
        writeFlowLog(record, record, "DISPOSITION_NOTIFY_REPLY", "通知人回复", replyConclusion,
                snapshot("notifyId", notify.getId(), "notifyUserId", notify.getNotifyUserId(),
                        "notifyUserName", notify.getNotifyUserName(), "replyTime", now));
    }

    private void autoApplyProductWorkstationCommand(QmsNcRecordDO ncr) {
        QmsNcDispositionExecutionDO execution = qmsNcDispositionExecutionMapper.selectByNcRecordId(ncr.getId());
        if (execution == null) {
            return;
        }
        QmsNcWorkstationCommandDO command = qmsNcWorkstationCommandMapper.selectByExecutionId(execution.getId());
        if (command == null) {
            throw exception(QMS_NCR_WORKSTATION_COMMAND_NOT_APPLIED);
        }
        if ("PICK".equals(execution.getDispositionType())
                && List.of("MOTHER_BATCH", "SEGMENT").contains(execution.getScopeLevel())
                && "COMPLETED".equals(execution.getExecutionStatus())) {
            updateDispositionRelationStatus(ncr, "DISPOSITION_EXECUTION", execution.getExecutionNo(), "COMPLETED",
                    "范围确认/执行分派已完成并改判合格");
            updateDispositionRelationStatus(ncr, "WORKSTATION_COMMAND", command.getCommandNo(), "APPLIED",
                    "范围确认/执行分派完成即应用挑选合格结果");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!"APPLIED".equals(execution.getExecutionStatus())) {
            qmsNcDispositionExecutionMapper.updateById(QmsNcDispositionExecutionDO.builder()
                    .id(execution.getId())
                    .executionStatus("APPLIED")
                    .build());
        }
        for (QmsNcDispositionScopeDO scope : qmsNcDispositionScopeMapper.selectListByExecutionId(execution.getId())) {
            if (!"APPLIED".equals(scope.getExecutionResult())) {
                qmsNcDispositionScopeMapper.updateById(QmsNcDispositionScopeDO.builder()
                        .id(scope.getId())
                        .executionResult("APPLIED")
                        .build());
            }
        }
        for (QmsNcReportGateDO gate : qmsNcReportGateMapper.selectListByExecutionId(execution.getId())) {
            if (!"APPLIED".equals(gate.getGateStatus())) {
                qmsNcReportGateMapper.updateById(QmsNcReportGateDO.builder()
                        .id(gate.getId())
                        .gateStatus("APPLIED")
                        .effectiveTime(now)
                        .build());
            }
        }
        if (!"APPLIED".equals(command.getCommandStatus())) {
            qmsNcWorkstationCommandMapper.updateById(QmsNcWorkstationCommandDO.builder()
                    .id(command.getId())
                    .commandStatus("APPLIED")
                    .dispatchTime(firstNonNull(command.getDispatchTime(), now))
                    .ackTime(firstNonNull(command.getAckTime(), now))
                    .ackUser(firstNotBlank(command.getAckUser(), currentUserName()))
                    .ackMessage(firstNotBlank(command.getAckMessage(), "产品NCR处置执行时自动下达并应用"))
                    .applyTime(firstNonNull(command.getApplyTime(), now))
                    .applyResult(firstNotBlank(command.getApplyResult(), "APPLIED"))
                    .build());
        }
        updateDispositionRelationStatus(ncr, "DISPOSITION_EXECUTION", execution.getExecutionNo(), "APPLIED",
                "产品NCR处置执行时自动确认执行单已下达");
        updateDispositionRelationStatus(ncr, "WORKSTATION_COMMAND", command.getCommandNo(), "APPLIED",
                "产品NCR处置执行时自动下达并应用工作台指令");
    }

    private void updateDispositionRelationStatus(QmsNcRecordDO ncr, String relationType,
                                                  String relatedObjectNo, String relationStatus,
                                                  String remark) {
        QmsNcRelationDO relation = qmsNcRelationMapper.selectByObjectNo(ncr.getId(), relationType, relatedObjectNo);
        if (relation == null || relationStatus.equals(relation.getRelationStatus())) {
            return;
        }
        qmsNcRelationMapper.updateById(QmsNcRelationDO.builder()
                .id(relation.getId())
                .relationStatus(relationStatus)
                .remark(remark)
                .build());
    }

    private void validateWorkstationCommandApplied(QmsNcRecordDO ncr) {
        QmsNcDispositionExecutionDO execution = qmsNcDispositionExecutionMapper.selectByNcRecordId(ncr.getId());
        if (execution == null) {
            return;
        }
        QmsNcWorkstationCommandDO command = qmsNcWorkstationCommandMapper.selectByExecutionId(execution.getId());
        if (command == null || !"APPLIED".equals(command.getCommandStatus())) {
            throw exception(QMS_NCR_WORKSTATION_COMMAND_NOT_APPLIED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncBpmProcessStatus(String businessKey, String processInstanceId, Integer bpmStatus, String reason) {
        if (!Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            return;
        }
        QmsNcRecordDO before = resolveNcRecordByBpmBusiness(businessKey, processInstanceId);
        if (before == null || STATUS_CANCELLED.equals(before.getStatus())) {
            return;
        }
        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(before.getId());
        updateObj.setStatus(STATUS_CANCELLED);
        updateObj.setCurrentNodeCode(NODE_CANCELLED);
        updateObj.setCurrentNodeName("已取消");
        updateObj.setCurrentHandlerUserId(null);
        updateObj.setCurrentHandlerUserName(null);
        qmsNcRecordMapper.updateById(updateObj);
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(before.getId());
        writeFlowLog(before, after, "CANCEL", "取消", StrUtil.blankToDefault(reason, "流程已取消"),
                snapshot("processInstanceId", after.getProcessInstanceId(), "bpmStatus", bpmStatus));
    }

    @Override
    public List<QmsNcRecordRespVO.FlowLog> getFlowLogList(Long ncRecordId) {
        validateNcExists(ncRecordId);
        return BeanUtils.toBean(qmsNcFlowLogMapper.selectListByNcRecordId(ncRecordId),
                QmsNcRecordRespVO.FlowLog.class);
    }

    private QmsNcRecordDO findExistingProductEventNcr(String sourceBizType, Long inspectionId) {
        return findExistingSourceNcr(sourceBizType, inspectionId);
    }

    private QmsNcRecordDO findExistingSourceNcr(String sourceBizType, Long inspectionId) {
        if (inspectionId == null || StrUtil.isBlank(sourceBizType)) {
            return null;
        }
        QmsNcRelationDO existingRelation = qmsNcRelationMapper.selectPrimaryByRelatedObject(sourceBizType,
                inspectionId);
        if (existingRelation == null) {
            existingRelation = qmsNcRelationMapper.selectByRelatedObject(sourceBizType, inspectionId);
        }
        if (existingRelation != null && existingRelation.getNcRecordId() != null) {
            QmsNcRecordDO existing = qmsNcRecordMapper.selectLightById(existingRelation.getNcRecordId());
            if (isNonCancelledNcr(existing)) {
                return existing;
            }
        }
        return qmsNcRecordMapper.selectNonCancelledLightBySourceBiz(sourceBizType, inspectionId);
    }

    private String buildRawMaterialIqcInspectionSummary(QmsIqcOrderDO order) {
        return StrUtil.format("IQC检验判定不合格：{} / 批次 {} / 供应商 {}",
                firstNotBlank(order.getMaterialName(), order.getMaterialCode(), "-"),
                firstNotBlank(order.getBatchNo(), "-"), firstNotBlank(order.getSupplierName(), "-"));
    }

    private String buildRawMaterialIqcInspectionSummary(QmsIqcRespVO detail) {
        return StrUtil.format("IQC检验判定不合格：{} / 批次 {} / 供应商 {}",
                firstNotBlank(detail.getMaterialName(), detail.getMaterialCode(), "-"),
                firstNotBlank(detail.getBatchNo(), "-"), firstNotBlank(detail.getSupplierName(), "-"));
    }

    private QmsRawMaterialNcInspectionRespVO buildRawMaterialIqcInspectionRow(QmsIqcOrderDO order) {
        QmsRawMaterialNcInspectionRespVO row = new QmsRawMaterialNcInspectionRespVO();
        row.setInspectionType(SOURCE_BIZ_IQC);
        row.setInspectionTypeName(resolveSourceBizTypeName(SOURCE_BIZ_IQC));
        row.setInspectionId(order.getId());
        row.setInspectionNo(order.getIqcNo());
        row.setInspectionTime(firstNonNull(order.getInspectionTime(), order.getQaTime(),
                order.getInspectionApplyTime(), order.getCreateTime()));
        row.setSupplierCode(order.getSupplierCode());
        row.setSupplierName(order.getSupplierName());
        row.setMaterialId(order.getMaterialId());
        row.setMaterialCode(order.getMaterialCode());
        row.setMaterialName(order.getMaterialName());
        row.setSpecification(firstNotBlank(order.getSpecification(), order.getModelNo()));
        row.setLotNo(order.getBatchNo());
        row.setQuantity(order.getReceiveQty());
        row.setUnitCode(order.getUnit());
        row.setQaInspectorName(order.getQaInspectorName());
        row.setQaTime(order.getQaTime());
        row.setJudgment(order.getJudgment());
        row.setStatus(order.getStatus());
        row.setRawMaterialAbnormalCategory(RAW_MATERIAL_CATEGORY_INCOMING);
        row.setRawMaterialAbnormalCategoryName(resolveRawMaterialCategoryName(RAW_MATERIAL_CATEGORY_INCOMING));
        row.setAbnormalSummary(buildRawMaterialIqcInspectionSummary(order));
        row.setCanGenerateNcr(true);
        row.setNcrGenerated(false);
        row.setNcrStatus(NCR_STATUS_PENDING);
        row.setRejectNextInspectionId(order.getRejectNextInspectionId());
        row.setRejectNextInspectionNo(order.getRejectNextInspectionNo());
        row.setRecheckStatus(RECHECK_STATUS_NONE);
        row.setRecheckStatusName(recheckStatusName(RECHECK_STATUS_NONE));
        row.setCanRejectRecheck(isRawMaterialIqcAbnormal(order));
        return row;
    }

    private List<QmsRawMaterialNcInspectionRespVO> mergeRawMaterialIqcRecheckRows(
            QmsRawMaterialNcInspectionPageReqVO pageReqVO,
            List<QmsRawMaterialNcInspectionRespVO> rawRows) {
        Set<String> chainedKeys = new HashSet<>();
        List<QmsRawMaterialNcInspectionRespVO> recheckRows = new ArrayList<>();
        for (QmsProductEventRecheckGroupDO group : qmsProductEventRecheckGroupMapper.selectListBySourceTypes(
                List.of(SOURCE_BIZ_IQC))) {
            List<QmsProductEventRecheckDetailDO> details =
                    qmsProductEventRecheckDetailMapper.selectListByGroupId(group.getId());
            for (QmsProductEventRecheckDetailDO detail : details) {
                chainedKeys.add(rawMaterialEventKey(detail.getSourceType(), detail.getInspectionId()));
            }
            QmsRawMaterialNcInspectionRespVO row = buildRawMaterialIqcRecheckDisplayRow(group, details);
            if (row != null && matchesRawMaterialInspectionRequest(row, pageReqVO)) {
                recheckRows.add(row);
            }
        }
        List<QmsRawMaterialNcInspectionRespVO> result = rawRows.stream()
                .filter(row -> !chainedKeys.contains(rawMaterialEventKey(row.getInspectionType(), row.getInspectionId())))
                .peek(this::applyRawMaterialDefaultRecheckState)
                .collect(Collectors.toCollection(ArrayList::new));
        result.addAll(recheckRows);
        return result;
    }

    private QmsRawMaterialNcInspectionRespVO buildRawMaterialIqcRecheckDisplayRow(
            QmsProductEventRecheckGroupDO group,
            List<QmsProductEventRecheckDetailDO> details) {
        if (group == null || details == null || details.isEmpty()) {
            return null;
        }
        refreshRawMaterialIqcRecheckGroupDisplayState(group);
        QmsIqcOrderDO rootOrder = qmsIqcOrderMapper.selectById(group.getRootInspectionId());
        QmsIqcOrderDO latestOrder = qmsIqcOrderMapper.selectById(group.getLatestInspectionId());
        if (rootOrder == null || latestOrder == null) {
            return null;
        }
        QmsRawMaterialNcInspectionRespVO row = buildRawMaterialIqcInspectionRow(rootOrder);
        QmsProductEventRecheckDetailDO latestDetail = details.get(details.size() - 1);
        row.setJudgment(latestOrder.getJudgment());
        row.setStatus(latestOrder.getStatus());
        row.setRecheckGroupId(group.getId());
        row.setRecheckStatus(group.getChainStatus());
        row.setRecheckStatusName(recheckStatusName(group.getChainStatus()));
        row.setRecheckCount(group.getTotalRecheckCount());
        row.setRecheckRoundNo(latestDetail.getRoundNo());
        row.setRecheckRootInspectionId(group.getRootInspectionId());
        row.setRecheckRootInspectionNo(group.getRootInspectionNo());
        row.setRecheckPrevInspectionId(latestDetail.getPrevInspectionId());
        row.setRecheckPrevInspectionNo(latestDetail.getPrevInspectionNo());
        row.setRecheckLatestInspectionId(group.getLatestInspectionId());
        row.setRecheckLatestInspectionNo(group.getLatestInspectionNo());
        row.setRejectNextInspectionId(firstNonNull(row.getRejectNextInspectionId(), group.getLatestInspectionId()));
        row.setRejectNextInspectionNo(firstNotBlank(row.getRejectNextInspectionNo(), group.getLatestInspectionNo()));
        row.setRecheckResult(group.getLatestJudgment());
        row.setRejectReason(group.getLastRejectReason());
        row.setRejectUserName(group.getLastRejectUserName());
        row.setRejectTime(group.getLastRejectTime());
        row.setCanRejectRecheck(RECHECK_STATUS_NG.equals(group.getChainStatus()));
        row.setCanGenerateNcr(RECHECK_STATUS_NG.equals(group.getChainStatus()));
        if (RECHECK_STATUS_RECHECKING.equals(group.getChainStatus())) {
            row.setAbnormalSummary("驳回复检中，待完成 IQC 复检判定");
        } else if (RECHECK_STATUS_OK.equals(group.getChainStatus())) {
            row.setAbnormalSummary("原 IQC 判定不合格，复检结果 OK");
        }
        return row;
    }

    private void refreshRawMaterialIqcRecheckGroupDisplayState(QmsProductEventRecheckGroupDO group) {
        QmsIqcOrderDO latestOrder = qmsIqcOrderMapper.selectById(group.getLatestInspectionId());
        if (latestOrder == null) {
            return;
        }
        String chainStatus = resolveRawMaterialIqcRecheckStatus(latestOrder);
        LocalDateTime resultTime = STATUS_COMPLETED.equals(latestOrder.getStatus())
                ? firstNonNull(latestOrder.getInspectionTime(), latestOrder.getQaTime()) : null;
        QmsProductEventRecheckDetailDO latestDetail =
                qmsProductEventRecheckDetailMapper.selectLatestByGroupId(group.getId());
        if (latestDetail != null) {
            QmsProductEventRecheckDetailDO updateDetail = new QmsProductEventRecheckDetailDO();
            updateDetail.setId(latestDetail.getId());
            updateDetail.setInspectionStatus(latestOrder.getStatus());
            updateDetail.setInspectionJudgment(latestOrder.getJudgment());
            updateDetail.setResultTime(resultTime);
            qmsProductEventRecheckDetailMapper.updateById(updateDetail);
        }
        group.setLatestStatus(latestOrder.getStatus());
        group.setLatestJudgment(latestOrder.getJudgment());
        group.setChainStatus(chainStatus);
        group.setLatestResultTime(resultTime);
        QmsProductEventRecheckGroupDO updateGroup = new QmsProductEventRecheckGroupDO();
        updateGroup.setId(group.getId());
        updateGroup.setLatestStatus(group.getLatestStatus());
        updateGroup.setLatestJudgment(group.getLatestJudgment());
        updateGroup.setChainStatus(group.getChainStatus());
        updateGroup.setLatestResultTime(group.getLatestResultTime());
        qmsProductEventRecheckGroupMapper.updateById(updateGroup);
        updateRawMaterialIqcRootRecheckResult(group.getRootInspectionId(), latestOrder.getJudgment(), resultTime);
    }

    private void updateRawMaterialIqcRootRecheckResult(Long rootInspectionId, String result,
                                                       LocalDateTime resultTime) {
        if (rootInspectionId == null) {
            return;
        }
        QmsIqcOrderDO update = new QmsIqcOrderDO();
        update.setId(rootInspectionId);
        update.setRejectRecheckResult(StrUtil.blankToDefault(result, JUDGMENT_PENDING));
        update.setRejectRecheckTime(resultTime);
        qmsIqcOrderMapper.updateById(update);
    }

    private String resolveRawMaterialIqcRecheckStatus(QmsIqcOrderDO latestOrder) {
        if (latestOrder == null) {
            return RECHECK_STATUS_RECHECKING;
        }
        if (JUDGMENT_OK.equalsIgnoreCase(StrUtil.blankToDefault(latestOrder.getJudgment(), ""))) {
            return RECHECK_STATUS_OK;
        }
        if (isRawMaterialIqcAbnormal(latestOrder)) {
            return RECHECK_STATUS_NG;
        }
        return RECHECK_STATUS_RECHECKING;
    }

    private void applyRawMaterialDefaultRecheckState(QmsRawMaterialNcInspectionRespVO row) {
        row.setRecheckStatus(RECHECK_STATUS_NONE);
        row.setRecheckStatusName(recheckStatusName(RECHECK_STATUS_NONE));
        row.setCanRejectRecheck(true);
        row.setCanGenerateNcr(!Boolean.FALSE.equals(row.getCanGenerateNcr()));
    }

    private boolean isRawMaterialIqcAbnormal(QmsIqcOrderDO order) {
        return order != null && (JUDGMENT_NG.equalsIgnoreCase(StrUtil.blankToDefault(order.getJudgment(), ""))
                || STATUS_REJECTED.equalsIgnoreCase(StrUtil.blankToDefault(order.getStatus(), "")));
    }

    private String recheckStatusName(String status) {
        return switch (StrUtil.blankToDefault(status, RECHECK_STATUS_NONE)) {
            case RECHECK_STATUS_RECHECKING -> "复检中";
            case RECHECK_STATUS_OK -> "复检OK";
            case RECHECK_STATUS_NG -> "复检NG";
            default -> "未驳回";
        };
    }

    private String rawMaterialEventKey(String inspectionType, Long inspectionId) {
        return normalizeRawMaterialInspectionType(inspectionType) + ":" + inspectionId;
    }

    private void fillRawMaterialNcrStatus(List<QmsRawMaterialNcInspectionRespVO> rows) {
        for (QmsRawMaterialNcInspectionRespVO row : rows) {
            QmsNcRecordDO existing = findExistingSourceNcr(row.getInspectionType(), row.getInspectionId());
            if (existing == null) {
                continue;
            }
            row.setNcrGenerated(true);
            row.setNcrId(existing.getId());
            row.setNcrNo(existing.getNcNo());
            row.setNcrStatus(NCR_STATUS_GENERATED);
            row.setCanGenerateNcr(false);
        }
    }

    private boolean matchesNcrStatus(QmsRawMaterialNcInspectionRespVO row, String ncrStatus) {
        String normalized = normalizeUpper(ncrStatus);
        if (isAllNcrStatus(normalized)) {
            return true;
        }
        if (NCR_STATUS_GENERATED.equals(normalized)) {
            return Boolean.TRUE.equals(row.getNcrGenerated());
        }
        if (NCR_STATUS_PENDING.equals(normalized)) {
            return !Boolean.TRUE.equals(row.getNcrGenerated());
        }
        return true;
    }

    private boolean matchesRawMaterialInspectionRequest(QmsRawMaterialNcInspectionRespVO row,
                                                        QmsRawMaterialNcInspectionPageReqVO reqVO) {
        return matchesRawMaterialInspectionNo(row, reqVO.getInspectionNo())
                && containsIfPresent(row.getSupplierName(), reqVO.getSupplierName())
                && containsIfPresent(row.getMaterialCode(), reqVO.getMaterialCode())
                && containsIfPresent(row.getMaterialName(), reqVO.getMaterialName())
                && containsIfPresent(row.getLotNo(), reqVO.getLotNo())
                && (matchesTimeRange(row.getInspectionTime(), reqVO.getInspectionTime())
                || matchesTimeRange(row.getRejectTime(), reqVO.getInspectionTime()));
    }

    private boolean matchesRawMaterialInspectionNo(QmsRawMaterialNcInspectionRespVO row, String keyword) {
        return containsIfPresent(row.getInspectionNo(), keyword)
                || containsIfPresent(row.getRejectNextInspectionNo(), keyword)
                || containsIfPresent(row.getRecheckRootInspectionNo(), keyword)
                || containsIfPresent(row.getRecheckPrevInspectionNo(), keyword)
                || containsIfPresent(row.getRecheckLatestInspectionNo(), keyword);
    }

    private boolean containsIfPresent(String value, String keyword) {
        return StrUtil.isBlank(keyword) || StrUtil.containsIgnoreCase(StrUtil.blankToDefault(value, ""), keyword.trim());
    }

    private boolean matchesTimeRange(LocalDateTime value, LocalDateTime[] range) {
        if (range == null || range.length == 0) {
            return true;
        }
        if (value == null) {
            return false;
        }
        LocalDateTime start = range.length > 0 ? range[0] : null;
        LocalDateTime end = range.length > 1 ? range[1] : null;
        return (start == null || !value.isBefore(start)) && (end == null || !value.isAfter(end));
    }

    private int compareRawMaterialInspectionTimeDesc(QmsRawMaterialNcInspectionRespVO left,
                                                     QmsRawMaterialNcInspectionRespVO right) {
        int timeCompare = Comparator.nullsLast(LocalDateTime::compareTo)
                .compare(rawMaterialSortTime(right), rawMaterialSortTime(left));
        if (timeCompare != 0) {
            return timeCompare;
        }
        return Comparator.nullsLast(Long::compareTo)
                .compare(right.getInspectionId(), left.getInspectionId());
    }

    private LocalDateTime rawMaterialSortTime(QmsRawMaterialNcInspectionRespVO row) {
        return firstNonNull(row.getRejectTime(), row.getInspectionTime());
    }

    private List<QmsRawMaterialNcInspectionRespVO> pageRawMaterialRows(
            List<QmsRawMaterialNcInspectionRespVO> rows,
            QmsRawMaterialNcInspectionPageReqVO reqVO) {
        Integer pageSize = reqVO.getPageSize();
        if (pageSize != null && pageSize < 0) {
            return rows;
        }
        int safePageNo = Math.max(1, Objects.requireNonNullElse(reqVO.getPageNo(), 1));
        int safePageSize = Math.max(1, Objects.requireNonNullElse(pageSize, 10));
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new ArrayList<>(rows.subList(fromIndex, toIndex));
    }

    private boolean isAllNcrStatus(String ncrStatus) {
        String normalized = normalizeUpper(ncrStatus);
        return StrUtil.isBlank(normalized) || NCR_STATUS_ALL.equals(normalized);
    }

    private QmsIqcRespVO getRawMaterialIqcDetail(Long inspectionId) {
        QmsIqcRespVO detail = qmsIqcService.getIqcResp(inspectionId);
        if (detail == null || detail.getId() == null || StrUtil.isBlank(detail.getIqcNo())
                || !"NG".equalsIgnoreCase(StrUtil.blankToDefault(detail.getJudgment(), ""))) {
            throw exception(QMS_RAW_MATERIAL_NCR_SOURCE_NOT_EXISTS);
        }
        return detail;
    }

    private QmsNcRecordSaveReqVO buildRawMaterialIqcNcr(QmsIqcRespVO detail) {
        QmsNcRecordSaveReqVO reqVO = new QmsNcRecordSaveReqVO();
        reqVO.setSourceType(NCR_TYPE_RAW_MATERIAL);
        reqVO.setSourceTypeName(resolveSourceTypeName(NCR_TYPE_RAW_MATERIAL));
        reqVO.setSourceBizType(SOURCE_BIZ_IQC);
        reqVO.setSourceBizTypeName(resolveSourceBizTypeName(SOURCE_BIZ_IQC));
        reqVO.setSourceId(detail.getId());
        reqVO.setSourceNo(detail.getIqcNo());
        reqVO.setHappenTime(firstNonNull(detail.getInspectionTime(), detail.getQaTime(),
                detail.getInspectionApplyTime(), detail.getCreateTime(), LocalDateTime.now()));
        reqVO.setProcessName(resolveSourceBizTypeName(SOURCE_BIZ_IQC));
        reqVO.setHappenDeptId(SecurityFrameworkUtils.getLoginUserDeptId());
        reqVO.setHappenDeptName("品质部");
        reqVO.setMaterialId(detail.getMaterialId());
        reqVO.setMaterialCode(detail.getMaterialCode());
        reqVO.setMaterialName(detail.getMaterialName());
        reqVO.setSpecification(firstNotBlank(detail.getSpecification(), detail.getModelNo()));
        reqVO.setUnitCode(detail.getUnit());
        reqVO.setLotNo(detail.getBatchNo());
        reqVO.setDefectQty(resolveRawMaterialDefectQty(detail));
        reqVO.setNcLevel("MAJOR");
        reqVO.setRawMaterialAbnormalCategory(RAW_MATERIAL_CATEGORY_INCOMING);
        reqVO.setRawMaterialAbnormalCategoryName(resolveRawMaterialCategoryName(RAW_MATERIAL_CATEGORY_INCOMING));
        reqVO.setIsolatedFlag(false);
        reqVO.setNcDescription(buildRawMaterialIqcDescription(detail));
        reqVO.setStatus(STATUS_DRAFT);

        List<QmsNcDefectReqVO> defects = buildRawMaterialIqcDefects(detail);
        reqVO.setDefects(defects);
        if (CollUtil.isNotEmpty(defects)) {
            QmsNcDefectReqVO primaryDefect = defects.get(0);
            reqVO.setDefectCode(primaryDefect.getDefectCode());
            reqVO.setDefectName(primaryDefect.getDefectName());
        }

        reqVO.setRelations(List.of(buildRawMaterialIqcRelation(detail, true)));
        reqVO.setRemark("从IQC检验单生成原物料不合格处置单");
        return reqVO;
    }

    private QmsNcRecordSaveReqVO buildMergedRawMaterialIqcNcr(
            List<QmsIqcRespVO> details,
            Map<Long, QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item> itemMap,
            QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO createReqVO) {
        QmsIqcRespVO primary = details.get(0);
        QmsNcRecordSaveReqVO reqVO = buildRawMaterialIqcNcr(primary);
        reqVO.setDefectQty(resolveMergedRawMaterialDefectQty(details, itemMap));
        reqVO.setNcLevel(firstNotBlank(normalizeUpper(createReqVO.getNcLevel()), reqVO.getNcLevel()));
        reqVO.setReviews(buildResponsibleReviews(createReqVO.getResponsibleDeptNames()));
        reqVO.setNcDescription(buildMergedRawMaterialIqcDescription(details, itemMap));
        reqVO.setRelations(buildMergedRawMaterialIqcRelations(details));
        reqVO.setDefects(buildMergedRawMaterialIqcDefects(details, itemMap));
        if (CollUtil.isNotEmpty(reqVO.getDefects())) {
            QmsNcDefectReqVO primaryDefect = reqVO.getDefects().get(0);
            reqVO.setDefectCode(primaryDefect.getDefectCode());
            reqVO.setDefectName(primaryDefect.getDefectName());
        }
        reqVO.setRemark(firstNotBlank(createReqVO.getRemark(), "合并IQC检验单生成原物料不合格处置单"));
        return reqVO;
    }

    private BigDecimal resolveMergedRawMaterialDefectQty(
            List<QmsIqcRespVO> details,
            Map<Long, QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item> itemMap) {
        BigDecimal total = BigDecimal.ZERO;
        for (QmsIqcRespVO detail : details) {
            QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item item = itemMap.get(detail.getId());
            BigDecimal itemQty = item == null ? null : item.getDefectQty();
            total = total.add(itemQty != null && itemQty.compareTo(BigDecimal.ZERO) > 0
                    ? itemQty : resolveRawMaterialDefectQty(detail));
        }
        return total.compareTo(BigDecimal.ZERO) > 0 ? total : BigDecimal.ONE;
    }

    private String buildMergedRawMaterialIqcDescription(
            List<QmsIqcRespVO> details,
            Map<Long, QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item> itemMap) {
        List<String> parts = new ArrayList<>();
        parts.add("合并" + details.size() + "条IQC检验异常生成");
        for (QmsIqcRespVO detail : details) {
            QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item item = itemMap.get(detail.getId());
            String manualDescription = item == null ? "" : StrUtil.trim(item.getNcDescription());
            String description = StrUtil.isNotBlank(manualDescription)
                    ? manualDescription : buildRawMaterialIqcDescription(detail);
            parts.add(StrUtil.format("{}：{} / 批次{} / 供应商{} / {}",
                    firstNotBlank(detail.getIqcNo(), "-"),
                    firstNotBlank(detail.getMaterialName(), detail.getMaterialCode(), "-"),
                    firstNotBlank(detail.getBatchNo(), "-"),
                    firstNotBlank(detail.getSupplierName(), "-"),
                    description));
        }
        return String.join("；", parts);
    }

    private List<QmsNcRelationReqVO> buildMergedRawMaterialIqcRelations(List<QmsIqcRespVO> details) {
        List<QmsNcRelationReqVO> relations = new ArrayList<>();
        for (int i = 0; i < details.size(); i++) {
            relations.add(buildRawMaterialIqcRelation(details.get(i), i == 0));
        }
        return relations;
    }

    private QmsNcRelationReqVO buildRawMaterialIqcRelation(QmsIqcRespVO detail, boolean primary) {
        QmsNcRelationReqVO relation = new QmsNcRelationReqVO();
        relation.setRelationType(SOURCE_BIZ_IQC);
        relation.setRelatedObjectId(detail.getId());
        relation.setRelatedObjectNo(detail.getIqcNo());
        relation.setRelatedObjectName(firstNotBlank(detail.getSupplierName(), detail.getReceiptNo(),
                resolveSourceBizTypeName(SOURCE_BIZ_IQC)));
        relation.setRelationStatus(detail.getStatus());
        relation.setPrimaryFlag(primary);
        relation.setRemark(detail.getRemark());
        return relation;
    }

    private BigDecimal resolveRawMaterialDefectQty(QmsIqcRespVO detail) {
        if (detail.getReceiveQty() != null && detail.getReceiveQty().compareTo(BigDecimal.ZERO) > 0) {
            return detail.getReceiveQty();
        }
        return BigDecimal.ONE;
    }

    private String buildRawMaterialIqcDescription(QmsIqcRespVO detail) {
        List<String> parts = new ArrayList<>();
        String quantityText = buildRawMaterialQuantityText(detail.getReceiveQty(), detail.getUnit());
        if (StrUtil.isNotBlank(quantityText)) {
            parts.add("共到料" + quantityText);
        }
        if (StrUtil.isNotBlank(detail.getBatchNo())) {
            parts.add("批号" + detail.getBatchNo());
        }
        List<String> abnormalItems = buildRawMaterialIqcAbnormalDescriptions(detail);
        if (CollUtil.isNotEmpty(abnormalItems)) {
            parts.add(String.join("；", abnormalItems));
        } else {
            parts.add("IQC检验判定不合格");
        }
        if (StrUtil.isNotBlank(detail.getRemark())) {
            parts.add("备注：" + detail.getRemark());
        }
        return String.join("，", parts);
    }

    private String buildRawMaterialQuantityText(BigDecimal quantity, String unitCode) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        return formatLogValue(quantity) + StrUtil.blankToDefault(StrUtil.trim(unitCode), "");
    }

    private List<String> buildRawMaterialIqcAbnormalDescriptions(QmsIqcRespVO detail) {
        List<String> descriptions = new ArrayList<>();
        if (CollUtil.isNotEmpty(detail.getItems())) {
            detail.getItems().stream()
                    .filter(item -> "NG".equalsIgnoreCase(StrUtil.blankToDefault(item.getItemResult(), "")))
                    .map(this::buildRawMaterialIqcItemDescription)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .limit(8)
                    .forEach(descriptions::add);
        }
        if (CollUtil.isNotEmpty(descriptions)) {
            return descriptions;
        }
        if (CollUtil.isNotEmpty(detail.getAbnormals())) {
            detail.getAbnormals().stream()
                    .map(item -> firstNotBlank(item.getAbnormalDesc(), item.getDefectName(), item.getDefectCode()))
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .limit(8)
                    .forEach(descriptions::add);
        }
        return descriptions;
    }

    private String buildRawMaterialIqcItemDescription(QmsIqcRespVO.IqcItem item) {
        String itemName = firstNotBlank(item.getInspectionItem(), item.getStandardDesc(), "检验项");
        List<QmsIqcRespVO.IqcSample> samples = item.getSamples() == null ? List.of() : item.getSamples();
        List<String> firstValues = samples.stream()
                .filter(sample -> !Boolean.TRUE.equals(sample.getRecheckItemFlag()))
                .map(this::displayRawMaterialSampleValue)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        List<String> recheckValues = samples.stream()
                .filter(sample -> Boolean.TRUE.equals(sample.getRecheckItemFlag()))
                .map(this::displayRawMaterialSampleValue)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        List<String> parts = new ArrayList<>();
        if (CollUtil.isNotEmpty(firstValues)) {
            parts.add(itemName + "实测" + String.join("、", firstValues));
        } else {
            parts.add(itemName + "判定不合格");
        }
        if (CollUtil.isNotEmpty(recheckValues)) {
            parts.add("第二次取样实测：" + String.join("、", recheckValues));
        }
        String standardText = buildRawMaterialIqcStandardText(item);
        return String.join("，", parts) + (StrUtil.isNotBlank(standardText) ? "。标准：" + standardText : "");
    }

    private String displayRawMaterialSampleValue(QmsIqcRespVO.IqcSample sample) {
        if (sample == null) {
            return "";
        }
        if (sample.getMeasuredValue() != null) {
            return formatLogValue(sample.getMeasuredValue());
        }
        if (sample.getResultValue() != null) {
            return formatLogValue(sample.getResultValue());
        }
        if (StrUtil.isNotBlank(sample.getQualitativeValue())) {
            return StrUtil.trim(sample.getQualitativeValue());
        }
        if (sample.getDateValue() != null) {
            return String.valueOf(sample.getDateValue());
        }
        return "";
    }

    private String buildRawMaterialIqcStandardText(QmsIqcRespVO.IqcItem item) {
        if (StrUtil.isNotBlank(item.getStandardDesc())) {
            return StrUtil.trim(item.getStandardDesc());
        }
        if (item.getMinValueLimit() != null && item.getMaxValueLimit() != null) {
            return formatLogValue(item.getMinValueLimit()) + "~" + formatLogValue(item.getMaxValueLimit());
        }
        if (item.getMaxValueLimit() != null) {
            return "≤" + formatLogValue(item.getMaxValueLimit());
        }
        if (item.getMinValueLimit() != null) {
            return "≥" + formatLogValue(item.getMinValueLimit());
        }
        return "";
    }

    private List<QmsNcDefectReqVO> buildRawMaterialIqcDefects(QmsIqcRespVO detail) {
        Map<String, QmsNcDefectReqVO> defectMap = new LinkedHashMap<>();
        int[] sort = {1};
        if (CollUtil.isNotEmpty(detail.getAbnormals())) {
            detail.getAbnormals().forEach(item -> addRawMaterialDefect(defectMap, sort,
                    item.getDefectCode(), item.getDefectName(), "IQC异常",
                    firstNotBlank(item.getAbnormalDesc(), item.getSampleBarcode()), item.getProcessStatus()));
        }
        if (CollUtil.isNotEmpty(detail.getItems())) {
            for (QmsIqcRespVO.IqcItem item : detail.getItems()) {
                if ("NG".equalsIgnoreCase(StrUtil.blankToDefault(item.getItemResult(), ""))) {
                    addRawMaterialDefect(defectMap, sort, null, item.getInspectionItem(), "IQC检验项",
                            item.getInspectionItem(), item.getItemResult());
                }
                if (CollUtil.isEmpty(item.getSamples())) {
                    continue;
                }
                item.getSamples().stream()
                        .filter(sample -> "NG".equalsIgnoreCase(StrUtil.blankToDefault(sample.getSampleResult(), "")))
                        .forEach(sample -> addRawMaterialDefect(defectMap, sort, sample.getDefectCode(),
                                sample.getDefectName(), firstNotBlank(item.getInspectionItem(), "IQC样本"),
                                firstNotBlank(sample.getSampleBarcode(), item.getInspectionItem()),
                                sample.getSampleResult()));
            }
        }
        return new ArrayList<>(defectMap.values());
    }

    private List<QmsNcDefectReqVO> buildMergedRawMaterialIqcDefects(
            List<QmsIqcRespVO> details,
            Map<Long, QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item> itemMap) {
        Map<String, QmsNcDefectReqVO> defectMap = new LinkedHashMap<>();
        int[] sort = {1};
        for (QmsIqcRespVO detail : details) {
            QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO.Item item = itemMap.get(detail.getId());
            if (item != null && (StrUtil.isNotBlank(item.getDefectCode())
                    || StrUtil.isNotBlank(item.getDefectName()))) {
                addRawMaterialDefect(defectMap, sort, item.getDefectCode(), item.getDefectName(),
                        firstNotBlank(detail.getIqcNo(), "IQC异常"),
                        firstNotBlank(item.getNcDescription(), buildRawMaterialIqcInspectionSummary(detail)),
                        JUDGMENT_NG);
            }
            for (QmsNcDefectReqVO defect : buildRawMaterialIqcDefects(detail)) {
                addRawMaterialDefect(defectMap, sort, defect.getDefectCode(), defect.getDefectName(),
                        firstNotBlank(detail.getIqcNo(), defect.getSourceSectionName()),
                        firstNotBlank(defect.getSourceInspectionItem(), buildRawMaterialIqcInspectionSummary(detail)),
                        defect.getSourceResult());
            }
        }
        return new ArrayList<>(defectMap.values());
    }

    private void addRawMaterialDefect(Map<String, QmsNcDefectReqVO> defectMap, int[] sort,
                                      String defectCode, String defectName, String sectionName,
                                      String inspectionItem, String sourceResult) {
        String normalizedCode = StrUtil.trim(defectCode);
        String normalizedName = firstNotBlank(defectName, inspectionItem);
        String key = firstNotBlank(normalizedCode, normalizedName, sectionName, sourceResult);
        if (StrUtil.isBlank(key) || defectMap.containsKey(key)) {
            return;
        }
        QmsNcDefectReqVO defect = new QmsNcDefectReqVO();
        defect.setDefectCode(normalizedCode);
        defect.setDefectName(normalizedName);
        defect.setSourceSectionName(sectionName);
        defect.setSourceInspectionItem(inspectionItem);
        defect.setSourceResult(sourceResult);
        defect.setPrimaryFlag(defectMap.isEmpty());
        defect.setSort(sort[0]++);
        defectMap.put(key, defect);
    }

    private QmsRawMaterialNcRecordCreateFromInspectionRespVO buildRawMaterialCreateResp(String inspectionType,
                                                                                       Long inspectionId,
                                                                                       QmsNcRecordDO ncRecord,
                                                                                       boolean existed) {
        QmsRawMaterialNcRecordCreateFromInspectionRespVO respVO =
                new QmsRawMaterialNcRecordCreateFromInspectionRespVO();
        respVO.setInspectionType(inspectionType);
        respVO.setInspectionId(inspectionId);
        respVO.setExisted(existed);
        if (ncRecord != null) {
            respVO.setNcRecordId(ncRecord.getId());
            respVO.setNcNo(ncRecord.getNcNo());
        }
        return respVO;
    }

    private void validateRawMaterialInspectionType(String inspectionType) {
        if (!SOURCE_BIZ_IQC.equals(inspectionType)) {
            throw exception(QMS_RAW_MATERIAL_NCR_INSPECTION_TYPE_UNSUPPORTED, inspectionType);
        }
    }

    private String normalizeRawMaterialInspectionType(String inspectionType) {
        return StrUtil.blankToDefault(inspectionType, SOURCE_BIZ_IQC).trim().toUpperCase();
    }

    private QmsProductAbnormalEventDetailRespVO getProductEventDetail(String sourceBizType, Long inspectionId) {
        QmsProductAbnormalEventDetailRespVO detail = qmsProductAbnormalEventService.getDetail(sourceBizType,
                inspectionId);
        if (detail == null || detail.getInspectionId() == null || StrUtil.isBlank(detail.getInspectionNo())) {
            throw exception(QMS_NCR_SOURCE_NOT_EXISTS);
        }
        return detail;
    }

    private List<QmsNcMrbReviewReqVO> buildResponsibleReviews(List<String> deptNames) {
        List<QmsNcMrbReviewReqVO> reviews = new ArrayList<>();
        if (CollUtil.isEmpty(deptNames)) {
            return reviews;
        }
        int sort = 1;
        List<String> uniqueNames = new ArrayList<>();
        Set<String> uniqueNameSet = new HashSet<>();
        for (String deptName : deptNames) {
            String normalizedName = StrUtil.trim(deptName);
            if (StrUtil.isBlank(normalizedName) || !uniqueNameSet.add(normalizedName)) {
                continue;
            }
            uniqueNames.add(normalizedName);
        }
        Map<String, QmsNcReviewConfigRespVO> configMap = qmsNcReviewConfigService.getEnabledConfigMap(uniqueNames);
        for (String normalizedName : uniqueNames) {
            QmsNcReviewConfigRespVO config = configMap.get(normalizedName);
            if (config != null && CollUtil.isNotEmpty(config.getHandlerUserIds())) {
                for (int i = 0; i < config.getHandlerUserIds().size(); i++) {
                    QmsNcMrbReviewReqVO review = new QmsNcMrbReviewReqVO();
                    review.setDeptId(config.getDeptId());
                    review.setDeptName(normalizedName);
                    review.setHandlerUserId(config.getHandlerUserIds().get(i));
                    review.setHandlerUserName(config.getHandlerUserNames() != null
                            && config.getHandlerUserNames().size() > i ? config.getHandlerUserNames().get(i) : null);
                    review.setReviewStatus("PENDING");
                    review.setSort(sort++);
                    reviews.add(review);
                }
                continue;
            }
            QmsNcMrbReviewReqVO review = new QmsNcMrbReviewReqVO();
            review.setDeptName(normalizedName);
            review.setReviewStatus("PENDING");
            review.setSort(sort++);
            reviews.add(review);
        }
        return reviews;
    }

    private QmsNcRecordCreateFromProductEventRespVO buildProductEventCreateResp(String sourceBizType,
                                                                                Long inspectionId,
                                                                                QmsNcRecordDO ncRecord,
                                                                                boolean existed) {
        QmsNcRecordCreateFromProductEventRespVO respVO = new QmsNcRecordCreateFromProductEventRespVO();
        respVO.setSourceType(sourceBizType);
        respVO.setInspectionId(inspectionId);
        respVO.setExisted(existed);
        if (ncRecord != null) {
            respVO.setNcRecordId(ncRecord.getId());
            respVO.setNcNo(ncRecord.getNcNo());
        }
        return respVO;
    }

    private QmsNcRecordSaveReqVO buildProductEventNcr(String sourceBizType,
                                                      QmsProductAbnormalEventDetailRespVO detail) {
        QmsNcRecordSaveReqVO reqVO = new QmsNcRecordSaveReqVO();
        String sourceType = resolveNcType(sourceBizType, detail.getOperationName());
        reqVO.setSourceType(sourceType);
        reqVO.setSourceTypeName(resolveSourceTypeName(sourceType));
        reqVO.setSourceBizType(sourceBizType);
        reqVO.setSourceBizTypeName(firstNotBlank(detail.getInspectionType(), resolveSourceBizTypeName(sourceBizType)));
        reqVO.setSourceId(detail.getInspectionId());
        reqVO.setSourceNo(detail.getInspectionNo());
        reqVO.setHappenTime(firstNonNull(detail.getInspectionTime(), detail.getSubmissionTime(), detail.getCreateTime(),
                LocalDateTime.now()));
        reqVO.setProcessName(detail.getOperationName());
        reqVO.setMaterialCode(detail.getMaterialCode());
        reqVO.setMaterialName(firstNotBlank(detail.getProductModel(), detail.getMaterialName()));
        reqVO.setSpecification(detail.getSpecification());
        reqVO.setLotNo(detail.getProductBatchNo());
        reqVO.setDefectQty(resolveInitialDefectQty(detail));
        reqVO.setNcDescription(buildProductEventDescription(detail));
        reqVO.setStatus(STATUS_DRAFT);
        List<QmsNcDefectReqVO> defects = buildProductEventDefects(detail);
        reqVO.setDefects(defects);
        if (CollUtil.isNotEmpty(defects)) {
            QmsNcDefectReqVO primaryDefect = defects.get(0);
            reqVO.setDefectCode(primaryDefect.getDefectCode());
            reqVO.setDefectName(primaryDefect.getDefectName());
        }

        QmsNcRelationReqVO relation = new QmsNcRelationReqVO();
        relation.setRelationType(sourceBizType);
        relation.setRelatedObjectId(detail.getInspectionId());
        relation.setRelatedObjectNo(detail.getInspectionNo());
        relation.setRelatedObjectName(firstNotBlank(detail.getInspectionType(), detail.getSourceReportNo(),
                detail.getWorkOrderNo()));
        relation.setRelationStatus(detail.getStatus());
        relation.setPrimaryFlag(true);
        relation.setRemark(detail.getAbnormalSummary());
        reqVO.setRelations(List.of(relation));
        return reqVO;
    }

    private List<QmsNcDefectReqVO> buildProductEventDefects(QmsProductAbnormalEventDetailRespVO detail) {
        List<ProductEventDefectPiece> defectPieces = collectProductEventDefectPieces(detail);
        if (CollUtil.isNotEmpty(defectPieces)) {
            List<QmsNcDefectReqVO> defects = new ArrayList<>();
            Set<String> uniqueKeys = new LinkedHashSet<>();
            int sort = 1;
            for (ProductEventDefectPiece defectPiece : defectPieces) {
                String key = defectPiece.defectName() + "\u0000" + defectPiece.pieceNo();
                if (!uniqueKeys.add(key)) {
                    continue;
                }
                QmsNcDefectReqVO defect = new QmsNcDefectReqVO();
                defect.setDefectCodeId(defectPiece.defectCodeId());
                defect.setDefectCode(defectPiece.defectCode());
                defect.setDefectName(defectPiece.defectName());
                defect.setSourceSectionName(defectPiece.sectionName());
                defect.setSourceInspectionItem(defectPiece.pieceNo());
                defect.setSourceResult(defectPiece.result());
                defect.setPrimaryFlag(defects.isEmpty());
                defect.setSort(sort++);
                defects.add(defect);
            }
            return defects;
        }
        if (CollUtil.isEmpty(detail.getDetails())) {
            return List.of();
        }
        Map<String, QmsNcDefectReqVO> defectMap = new LinkedHashMap<>();
        int sort = 1;
        for (QmsProductAbnormalEventDetailRespVO.DetailItem item : detail.getDetails()) {
            if (!isAbnormalDetail(item)) {
                continue;
            }
            String defectCode = StrUtil.trim(item.getDefectCode());
            String defectName = firstNotBlank(item.getDefectName(), item.getAbnormalDesc(), item.getInspectionItem());
            String key = firstNotBlank(defectCode, defectName, item.getInspectionItem(), item.getStandardDesc());
            if (StrUtil.isBlank(key) || defectMap.containsKey(key)) {
                continue;
            }
            QmsNcDefectReqVO defect = new QmsNcDefectReqVO();
            defect.setDefectCodeId(item.getDefectCodeId());
            defect.setDefectCode(defectCode);
            defect.setDefectName(defectName);
            defect.setSourceSectionName(item.getSectionName());
            defect.setSourceInspectionItem(item.getInspectionItem());
            defect.setSourceResult(item.getResult());
            defect.setPrimaryFlag(defectMap.isEmpty());
            defect.setSort(sort++);
            defectMap.put(key, defect);
        }
        return new ArrayList<>(defectMap.values());
    }

    private String resolveNcType(String sourceBizType, String operationName) {
        if (SOURCE_BIZ_CUT_ROUND_FQC.equals(sourceBizType)
                || SOURCE_BIZ_FG_SHIPPING_FQC.equals(sourceBizType)
                || SOURCE_BIZ_OQC.equals(sourceBizType)) {
            return NCR_TYPE_FINISHED_PRODUCT;
        }
        String normalizedOperation = StrUtil.blankToDefault(operationName, "");
        if (StrUtil.containsAny(normalizedOperation, "配料", "湿法", "磨皮", "粘胶", "分切", "压槽")) {
            return NCR_TYPE_SEMI_FINISHED;
        }
        return NCR_TYPE_SEMI_FINISHED;
    }

    private BigDecimal resolveInitialDefectQty(QmsProductAbnormalEventDetailRespVO detail) {
        List<ProductEventDefectPiece> defectPieces = collectProductEventDefectPieces(detail);
        if (CollUtil.isNotEmpty(defectPieces)) {
            long pieceCount = defectPieces.stream()
                    .map(ProductEventDefectPiece::pieceNo)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .count();
            if (pieceCount > 0) {
                return BigDecimal.valueOf(pieceCount);
            }
        }
        if (detail.getUnqualifiedQty() != null && detail.getUnqualifiedQty().compareTo(BigDecimal.ZERO) > 0) {
            return detail.getUnqualifiedQty();
        }
        if (CollUtil.isNotEmpty(detail.getDetails())) {
            long abnormalDetailCount = detail.getDetails().stream()
                    .filter(this::isAbnormalDetail)
                    .count();
            if (abnormalDetailCount > 0) {
                return BigDecimal.valueOf(abnormalDetailCount);
            }
        }
        if (CollUtil.isNotEmpty(detail.getAbnormalItems())) {
            return BigDecimal.valueOf(detail.getAbnormalItems().size());
        }
        if (detail.getInspectionQty() != null && detail.getInspectionQty().compareTo(BigDecimal.ZERO) > 0) {
            return detail.getInspectionQty();
        }
        if (detail.getSampleQty() != null && detail.getSampleQty() > 0) {
            return BigDecimal.valueOf(detail.getSampleQty());
        }
        return BigDecimal.ONE;
    }

    private String buildProductEventDescription(QmsProductAbnormalEventDetailRespVO detail) {
        String pieceDescription = buildProductEventPieceDescription(detail);
        if (StrUtil.isNotBlank(pieceDescription)) {
            return pieceDescription;
        }
        List<String> parts = new ArrayList<>();
        String header = StrUtil.format("{}{} 判定不合格",
                firstNotBlank(detail.getInspectionType(), "检验单"),
                StrUtil.isBlank(detail.getInspectionNo()) ? "" : " " + detail.getInspectionNo());
        parts.add(header);
        if (StrUtil.isNotBlank(detail.getAbnormalSummary())) {
            parts.add("不良摘要：" + detail.getAbnormalSummary());
        }
        List<String> abnormalItems = new ArrayList<>();
        if (CollUtil.isNotEmpty(detail.getAbnormalItems())) {
            abnormalItems.addAll(detail.getAbnormalItems().stream()
                    .map(item -> firstNotBlank(item.getTargetNo(), "-") + " / "
                            + firstNotBlank(item.getInspectionItem(), "不合格项"))
                    .distinct()
                    .limit(8)
                    .collect(Collectors.toList()));
        }
        if (CollUtil.isEmpty(abnormalItems) && CollUtil.isNotEmpty(detail.getDetails())) {
            abnormalItems.addAll(detail.getDetails().stream()
                    .filter(item -> isAbnormalDetail(item))
                    .map(item -> firstNotBlank(item.getInspectionItem(), item.getDefectName(), item.getAbnormalDesc()))
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .limit(8)
                    .collect(Collectors.toList()));
        }
        if (CollUtil.isNotEmpty(abnormalItems)) {
            parts.add("不合格项：" + String.join("；", abnormalItems));
        }
        if (StrUtil.isNotBlank(detail.getRemark())) {
            parts.add("备注：" + detail.getRemark());
        }
        return String.join("\n", parts);
    }

    private String buildProductEventPieceDescription(QmsProductAbnormalEventDetailRespVO detail) {
        List<ProductEventDefectPiece> defectPieces = collectProductEventDefectPieces(detail);
        if (CollUtil.isEmpty(defectPieces)) {
            return null;
        }
        Map<String, LinkedHashSet<String>> serialsByDefect = new LinkedHashMap<>();
        for (ProductEventDefectPiece defectPiece : defectPieces) {
            serialsByDefect.computeIfAbsent(defectPiece.defectName(), key -> new LinkedHashSet<>())
                    .add(defectPiece.pieceSerial());
        }
        return serialsByDefect.entrySet().stream()
                .map(entry -> entry.getKey() + "：" + entry.getValue().size() + "片，"
                        + String.join("、", entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private List<ProductEventDefectPiece> collectProductEventDefectPieces(QmsProductAbnormalEventDetailRespVO detail) {
        if (detail == null || CollUtil.isEmpty(detail.getDetails())) {
            return List.of();
        }
        List<ProductEventDefectPiece> pieces = new ArrayList<>();
        Set<String> uniqueKeys = new LinkedHashSet<>();
        for (QmsProductAbnormalEventDetailRespVO.DetailItem item : detail.getDetails()) {
            if (!isProductPieceDetail(item) || !isAbnormalDetail(item)) {
                continue;
            }
            String pieceNo = StrUtil.trim(item.getInspectionItem());
            String defectName = normalizeDefectDisplayName(item.getDefectName(), item.getAbnormalDesc(), null);
            if (StrUtil.isBlank(pieceNo) || StrUtil.isBlank(defectName)) {
                continue;
            }
            String key = defectName + "\u0000" + pieceNo;
            if (!uniqueKeys.add(key)) {
                continue;
            }
            pieces.add(new ProductEventDefectPiece(StrUtil.trim(item.getDefectCode()), item.getDefectCodeId(),
                    defectName, pieceNo, extractPieceSerial(pieceNo), item.getSectionName(), item.getResult()));
        }
        return pieces;
    }

    private boolean isProductPieceDetail(QmsProductAbnormalEventDetailRespVO.DetailItem item) {
        if (item == null) {
            return false;
        }
        String sectionName = StrUtil.trim(item.getSectionName());
        return "送检明细".equals(sectionName) || "发货明细".equals(sectionName);
    }

    private String normalizeDefectDisplayName(String defectName, String abnormalDesc, String fallback) {
        String value = firstNotBlank(defectName, abnormalDesc, fallback);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        value = StrUtil.trim(value);
        int defectMarkerIndex = Math.max(value.lastIndexOf("缺陷："), value.lastIndexOf("缺陷:"));
        if (defectMarkerIndex >= 0) {
            value = value.substring(defectMarkerIndex + 3);
        }
        value = splitBeforeAny(value, "；", ";", "，", ",", "。");
        value = value.replaceFirst("^[A-Za-z0-9_-]+\\s*[-:：]\\s*", "");
        return StrUtil.trim(value);
    }

    private String splitBeforeAny(String value, String... separators) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        int splitIndex = -1;
        for (String separator : separators) {
            int index = value.indexOf(separator);
            if (index >= 0 && (splitIndex < 0 || index < splitIndex)) {
                splitIndex = index;
            }
        }
        return splitIndex >= 0 ? value.substring(0, splitIndex) : value;
    }

    private String extractPieceSerial(String pieceNo) {
        String value = StrUtil.trim(pieceNo);
        if (StrUtil.isBlank(value)) {
            return "";
        }
        int end = value.length() - 1;
        while (end >= 0 && !Character.isDigit(value.charAt(end))) {
            end--;
        }
        if (end < 0) {
            return value;
        }
        int start = end;
        while (start >= 0 && Character.isDigit(value.charAt(start))) {
            start--;
        }
        return value.substring(start + 1, end + 1);
    }

    private boolean isAbnormalDetail(QmsProductAbnormalEventDetailRespVO.DetailItem item) {
        return item != null && ("NG".equalsIgnoreCase(StrUtil.blankToDefault(item.getResult(), ""))
                || "ABNORMAL".equalsIgnoreCase(StrUtil.blankToDefault(item.getResult(), ""))
                || StrUtil.isNotBlank(item.getDefectName())
                || StrUtil.isNotBlank(item.getAbnormalDesc()));
    }

    private record ProductEventDefectPiece(String defectCode, Long defectCodeId, String defectName, String pieceNo,
                                           String pieceSerial, String sectionName, String result) {
    }

    private void validateSubmitRequired(QmsNcRecordDO entity) {
        boolean baseRequiredMissing = StrUtil.isBlank(entity.getLotNo())
                || entity.getDefectQty() == null
                || entity.getDefectQty().compareTo(BigDecimal.ZERO) <= 0
                || StrUtil.isBlank(entity.getNcDescription());
        if (baseRequiredMissing) {
            if (isRawMaterialNcr(entity)) {
                throw exception(QMS_RAW_MATERIAL_NCR_REGISTER_REQUIRED);
            }
            throw exception(QMS_PRODUCT_NCR_REGISTER_REQUIRED);
        }
    }

    private void validateQualityConfirmation(QmsNcRecordDO entity) {
        if (StrUtil.isBlank(entity.getDefectName()) || StrUtil.isBlank(entity.getNcLevel())) {
            if (isRawMaterialNcr(entity)) {
                throw exception(QMS_RAW_MATERIAL_NCR_QUALITY_CONFIRM_REQUIRED);
            }
            throw exception(QMS_PRODUCT_NCR_QUALITY_CONFIRM_REQUIRED);
        }
    }

    private List<QmsNcMrbReviewDO> validateSubmitReviewHandlers(Long ncRecordId) {
        List<QmsNcMrbReviewDO> reviews = qmsNcMrbReviewMapper.selectListByNcRecordId(ncRecordId);
        if (CollUtil.isEmpty(reviews)) {
            throw exception(QMS_NCR_REVIEW_REQUIRED);
        }
        reviews.stream()
                .filter(review -> review.getHandlerUserId() == null)
                .findFirst()
                .ifPresent(review -> {
                    throw exception(QMS_NCR_REVIEW_HANDLER_NOT_CONFIG, review.getDeptName());
                });
        return reviews;
    }

    private String startNcrBpmProcess(QmsNcRecordDO ncRecord, List<QmsNcMrbReviewDO> reviews,
                                      Long contentConfirmUserId) {
        if (StrUtil.isNotBlank(ncRecord.getProcessInstanceId())) {
            return ncRecord.getProcessInstanceId();
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        List<Long> reviewUserIds = reviews.stream()
                .map(QmsNcMrbReviewDO::getHandlerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Object> variables = new HashMap<>();
        variables.put("ncRecordId", ncRecord.getId());
        variables.put("ncNo", ncRecord.getNcNo());
        variables.put("ncLevel", ncRecord.getNcLevel());
        variables.put("responsibleDeptNames", reviews.stream()
                .map(QmsNcMrbReviewDO::getDeptName).filter(StrUtil::isNotBlank)
                .distinct().collect(Collectors.toList()));
        variables.put("coll_userList", reviewUserIds);
        variables.put("contentConfirmUserId", contentConfirmUserId);
        // 审批详情会在用户选择转办路线前预演全部分支。变量必须从流程发起时存在，
        // 否则 Flowable 解析 ${productTransferRoute == ...} / ${rawMaterialTransferRoute == ...} 会抛异常。
        variables.put(resolveTransferRouteVariable(ncRecord), TRANSFER_ROUTE_UNSELECTED);
        Map<String, List<Long>> approveUserSelectAssignees = new HashMap<>();
        if (CollUtil.isNotEmpty(reviewUserIds)) {
            approveUserSelectAssignees.put(BPM_NODE_MRB_REVIEW, reviewUserIds);
        }
        variables.put("PROCESS_APPROVE_USER_SELECT_ASSIGNEES", approveUserSelectAssignees);

        Map<String, List<Long>> startUserSelectAssignees = new HashMap<>();
        if (contentConfirmUserId != null) {
            startUserSelectAssignees.put(BPM_NODE_CONTENT_CONFIRM, List.of(contentConfirmUserId));
        }
        if (CollUtil.isNotEmpty(reviewUserIds)) {
            startUserSelectAssignees.put(BPM_NODE_MRB_REVIEW, reviewUserIds);
        }
        String processKey = resolveBpmProcessKey(ncRecord);
        String modelId = resolveBpmModelId(ncRecord);
        BpmProcessInstanceCreateReqDTO reqDTO = new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(processKey)
                .setBusinessKey(String.valueOf(ncRecord.getId()))
                .setVariables(variables)
                .setStartUserSelectAssignees(startUserSelectAssignees);
        return createNcrProcessInstance(loginUserId, reqDTO, processKey, modelId);
    }

    private String createNcrProcessInstance(Long loginUserId, BpmProcessInstanceCreateReqDTO reqDTO,
                                            String processKey, String modelId) {
        try {
            return bpmProcessInstanceApi.createProcessInstance(loginUserId, reqDTO);
        } catch (ServiceException ex) {
            if (!Objects.equals(ex.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                throw ex;
            }
            deployBuiltInNcrBpmModel(modelId);
            try {
                return bpmProcessInstanceApi.createProcessInstance(loginUserId, reqDTO);
            } catch (ServiceException retryEx) {
                if (Objects.equals(retryEx.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                    throw exception(QMS_NCR_PROCESS_NOT_PUBLISHED, processKey);
                }
                throw retryEx;
            }
        }
    }

    private void deployBuiltInNcrBpmModel(String modelId) {
        bpmModelService.deployModel(BPM_NCR_BUILT_IN_MANAGER_USER_ID, modelId);
    }

    private String resolveBpmProcessKey(QmsNcRecordDO ncRecord) {
        return isRawMaterialNcr(ncRecord) ? BPM_RAW_MATERIAL_NCR_PROCESS_KEY : BPM_NCR_PROCESS_KEY;
    }

    private String resolveBpmModelId(QmsNcRecordDO ncRecord) {
        return isRawMaterialNcr(ncRecord) ? BPM_RAW_MATERIAL_NCR_MODEL_ID : BPM_NCR_MODEL_ID;
    }

    private boolean isRawMaterialNcr(QmsNcRecordDO ncRecord) {
        return ncRecord != null && NCR_TYPE_RAW_MATERIAL.equals(ncRecord.getSourceType());
    }

    private QmsNcRecordDO repairLegacyReviewAssignStatus(QmsNcRecordDO entity) {
        return entity;
    }

    private QmsNcRecordDO syncCancelledFromBpm(QmsNcRecordDO entity) {
        if (entity == null || StrUtil.isBlank(entity.getProcessInstanceId())
                || STATUS_CANCELLED.equals(entity.getStatus())) {
            return entity;
        }
        Integer bpmStatus = bpmProcessInstanceApi.getProcessInstanceStatus(entity.getProcessInstanceId());
        if (!Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            return entity;
        }
        syncBpmProcessStatus(String.valueOf(entity.getId()), entity.getProcessInstanceId(), bpmStatus, "流程已取消");
        return qmsNcRecordMapper.selectById(entity.getId());
    }

    private QmsNcRecordDO resolveNcRecordByBpmBusiness(String businessKey, String processInstanceId) {
        Long ncRecordId = parseLongOrNull(businessKey);
        if (ncRecordId != null) {
            QmsNcRecordDO record = qmsNcRecordMapper.selectById(ncRecordId);
            if (record != null) {
                return record;
            }
        }
        if (StrUtil.isBlank(processInstanceId)) {
            return null;
        }
        return qmsNcRecordMapper.selectByProcessInstanceId(processInstanceId);
    }

    private boolean syncBusinessStatusFromRunningBpm(QmsNcRecordDO record) {
        if (record == null || StrUtil.isBlank(record.getProcessInstanceId())
                || statusIn(record.getStatus(), STATUS_DRAFT, STATUS_CLOSED, STATUS_CANCELLED)) {
            return false;
        }
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                record.getProcessInstanceId(), null, null);
        if (CollUtil.isEmpty(tasks)) {
            return false;
        }
        String targetStatus = resolveStatusByBpmTaskKey(record, tasks.get(0).getTaskDefinitionKey());
        if (StrUtil.isBlank(targetStatus) || targetStatus.equals(record.getStatus())) {
            return false;
        }
        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(record.getId());
        updateObj.setStatus(targetStatus);
        applyNode(updateObj, targetStatus);
        Long assigneeUserId = parseTaskAssignee(tasks.get(0));
        if (assigneeUserId != null && !STATUS_MRB_REVIEW.equals(targetStatus)) {
            updateObj.setCurrentHandlerUserId(assigneeUserId);
        }
        boolean clearBusinessData = Boolean.TRUE.equals(bpmProcessInstanceApi
                .getProcessInstanceVariables(record.getProcessInstanceId())
                .get(BPM_ADJUST_CLEAR_BUSINESS_DATA));
        if (clearBusinessData) {
            clearBusinessDataAfterAdjust(record.getId(), updateObj, targetStatus);
        }
        qmsNcRecordMapper.updateById(updateObj);
        if (clearBusinessData) {
            persistClearedBusinessDataAfterAdjust(record.getId(), targetStatus);
        }
        QmsNcRecordDO after = qmsNcRecordMapper.selectById(record.getId());
        writeFlowLog(record, after, "BPM_ADJUST_SYNC", "流程调整同步",
                "根据流程当前节点同步业务状态",
                snapshot("bpmTaskKey", tasks.get(0).getTaskDefinitionKey(), "targetStatus", targetStatus,
                        "clearBusinessData", clearBusinessData));
        return true;
    }

    private void clearBusinessDataAfterAdjust(Long ncRecordId, QmsNcRecordDO updateObj, String targetStatus) {
        if (statusIn(targetStatus, STATUS_CONTENT_CONFIRM, STATUS_SUBMITTED)) {
            updateObj.setContentConfirmTime(null);
            updateObj.setQualityConfirmUserId(null);
            updateObj.setQualityConfirmUserName(null);
            updateObj.setQualityConfirmTime(null);
            resetMrbReviewsForAdjust(ncRecordId);
            clearFinalFields(updateObj);
            clearStockDisposeFields(updateObj);
            clearCloseFields(updateObj);
            return;
        }
        if (STATUS_MRB_REVIEW.equals(targetStatus)) {
            resetMrbReviewsForAdjust(ncRecordId);
            clearFinalFields(updateObj);
            clearStockDisposeFields(updateObj);
            clearCloseFields(updateObj);
            return;
        }
        if (statusIn(targetStatus, STATUS_REVIEW_ASSIGN, STATUS_FINAL_APPROVAL)) {
            clearFinalFields(updateObj);
            clearStockDisposeFields(updateObj);
            clearCloseFields(updateObj);
            return;
        }
        if (statusIn(targetStatus, STATUS_EXECUTION_ASSIGN, STATUS_PENDING_STOCK_DISPOSE)) {
            clearStockDisposeFields(updateObj);
            clearCloseFields(updateObj);
            return;
        }
        if (STATUS_CLOSE_CONFIRM.equals(targetStatus)) {
            clearCloseFields(updateObj);
        }
    }

    private void clearBusinessDataAfterWithdraw(Long ncRecordId, QmsNcRecordDO updateObj, String targetStatus) {
        if (STATUS_DRAFT.equals(targetStatus)) {
            updateObj.setContentConfirmUserId(null);
            updateObj.setContentConfirmUserName(null);
            updateObj.setContentConfirmTime(null);
            clearBusinessDataAfterAdjust(ncRecordId, updateObj, STATUS_CONTENT_CONFIRM);
        } else {
            clearBusinessDataAfterAdjust(ncRecordId, updateObj, targetStatus);
        }
        if (!STATUS_CLOSE_CONFIRM.equals(targetStatus)) {
            updateObj.setEffectConfirmResult(null);
            updateObj.setEffectConfirmUserId(null);
            updateObj.setEffectConfirmUserName(null);
            updateObj.setEffectConfirmTime(null);
        }
        qmsNcDispositionNotifyMapper.cancelPendingByNcRecordId(ncRecordId, "NCR撤回修改，后续通知待办取消");
    }

    private void persistClearedBusinessDataAfterWithdraw(Long ncRecordId, String targetStatus) {
        if (STATUS_DRAFT.equals(targetStatus)) {
            qmsNcRecordMapper.clearContentConfirmFields(ncRecordId);
            persistClearedBusinessDataAfterAdjust(ncRecordId, STATUS_CONTENT_CONFIRM);
        } else {
            persistClearedBusinessDataAfterAdjust(ncRecordId, targetStatus);
        }
        if (!STATUS_CLOSE_CONFIRM.equals(targetStatus)) {
            qmsNcRecordMapper.clearEffectConfirmFields(ncRecordId);
        }
    }

    private void persistClearedBusinessDataAfterAdjust(Long ncRecordId, String targetStatus) {
        if (statusIn(targetStatus, STATUS_CONTENT_CONFIRM, STATUS_SUBMITTED)) {
            qmsNcRecordMapper.clearQualityConfirmFields(ncRecordId);
            qmsNcRecordMapper.clearFinalFields(ncRecordId);
            qmsNcRecordMapper.clearStockDisposeFields(ncRecordId);
            qmsNcRecordMapper.clearCloseFields(ncRecordId);
            return;
        }
        if (STATUS_MRB_REVIEW.equals(targetStatus)) {
            qmsNcRecordMapper.clearFinalFields(ncRecordId);
            qmsNcRecordMapper.clearStockDisposeFields(ncRecordId);
            qmsNcRecordMapper.clearCloseFields(ncRecordId);
            return;
        }
        if (statusIn(targetStatus, STATUS_REVIEW_ASSIGN, STATUS_FINAL_APPROVAL)) {
            qmsNcRecordMapper.clearFinalFields(ncRecordId);
            qmsNcRecordMapper.clearStockDisposeFields(ncRecordId);
            qmsNcRecordMapper.clearCloseFields(ncRecordId);
            return;
        }
        if (statusIn(targetStatus, STATUS_EXECUTION_ASSIGN, STATUS_PENDING_STOCK_DISPOSE)) {
            qmsNcRecordMapper.clearStockDisposeFields(ncRecordId);
            qmsNcRecordMapper.clearCloseFields(ncRecordId);
            return;
        }
        if (STATUS_CLOSE_CONFIRM.equals(targetStatus)) {
            qmsNcRecordMapper.clearCloseFields(ncRecordId);
        }
    }

    private String resolveWithdrawStatusByBpmTaskKey(QmsNcRecordDO record, String taskDefinitionKey) {
        if (BPM_NODE_START_USER.equals(taskDefinitionKey)) {
            return STATUS_DRAFT;
        }
        return resolveStatusByBpmTaskKey(record, taskDefinitionKey);
    }

    private void disableStartUserNodeAutoApproveWhenWithdrawToDraft(QmsNcRecordDO record,
                                                                    HistoricTaskInstance withdrawTask) {
        if (record == null || withdrawTask == null || StrUtil.isBlank(record.getProcessInstanceId())
                || !BPM_NODE_START_USER.equals(withdrawTask.getTaskDefinitionKey())) {
            return;
        }
        runtimeService.setVariable(record.getProcessInstanceId(),
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_SKIP_START_USER_NODE, "false");
    }

    private void applyWithdrawCurrentHandler(QmsNcRecordDO before, QmsNcRecordDO updateObj,
                                             String targetStatus, Task targetTask) {
        Long handlerUserId = parseTaskAssignee(targetTask);
        String handlerUserName = resolveUserDisplayNameQuietly(handlerUserId);
        if (STATUS_DRAFT.equals(targetStatus)) {
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getApplicantUserId(),
                    SecurityFrameworkUtils.getLoginUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getApplicantUserName(), currentUserName()));
            return;
        }
        if (STATUS_CONTENT_CONFIRM.equals(targetStatus)) {
            updateObj.setCurrentHandlerUserId(firstNonNull(handlerUserId, before.getContentConfirmUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(handlerUserName, before.getContentConfirmUserName()));
            return;
        }
        if (STATUS_SUBMITTED.equals(targetStatus) || STATUS_MRB_REVIEW.equals(targetStatus)) {
            updateObj.setCurrentHandlerUserId(null);
            updateObj.setCurrentHandlerUserName(null);
            return;
        }
        updateObj.setCurrentHandlerUserId(handlerUserId);
        updateObj.setCurrentHandlerUserName(handlerUserName);
    }

    private String resolveUserDisplayNameQuietly(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return firstNotBlank(user == null ? null : user.getNickname(),
                user == null ? null : user.getUsername(), String.valueOf(userId));
    }

    private void resetMrbReviewsForAdjust(Long ncRecordId) {
        List<QmsNcMrbReviewDO> reviews = qmsNcMrbReviewMapper.selectListByNcRecordId(ncRecordId);
        for (QmsNcMrbReviewDO review : reviews) {
            QmsNcMrbReviewDO updateObj = new QmsNcMrbReviewDO();
            updateObj.setId(review.getId());
            updateObj.setSuggestedDisposition(null);
            updateObj.setDispositionDetail(null);
            updateObj.setRootCauseCategory(null);
            updateObj.setCauseAnalysis(null);
            updateObj.setReviewOpinion(null);
            updateObj.setReviewStatus("PENDING");
            updateObj.setHandleTime(null);
            qmsNcMrbReviewMapper.updateById(updateObj);
        }
    }

    private void clearFinalFields(QmsNcRecordDO updateObj) {
        updateObj.setFinalDisposition(null);
        updateObj.setFinalOpinion(null);
        updateObj.setFinalDisposeDescription(null);
        updateObj.setFinalApproverId(null);
        updateObj.setFinalApproverName(null);
        updateObj.setFinalApproveTime(null);
        updateObj.setMrbDecision("PENDING");
    }

    private void clearStockDisposeFields(QmsNcRecordDO updateObj) {
        updateObj.setStockDisposeStatus(null);
        updateObj.setStockDisposeQty(null);
        updateObj.setStockDisposeUserId(null);
        updateObj.setStockDisposeUserName(null);
        updateObj.setStockDisposeResult(null);
        updateObj.setStockDisposeTime(null);
    }

    private void clearCloseFields(QmsNcRecordDO updateObj) {
        updateObj.setCloseTime(null);
        updateObj.setCloseUserId(null);
        updateObj.setCloseUserName(null);
    }

    private String resolveStatusByBpmTaskKey(QmsNcRecordDO record, String taskDefinitionKey) {
        if (BPM_NODE_CONTENT_CONFIRM.equals(taskDefinitionKey)) {
            return STATUS_CONTENT_CONFIRM;
        }
        if (BPM_NODE_QUALITY_CONFIRM.equals(taskDefinitionKey)) {
            return STATUS_SUBMITTED;
        }
        if (BPM_NODE_MRB_REVIEW.equals(taskDefinitionKey)) {
            return STATUS_MRB_REVIEW;
        }
        if (BPM_NODE_REVIEW_ASSIGN.equals(taskDefinitionKey)) {
            if ("REWORK".equals(record.getStockDisposeStatus())) {
                return STATUS_EXECUTION_ASSIGN;
            }
            return STATUS_REVIEW_ASSIGN;
        }
        if (BPM_NODE_FINAL_DISPOSE.equals(taskDefinitionKey)) {
            return STATUS_FINAL_APPROVAL;
        }
        if (BPM_NODE_EXECUTION_ASSIGN.equals(taskDefinitionKey)) {
            return STATUS_EXECUTION_ASSIGN;
        }
        if (BPM_NODE_EXECUTION_UPLOAD.equals(taskDefinitionKey)) {
            return STATUS_PENDING_STOCK_DISPOSE;
        }
        if (BPM_NODE_REVIEW_CLOSE.equals(taskDefinitionKey)) {
            return STATUS_CLOSE_CONFIRM;
        }
        return null;
    }

    private void syncBpmToBusinessStatus(QmsNcRecordDO record, String reason) {
        if (record == null || StrUtil.isBlank(record.getProcessInstanceId())) {
            return;
        }
        if (usesIndependentTransferWorkflow(record)) {
            // 产品和原材料均包含可选终审分支，必须按本次业务动作精确推进，禁止按状态跨节点自动补审批。
            return;
        }
        String status = record.getStatus();
        if (statusIn(status, STATUS_SUBMITTED, STATUS_MRB_REVIEW, STATUS_REVIEW_ASSIGN, STATUS_FINAL_APPROVAL,
                STATUS_EXECUTION_ASSIGN, STATUS_PENDING_STOCK_DISPOSE, STATUS_CLOSE_CONFIRM, STATUS_CLOSED)) {
            approveRunningBpmTasks(record, BPM_NODE_START_USER, reason,
                    singleNextAssignees(BPM_NODE_QUALITY_CONFIRM, record.getCurrentHandlerUserId()));
        }
        if (statusIn(status, STATUS_MRB_REVIEW, STATUS_REVIEW_ASSIGN, STATUS_FINAL_APPROVAL,
                STATUS_EXECUTION_ASSIGN, STATUS_PENDING_STOCK_DISPOSE, STATUS_CLOSE_CONFIRM, STATUS_CLOSED)) {
            approveRunningBpmTasks(record, BPM_NODE_QUALITY_CONFIRM, reason,
                    singleNextAssignees(BPM_NODE_MRB_REVIEW, resolveReviewHandlerUserIds(record.getId())));
        }
        if (statusIn(status, STATUS_REVIEW_ASSIGN, STATUS_FINAL_APPROVAL, STATUS_EXECUTION_ASSIGN,
                STATUS_PENDING_STOCK_DISPOSE, STATUS_CLOSE_CONFIRM, STATUS_CLOSED)) {
            approveRunningBpmTasks(record, BPM_NODE_MRB_REVIEW, reason,
                    singleNextAssignees(BPM_NODE_REVIEW_ASSIGN, resolveAssignmentHandlerUserId(record)));
        }
        if (STATUS_FINAL_APPROVAL.equals(status)) {
            approveRunningBpmTasks(record, BPM_NODE_REVIEW_ASSIGN, reason,
                    singleNextAssignees(BPM_NODE_FINAL_DISPOSE, record.getCurrentHandlerUserId()));
        }
        if (statusIn(status, STATUS_EXECUTION_ASSIGN, STATUS_PENDING_STOCK_DISPOSE,
                STATUS_CLOSE_CONFIRM, STATUS_CLOSED)) {
            approveRunningBpmTasks(record, BPM_NODE_FINAL_DISPOSE, reason,
                    singleNextAssignees(BPM_NODE_EXECUTION_ASSIGN, resolveAssignmentHandlerUserId(record)));
        }
        if (statusIn(status, STATUS_PENDING_STOCK_DISPOSE, STATUS_CLOSE_CONFIRM, STATUS_CLOSED)) {
            approveRunningBpmTasks(record, BPM_NODE_EXECUTION_ASSIGN, reason,
                    singleNextAssignees(BPM_NODE_EXECUTION_UPLOAD,
                            firstNonNull(record.getStockDisposeUserId(), record.getCurrentHandlerUserId())));
        }
        if (statusIn(status, STATUS_CLOSE_CONFIRM, STATUS_CLOSED)) {
            approveRunningBpmTasks(record, BPM_NODE_EXECUTION_UPLOAD, reason,
                    singleNextAssignees(BPM_NODE_REVIEW_CLOSE, resolveAssignmentHandlerUserId(record)));
        }
        if (STATUS_CLOSED.equals(status)) {
            approveRunningBpmTasks(record, BPM_NODE_REVIEW_CLOSE, reason, null);
        }
    }

    private void returnRunningBpmTask(QmsNcRecordDO record, String sourceTaskDefinitionKey,
                                      String targetTaskDefinitionKey, String reason) {
        if (record == null || StrUtil.isBlank(record.getProcessInstanceId())) {
            return;
        }
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                record.getProcessInstanceId(), true, sourceTaskDefinitionKey);
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        for (Task task : tasks) {
            Long assigneeUserId = parseTaskAssignee(task);
            if (assigneeUserId == null) {
                assigneeUserId = SecurityFrameworkUtils.getLoginUserId();
            }
            BpmTaskReturnReqVO reqVO = new BpmTaskReturnReqVO();
            reqVO.setId(task.getId());
            reqVO.setTargetTaskDefinitionKey(targetTaskDefinitionKey);
            reqVO.setReason(reason);
            bpmTaskService.returnTask(assigneeUserId, reqVO);
        }
    }

    private void approveRunningBpmTasks(QmsNcRecordDO record, String taskDefinitionKey, String reason,
                                        Map<String, List<Long>> nextAssignees) {
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                record.getProcessInstanceId(), true, taskDefinitionKey);
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        for (Task task : tasks) {
            Long assigneeUserId = parseTaskAssignee(task);
            if (assigneeUserId == null) {
                continue;
            }
            BpmTaskApproveReqVO reqVO = new BpmTaskApproveReqVO()
                    .setId(task.getId())
                    .setReason(reason)
                    .setVariables(buildNcrBpmVariables(record));
            if (nextAssignees != null && !nextAssignees.isEmpty()) {
                reqVO.setNextAssignees(nextAssignees);
            }
            bpmTaskService.approveTask(assigneeUserId, reqVO);
        }
    }

    private void approveCurrentUserMrbReviewTask(QmsNcRecordDO before, QmsNcRecordDO after, String reason) {
        if (!STATUS_MRB_REVIEW.equals(before.getStatus()) || after == null
                || StrUtil.isBlank(after.getProcessInstanceId())) {
            return;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return;
        }
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                after.getProcessInstanceId(), true, BPM_NODE_MRB_REVIEW);
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        Set<Long> handledAssigneeUserIds = qmsNcMrbReviewMapper.selectListByNcRecordId(after.getId()).stream()
                .filter(review -> Objects.equals(review.getActualHandlerUserId(), loginUserId)
                        || Objects.equals(review.getHandlerUserId(), loginUserId))
                .map(QmsNcMrbReviewDO::getHandlerUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(handledAssigneeUserIds)) {
            return;
        }
        Map<String, List<Long>> nextAssignees = singleNextAssignees(
                BPM_NODE_REVIEW_ASSIGN, resolveAssignmentHandlerUserId(after));
        for (Task task : tasks) {
            Long assigneeUserId = parseTaskAssignee(task);
            if (!handledAssigneeUserIds.contains(assigneeUserId)) {
                continue;
            }
            BpmTaskApproveReqVO reqVO = new BpmTaskApproveReqVO()
                    .setId(task.getId())
                    .setReason(reason)
                    .setVariables(buildNcrBpmVariables(after));
            if (nextAssignees != null && !nextAssignees.isEmpty()) {
                reqVO.setNextAssignees(nextAssignees);
            }
            bpmTaskService.approveTask(assigneeUserId, reqVO);
        }
    }

    private Map<String, Object> buildNcrBpmVariables(QmsNcRecordDO record) {
        List<QmsNcMrbReviewDO> reviews = qmsNcMrbReviewMapper.selectListByNcRecordId(record.getId());
        List<Long> reviewUserIds = reviews.stream()
                .map(QmsNcMrbReviewDO::getHandlerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Object> variables = new HashMap<>();
        variables.put("ncRecordId", record.getId());
        variables.put("ncNo", record.getNcNo());
        variables.put("ncLevel", record.getNcLevel());
        variables.put("status", record.getStatus());
        variables.put("contentConfirmUserId", record.getContentConfirmUserId());
        variables.put("coll_userList", reviewUserIds);
        variables.put("responsibleDeptNames", reviews.stream()
                .map(QmsNcMrbReviewDO::getDeptName)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList()));
        return variables;
    }

    private void refreshStartUserSelectAssignees(String processInstanceId, Map<String, List<Long>> nextAssignees) {
        if (StrUtil.isBlank(processInstanceId) || CollUtil.isEmpty(nextAssignees)) {
            return;
        }
        Map<String, List<Long>> startUserSelectAssignees = new HashMap<>();
        Object existingValue = runtimeService.getVariable(processInstanceId,
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES);
        if (existingValue instanceof Map<?, ?> existingMap) {
            existingMap.forEach((key, value) -> {
                if (key == null) {
                    return;
                }
                List<Long> userIds = toLongList(value);
                if (CollUtil.isNotEmpty(userIds)) {
                    startUserSelectAssignees.put(String.valueOf(key), userIds);
                }
            });
        }
        nextAssignees.forEach((taskDefinitionKey, userIds) -> {
            List<Long> assigneeIds = userIds == null ? List.of() : userIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (StrUtil.isNotBlank(taskDefinitionKey) && CollUtil.isNotEmpty(assigneeIds)) {
                startUserSelectAssignees.put(taskDefinitionKey, assigneeIds);
            }
        });
        runtimeService.setVariable(processInstanceId,
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                startUserSelectAssignees);
    }

    private List<Long> toLongList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Object item : iterable) {
            Long userId = toLong(item);
            if (userId != null && !result.contains(userId)) {
                result.add(userId);
            }
        }
        return result;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StrUtil.isNotBlank(text)) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Map<String, List<Long>> singleNextAssignees(String taskDefinitionKey, Long userId) {
        if (userId == null) {
            return null;
        }
        return singleNextAssignees(taskDefinitionKey, List.of(userId));
    }

    private Map<String, List<Long>> singleNextAssignees(String taskDefinitionKey, List<Long> userIds) {
        if (StrUtil.isBlank(taskDefinitionKey) || CollUtil.isEmpty(userIds)) {
            return null;
        }
        List<Long> assigneeIds = userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(assigneeIds)) {
            return null;
        }
        Map<String, List<Long>> nextAssignees = new HashMap<>();
        nextAssignees.put(taskDefinitionKey, assigneeIds);
        return nextAssignees;
    }

    private Long parseTaskAssignee(Task task) {
        if (task == null || StrUtil.isBlank(task.getAssignee())) {
            return null;
        }
        try {
            return Long.valueOf(task.getAssignee());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean statusIn(String status, String... statuses) {
        if (StrUtil.isBlank(status) || statuses == null) {
            return false;
        }
        for (String item : statuses) {
            if (status.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNonCancelledNcr(QmsNcRecordDO record) {
        return record != null && record.getId() != null && !STATUS_CANCELLED.equals(record.getStatus());
    }

    private QmsNcRecordDO validateNcExists(Long id) {
        QmsNcRecordDO entity = qmsNcRecordMapper.selectById(id);
        if (entity == null) {
            throw exception(QMS_NCR_NOT_EXISTS);
        }
        return entity;
    }

    private QmsNcRecordDO validateNcCanEdit(Long id) {
        QmsNcRecordDO entity = validateNcExists(id);
        if (STATUS_CLOSED.equals(entity.getStatus()) || STATUS_CANCELLED.equals(entity.getStatus())) {
            throw exception(QMS_NCR_CLOSED);
        }
        return entity;
    }

    private QmsNcRecordDO validateNcCanFlow(Long id) {
        return validateNcCanEdit(id);
    }

    private void applyNode(QmsNcRecordDO entity, String status) {
        if (STATUS_DRAFT.equals(status)) {
            entity.setCurrentNodeCode(NODE_DRAFT);
            entity.setCurrentNodeName("草稿");
        } else if (STATUS_CONTENT_CONFIRM.equals(status)) {
            entity.setCurrentNodeCode(NODE_CONTENT_CONFIRM);
            entity.setCurrentNodeName("再次确认内容");
        } else if (STATUS_SUBMITTED.equals(status)) {
            entity.setCurrentNodeCode(NODE_SUBMITTED);
            entity.setCurrentNodeName("品质确认/分派");
        } else if (STATUS_MRB_REVIEW.equals(status)) {
            entity.setCurrentNodeCode(NODE_MRB_REVIEW);
            entity.setCurrentNodeName("责任单位会签");
        } else if (STATUS_REVIEW_ASSIGN.equals(status)) {
            entity.setCurrentNodeCode(NODE_REVIEW_ASSIGN);
            entity.setCurrentNodeName("会签完成/终审分派");
        } else if (STATUS_FINAL_APPROVAL.equals(status)) {
            entity.setCurrentNodeCode(NODE_FINAL_APPROVAL);
            entity.setCurrentNodeName("终审审批");
        } else if (STATUS_EXECUTION_ASSIGN.equals(status)) {
            entity.setCurrentNodeCode(NODE_EXECUTION_ASSIGN);
            entity.setCurrentNodeName("终审完成/处置分派");
        } else if (STATUS_PENDING_STOCK_DISPOSE.equals(status)) {
            entity.setCurrentNodeCode(NODE_STOCK_DISPOSE);
            entity.setCurrentNodeName("不良品处置执行");
        } else if (STATUS_CLOSE_CONFIRM.equals(status)) {
            entity.setCurrentNodeCode(NODE_CLOSE_CONFIRM);
            entity.setCurrentNodeName("处置完成/复核关闭");
        } else if (STATUS_CLOSED.equals(status)) {
            entity.setCurrentNodeCode(NODE_CLOSED);
            entity.setCurrentNodeName("流程结束(归档)");
        } else if (STATUS_CANCELLED.equals(status)) {
            entity.setCurrentNodeCode(NODE_CANCELLED);
            entity.setCurrentNodeName("已取消");
        } else if (STATUS_RETURNED.equals(status)) {
            entity.setCurrentNodeCode(NODE_RETURNED);
            entity.setCurrentNodeName("退回补充");
        }
    }

    private void applyNextNode(QmsNcRecordDO before, QmsNcRecordDO updateObj,
                               QmsNcRecordHandleReqVO handleReqVO) {
        if (STATUS_CONTENT_CONFIRM.equals(before.getStatus())) {
            updateObj.setStatus(STATUS_SUBMITTED);
            updateObj.setCurrentNodeCode(NODE_SUBMITTED);
            updateObj.setCurrentNodeName("品质部确认/分派");
        } else if (STATUS_SUBMITTED.equals(before.getStatus()) || STATUS_RETURNED.equals(before.getStatus())) {
            updateObj.setStatus(STATUS_MRB_REVIEW);
            updateObj.setCurrentNodeCode(NODE_MRB_REVIEW);
            updateObj.setCurrentNodeName("责任单位处置会签");
        } else if (STATUS_MRB_REVIEW.equals(before.getStatus())) {
            boolean allHandled = qmsNcMrbReviewMapper.selectListByNcRecordId(before.getId()).stream()
                    .allMatch(review -> "HANDLED".equals(review.getReviewStatus()) || "SKIPPED".equals(review.getReviewStatus()));
            updateObj.setStatus(allHandled ? STATUS_REVIEW_ASSIGN : STATUS_MRB_REVIEW);
            updateObj.setCurrentNodeCode(allHandled ? NODE_REVIEW_ASSIGN : NODE_MRB_REVIEW);
            updateObj.setCurrentNodeName(allHandled
                    ? "品质部转办" : "责任单位处置会签");
        } else if (STATUS_REVIEW_ASSIGN.equals(before.getStatus())) {
            validateQualityTransferRoute(before, handleReqVO.getTransferRoute());
            if (RAW_TRANSFER_ROUTE_FINAL.equals(handleReqVO.getTransferRoute())) {
                updateObj.setStatus(STATUS_FINAL_APPROVAL);
                updateObj.setCurrentNodeCode(NODE_FINAL_APPROVAL);
                updateObj.setCurrentNodeName("终审人办理");
            } else if (RAW_TRANSFER_ROUTE_CLOSE.equals(handleReqVO.getTransferRoute())) {
                applyDirectCloseNode(updateObj);
            } else {
                if (isRawMaterialNcr(before)) {
                    updateObj.setStatus(STATUS_PENDING_STOCK_DISPOSE);
                    updateObj.setCurrentNodeCode(NODE_STOCK_DISPOSE);
                    updateObj.setCurrentNodeName("不合格处置结果上传");
                } else {
                    updateObj.setStatus(STATUS_EXECUTION_ASSIGN);
                    updateObj.setCurrentNodeCode(NODE_EXECUTION_ASSIGN);
                    updateObj.setCurrentNodeName("产品处置范围确认/执行分派");
                }
            }
        } else if (STATUS_EXECUTION_ASSIGN.equals(before.getStatus())) {
            validateQualityTransferRoute(before, handleReqVO.getTransferRoute());
            if (RAW_TRANSFER_ROUTE_CLOSE.equals(handleReqVO.getTransferRoute())) {
                applyDirectCloseNode(updateObj);
            } else {
                updateObj.setStatus(STATUS_PENDING_STOCK_DISPOSE);
                updateObj.setCurrentNodeCode(NODE_STOCK_DISPOSE);
                updateObj.setCurrentNodeName("不合格处置结果上传");
            }
        } else if (STATUS_CLOSE_CONFIRM.equals(before.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            updateObj.setStatus(STATUS_CLOSED);
            updateObj.setCurrentNodeCode(NODE_CLOSED);
            updateObj.setCurrentNodeName("流程结束(归档)");
            updateObj.setCloseTime(now);
            updateObj.setCloseUserId(SecurityFrameworkUtils.getLoginUserId());
            updateObj.setCloseUserName(currentUserName());
        } else {
            updateObj.setStatus(STATUS_MRB_REVIEW);
            updateObj.setCurrentNodeCode(NODE_MRB_REVIEW);
            updateObj.setCurrentNodeName("责任单位会签");
        }
    }

    private List<QmsNcMrbReviewReqVO> buildRelaunchReviews(Long sourceNcRecordId) {
        List<QmsNcMrbReviewDO> reviews = qmsNcMrbReviewMapper.selectListByNcRecordId(sourceNcRecordId);
        if (CollUtil.isEmpty(reviews)) {
            return List.of();
        }
        return reviews.stream().map(review -> {
            QmsNcMrbReviewReqVO reqVO = new QmsNcMrbReviewReqVO();
            reqVO.setDeptId(review.getDeptId());
            reqVO.setDeptName(review.getDeptName());
            reqVO.setHandlerUserId(review.getHandlerUserId());
            reqVO.setHandlerUserName(review.getHandlerUserName());
            reqVO.setSort(review.getSort());
            return reqVO;
        }).collect(Collectors.toList());
    }

    private List<QmsNcRelationReqVO> buildRelaunchRelations(Long sourceNcRecordId) {
        List<QmsNcRelationDO> relations = qmsNcRelationMapper.selectListByNcRecordId(sourceNcRecordId);
        if (CollUtil.isEmpty(relations)) {
            return List.of();
        }
        return relations.stream()
                .filter(relation -> !ATTACHMENT_RELATION_TYPE.equals(relation.getRelationType()))
                .map(relation -> {
                    QmsNcRelationReqVO reqVO = new QmsNcRelationReqVO();
                    reqVO.setRelationType(relation.getRelationType());
                    reqVO.setRelatedObjectId(relation.getRelatedObjectId());
                    reqVO.setRelatedObjectNo(relation.getRelatedObjectNo());
                    reqVO.setRelatedObjectName(relation.getRelatedObjectName());
                    reqVO.setRelationStatus(relation.getRelationStatus());
                    reqVO.setPrimaryFlag(relation.getPrimaryFlag());
                    reqVO.setRemark(relation.getRemark());
                    return reqVO;
                }).collect(Collectors.toList());
    }

    private List<QmsNcDefectReqVO> buildRelaunchDefects(Long sourceNcRecordId) {
        List<QmsNcDefectDO> defects = qmsNcDefectMapper.selectListByNcRecordId(sourceNcRecordId);
        if (CollUtil.isEmpty(defects)) {
            return List.of();
        }
        return defects.stream().map(defect -> {
            QmsNcDefectReqVO reqVO = new QmsNcDefectReqVO();
            reqVO.setDefectCodeId(defect.getDefectCodeId());
            reqVO.setDefectCode(defect.getDefectCode());
            reqVO.setDefectName(defect.getDefectName());
            reqVO.setDefectPath(defect.getDefectPath());
            reqVO.setSourceSectionName(defect.getSourceSectionName());
            reqVO.setSourceInspectionItem(defect.getSourceInspectionItem());
            reqVO.setSourceResult(defect.getSourceResult());
            reqVO.setPrimaryFlag(defect.getPrimaryFlag());
            reqVO.setSort(defect.getSort());
            return reqVO;
        }).collect(Collectors.toList());
    }

    private void saveReviews(QmsNcRecordDO ncRecord, List<QmsNcMrbReviewReqVO> reviews, boolean markHandled) {
        if (reviews == null) {
            return;
        }
        List<QmsNcMrbReviewDO> existingReviews = qmsNcMrbReviewMapper.selectListByNcRecordId(ncRecord.getId());
        qmsNcMrbReviewMapper.delete(QmsNcMrbReviewDO::getNcRecordId, ncRecord.getId());
        if (CollUtil.isEmpty(reviews)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = currentUserName();
        List<QmsNcMrbReviewDO> reviewDOs = reviews.stream()
                .filter(review -> review.getDeptId() != null || StrUtil.isNotBlank(review.getDeptName()))
                .map(review -> {
                    QmsNcMrbReviewDO existingReview = findExistingReview(existingReviews, review);
                    boolean handledByCurrentUser = shouldMarkReviewHandled(review, existingReview, markHandled, loginUserId);
                    boolean keepExistingRouting = markHandled && existingReview != null;
                    Long handlerUserId = keepExistingRouting ? existingReview.getHandlerUserId() : review.getHandlerUserId();
                    String handlerUserName = keepExistingRouting ? existingReview.getHandlerUserName() : review.getHandlerUserName();
                    String dispositionDetail = handledByCurrentUser
                            ? firstNotBlank(review.getDispositionDetail(), review.getReviewOpinion(),
                            review.getCauseAnalysis())
                            : (existingReview == null ? null : existingReview.getDispositionDetail());
                    String suggestedDisposition = handledByCurrentUser ? review.getSuggestedDisposition()
                            : (existingReview == null ? null : existingReview.getSuggestedDisposition());
                    String rootCauseCategory = handledByCurrentUser ? review.getRootCauseCategory()
                            : (existingReview == null ? null : existingReview.getRootCauseCategory());
                    String causeAnalysis = handledByCurrentUser ? review.getCauseAnalysis()
                            : (existingReview == null ? null : existingReview.getCauseAnalysis());
                    String reviewOpinion = handledByCurrentUser ? firstNotBlank(review.getReviewOpinion(), dispositionDetail)
                            : (existingReview == null ? null : existingReview.getReviewOpinion());
                    return QmsNcMrbReviewDO.builder()
                            .ncRecordId(ncRecord.getId())
                            .ncNo(ncRecord.getNcNo())
                            .deptId(review.getDeptId())
                            .deptName(firstNotBlank(review.getDeptName(), "未指定部门"))
                            .handlerUserId(handlerUserId)
                            .handlerUserName(handlerUserName)
                            .delegateUserId(existingReview == null ? null : existingReview.getDelegateUserId())
                            .delegateUserName(existingReview == null ? null : existingReview.getDelegateUserName())
                            .delegateTime(existingReview == null ? null : existingReview.getDelegateTime())
                            .actualHandlerUserId(handledByCurrentUser ? loginUserId
                                    : existingReview == null ? null : existingReview.getActualHandlerUserId())
                            .actualHandlerUserName(handledByCurrentUser ? loginUserName
                                    : existingReview == null ? null : existingReview.getActualHandlerUserName())
                            .suggestedDisposition(suggestedDisposition)
                            .dispositionDetail(dispositionDetail)
                            .rootCauseCategory(rootCauseCategory)
                            .causeAnalysis(causeAnalysis)
                            .reviewOpinion(reviewOpinion)
                            .reviewStatus(handledByCurrentUser ? "HANDLED"
                                    : (existingReview == null ? "PENDING"
                                    : StrUtil.blankToDefault(existingReview.getReviewStatus(), "PENDING")))
                            .handleTime(handledByCurrentUser ? now
                                    : (existingReview == null ? null : existingReview.getHandleTime()))
                            .sort(review.getSort())
                            .build();
                })
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(reviewDOs)) {
            qmsNcMrbReviewMapper.insertBatch(reviewDOs);
        }
    }

    private void saveDefects(QmsNcRecordDO ncRecord, List<QmsNcDefectReqVO> defects) {
        if (defects == null) {
            return;
        }
        qmsNcDefectMapper.delete(QmsNcDefectDO::getNcRecordId, ncRecord.getId());
        if (CollUtil.isEmpty(defects)) {
            return;
        }
        boolean hasPrimary = defects.stream().anyMatch(defect -> Boolean.TRUE.equals(defect.getPrimaryFlag()));
        List<QmsNcDefectDO> defectDOs = new ArrayList<>();
        int sort = 1;
        for (QmsNcDefectReqVO defect : defects) {
            if (StrUtil.isBlank(defect.getDefectCode()) && StrUtil.isBlank(defect.getDefectName())) {
                continue;
            }
            boolean primary = Boolean.TRUE.equals(defect.getPrimaryFlag()) || (!hasPrimary && defectDOs.isEmpty());
            defectDOs.add(QmsNcDefectDO.builder()
                    .ncRecordId(ncRecord.getId())
                    .ncNo(ncRecord.getNcNo())
                    .defectCodeId(defect.getDefectCodeId())
                    .defectCode(StrUtil.trim(defect.getDefectCode()))
                    .defectName(StrUtil.trim(defect.getDefectName()))
                    .defectPath(StrUtil.trim(defect.getDefectPath()))
                    .sourceSectionName(StrUtil.trim(defect.getSourceSectionName()))
                    .sourceInspectionItem(StrUtil.trim(defect.getSourceInspectionItem()))
                    .sourceResult(StrUtil.trim(defect.getSourceResult()))
                    .primaryFlag(primary)
                    .sort(defect.getSort() == null ? sort : defect.getSort())
                    .build());
            sort++;
        }
        if (CollUtil.isNotEmpty(defectDOs)) {
            qmsNcDefectMapper.insertBatch(defectDOs);
        }
    }

    private boolean isQualityTransferStage(QmsNcRecordDO record) {
        return record != null && statusIn(record.getStatus(), STATUS_REVIEW_ASSIGN, STATUS_EXECUTION_ASSIGN);
    }

    private boolean usesIndependentTransferWorkflow(QmsNcRecordDO record) {
        return record != null;
    }

    private void applyQualityTransferDecision(QmsNcRecordDO before, QmsNcRecordDO updateObj,
                                              QmsNcRecordHandleReqVO handleReqVO) {
        if (!isQualityTransferStage(before)) {
            return;
        }
        boolean sendToFinalApprover = RAW_TRANSFER_ROUTE_FINAL.equals(handleReqVO.getTransferRoute());
        String disposition = firstNotBlank(handleReqVO.getFinalDisposition(), before.getFinalDisposition());
        String finalOpinion = firstNotBlank(handleReqVO.getFinalOpinion(), before.getFinalOpinion());
        String measure = firstNotBlank(handleReqVO.getFinalDisposeDescription(), before.getFinalDisposeDescription());
        boolean transferDecisionRequired = before.getFinalApproveTime() == null && !sendToFinalApprover;
        if (transferDecisionRequired
                && (StrUtil.isBlank(disposition) || StrUtil.isBlank(finalOpinion))) {
            throw exception(QMS_RAW_MATERIAL_NCR_TRANSFER_DECISION_REQUIRED);
        }
        if (StrUtil.isNotBlank(measure)) {
            updateObj.setFinalDisposeDescription(StrUtil.trim(measure));
        }
        if (!sendToFinalApprover && StrUtil.isNotBlank(disposition)) {
            updateObj.setFinalDisposition(StrUtil.trim(disposition));
            updateObj.setMrbDecision(StrUtil.trim(disposition));
        }
        if (!sendToFinalApprover && StrUtil.isNotBlank(finalOpinion)) {
            updateObj.setFinalOpinion(StrUtil.trim(finalOpinion));
        }
        if (handleReqVO.getCreateExceptionFlag() != null) {
            updateObj.setCreateExceptionFlag(Boolean.TRUE.equals(handleReqVO.getCreateExceptionFlag()));
        }
    }

    private void applyNextHandler(QmsNcRecordDO before, QmsNcRecordDO updateObj,
                                  QmsNcRecordHandleReqVO handleReqVO) {
        if (STATUS_REVIEW_ASSIGN.equals(before.getStatus())) {
            if (RAW_TRANSFER_ROUTE_CLOSE.equals(handleReqVO.getTransferRoute())) {
                updateObj.setCurrentHandlerUserId(null);
                updateObj.setCurrentHandlerUserName(null);
            } else if (RAW_TRANSFER_ROUTE_EXECUTION.equals(handleReqVO.getTransferRoute())
                    && isRawMaterialNcr(before)) {
                requireNextHandler(handleReqVO);
                setExecutionHandler(updateObj, handleReqVO);
            } else if (RAW_TRANSFER_ROUTE_EXECUTION.equals(handleReqVO.getTransferRoute())) {
                List<Long> handlerUserIds = requireNextHandlerUserIds(handleReqVO);
                List<String> handlerUserNames = resolveNextHandlerUserNames(handleReqVO, handlerUserIds);
                setCurrentHandlerFromFirst(updateObj, handlerUserIds, handlerUserNames);
            } else {
                requireNextHandler(handleReqVO);
                String finalApproverName = validateAndResolveFinalApproverName(handleReqVO.getNextHandlerUserId());
                updateObj.setCurrentHandlerUserId(handleReqVO.getNextHandlerUserId());
                updateObj.setCurrentHandlerUserName(firstNotBlank(finalApproverName,
                        handleReqVO.getNextHandlerUserName(), "指定终审人"));
            }
            return;
        }
        if (STATUS_EXECUTION_ASSIGN.equals(before.getStatus())) {
            if (RAW_TRANSFER_ROUTE_CLOSE.equals(handleReqVO.getTransferRoute())) {
                updateObj.setCurrentHandlerUserId(null);
                updateObj.setCurrentHandlerUserName(null);
            } else {
                requireNextHandler(handleReqVO);
                setExecutionHandler(updateObj, handleReqVO);
            }
            return;
        }
        if (STATUS_SUBMITTED.equals(before.getStatus()) || STATUS_RETURNED.equals(before.getStatus())) {
            updateObj.setCurrentHandlerUserId(SecurityFrameworkUtils.getLoginUserId());
            updateObj.setCurrentHandlerUserName(currentUserName());
            return;
        }
        if (STATUS_MRB_REVIEW.equals(before.getStatus()) && STATUS_REVIEW_ASSIGN.equals(updateObj.getStatus())) {
            updateObj.setCurrentHandlerUserId(resolveAssignmentHandlerUserId(before));
            updateObj.setCurrentHandlerUserName(resolveAssignmentHandlerUserName(before));
            return;
        }
        if (STATUS_MRB_REVIEW.equals(before.getStatus())) {
            return;
        }
        if (STATUS_CLOSE_CONFIRM.equals(before.getStatus())) {
            updateObj.setCurrentHandlerUserId(before.getCurrentHandlerUserId());
            updateObj.setCurrentHandlerUserName(before.getCurrentHandlerUserName());
            return;
        }
        updateObj.setCurrentHandlerUserId(handleReqVO.getNextHandlerUserId() != null
                ? handleReqVO.getNextHandlerUserId() : before.getCurrentHandlerUserId());
        updateObj.setCurrentHandlerUserName(firstNotBlank(handleReqVO.getNextHandlerUserName(),
                before.getCurrentHandlerUserName(), currentUserName()));
    }

    private void requireNextHandler(QmsNcRecordHandleReqVO handleReqVO) {
        if (handleReqVO.getNextHandlerUserId() == null) {
            throw exception(QMS_NCR_NEXT_HANDLER_REQUIRED);
        }
    }

    private List<Long> requireNextHandlerUserIds(QmsNcRecordHandleReqVO handleReqVO) {
        List<Long> userIds = resolveNextHandlerUserIds(handleReqVO);
        if (CollUtil.isEmpty(userIds)) {
            throw exception(QMS_NCR_NEXT_HANDLER_REQUIRED);
        }
        return userIds;
    }

    private List<Long> resolveNextHandlerUserIds(QmsNcRecordHandleReqVO handleReqVO) {
        List<Long> userIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(handleReqVO.getNextHandlerUserIds())) {
            userIds.addAll(handleReqVO.getNextHandlerUserIds());
        }
        if (handleReqVO.getNextHandlerUserId() != null) {
            userIds.add(handleReqVO.getNextHandlerUserId());
        }
        return userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private List<String> resolveNextHandlerUserNames(QmsNcRecordHandleReqVO handleReqVO, List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        Map<Long, String> requestNameMap = new HashMap<>();
        List<Long> reqUserIds = handleReqVO.getNextHandlerUserIds();
        List<String> reqUserNames = handleReqVO.getNextHandlerUserNames();
        if (CollUtil.isNotEmpty(reqUserIds) && CollUtil.isNotEmpty(reqUserNames)) {
            for (int i = 0; i < reqUserIds.size() && i < reqUserNames.size(); i++) {
                Long userId = reqUserIds.get(i);
                String userName = StrUtil.trim(reqUserNames.get(i));
                if (userId != null && StrUtil.isNotBlank(userName)) {
                    requestNameMap.putIfAbsent(userId, userName);
                }
            }
        }
        if (handleReqVO.getNextHandlerUserId() != null && StrUtil.isNotBlank(handleReqVO.getNextHandlerUserName())) {
            requestNameMap.putIfAbsent(handleReqVO.getNextHandlerUserId(), StrUtil.trim(handleReqVO.getNextHandlerUserName()));
        }
        return userIds.stream()
                .map(userId -> firstNotBlank(requestNameMap.get(userId), resolveUserName(userId), String.valueOf(userId)))
                .collect(Collectors.toList());
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user == null ? null : user.getNickname();
    }

    private void setCurrentHandlerFromFirst(QmsNcRecordDO updateObj, List<Long> userIds, List<String> userNames) {
        Long firstUserId = CollUtil.isEmpty(userIds) ? null : userIds.get(0);
        String firstUserName = CollUtil.isEmpty(userNames) ? null : userNames.get(0);
        updateObj.setCurrentHandlerUserId(firstUserId);
        updateObj.setCurrentHandlerUserName(firstNotBlank(firstUserName, firstUserId == null ? null : String.valueOf(firstUserId)));
    }

    private void applyDisposeAssignment(QmsNcRecordDO before, QmsNcRecordDO updateObj,
                                        QmsNcRecordHandleReqVO handleReqVO) {
        if (!STATUS_EXECUTION_ASSIGN.equals(before.getStatus()) || isRawMaterialNcr(before)
                || RAW_TRANSFER_ROUTE_CLOSE.equals(handleReqVO.getTransferRoute())) {
            return;
        }
        BigDecimal disposeQty = handleReqVO.getStockDisposeQty();
        if (disposeQty == null || disposeQty.compareTo(BigDecimal.ZERO) <= 0
                || (before.getDefectQty() != null && disposeQty.compareTo(before.getDefectQty()) > 0)) {
            throw exception(QMS_NCR_DISPOSE_QTY_INVALID);
        }
        if (StrUtil.isBlank(handleReqVO.getFinalDisposeDescription())) {
            throw exception(QMS_NCR_DISPOSE_ASSIGN_REQUIRED);
        }
        updateObj.setStockDisposeQty(disposeQty);
        updateObj.setFinalDisposeDescription(StrUtil.trim(handleReqVO.getFinalDisposeDescription()));
    }

    private void syncDispositionNotifies(QmsNcRecordDO before, QmsNcRecordDO after,
                                         QmsNcRecordHandleReqVO handleReqVO) {
        if (!isQualityTransferStage(before)) {
            return;
        }
        if (!RAW_TRANSFER_ROUTE_EXECUTION.equals(handleReqVO.getTransferRoute())) {
            qmsNcDispositionNotifyMapper.cancelPendingByNcRecordId(before.getId(), "品质部转办方式变更，通知待办取消");
            return;
        }
        qmsNcDispositionNotifyMapper.cancelPendingByNcRecordId(before.getId(), "品质部重新分派处置通知人");
        List<Long> notifyUserIds = resolveDispositionNotifyUserIds(handleReqVO);
        if (CollUtil.isEmpty(notifyUserIds)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Long tenantId = firstNonNull(after.getTenantId(), before.getTenantId(), TenantContextHolder.getRequiredTenantId());
        QmsNcDispositionExecutionDO execution = qmsNcDispositionExecutionMapper.selectByNcRecordId(before.getId());
        Map<Long, String> reqUserNameMap = buildDispositionNotifyUserNameMap(handleReqVO);
        List<QmsNcDispositionNotifyDO> notifies = notifyUserIds.stream()
                .map(userId -> buildDispositionNotify(before, after, execution, reqUserNameMap, userId, now, tenantId))
                .collect(Collectors.toList());
        qmsNcDispositionNotifyMapper.insertBatch(notifies);
    }

    private QmsNcDispositionNotifyDO buildDispositionNotify(QmsNcRecordDO before, QmsNcRecordDO after,
                                                            QmsNcDispositionExecutionDO execution,
                                                            Map<Long, String> reqUserNameMap,
                                                            Long userId, LocalDateTime now, Long tenantId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        String userName = firstNotBlank(reqUserNameMap.get(userId),
                user == null ? null : user.getNickname(), String.valueOf(userId));
        return QmsNcDispositionNotifyDO.builder()
                .ncRecordId(before.getId())
                .ncNo(before.getNcNo())
                .executionId(execution == null ? null : execution.getId())
                .executionNo(execution == null ? null : execution.getExecutionNo())
                .sourceNodeCode(before.getCurrentNodeCode())
                .sourceNodeName(before.getCurrentNodeName())
                .dispositionType(firstNotBlank(after.getFinalDisposition(), before.getFinalDisposition()))
                .notifyUserId(userId)
                .notifyUserName(userName)
                .notifyStatus(DISPOSITION_NOTIFY_STATUS_PENDING)
                .notifyTime(now)
                .tenantId(tenantId)
                .build();
    }

    private List<Long> resolveDispositionNotifyUserIds(QmsNcRecordHandleReqVO handleReqVO) {
        if (CollUtil.isEmpty(handleReqVO.getDispositionNotifyUserIds())) {
            return List.of();
        }
        Long executionUserId = handleReqVO.getNextHandlerUserId();
        return handleReqVO.getDispositionNotifyUserIds().stream()
                .filter(Objects::nonNull)
                .filter(userId -> !Objects.equals(userId, executionUserId))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }

    private Map<Long, String> buildDispositionNotifyUserNameMap(QmsNcRecordHandleReqVO handleReqVO) {
        Map<Long, String> result = new HashMap<>();
        List<Long> userIds = handleReqVO.getDispositionNotifyUserIds();
        List<String> userNames = handleReqVO.getDispositionNotifyUserNames();
        if (CollUtil.isEmpty(userIds) || CollUtil.isEmpty(userNames)) {
            return result;
        }
        for (int i = 0; i < userIds.size() && i < userNames.size(); i++) {
            Long userId = userIds.get(i);
            String userName = StrUtil.trim(userNames.get(i));
            if (userId != null && StrUtil.isNotBlank(userName)) {
                result.putIfAbsent(userId, userName);
            }
        }
        return result;
    }

    private void syncQualityTransferBpmTransition(QmsNcRecordDO before, QmsNcRecordDO after,
                                                  QmsNcRecordHandleReqVO handleReqVO, String reason) {
        if (before == null || after == null || StrUtil.isBlank(after.getProcessInstanceId())) {
            return;
        }
        if (STATUS_SUBMITTED.equals(before.getStatus()) || STATUS_RETURNED.equals(before.getStatus())) {
            approveCurrentUserBpmTask(after, BPM_NODE_QUALITY_CONFIRM, reason,
                    singleNextAssignees(BPM_NODE_MRB_REVIEW, resolveReviewHandlerUserIds(after.getId())), null);
            return;
        }
        if (STATUS_REVIEW_ASSIGN.equals(before.getStatus())) {
            Map<String, Object> routeVariables = Map.of(
                    resolveTransferRouteVariable(before), handleReqVO.getTransferRoute());
            String nextNode = RAW_TRANSFER_ROUTE_FINAL.equals(handleReqVO.getTransferRoute())
                    ? BPM_NODE_FINAL_DISPOSE
                    : RAW_TRANSFER_ROUTE_EXECUTION.equals(handleReqVO.getTransferRoute())
                    ? (isRawMaterialNcr(before) ? BPM_NODE_EXECUTION_UPLOAD : BPM_NODE_EXECUTION_ASSIGN) : null;
            List<Long> nextUserIds = nextNode == null ? List.of()
                    : (!isRawMaterialNcr(before) && RAW_TRANSFER_ROUTE_EXECUTION.equals(handleReqVO.getTransferRoute())
                    ? resolveNextHandlerUserIds(handleReqVO)
                    : (after.getCurrentHandlerUserId() == null ? List.of() : List.of(after.getCurrentHandlerUserId())));
            approveCurrentUserBpmTask(after, BPM_NODE_REVIEW_ASSIGN, reason,
                    nextNode == null ? null : singleNextAssignees(nextNode, nextUserIds),
                    routeVariables);
            finishBpmAfterDirectClose(after, handleReqVO.getTransferRoute(), reason);
            return;
        }
        if (STATUS_EXECUTION_ASSIGN.equals(before.getStatus())) {
            Map<String, Object> routeVariables = Map.of(
                    resolveTransferRouteVariable(before), handleReqVO.getTransferRoute());
            Map<String, List<Long>> nextAssignees = RAW_TRANSFER_ROUTE_EXECUTION.equals(
                    handleReqVO.getTransferRoute())
                    ? singleNextAssignees(BPM_NODE_EXECUTION_UPLOAD, after.getCurrentHandlerUserId()) : null;
            boolean approved = approveCurrentUserBpmTask(after, BPM_NODE_EXECUTION_ASSIGN, reason,
                    nextAssignees, routeVariables);
            if (!approved) {
                // 未经过终审的执行结果被复检退回时，流程回到初始品质转办节点。
                approveCurrentUserBpmTask(after, BPM_NODE_REVIEW_ASSIGN, reason,
                        nextAssignees, routeVariables);
            }
            finishBpmAfterDirectClose(after, handleReqVO.getTransferRoute(), reason);
            return;
        }
        if (STATUS_CLOSE_CONFIRM.equals(before.getStatus())) {
            approveCurrentUserBpmTask(after, BPM_NODE_REVIEW_CLOSE, reason, null, null);
        }
    }

    private void applyDirectCloseNode(QmsNcRecordDO updateObj) {
        LocalDateTime now = LocalDateTime.now();
        updateObj.setStatus(STATUS_CLOSED);
        updateObj.setCurrentNodeCode(NODE_CLOSED);
        updateObj.setCurrentNodeName("流程结束(直接关闭)");
        updateObj.setCloseTime(now);
        updateObj.setCloseUserId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setCloseUserName(currentUserName());
    }

    private void finishBpmAfterDirectClose(QmsNcRecordDO record, String transferRoute,
                                           String reason) {
        if (!RAW_TRANSFER_ROUTE_CLOSE.equals(transferRoute)
                || record == null || StrUtil.isBlank(record.getProcessInstanceId())) {
            return;
        }
        long runningCount = runtimeService.createProcessInstanceQuery()
                .processInstanceId(record.getProcessInstanceId()).count();
        if (runningCount > 0) {
            // 兼容本次模型发布前已发起的历史实例：旧定义没有直接关闭分支时，强制移动到结束事件。
            bpmTaskService.moveTaskToEnd(record.getProcessInstanceId(), reason + "（直接关闭）");
        }
    }

    private String resolveTransferRouteVariable(QmsNcRecordDO record) {
        return isRawMaterialNcr(record) ? RAW_TRANSFER_ROUTE_VARIABLE : PRODUCT_TRANSFER_ROUTE_VARIABLE;
    }

    /**
     * 为本次缺陷修复上线前已启动的 NCR 流程补齐路线变量。
     *
     * <p>只处理运行中的实例且仅在变量缺失时写入空值，不会覆盖已经选择的流程路线。</p>
     */
    private void ensureNcrTransferRouteVariable(QmsNcRecordDO record) {
        if (record == null || StrUtil.isBlank(record.getProcessInstanceId())) {
            return;
        }
        Integer processStatus = bpmProcessInstanceApi.getProcessInstanceStatus(record.getProcessInstanceId());
        if (!Objects.equals(processStatus, BpmProcessInstanceStatusEnum.RUNNING.getStatus())) {
            return;
        }
        String variableName = resolveTransferRouteVariable(record);
        Map<String, Object> processVariables = bpmProcessInstanceApi
                .getProcessInstanceVariables(record.getProcessInstanceId());
        if (processVariables.containsKey(variableName)) {
            return;
        }
        bpmProcessInstanceApi.updateProcessInstanceVariables(record.getProcessInstanceId(),
                Map.of(variableName, (Object) TRANSFER_ROUTE_UNSELECTED));
    }

    private boolean approveCurrentUserBpmTask(QmsNcRecordDO record, String taskDefinitionKey, String reason,
                                              Map<String, List<Long>> nextAssignees,
                                              Map<String, Object> extraVariables) {
        if (record == null || StrUtil.isBlank(record.getProcessInstanceId())) {
            return false;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return false;
        }
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                record.getProcessInstanceId(), true, taskDefinitionKey);
        Task currentTask = tasks.stream()
                .filter(task -> Objects.equals(parseTaskAssignee(task), loginUserId))
                .findFirst()
                .orElse(null);
        if (currentTask == null) {
            return false;
        }
        Map<String, Object> variables = buildNcrBpmVariables(record);
        if (extraVariables != null) {
            variables.putAll(extraVariables);
        }
        BpmTaskApproveReqVO reqVO = new BpmTaskApproveReqVO()
                .setId(currentTask.getId())
                .setReason(reason)
                .setVariables(variables);
        if (nextAssignees != null && !nextAssignees.isEmpty()) {
            reqVO.setNextAssignees(nextAssignees);
        }
        bpmTaskService.approveTask(loginUserId, reqVO);
        return true;
    }

    private void setExecutionHandler(QmsNcRecordDO updateObj, QmsNcRecordHandleReqVO handleReqVO) {
        String handlerName = firstNotBlank(handleReqVO.getNextHandlerUserName(), "指定执行人");
        updateObj.setCurrentHandlerUserId(handleReqVO.getNextHandlerUserId());
        updateObj.setCurrentHandlerUserName(handlerName);
        updateObj.setStockDisposeUserId(handleReqVO.getNextHandlerUserId());
        updateObj.setStockDisposeUserName(handlerName);
    }

    private QmsNcMrbReviewDO findExistingReview(List<QmsNcMrbReviewDO> existingReviews,
                                                QmsNcMrbReviewReqVO review) {
        if (CollUtil.isEmpty(existingReviews)) {
            return null;
        }
        return existingReviews.stream()
                .filter(existing -> sameReviewSlot(existing, review))
                .findFirst()
                .orElse(null);
    }

    private boolean sameReviewSlot(QmsNcMrbReviewDO existing, QmsNcMrbReviewReqVO review) {
        if (existing.getId() != null && existing.getId().equals(review.getId())) {
            return true;
        }
        if (existing.getHandlerUserId() != null || review.getHandlerUserId() != null) {
            return Objects.equals(existing.getHandlerUserId(), review.getHandlerUserId())
                    && (Objects.equals(existing.getDeptId(), review.getDeptId())
                    || StrUtil.equals(existing.getDeptName(), review.getDeptName()));
        }
        if (existing.getDeptId() != null && existing.getDeptId().equals(review.getDeptId())) {
            return true;
        }
        return StrUtil.isNotBlank(existing.getDeptName())
                && existing.getDeptName().equals(review.getDeptName());
    }

    private List<Long> resolveReviewHandlerUserIds(Long ncRecordId) {
        return qmsNcMrbReviewMapper.selectListByNcRecordId(ncRecordId).stream()
                .map(QmsNcMrbReviewDO::getHandlerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean shouldMarkReviewHandled(QmsNcMrbReviewReqVO review, QmsNcMrbReviewDO existingReview,
                                            boolean markHandled, Long loginUserId) {
        if (!markHandled || loginUserId == null || review == null) {
            return false;
        }
        if (Objects.equals(loginUserId, review.getHandlerUserId())) {
            return true;
        }
        return existingReview != null && Objects.equals(loginUserId, existingReview.getDelegateUserId());
    }

    private boolean isPendingMrbReview(QmsNcMrbReviewDO review) {
        return review != null && "PENDING".equals(firstNotBlank(review.getReviewStatus(), "PENDING"));
    }

    private void validateCurrentHandlerForHandle(QmsNcRecordDO before) {
        if (STATUS_MRB_REVIEW.equals(before.getStatus())) {
            return;
        }
        if (STATUS_SUBMITTED.equals(before.getStatus())) {
            validateQualityConfirmRole(before);
            return;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (before.getCurrentHandlerUserId() != null && !Objects.equals(before.getCurrentHandlerUserId(), loginUserId)) {
            if (STATUS_EXECUTION_ASSIGN.equals(before.getStatus())
                    && isCurrentUserRunningBpmTaskAssignee(before.getProcessInstanceId(), BPM_NODE_EXECUTION_ASSIGN,
                    loginUserId)) {
                return;
            }
            throw exception(QMS_NCR_CURRENT_HANDLER_INVALID);
        }
    }

    private boolean isCurrentUserRunningBpmTaskAssignee(String processInstanceId, String taskDefinitionKey,
                                                       Long loginUserId) {
        if (loginUserId == null || StrUtil.isBlank(processInstanceId) || StrUtil.isBlank(taskDefinitionKey)) {
            return false;
        }
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                processInstanceId, true, taskDefinitionKey);
        if (CollUtil.isEmpty(tasks)) {
            return false;
        }
        return tasks.stream().anyMatch(task -> Objects.equals(parseTaskAssignee(task), loginUserId));
    }

    private void validateQualityConfirmRole(QmsNcRecordDO ncRecord) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (isRawMaterialNcr(ncRecord)) {
            if (!isRawMaterialQualityConfirmUser(loginUserId)) {
                throw exception(QMS_RAW_MATERIAL_NCR_QUALITY_ROLE_REQUIRED);
            }
            return;
        }
        if (!isProductQualityConfirmUser(loginUserId)) {
            throw exception(QMS_PRODUCT_NCR_QUALITY_ROLE_REQUIRED);
        }
    }

    private boolean isRawMaterialQualityConfirmUser(Long userId) {
        return userId != null && permissionApi.hasAnyRoles(userId, RAW_MATERIAL_QUALITY_CONFIRM_ROLE_CODE);
    }

    private boolean isProductQualityConfirmUser(Long userId) {
        return userId != null && permissionApi.hasAnyRoles(userId, PRODUCT_NCR_QUALITY_CONFIRM_ROLE_CODE);
    }

    private void validateQualityTransferRoute(QmsNcRecordDO before, String transferRoute) {
        if (!RAW_TRANSFER_ROUTE_FINAL.equals(transferRoute)
                && !RAW_TRANSFER_ROUTE_EXECUTION.equals(transferRoute)
                && !RAW_TRANSFER_ROUTE_CLOSE.equals(transferRoute)) {
            throw exception(QMS_RAW_MATERIAL_NCR_TRANSFER_ROUTE_REQUIRED);
        }
        if (RAW_TRANSFER_ROUTE_FINAL.equals(transferRoute)
                && (before.getFinalApproveTime() != null
                || STATUS_EXECUTION_ASSIGN.equals(before.getStatus()))) {
            throw exception(QMS_RAW_MATERIAL_NCR_FINAL_REPEAT_INVALID);
        }
    }

    private String resolveUserDisplayName(Long userId, String fallbackName) {
        if (userId == null) {
            throw exception(QMS_NCR_CONTENT_CONFIRM_USER_REQUIRED);
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            throw exception(QMS_NCR_CONTENT_CONFIRM_USER_REQUIRED);
        }
        return firstNotBlank(user.getNickname(), user.getUsername(), fallbackName, "指定人员");
    }

    private String validateAndResolveFinalApproverName(Long userId) {
        QmsNcReviewConfigRespVO config = qmsNcReviewConfigService.getFinalApproverConfig();
        if (config == null || CollUtil.isEmpty(config.getHandlerUserIds())) {
            throw exception(QMS_NCR_FINAL_APPROVER_INVALID);
        }
        List<Long> handlerUserIds = config.getHandlerUserIds();
        List<String> handlerUserNames = config.getHandlerUserNames();
        for (int i = 0; i < handlerUserIds.size(); i++) {
            if (!Objects.equals(handlerUserIds.get(i), userId)) {
                continue;
            }
            return handlerUserNames != null && handlerUserNames.size() > i ? handlerUserNames.get(i) : null;
        }
        throw exception(QMS_NCR_FINAL_APPROVER_INVALID);
    }

    private List<Long> resolveDefaultFinalApproverUserIds() {
        QmsNcReviewConfigRespVO config = qmsNcReviewConfigService.getFinalApproverConfig();
        if (config == null || CollUtil.isEmpty(config.getHandlerUserIds())) {
            return List.of();
        }
        return config.getHandlerUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long resolveAssignmentHandlerUserId(QmsNcRecordDO before) {
        return firstNonNull(before.getQualityConfirmUserId(), before.getCurrentHandlerUserId(), before.getApplicantUserId());
    }

    private String resolveAssignmentHandlerUserName(QmsNcRecordDO before) {
        return firstNotBlank(before.getQualityConfirmUserName(), before.getCurrentHandlerUserName(), before.getApplicantUserName(),
                currentUserName());
    }

    private void saveRelations(QmsNcRecordDO ncRecord, List<QmsNcRelationReqVO> relations) {
        if (relations == null) {
            return;
        }
        qmsNcRelationMapper.physicalDeleteByNcRecordId(ncRecord.getId(),
                firstNonNull(ncRecord.getTenantId(), TenantContextHolder.getRequiredTenantId()));
        if (CollUtil.isEmpty(relations)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<QmsNcRelationDO> relationDOs = relations.stream()
                .filter(relation -> StrUtil.isNotBlank(relation.getRelationType())
                        && StrUtil.isNotBlank(resolveRelationObjectValue(relation)))
                .map(relation -> QmsNcRelationDO.builder()
                        .ncRecordId(ncRecord.getId())
                        .ncNo(ncRecord.getNcNo())
                        .relationType(relation.getRelationType())
                        .relatedObjectId(relation.getRelatedObjectId())
                        .relatedObjectNo(resolveRelationObjectNo(relation))
                        .relatedObjectName(relation.getRelatedObjectName())
                        .relationStatus(relation.getRelationStatus())
                        .primaryFlag(Boolean.TRUE.equals(relation.getPrimaryFlag()))
                        .relationTime(now)
                        .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                        .relationUserName(currentUserName())
                        .remark(resolveRelationRemark(relation))
                        .build())
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(relationDOs)) {
            qmsNcRelationMapper.insertBatch(relationDOs);
        }
    }

    private void saveDispositionAttachments(QmsNcRecordDO ncRecord, List<QmsNcRelationReqVO> relations) {
        if (CollUtil.isEmpty(relations)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        for (QmsNcRelationReqVO relation : relations) {
            String attachmentUrl = resolveRelationObjectValue(relation);
            String attachmentKey = buildAttachmentObjectKey(attachmentUrl);
            if (!DISPOSITION_ATTACHMENT_RELATION_TYPE.equals(relation.getRelationType())
                    || StrUtil.isBlank(attachmentUrl)
                    || qmsNcRelationMapper.selectAttachment(ncRecord.getId(),
                    DISPOSITION_ATTACHMENT_RELATION_TYPE, attachmentKey, attachmentUrl) != null) {
                continue;
            }
            qmsNcRelationMapper.insert(QmsNcRelationDO.builder()
                    .ncRecordId(ncRecord.getId())
                    .ncNo(ncRecord.getNcNo())
                    .relationType(DISPOSITION_ATTACHMENT_RELATION_TYPE)
                    .relatedObjectNo(attachmentKey)
                    .relatedObjectName(firstNotBlank(relation.getRelatedObjectName(), "处置结果附件"))
                    .relationStatus("ACTIVE")
                    .primaryFlag(false)
                    .relationTime(now)
                    .relationUserId(loginUserId)
                    .relationUserName(currentUserName())
                    .remark(attachmentUrl)
                    .build());
        }
    }

    private String resolveRelationObjectValue(QmsNcRelationReqVO relation) {
        if (relation == null) {
            return null;
        }
        if (isAttachmentRelationType(relation.getRelationType())) {
            return firstNotBlank(relation.getRemark(), relation.getRelatedObjectNo());
        }
        return StrUtil.trim(relation.getRelatedObjectNo());
    }

    private String resolveRelationObjectNo(QmsNcRelationReqVO relation) {
        String objectValue = resolveRelationObjectValue(relation);
        return isAttachmentRelationType(relation.getRelationType())
                ? buildAttachmentObjectKey(objectValue) : objectValue;
    }

    private String resolveRelationRemark(QmsNcRelationReqVO relation) {
        return isAttachmentRelationType(relation.getRelationType())
                ? resolveRelationObjectValue(relation) : relation.getRemark();
    }

    private boolean isAttachmentRelationType(String relationType) {
        return ATTACHMENT_RELATION_TYPE.equals(relationType)
                || DISPOSITION_ATTACHMENT_RELATION_TYPE.equals(relationType);
    }

    private String buildAttachmentObjectKey(String attachmentUrl) {
        return StrUtil.isBlank(attachmentUrl) ? null : DigestUtil.sha256Hex(attachmentUrl);
    }

    private void applyDictSnapshots(QmsNcRecordDO entity) {
        if (StrUtil.isNotBlank(entity.getSourceType())) {
            entity.setSourceTypeName(firstNotBlank(entity.getSourceTypeName(),
                    resolveSourceTypeName(entity.getSourceType())));
        }
        if (StrUtil.isNotBlank(entity.getSourceBizType())) {
            entity.setSourceBizTypeName(firstNotBlank(entity.getSourceBizTypeName(),
                    resolveSourceBizTypeName(entity.getSourceBizType())));
        }
        if (StrUtil.isNotBlank(entity.getNcLevel())) {
            entity.setNcLevelName(firstNotBlank(entity.getNcLevelName(),
                    resolveNcLevelName(entity.getNcLevel())));
        }
        if (StrUtil.isNotBlank(entity.getRawMaterialAbnormalCategory())) {
            entity.setRawMaterialAbnormalCategoryName(firstNotBlank(entity.getRawMaterialAbnormalCategoryName(),
                    resolveRawMaterialCategoryName(entity.getRawMaterialAbnormalCategory())));
        }
    }

    private void applyDraftDefaults(QmsNcRecordDO entity) {
        if (StrUtil.isBlank(entity.getDefectCode())) {
            entity.setDefectCode(DEFECT_CODE_DRAFT_PENDING);
        }
        if (entity.getDefectQty() == null || entity.getDefectQty().compareTo(BigDecimal.ZERO) <= 0) {
            entity.setDefectQty(BigDecimal.ONE);
        }
    }

    private String resolveSourceTypeName(String sourceType) {
        if ("FINISHED_PRODUCT".equals(sourceType)) {
            return "成品";
        }
        if ("SEMI_FINISHED".equals(sourceType)) {
            return "半成品";
        }
        if (NCR_TYPE_CUSTOMER_RETURN.equals(sourceType)) {
            return "客退品";
        }
        if (NCR_TYPE_RAW_MATERIAL.equals(sourceType)) {
            return "原物料";
        }
        return sourceType;
    }

    private String resolveSourceBizTypeName(String sourceBizType) {
        if ("FAI".equals(sourceBizType)) {
            return "首件检验";
        }
        if ("GLUE_BOARD_FAI".equals(sourceBizType)) {
            return "胶板检验";
        }
        if (SOURCE_BIZ_CUT_ROUND_FQC.equals(sourceBizType)) {
            return "裁切成品检验";
        }
        if (SOURCE_BIZ_FG_SHIPPING_FQC.equals(sourceBizType)) {
            return "发货成品检验";
        }
        if (SOURCE_BIZ_OQC.equals(sourceBizType)) {
            return "出货检验(OQC)";
        }
        if ("EXCEPTION".equals(sourceBizType)) {
            return "异常事件";
        }
        if (NCR_TYPE_CUSTOMER_RETURN.equals(sourceBizType)) {
            return "客户退货";
        }
        if (SOURCE_BIZ_IQC.equals(sourceBizType)) {
            return "进料检验(IQC)";
        }
        return sourceBizType;
    }

    private String resolveRawMaterialCategoryName(String value) {
        if (RAW_MATERIAL_CATEGORY_INCOMING.equals(value)) {
            return "来料品异常";
        }
        if (RAW_MATERIAL_CATEGORY_STOCK.equals(value)) {
            return "在库品异常";
        }
        return value;
    }

    private String resolveNcLevelName(String ncLevel) {
        if ("MINOR".equals(ncLevel)) {
            return "轻微";
        }
        if ("MAJOR".equals(ncLevel)) {
            return "一般";
        }
        if ("CRITICAL".equals(ncLevel)) {
            return "严重";
        }
        return ncLevel;
    }

    private QmsNcRecordRespVO buildNcResp(QmsNcRecordDO entity, boolean detail) {
        QmsNcRecordRespVO respVO = BeanUtils.toBean(entity, QmsNcRecordRespVO.class);
        clearDraftPendingDefect(respVO);
        if (!detail) {
            return respVO;
        }
        respVO.setReviews(BeanUtils.toBean(qmsNcMrbReviewMapper.selectListByNcRecordId(entity.getId()),
                QmsNcRecordRespVO.Review.class));
        respVO.setDefects(BeanUtils.toBean(qmsNcDefectMapper.selectListByNcRecordId(entity.getId()),
                QmsNcRecordRespVO.Defect.class));
        List<QmsNcRecordRespVO.Relation> relations = BeanUtils.toBean(
                qmsNcRelationMapper.selectListByNcRecordId(entity.getId()), QmsNcRecordRespVO.Relation.class);
        fillIqcRelationDetails(relations);
        respVO.setRelations(relations);
        respVO.setDispositionNotifies(BeanUtils.toBean(
                qmsNcDispositionNotifyMapper.selectListByNcRecordId(entity.getId()),
                QmsNcRecordRespVO.DispositionNotify.class));
        respVO.setFlowLogs(getFlowLogList(entity.getId()));
        return respVO;
    }

    private void fillIqcRelationDetails(List<QmsNcRecordRespVO.Relation> relations) {
        if (CollUtil.isEmpty(relations)) {
            return;
        }
        List<Long> iqcIds = relations.stream()
                .filter(relation -> "IQC".equals(relation.getRelationType()))
                .map(QmsNcRecordRespVO.Relation::getRelatedObjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(iqcIds)) {
            return;
        }
        Map<Long, QmsIqcOrderDO> iqcMap = qmsIqcOrderMapper.selectBatchIds(iqcIds).stream()
                .collect(Collectors.toMap(QmsIqcOrderDO::getId, order -> order, (first, second) -> first));
        relations.stream()
                .filter(relation -> "IQC".equals(relation.getRelationType()))
                .forEach(relation -> {
                    QmsIqcOrderDO iqc = iqcMap.get(relation.getRelatedObjectId());
                    if (iqc == null) {
                        return;
                    }
                    relation.setMaterialCode(iqc.getMaterialCode());
                    relation.setMaterialName(iqc.getMaterialName());
                    relation.setSpecification(iqc.getSpecification());
                    relation.setBatchNo(iqc.getBatchNo());
                    relation.setArrivalDate(iqc.getArrivalDate());
                });
    }

    private void clearDraftPendingDefect(QmsNcRecordRespVO respVO) {
        if (DEFECT_CODE_DRAFT_PENDING.equals(respVO.getDefectCode())) {
            respVO.setDefectCode(null);
        }
    }

    private void fillCurrentUserListState(QmsNcRecordRespVO respVO, Long loginUserId, Set<Long> participatedIds,
                                          Set<Long> pendingReviewIds, Set<Long> pendingNotifyIds,
                                          boolean rawMaterialQualityConfirmUser,
                                          boolean productQualityConfirmUser) {
        if (respVO == null) {
            return;
        }
        boolean minePending = isCurrentUserPending(respVO, loginUserId, pendingReviewIds,
                pendingNotifyIds, rawMaterialQualityConfirmUser, productQualityConfirmUser);
        boolean mineNotifyPending = isCurrentUserDispositionNotifyPending(respVO, loginUserId, pendingNotifyIds);
        boolean mineDiscovered = loginUserId != null && Objects.equals(respVO.getApplicantUserId(), loginUserId);
        boolean mineParticipated = respVO.getId() != null && participatedIds != null
                && participatedIds.contains(respVO.getId());
        respVO.setMinePending(minePending);
        respVO.setMineDiscovered(mineDiscovered);
        respVO.setMineParticipated(mineParticipated);
        respVO.setCanHandle(minePending);
        respVO.setCanWithdraw(canWithdrawCurrentUser(respVO.getId(), loginUserId));
        respVO.setListActionCode(minePending
                ? (mineNotifyPending ? "DISPOSITION_NOTIFY_REPLY" : resolveListActionCode(respVO.getStatus()))
                : "VIEW");
        respVO.setListActionName(minePending
                ? (mineNotifyPending ? "回复" : resolveListActionName(respVO.getStatus()))
                : "详情");
    }

    private boolean canWithdrawCurrentUser(Long ncRecordId, Long loginUserId) {
        if (ncRecordId == null || loginUserId == null) {
            return false;
        }
        QmsNcRecordDO record = qmsNcRecordMapper.selectById(ncRecordId);
        return findWithdrawableHistoricTask(record, loginUserId) != null;
    }

    private HistoricTaskInstance findWithdrawableHistoricTask(QmsNcRecordDO record, Long loginUserId) {
        if (record == null || loginUserId == null || StrUtil.isBlank(record.getProcessInstanceId())
                || isTerminalNcStatus(record.getStatus())) {
            return null;
        }
        if (!Objects.equals(bpmProcessInstanceApi.getProcessInstanceStatus(record.getProcessInstanceId()),
                BpmProcessInstanceStatusEnum.RUNNING.getStatus())) {
            return null;
        }
        List<Task> runningTasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                record.getProcessInstanceId(), true, null);
        if (CollUtil.isEmpty(runningTasks)) {
            return null;
        }
        List<HistoricTaskInstance> historicTasks = bpmTaskService.getTaskListByProcessInstanceId(
                record.getProcessInstanceId(), false);
        if (CollUtil.isEmpty(historicTasks)) {
            return null;
        }
        List<HistoricTaskInstance> userFinishedTasks = historicTasks.stream()
                .filter(task -> task.getEndTime() != null
                        && Objects.equals(task.getAssignee(), String.valueOf(loginUserId)))
                .sorted(Comparator.comparing(HistoricTaskInstance::getEndTime).reversed())
                .collect(Collectors.toList());
        for (HistoricTaskInstance task : userFinishedTasks) {
            if (hasNonCancelledFinishedTaskAfter(historicTasks, task)) {
                continue;
            }
            boolean nextTaskStillPending = runningTasks.stream()
                    .anyMatch(runningTask -> runningTask.getCreateTime() != null
                            && !runningTask.getCreateTime().before(task.getEndTime()));
            if (nextTaskStillPending) {
                return task;
            }
        }
        return null;
    }

    private boolean hasNonCancelledFinishedTaskAfter(List<HistoricTaskInstance> tasks,
                                                     HistoricTaskInstance baseTask) {
        if (CollUtil.isEmpty(tasks) || baseTask == null || baseTask.getEndTime() == null) {
            return false;
        }
        return tasks.stream()
                .anyMatch(task -> task.getEndTime() != null
                        && task.getEndTime().after(baseTask.getEndTime())
                        && !isHistoricTaskCancelled(task));
    }

    private boolean isHistoricTaskCancelled(HistoricTaskInstance task) {
        Object value = task == null || task.getTaskLocalVariables() == null ? null
                : task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS);
        Integer status = null;
        if (value instanceof Number number) {
            status = number.intValue();
        } else if (value instanceof String text && StrUtil.isNotBlank(text)) {
            try {
                status = Integer.valueOf(text);
            } catch (NumberFormatException ignored) {
                status = null;
            }
        }
        return BpmTaskStatusEnum.isCancelStatus(status);
    }

    private boolean isCurrentUserPending(QmsNcRecordRespVO respVO, Long loginUserId, Set<Long> pendingReviewIds,
                                          Set<Long> pendingNotifyIds,
                                          boolean rawMaterialQualityConfirmUser,
                                          boolean productQualityConfirmUser) {
        if (loginUserId == null || respVO.getId() == null) {
            return false;
        }
        if (isCurrentUserDispositionNotifyPending(respVO, loginUserId, pendingNotifyIds)) {
            return true;
        }
        if (isTerminalNcStatus(respVO.getStatus())) {
            return false;
        }
        if (Objects.equals(respVO.getCurrentHandlerUserId(), loginUserId)) {
            return true;
        }
        if (STATUS_EXECUTION_ASSIGN.equals(respVO.getStatus())
                && isCurrentUserRunningBpmTaskAssignee(respVO.getProcessInstanceId(), BPM_NODE_EXECUTION_ASSIGN,
                loginUserId)) {
            return true;
        }
        if (pendingReviewIds != null && pendingReviewIds.contains(respVO.getId())) {
            return true;
        }
        if (rawMaterialQualityConfirmUser && NCR_TYPE_RAW_MATERIAL.equals(respVO.getSourceType())
                && STATUS_SUBMITTED.equals(respVO.getStatus())) {
            return true;
        }
        if (productQualityConfirmUser && !NCR_TYPE_RAW_MATERIAL.equals(respVO.getSourceType())
                && STATUS_SUBMITTED.equals(respVO.getStatus())) {
            return true;
        }
        return (STATUS_DRAFT.equals(respVO.getStatus()) || STATUS_RETURNED.equals(respVO.getStatus()))
                && respVO.getCurrentHandlerUserId() == null
                && Objects.equals(respVO.getApplicantUserId(), loginUserId);
    }

    private boolean isCurrentUserDispositionNotifyPending(QmsNcRecordRespVO respVO, Long loginUserId,
                                                           Set<Long> pendingNotifyIds) {
        return loginUserId != null && respVO.getId() != null && pendingNotifyIds != null
                && pendingNotifyIds.contains(respVO.getId())
                && !Objects.equals(respVO.getCurrentHandlerUserId(), loginUserId);
    }

    private boolean isTerminalNcStatus(String status) {
        return STATUS_CLOSED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    private String resolveListActionCode(String status) {
        if (STATUS_DRAFT.equals(status) || STATUS_RETURNED.equals(status)) {
            return "SUBMIT";
        }
        if (STATUS_CONTENT_CONFIRM.equals(status)) {
            return "CONTENT_CONFIRM";
        }
        if (STATUS_SUBMITTED.equals(status)) {
            return "QUALITY_CONFIRM";
        }
        if (STATUS_MRB_REVIEW.equals(status)) {
            return "MRB_REVIEW";
        }
        if (STATUS_REVIEW_ASSIGN.equals(status)) {
            return "REVIEW_ASSIGN";
        }
        if (STATUS_FINAL_APPROVAL.equals(status)) {
            return "FINAL_APPROVE";
        }
        if (STATUS_EXECUTION_ASSIGN.equals(status)) {
            return "EXECUTION_ASSIGN";
        }
        if (STATUS_PENDING_STOCK_DISPOSE.equals(status)) {
            return "STOCK_DISPOSE";
        }
        if (STATUS_CLOSE_CONFIRM.equals(status)) {
            return "CLOSE";
        }
        return "HANDLE";
    }

    private String resolveListActionName(String status) {
        if (STATUS_DRAFT.equals(status) || STATUS_RETURNED.equals(status)) {
            return "提交";
        }
        if (STATUS_CONTENT_CONFIRM.equals(status)) {
            return "再次确认";
        }
        if (STATUS_SUBMITTED.equals(status)) {
            return "确认";
        }
        if (STATUS_MRB_REVIEW.equals(status)) {
            return "会签";
        }
        if (STATUS_REVIEW_ASSIGN.equals(status)) {
            return "推送终审";
        }
        if (STATUS_FINAL_APPROVAL.equals(status)) {
            return "终审";
        }
        if (STATUS_EXECUTION_ASSIGN.equals(status)) {
            return "推送处置";
        }
        if (STATUS_PENDING_STOCK_DISPOSE.equals(status)) {
            return "处置";
        }
        if (STATUS_CLOSE_CONFIRM.equals(status)) {
            return "关闭";
        }
        return "办理";
    }

    private Map<String, Object> buildUpdateSnapshot(QmsNcRecordDO before, QmsNcRecordDO after,
                                                    List<QmsNcMrbReviewDO> beforeReviews,
                                                    List<QmsNcMrbReviewDO> afterReviews,
                                                    List<QmsNcRelationDO> beforeRelations,
                                                    List<QmsNcRelationDO> afterRelations) {
        List<Map<String, Object>> changes = new ArrayList<>();
        addChange(changes, "sourceType", "类型",
                firstNotBlank(before.getSourceTypeName(), before.getSourceType()),
                firstNotBlank(after.getSourceTypeName(), after.getSourceType()));
        addChange(changes, "sourceBizType", "来源单据类型",
                firstNotBlank(before.getSourceBizTypeName(), before.getSourceBizType()),
                firstNotBlank(after.getSourceBizTypeName(), after.getSourceBizType()));
        addChange(changes, "sourceNo", "来源单号", before.getSourceNo(), after.getSourceNo());
        addChange(changes, "happenTime", "发生时间", before.getHappenTime(), after.getHappenTime());
        addChange(changes, "processName", "发生工序", before.getProcessName(), after.getProcessName());
        addChange(changes, "materialCode", "物料编码", before.getMaterialCode(), after.getMaterialCode());
        addChange(changes, "materialName", "品名", before.getMaterialName(), after.getMaterialName());
        addChange(changes, "specification", "规格", before.getSpecification(), after.getSpecification());
        addChange(changes, "unitCode", "单位", before.getUnitCode(), after.getUnitCode());
        addChange(changes, "lotNo", "批号", before.getLotNo(), after.getLotNo());
        addChange(changes, "defectCode", "缺陷代码", before.getDefectCode(), after.getDefectCode());
        addChange(changes, "defectName", "缺陷名称", before.getDefectName(), after.getDefectName());
        addChange(changes, "defectQty", "不合格数量", before.getDefectQty(), after.getDefectQty());
        addChange(changes, "ncLevel", "不合格等级",
                firstNotBlank(before.getNcLevelName(), before.getNcLevel()),
                firstNotBlank(after.getNcLevelName(), after.getNcLevel()));
        addChange(changes, "responsibilityDeptNames", "责任部门",
                before.getResponsibilityDeptNames(), after.getResponsibilityDeptNames());
        addChange(changes, "ncDescription", "不良描述", before.getNcDescription(), after.getNcDescription());
        addChange(changes, "responsibleDeptNames", "会签单位",
                joinReviewDeptNames(beforeReviews), joinReviewDeptNames(afterReviews));
        addChange(changes, "responsibleHandlers", "会签办理人",
                joinReviewHandlers(beforeReviews), joinReviewHandlers(afterReviews));
        addChange(changes, "finalDisposition", "处置结论", before.getFinalDisposition(), after.getFinalDisposition());
        addChange(changes, "finalOpinion", "终审意见", before.getFinalOpinion(), after.getFinalOpinion());
        addChange(changes, "stockDisposeQty", "处置数量", before.getStockDisposeQty(), after.getStockDisposeQty());
        addChange(changes, "finalDisposeDescription", "最终处置说明",
                before.getFinalDisposeDescription(), after.getFinalDisposeDescription());
        addChange(changes, "stockDisposeStatus", "库存处置状态",
                before.getStockDisposeStatus(), after.getStockDisposeStatus());
        addChange(changes, "relatedExceptionNo", "关联异常单",
                before.getRelatedExceptionNo(), after.getRelatedExceptionNo());
        addChange(changes, "related8dNo", "关联8D",
                before.getRelated8dNo(), after.getRelated8dNo());
        addChange(changes, "relations", "关联对象/附件",
                joinRelations(beforeRelations), joinRelations(afterRelations));
        addChange(changes, "remark", "备注", before.getRemark(), after.getRemark());
        String summary = CollUtil.isEmpty(changes) ? "未检测到字段变化"
                : changes.stream().map(item -> String.valueOf(item.get("label"))).collect(Collectors.joining("、"));
        return snapshot("changeCount", changes.size(), "changeSummary", summary, "fieldChanges", changes,
                "sourceNo", after.getSourceNo(), "lotNo", after.getLotNo(), "ncLevel", after.getNcLevel());
    }

    private void addChange(List<Map<String, Object>> changes, String field, String label,
                           Object beforeValue, Object afterValue) {
        String beforeText = formatLogValue(beforeValue);
        String afterText = formatLogValue(afterValue);
        if (Objects.equals(beforeText, afterText)) {
            return;
        }
        changes.add(snapshot("field", field, "label", label,
                "beforeValue", beforeText, "afterValue", afterText));
    }

    private String formatLogValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private String joinReviewDeptNames(List<QmsNcMrbReviewDO> reviews) {
        if (CollUtil.isEmpty(reviews)) {
            return "";
        }
        return reviews.stream()
                .map(review -> firstNotBlank(review.getDeptName(), "未指定部门"))
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("、"));
    }

    private String joinReviewHandlers(List<QmsNcMrbReviewDO> reviews) {
        if (CollUtil.isEmpty(reviews)) {
            return "";
        }
        return reviews.stream()
                .map(review -> firstNotBlank(review.getDeptName(), "未指定部门") + "："
                        + firstNotBlank(review.getHandlerUserName(), "-"))
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
    }

    private String joinRelations(List<QmsNcRelationDO> relations) {
        if (CollUtil.isEmpty(relations)) {
            return "";
        }
        return relations.stream()
                .map(relation -> firstNotBlank(relation.getRelationType(), "关联") + "："
                        + firstNotBlank(relation.getRelatedObjectNo(), relation.getRelatedObjectName(), "-"))
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
    }

    private void writeFlowLog(QmsNcRecordDO before, QmsNcRecordDO after, String actionCode,
                              String actionName, String opinion, Map<String, Object> snapshot) {
        qmsNcFlowLogMapper.insert(QmsNcFlowLogDO.builder()
                .ncRecordId(after.getId())
                .ncNo(after.getNcNo())
                .actionCode(actionCode)
                .actionName(actionName)
                .fromStatus(before == null ? null : before.getStatus())
                .toStatus(after.getStatus())
                .fromNodeCode(before == null ? null : before.getCurrentNodeCode())
                .fromNodeName(before == null ? null : before.getCurrentNodeName())
                .toNodeCode(after.getCurrentNodeCode())
                .toNodeName(after.getCurrentNodeName())
                .opinion(opinion)
                .handlerUserId(SecurityFrameworkUtils.getLoginUserId())
                .handlerUserName(currentUserName())
                .handleTime(LocalDateTime.now())
                .businessSnapshot(snapshot)
                .build());
    }

    private Set<Long> buildParticipatedNcRecordIds(Long loginUserId) {
        Set<Long> ids = new HashSet<>();
        if (loginUserId == null) {
            return ids;
        }
        qmsNcFlowLogMapper.selectListByHandlerUserId(loginUserId)
                .forEach(log -> ids.add(log.getNcRecordId()));
        qmsNcMrbReviewMapper.selectListByHandlerUserId(loginUserId)
                .forEach(review -> ids.add(review.getNcRecordId()));
        qmsNcDispositionNotifyMapper.selectListByNotifyUserId(loginUserId)
                .forEach(notify -> ids.add(notify.getNcRecordId()));
        return ids;
    }

    private Set<Long> buildPendingReviewNcRecordIds(Long loginUserId) {
        Set<Long> ids = new HashSet<>();
        if (loginUserId == null) {
            return ids;
        }
        List<QmsNcMrbReviewDO> reviews = qmsNcMrbReviewMapper.selectPendingListByHandlerUserId(loginUserId);
        if (CollUtil.isEmpty(reviews)) {
            return ids;
        }
        Set<Long> reviewNcRecordIds = reviews.stream()
                .map(QmsNcMrbReviewDO::getNcRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(reviewNcRecordIds)) {
            return ids;
        }
        qmsNcRecordMapper.selectBatchIds(reviewNcRecordIds).stream()
                .filter(this::isMrbReviewActive)
                .map(QmsNcRecordDO::getId)
                .forEach(ids::add);
        return ids;
    }

    private Set<Long> buildPendingDispositionNotifyNcRecordIds(Long loginUserId) {
        Set<Long> ids = new HashSet<>();
        if (loginUserId == null) {
            return ids;
        }
        List<QmsNcDispositionNotifyDO> notifies =
                qmsNcDispositionNotifyMapper.selectPendingListByNotifyUserId(loginUserId);
        if (CollUtil.isEmpty(notifies)) {
            return ids;
        }
        Set<Long> notifyNcRecordIds = notifies.stream()
                .map(QmsNcDispositionNotifyDO::getNcRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(notifyNcRecordIds)) {
            return ids;
        }
        qmsNcRecordMapper.selectBatchIds(notifyNcRecordIds).stream()
                .filter(record -> record != null && !STATUS_CANCELLED.equals(record.getStatus()))
                .map(QmsNcRecordDO::getId)
                .forEach(ids::add);
        return ids;
    }

    private boolean isMrbReviewActive(QmsNcRecordDO record) {
        return record != null && (STATUS_MRB_REVIEW.equals(record.getStatus())
                || NODE_MRB_REVIEW.equals(record.getCurrentNodeCode()));
    }

    private String generateNcNo(LocalDateTime now, String sourceType, String sourceTypeName) {
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String typeCode = resolveNcTypeCode(sourceType, sourceTypeName);
        String prefix = "NCR-" + datePart + typeCode;
        int nextSequence = resolveNextNcNoSequence(prefix);
        for (int sequence = nextSequence; sequence <= 999; sequence++) {
            String ncNo = prefix + String.format("%03d", sequence);
            if (qmsNcRecordMapper.selectByNcNo(ncNo) == null) {
                return ncNo;
            }
        }
        throw exception(QMS_NCR_NO_EXHAUSTED, prefix);
    }

    private int resolveNextNcNoSequence(String prefix) {
        QmsNcRecordDO lastRecord = qmsNcRecordMapper.selectLastByNcNoPrefix(prefix);
        if (lastRecord == null || StrUtil.isBlank(lastRecord.getNcNo())) {
            return 1;
        }
        String sequenceText = StrUtil.removePrefix(lastRecord.getNcNo(), prefix);
        if (sequenceText.length() != 3 || !sequenceText.chars().allMatch(Character::isDigit)) {
            return 1;
        }
        return Integer.parseInt(sequenceText) + 1;
    }

    private String resolveNcTypeCode(String sourceType, String sourceTypeName) {
        if (StrUtil.equalsAny(sourceType, NCR_TYPE_FINISHED_PRODUCT, "成品")
                || StrUtil.equals(sourceTypeName, "成品")) {
            return "CP";
        }
        if (StrUtil.equalsAny(sourceType, NCR_TYPE_SEMI_FINISHED, "半成品")
                || StrUtil.equals(sourceTypeName, "半成品")) {
            return "BCP";
        }
        if (StrUtil.equalsAny(sourceType, NCR_TYPE_CUSTOMER_RETURN, "客退品")
                || StrUtil.equals(sourceTypeName, "客退品")) {
            return "KTP";
        }
        if (StrUtil.equalsAny(sourceType, NCR_TYPE_RAW_MATERIAL, "原物料", "原材料")
                || StrUtil.equalsAny(sourceTypeName, "原物料", "原材料")) {
            return "YWL";
        }
        return "QT";
    }

    private Map<String, Object> snapshot(Object... keyValues) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            snapshot.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return snapshot;
    }

    private String buildExceptionInitialImpact(QmsNcRecordDO ncr) {
        List<String> parts = new ArrayList<>();
        if (StrUtil.isNotBlank(ncr.getLotNo())) {
            parts.add("批次：" + ncr.getLotNo());
        }
        if (StrUtil.isNotBlank(ncr.getMaterialCode()) || StrUtil.isNotBlank(ncr.getMaterialName())) {
            parts.add("物料：" + firstNotBlank(ncr.getMaterialCode(), "-") + " / "
                    + firstNotBlank(ncr.getMaterialName(), "-"));
        }
        if (ncr.getDefectQty() != null) {
            parts.add("不合格数量：" + ncr.getDefectQty());
        }
        if (StrUtil.isNotBlank(ncr.getDefectName()) || StrUtil.isNotBlank(ncr.getDefectCode())) {
            parts.add("缺陷：" + firstNotBlank(ncr.getDefectName(), ncr.getDefectCode()));
        }
        return CollUtil.isEmpty(parts) ? firstNotBlank(ncr.getFinalOpinion(), ncr.getRemark()) : String.join("；", parts);
    }

    private String defaultCode(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private String currentUserName() {
        return StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String normalizeSourceType(String value) {
        return normalizeUpper(value);
    }

    private Long parseLongOrNull(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeUpper(String value) {
        return StrUtil.blankToDefault(value, "").trim().toUpperCase();
    }
}

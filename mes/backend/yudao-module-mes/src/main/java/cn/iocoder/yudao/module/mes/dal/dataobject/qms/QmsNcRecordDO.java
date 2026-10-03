package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 不合格品处理单(MRB) DO
 */
@TableName("mes_qms_nc_record")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsNcRecordDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ncNo;
    private String sourceType;
    private String sourceTypeName;
    private String sourceBizType;
    private String sourceBizTypeName;
    private Long sourceId;
    private String sourceNo;
    private Long sourceNcRecordId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime happenTime;

    private Long subOrderId;
    private Long processId;
    private String processName;
    private Long happenDeptId;
    private String happenDeptName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private String unitCode;
    private String lotNo;
    private String defectCode;
    private String defectName;
    private BigDecimal defectQty;
    private String ncLevel;
    private String ncLevelName;
    private String responsibilityDeptCodes;
    private String responsibilityDeptNames;
    private String ncDescription;
    private String rawMaterialAbnormalCategory;
    private String rawMaterialAbnormalCategoryName;
    private Boolean isolatedFlag;
    private String status;
    private String processInstanceId;
    private String currentNodeCode;
    private String currentNodeName;
    private Long currentHandlerUserId;
    private String currentHandlerUserName;
    private Long applicantUserId;
    private String applicantUserName;
    private Long applicantDeptId;
    private String applicantDeptName;
    private Long contentConfirmUserId;
    private String contentConfirmUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime contentConfirmTime;

    private Long qualityConfirmUserId;
    private String qualityConfirmUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime qualityConfirmTime;

    /**
     * 旧字段，兼容异常事件 NCR 选择器和历史 MRB 决策。
     */
    private String mrbDecision;

    private String finalDisposition;
    private String finalOpinion;
    private String finalDisposeDescription;
    private Long finalApproverId;
    private String finalApproverName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finalApproveTime;

    private String stockDisposeStatus;
    private BigDecimal stockDisposeQty;
    private Long stockDisposeUserId;
    private String stockDisposeUserName;
    private String stockDisposeResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime stockDisposeTime;

    private String effectConfirmResult;
    private Long effectConfirmUserId;
    private String effectConfirmUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectConfirmTime;

    private String relatedExceptionNo;
    private Long relatedExceptionId;
    private Boolean createExceptionFlag;
    private String related8dNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    private Long closeUserId;
    private String closeUserName;
    private Long tenantId;
    private String remark;

}

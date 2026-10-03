package cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_qms_coa_template")
@KeySequence("mes_qms_coa_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsCoaTemplateDO extends BaseDO {

    @TableId
    private Long id;
    private String templateCode;
    private String templateName;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String customerProductCode;
    private String customerProductName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long productModelId;
    private String productModelCode;
    private String productModelName;
    private String languageType;
    private String versionNo;
    private Integer status;
    private String auditStatus;
    private String reportTitle;
    private String footerStatement;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime auditTime;
    private String auditOpinion;
    private String remark;
    private Long tenantId;
}

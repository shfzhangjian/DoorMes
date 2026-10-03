package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_evaluation_template_version")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmEvaluationTemplateVersionDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String versionNo;
    private String status;
    private BigDecimal totalScore;
    private BigDecimal qualificationScore;
    private Long previousVersionId;
    private String changeSummary;
    private Long submitterId;
    private String submitterName;
    private LocalDateTime submitTime;
    private Long auditorId;
    private String auditorName;
    private String auditOpinion;
    private LocalDateTime auditTime;
    private Long publisherId;
    private String publisherName;
    private LocalDateTime publishTime;
    private String remark;
    @Version
    private Integer version;
    private Long tenantId;

}

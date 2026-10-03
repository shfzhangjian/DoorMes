package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@TableName("mes_qms_nc_mrb_review")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsNcMrbReviewDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ncRecordId;
    private String ncNo;
    private Long deptId;
    private String deptName;
    private Long handlerUserId;
    private String handlerUserName;
    private Long delegateUserId;
    private String delegateUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime delegateTime;

    private Long actualHandlerUserId;
    private String actualHandlerUserName;
    private String suggestedDisposition;
    private String dispositionDetail;
    private String rootCauseCategory;
    private String causeAnalysis;
    private String reviewOpinion;
    private String reviewStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    private Integer sort;
    private Long tenantId;
}

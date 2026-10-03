package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_survey_review")
@KeySequence("mes_srm_survey_review_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSurveyReviewDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long surveyId;
    private String reviewProject;
    private String reviewDept;
    private String reviewResult;
    private String reviewOpinion;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private Integer sort;

    private Long tenantId;

}

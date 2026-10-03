package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_sample_evaluation_item")
@KeySequence("mes_srm_sample_evaluation_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSampleEvaluationItemDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long evaluationId;
    private Integer rowNo;
    private String itemName;
    private String technicalRequirement;
    private String testData1;
    private String testData2;
    private String testData3;
    private String testData4;
    private String testData5;
    private String itemJudgement;
    private String itemStatus;
    private BigDecimal sortNo;
    private String remark;
    private Long tenantId;

}

package cn.iocoder.yudao.module.mes.dal.dataobject.hc.qtimeconfig;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_hc_qtime_config")
@KeySequence("mes_hc_qtime_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcQtimeConfigDO extends BaseDO {

    @TableId
    private Long id;

    private String modelPrefix;
    private Integer formulaToWetMinutes;
    private Integer wetToGrindingMinutes;
    private Integer firstGrindingToSecondMinutes;
    private Integer grindingToAdhesiveMinutes;
    private Integer adhesiveToSlittingMinutes;
    private Integer slittingToPressSlotMinutes;
    private Integer pressSlotToAdhesive2Minutes;
    private Integer adhesive2ToCutRoundMinutes;
    private Integer cutRoundToFqcMinutes;
    private String status;
    private String remark;
    private Long tenantId;

}

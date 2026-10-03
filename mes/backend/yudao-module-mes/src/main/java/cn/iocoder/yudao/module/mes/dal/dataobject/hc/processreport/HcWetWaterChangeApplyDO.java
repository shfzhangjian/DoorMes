package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_wet_water_change_apply")
@KeySequence("mes_sfc_wet_water_change_apply_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcWetWaterChangeApplyDO extends BaseDO {

    @TableId
    private Long id;

    private LocalDate changeStartDate;

    private LocalDate changeEndDate;

    private String areaDesc;

    private String status;

    private Long applicantId;

    private String applicantName;

    private LocalDateTime applyTime;

    private Long confirmerId;

    private String confirmerName;

    private LocalDateTime confirmTime;

    private String remark;

    private Long tenantId;
}

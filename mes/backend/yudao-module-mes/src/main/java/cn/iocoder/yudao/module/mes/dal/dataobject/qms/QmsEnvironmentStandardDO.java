package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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

@TableName("mes_qms_environment_standard")
@KeySequence("mes_qms_environment_standard_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsEnvironmentStandardDO extends BaseDO {

    @TableId
    private Long id;

    private String workshopCode;
    private String workshopName;
    private BigDecimal temperatureMin;
    private BigDecimal temperatureMax;
    private BigDecimal humidityMin;
    private BigDecimal humidityMax;
    private String remark;
    private Long tenantId;
}

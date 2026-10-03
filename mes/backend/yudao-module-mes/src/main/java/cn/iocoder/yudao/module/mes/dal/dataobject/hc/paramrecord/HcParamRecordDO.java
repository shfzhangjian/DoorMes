package cn.iocoder.yudao.module.mes.dal.dataobject.hc.paramrecord;

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

/**
 * 工艺参数记录 DO
 */
@TableName("mes_sfc_param_record")
@KeySequence("mes_sfc_param_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcParamRecordDO extends BaseDO {

    private Long reportId;

    private String reportNo;

    private String paramCode;

    private String paramName;

    private String paramValue;

    private BigDecimal valueNum;

    private String uom;

    private String judgeResult;

    @TableId
    private Long id;

    private Long tenantId;
}

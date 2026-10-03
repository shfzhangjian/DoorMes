package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processanalysis;

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

@TableName("mes_pp_process_analysis_config")
@KeySequence("mes_pp_process_analysis_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProcessAnalysisConfigDO extends BaseDO {

    @TableId
    private Long id;

    private String configName;
    private String scopeType;
    private Long ownerUserId;
    private Boolean defaultFlag;
    private Integer configVersion;
    private String configJson;
    private String remark;
    private Long tenantId;

}

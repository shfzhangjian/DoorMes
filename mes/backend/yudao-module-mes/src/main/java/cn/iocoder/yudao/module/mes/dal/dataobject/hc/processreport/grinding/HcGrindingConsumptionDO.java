package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_sfc_grinding_consumption")
public class HcGrindingConsumptionDO extends BaseDO {
    @TableId
    private Long id;
    private Long tenantId;
    private String requestKey;
    private String sourceType;
    private Long sourceId;
    private Long resultId;
    private String requestHash;
    private String snapshotJson;
    private Long sandpaperConsumeId;
    private Long guideClothConsumeId;
    private Boolean cancelled;
}

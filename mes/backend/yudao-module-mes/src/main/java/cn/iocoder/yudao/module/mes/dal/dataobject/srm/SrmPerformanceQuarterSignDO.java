package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_performance_quarter_sign")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceQuarterSignDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    private String deptCode;
    private String deptName;
    private Long userId;
    private String userName;
    private String signStatus;
    private String signResult;
    private String signOpinion;
    private LocalDateTime signTime;
    private Long tenantId;

}

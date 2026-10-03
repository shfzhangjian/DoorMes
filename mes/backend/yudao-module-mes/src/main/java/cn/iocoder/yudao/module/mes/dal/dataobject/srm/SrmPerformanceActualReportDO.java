package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_performance_actual_report")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceActualReportDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierSourceType;
    private String periodType;
    private Integer evalYear;
    private Integer evalQuarter;
    private Integer evalMonth;
    private String status;
    private Long reporterUserId;
    private String reporterUserName;
    private Long confirmUserId;
    private String confirmUserName;
    private LocalDateTime submitTime;
    private LocalDateTime confirmTime;
    private String confirmOpinion;
    private String remark;
    @Version
    private Integer version;
    private Long tenantId;

}

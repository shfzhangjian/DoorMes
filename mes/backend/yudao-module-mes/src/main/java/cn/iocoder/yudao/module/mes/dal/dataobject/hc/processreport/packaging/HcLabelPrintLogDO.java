package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_label_print_log")
@KeySequence("mes_sfc_label_print_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcLabelPrintLogDO extends BaseDO {

    @TableId
    private Long id;

    private String labelNo;
    private String labelType;
    private Long bizId;
    private String bizNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Integer printCount;
    private String printReason;
    private String printerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime printTime;
    private String labelContentJson;
    private Long tenantId;
}

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

@TableName("mes_sfc_outer_pack_box")
@KeySequence("mes_sfc_outer_pack_box_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcOuterPackBoxDO extends BaseDO {

    @TableId
    private Long id;

    private String outerBoxNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String packMethod;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String batchNo;
    private String productSize;
    private Integer standardQty;
    private Integer currentQty;
    private Boolean tailBoxFlag;
    private String outerLabelNo;
    private String boxStatus;
    private Integer printCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPrintTime;
    private String reviewerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reviewTime;
    private String recorderName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;
    private String remark;
    private String extraJson;
    private Long tenantId;
}

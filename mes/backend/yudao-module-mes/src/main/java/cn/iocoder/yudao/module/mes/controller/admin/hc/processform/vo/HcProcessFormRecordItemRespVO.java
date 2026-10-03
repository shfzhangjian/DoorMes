package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HcProcessFormRecordItemRespVO {

    private Long id;
    private Long recordId;
    private Long templateItemId;
    private Integer itemSeq;
    private String fieldKey;
    private String fieldLabel;
    private String itemCategory;
    private String stepNode;
    private String standardText;
    private String unit;
    private String valueMode;
    private String controlType;
    private String actualValue;
    private String actualValue2;
    private BigDecimal actualNumber;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime actualTime;
    private String resultFlag;
    private String abnormalRemark;
    private String sourceRowJson;
}

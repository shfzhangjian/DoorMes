package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - FAI原始记录表模板 Response VO")
@Data
public class QmsFaiSheetTemplateRespVO {

    private Long id;
    private String templateCode;
    private String templateName;
    private String templateVersion;
    private String productModel;
    private String sheetName;
    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    private List<Section> sections;

    @Data
    public static class Section {
        private Long id;
        private Long templateId;
        private String sectionCode;
        private String sectionName;
        private String sectionType;
        private Integer expectedRows;
        private Integer expectedColumns;
        private String excelRange;
        private Integer sort;
        private List<Field> fields;
    }

    @Data
    public static class Field {
        private Long id;
        private Long templateId;
        private Long sectionId;
        private String metricCode;
        private String metricName;
        private String fieldCode;
        private String fieldName;
        private String fieldRole;
        private String unit;
        private String formulaExpr;
        private BigDecimal avgMinLimit;
        private BigDecimal avgMaxLimit;
        private BigDecimal stdMinLimit;
        private BigDecimal stdMaxLimit;
        private String excelColumn;
        private Integer sort;
    }
}

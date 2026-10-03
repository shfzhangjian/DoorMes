package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 量检具MSA分析历史 Response VO")
@Data
public class QmsMeasureToolMsaRecordRespVO {
    private Long id;
    private String recordNo;
    private Long ledgerId;
    private String toolCode;
    private String toolName;
    private Long categoryId;
    private String categoryName;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate msaDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate nextMsaDate;
    private String msaResult;
    private String msaReport;
    private String analyst;
    private Integer missedCount;
    private Integer overdueFlag;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate overdueDueDate;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}

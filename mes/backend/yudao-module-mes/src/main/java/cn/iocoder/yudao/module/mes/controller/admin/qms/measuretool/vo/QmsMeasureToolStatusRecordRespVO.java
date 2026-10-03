package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 量检具状态调整记录 Response VO")
@Data
public class QmsMeasureToolStatusRecordRespVO {

    private Long id;
    private Long ledgerId;
    private String previousStatus;
    private String status;
    private String handler;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime handleTime;

    private String oaProcessNo;
    private String handleRemark;
    private String attachments;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}

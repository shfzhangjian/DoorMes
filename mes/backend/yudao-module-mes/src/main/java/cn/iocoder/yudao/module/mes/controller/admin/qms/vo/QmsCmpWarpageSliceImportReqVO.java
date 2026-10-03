package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "管理后台 - CMP软垫翘曲裁切送检片号导入 Request VO")
@Data
public class QmsCmpWarpageSliceImportReqVO {

    @Schema(description = "关键词：母批/分段片号/生产片号/裁切FQC单号")
    private String keyword;

    @Schema(description = "产品型号")
    private String modelCode;

    @Schema(description = "母批号")
    private String parentBatchNo;

    @Schema(description = "分段片号")
    private String segmentSliceNo;

    @Schema(description = "送检日期开始")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDateStart;

    @Schema(description = "送检日期结束")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDateEnd;
}

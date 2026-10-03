package cn.iocoder.yudao.module.mes.controller.admin.process.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - MES标准工序 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ProcessRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "23639")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "工序编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("工序编码")
    private String code;

    @Schema(description = "工序名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("工序名称")
    private String name;

    @Schema(description = "默认车间ID", example = "10939")
    @ExcelProperty("默认车间ID")
    private Long workshopId;

    @Schema(description = "车间编码")
    @ExcelProperty("车间编码")
    private String workshopCode;

    @Schema(description = "车间名称", example = "李四")
    @ExcelProperty("车间名称")
    private String workshopName;

    @Schema(description = "工序类型", example = "2")
    @ExcelProperty(value = "工序类型", converter = DictConvert.class)
    @DictFormat("mes_process_type")
    private String processType;

    @Schema(description = "是否绑定工位")
    @ExcelProperty("是否绑定工位")
    private Boolean bindStation;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "可用工位/设备列表")
    private List<ProcessStation> stations;

    @Data
    public static class ProcessStation {
        private Long id;
        private Long stationId;
        private String stationCode;
        private String stationName;
        private Boolean defaultStatus;
        private Integer sort;
        private String remark;
    }
}

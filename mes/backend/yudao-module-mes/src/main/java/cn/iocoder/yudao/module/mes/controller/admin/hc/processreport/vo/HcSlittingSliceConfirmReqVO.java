package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 分切切片扫码确认 Request VO")
@Data
public class HcSlittingSliceConfirmReqVO {

    @NotNull(message = "切片记录ID不能为空")
    private Long id;

    private String scannedSliceNo;
    private String selfCheck;
    private String sizeCode;
    private String sizeName;
    private String remark;
    private List<VisualItem> visualItems;

    @Data
    public static class VisualItem {
        private String itemName;
        private String result;
        private String remark;
    }
}

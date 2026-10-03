package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 湿法泡孔自检图片保存 Request VO")
@Data
public class QmsWetPoreSelfCheckPhotoReqVO {

    @Schema(description = "工位记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工位记录ID不能为空")
    private Long stationRecordId;

    @Schema(description = "泡孔自检记录序号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "泡孔自检记录序号不能为空")
    @Min(value = 0, message = "泡孔自检记录序号不能小于 0")
    private Integer recordIndex;

    @Schema(description = "前端记录键")
    private String clientKey;

    @Schema(description = "历史单张泡孔图片地址，兼容旧调用方")
    private String photo;

    @Schema(description = "泡孔图片地址列表，每条自检记录最多 10 张")
    private List<String> photos;

}

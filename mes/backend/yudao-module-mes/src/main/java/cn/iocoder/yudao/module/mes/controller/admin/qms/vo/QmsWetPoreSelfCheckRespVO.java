package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 湿法泡孔自检 Response VO")
@Data
public class QmsWetPoreSelfCheckRespVO {

    @Schema(description = "行ID")
    private String id;

    @Schema(description = "工位记录ID")
    private Long stationRecordId;

    @Schema(description = "泡孔自检记录序号")
    private Integer recordIndex;

    @Schema(description = "前端记录键")
    private String clientKey;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "计划工序ID")
    private Long planOperationId;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "产品型号")
    private String productModel;

    @Schema(description = "母批批号")
    private String motherBatchNo;

    @Schema(description = "产品料号")
    private String productMaterialCode;

    @Schema(description = "泡孔自检时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime selfCheckTime;

    @Schema(description = "泡孔自检结果")
    private String selfCheckResult;

    @Schema(description = "检验人")
    private String inspector;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "泡孔图片首图，兼容旧调用方")
    private String photo;

    @Schema(description = "泡孔图片列表")
    private List<String> photos;

    @Schema(description = "图片状态：UPLOADED 已上传，MISSING 未上传")
    private String imageStatus;

    @Schema(description = "工位记录状态")
    private String recordStatus;

    @Schema(description = "工位记录人")
    private String recordUserName;

    @Schema(description = "工位记录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}

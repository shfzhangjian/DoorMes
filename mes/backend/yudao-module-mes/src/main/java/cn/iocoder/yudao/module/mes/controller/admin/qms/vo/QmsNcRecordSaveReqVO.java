package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 不合格品处理单保存 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsNcRecordSaveReqVO extends QmsNcRecordBaseVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "MRB会签明细")
    private List<QmsNcMrbReviewReqVO> reviews;

    @Schema(description = "其他缺陷明细")
    private List<QmsNcDefectReqVO> defects;

    @Schema(description = "关联对象")
    private List<QmsNcRelationReqVO> relations;
}

package cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工位记录明细 Response VO")
@Data
public class HcStationRecordItemRespVO {

    private Long id;
    private Long recordId;
    private Integer itemSeq;
    private String itemCategory;
    private String stepNode;
    private String itemName;
    private String standardText;
    private String valueMode;
    private String dualLabel1;
    private String dualLabel2;

    /** 多字段定义快照，按稳定 key 对应实际值。 */
    private String fieldDefinitionsJson;
    private String fieldValuesJson;
    private String actualValue;
    private String actualValue2;
    private String resultFlag;
    private String abnormalRemark;
}

package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 量检具台账可输入下拉候选 Response VO")
@Data
public class QmsMeasureToolLedgerSelectOptionRespVO {

    @Schema(description = "历史填写的人员名称")
    private List<String> personnelNames;

    @Schema(description = "历史填写的使用部门")
    private List<String> usingDepartments;

    @Schema(description = "历史填写的校准机构")
    private List<String> calibrationOrgs;

}

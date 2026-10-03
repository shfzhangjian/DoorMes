package cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate.HcFormTemplateVersionDO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 表单模板 Detail Response VO")
@Data
public class HcFormTemplateDetailRespVO extends HcFormTemplateRespVO {

    @Schema(description = "模板版本列表")
    private List<HcFormTemplateVersionDO> templateVersions;

}
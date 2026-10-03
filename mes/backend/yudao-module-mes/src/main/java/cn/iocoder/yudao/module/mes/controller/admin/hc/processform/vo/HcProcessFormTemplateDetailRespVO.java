package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcProcessFormTemplateDetailRespVO extends HcProcessFormTemplateRespVO {

    private HcProcessFormVersionRespVO currentVersion;
    private List<HcProcessFormSectionRespVO> sections;
    private List<HcProcessFormItemRespVO> items;
}

package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcProcessFormRecordDetailRespVO extends HcProcessFormRecordRespVO {

    private HcProcessFormTemplateDetailRespVO template;
    private List<HcProcessFormRecordItemRespVO> items;
}

package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcProductModelDetailRespVO extends HcProductModelRespVO {

    private List<HcProductModelSegmentRespVO> modelSegments;
    private List<HcProductModelMaterialRespVO> modelMaterials;

}

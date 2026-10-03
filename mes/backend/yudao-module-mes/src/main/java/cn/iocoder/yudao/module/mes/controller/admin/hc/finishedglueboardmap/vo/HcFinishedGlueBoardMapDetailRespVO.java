package cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HcFinishedGlueBoardMapDetailRespVO extends HcFinishedGlueBoardMapRespVO {

    private List<HcFinishedGlueBoardMapItemRespVO> items;

}

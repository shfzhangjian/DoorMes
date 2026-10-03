package cn.iocoder.yudao.module.mes.service.hc.workcenter;

public interface HcProductionLineResolverService {

    HcProductionLineContext resolveByWorkCenterId(Long workCenterId);

    HcProductionLineContext resolveByWorkCenterCode(String workCenterCode);

    HcProductionLineContext resolveByMotherModelCode(String motherModelCode);
}

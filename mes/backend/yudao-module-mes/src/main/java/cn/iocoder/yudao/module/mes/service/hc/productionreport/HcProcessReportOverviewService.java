package cn.iocoder.yudao.module.mes.service.hc.productionreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.productionreport.vo.HcProcessReportOverviewDetailRespVO;

public interface HcProcessReportOverviewService {

    HcProcessReportOverviewDetailRespVO getDetail(Long planId);
}

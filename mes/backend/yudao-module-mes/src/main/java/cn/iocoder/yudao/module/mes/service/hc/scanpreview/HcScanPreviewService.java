package cn.iocoder.yudao.module.mes.service.hc.scanpreview;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview.vo.HcScanPreviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview.vo.HcScanPreviewRecordRespVO;

public interface HcScanPreviewService {

    PageResult<HcScanPreviewRecordRespVO> getPage(HcScanPreviewPageReqVO reqVO);

}

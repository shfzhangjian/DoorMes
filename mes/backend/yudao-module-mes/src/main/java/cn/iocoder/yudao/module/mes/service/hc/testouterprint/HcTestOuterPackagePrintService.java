package cn.iocoder.yudao.module.mes.service.hc.testouterprint;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterCustomerProductPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterCustomerProductRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPrintDesignRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPrintPayloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterWaitPieceListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterWaitSegmentPageReqVO;
import java.util.List;

public interface HcTestOuterPackagePrintService {

    PageResult<TestOuterSegmentRespVO> getWaitSegmentPage(TestOuterWaitSegmentPageReqVO reqVO);

    List<TestOuterPieceRespVO> getWaitSegmentPieceList(TestOuterWaitPieceListReqVO reqVO);

    PageResult<TestOuterCustomerProductRespVO> getCustomerProductPage(TestOuterCustomerProductPageReqVO reqVO);

    List<TestOuterPrintDesignRespVO> getTemplateList(Long customerInfoId, Long productItemId, String labelKind);

    TestOuterPrintPayloadRespVO buildPrintPayload(Long customerInfoId, Long productItemId, Long designId,
                                                  String labelKind,
                                                  String sliceBatchNos);
}

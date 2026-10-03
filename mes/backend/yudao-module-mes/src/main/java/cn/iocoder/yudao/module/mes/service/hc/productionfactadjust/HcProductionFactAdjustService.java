package cn.iocoder.yudao.module.mes.service.hc.productionfactadjust;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.ApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.CreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.DetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.ExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.InstructionOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.OperationOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.OrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.PageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.PreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.PreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.ProductOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.SegmentOptionRespVO;
import java.util.List;

public interface HcProductionFactAdjustService {

    List<OperationOptionRespVO> getOperationOptions(String planNo);

    List<SegmentOptionRespVO> getSegmentOptions(Long planOperationId);

    List<ProductOptionRespVO> getProductOptions(String keyword);

    List<InstructionOptionRespVO> getInstructionOptions(Long planOperationId, String segmentBatchNo);

    PreviewRespVO preview(PreviewReqVO reqVO);

    Long create(CreateReqVO reqVO);

    void approve(ApproveReqVO reqVO);

    void execute(ExecuteReqVO reqVO);

    PageResult<OrderRespVO> getPage(PageReqVO reqVO);

    List<DetailRespVO> getDetailList(Long orderId);
}

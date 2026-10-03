package cn.iocoder.yudao.module.mes.service.hc.historypiecestock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockGoodStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockRespVO;

import java.util.List;

public interface HcHistoryPieceMassStockService {

    PageResult<HcHistoryPieceMassStockRespVO> getPage(HcHistoryPieceMassStockPageReqVO pageReqVO);

    List<HcHistoryPieceMassStockGoodStockRespVO> getGoodStockList(String modelCode, String segmentBatchNo);

}

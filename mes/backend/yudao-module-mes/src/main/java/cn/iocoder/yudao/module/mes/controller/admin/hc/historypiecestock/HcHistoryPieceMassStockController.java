package cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockGoodStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockRespVO;
import cn.iocoder.yudao.module.mes.service.hc.historypiecestock.HcHistoryPieceMassStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 历史片量产备货库存")
@RestController
@RequestMapping("/mes/hc/plan/history-piece-mass-stock")
@Validated
public class HcHistoryPieceMassStockController {

    @Resource
    private HcHistoryPieceMassStockService hcHistoryPieceMassStockService;

    @GetMapping("/page")
    @Operation(summary = "查询历史片量产备货库存分页")
    public CommonResult<PageResult<HcHistoryPieceMassStockRespVO>> getPage(
            @Valid HcHistoryPieceMassStockPageReqVO pageReqVO) {
        return success(hcHistoryPieceMassStockService.getPage(pageReqVO));
    }

    @GetMapping("/good-stock/list")
    @Operation(summary = "查询历史片量产备货库存良品库存明细")
    public CommonResult<List<HcHistoryPieceMassStockGoodStockRespVO>> getGoodStockList(
            @RequestParam(required = false) String modelCode,
            @RequestParam String segmentBatchNo) {
        return success(hcHistoryPieceMassStockService.getGoodStockList(modelCode, segmentBatchNo));
    }

}

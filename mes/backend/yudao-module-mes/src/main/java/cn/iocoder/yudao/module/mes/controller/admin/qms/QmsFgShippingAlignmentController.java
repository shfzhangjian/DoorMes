package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentNoticeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentSaveReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsFgShippingFqcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 发货客户批号对齐")
@RestController
@RequestMapping("/mes/quality/fg-shipping-alignment")
@Validated
public class QmsFgShippingAlignmentController {

    @Resource
    private QmsFgShippingFqcService qmsFgShippingFqcService;

    @GetMapping("/notice-list")
    @Operation(summary = "获取可对齐的发货通知单")
    public CommonResult<List<QmsFgShippingAlignmentNoticeRespVO>> getNoticeList(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(qmsFgShippingFqcService.getAlignmentNoticeList(keyword));
    }

    @GetMapping("/notice-page")
    @Operation(summary = "分页获取可对齐的发货通知单")
    public CommonResult<PageResult<QmsFgShippingAlignmentNoticeRespVO>> getNoticePage(
            @Valid QmsFgShippingAlignmentPageReqVO pageReqVO) {
        return success(qmsFgShippingFqcService.getAlignmentNoticePage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取发货通知单客户批号对齐详情")
    @Parameter(name = "shippingNoticeId", description = "发货通知单ID", required = true)
    public CommonResult<QmsFgShippingAlignmentRespVO> get(@RequestParam("shippingNoticeId") Long shippingNoticeId) {
        return success(qmsFgShippingFqcService.getAlignment(shippingNoticeId));
    }

    @PutMapping("/save")
    @Operation(summary = "分批保存发货客户批号对齐")
    public CommonResult<QmsFgShippingAlignmentRespVO> save(
            @Valid @RequestBody QmsFgShippingAlignmentSaveReqVO reqVO) {
        return success(qmsFgShippingFqcService.saveShippingAlignment(reqVO));
    }
}

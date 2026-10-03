package cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview.vo.HcScanPreviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview.vo.HcScanPreviewRecordRespVO;
import cn.iocoder.yudao.module.mes.service.hc.scanpreview.HcScanPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HC 扫码预览")
@RestController
@RequestMapping("/mes/hc/plan/scan-preview")
@Validated
public class HcScanPreviewController {

    @Resource
    private HcScanPreviewService scanPreviewService;

    @GetMapping("/page")
    @Operation(summary = "分页查询报工扫码预览记录")
    public CommonResult<PageResult<HcScanPreviewRecordRespVO>> page(@Valid HcScanPreviewPageReqVO reqVO) {
        return success(scanPreviewService.getPage(reqVO));
    }

}

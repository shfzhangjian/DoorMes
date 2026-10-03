package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsPackagingCoaInspectionVO.FaiRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsPackagingCoaInspectionVO.SubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsPackagingCoaInspectionVO.SubmitRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsPackagingCoaInspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 包装段 COA 送检")
@RestController
@RequestMapping("/mes/hc/execution/coa-inspection")
@Validated
public class QmsPackagingCoaInspectionController {

    @Resource
    private QmsPackagingCoaInspectionService qmsPackagingCoaInspectionService;

    @GetMapping("/fai-page")
    @Operation(summary = "获取包装段 COA 送检记录分页")
    public CommonResult<PageResult<FaiRecordRespVO>> getFaiPage(@Valid QmsFaiPageReqVO pageReqVO) {
        return success(qmsPackagingCoaInspectionService.getFaiPage(pageReqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "从不合格待包装段选择样片送至 FAI 检验")
    public CommonResult<SubmitRespVO> submit(@Valid @RequestBody SubmitReqVO reqVO) {
        return success(qmsPackagingCoaInspectionService.submit(reqVO));
    }
}

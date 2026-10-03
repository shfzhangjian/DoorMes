package cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordSubmitReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.paramrecord.HcParamRecordDO;
import cn.iocoder.yudao.module.mes.service.hc.paramrecord.HcParamRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工艺参数采集")
@RestController
@RequestMapping("/mes/hc/execution/param-record")
@Validated
public class HcParamRecordController {

    @Resource
    private HcParamRecordService hcParamRecordService;

    @GetMapping("/context")
    @Operation(summary = "获取工艺参数采集上下文")
    public CommonResult<HcParamRecordContextRespVO> getParamRecordContext(
            @Parameter(name = "reportId", description = "报工ID") @RequestParam(value = "reportId", required = false) Long reportId,
            @Parameter(name = "reportNo", description = "报工单号") @RequestParam(value = "reportNo", required = false) String reportNo) {
        return success(hcParamRecordService.getParamRecordContext(reportId, reportNo));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交工艺参数采集结果")
    public CommonResult<Boolean> submitParamRecord(@Valid @RequestBody HcParamRecordSubmitReqVO reqVO) {
        hcParamRecordService.submitParamRecord(reqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询工艺参数记录")
    public CommonResult<PageResult<HcParamRecordRespVO>> getParamRecordPage(@Valid HcParamRecordPageReqVO pageReqVO) {
        PageResult<HcParamRecordDO> pageResult = hcParamRecordService.getParamRecordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcParamRecordRespVO.class));
    }
}

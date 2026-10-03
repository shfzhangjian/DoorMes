package cn.iocoder.yudao.module.mes.controller.admin.hc.processoutputbalance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processoutputbalance.vo.HcProcessOutputBalancePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processoutputbalance.vo.HcProcessOutputBalanceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processoutputbalance.HcProcessOutputBalanceDO;
import cn.iocoder.yudao.module.mes.service.hc.processoutputbalance.HcProcessOutputBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工序产出物结存台账")
@RestController
@RequestMapping("/mes/hc/execution/process-output-balance")
@Validated
public class HcProcessOutputBalanceController {

    @Resource
    private HcProcessOutputBalanceService processOutputBalanceService;

    @GetMapping("/page")
    @Operation(summary = "查询工序产出物结存台账分页")
    public CommonResult<PageResult<HcProcessOutputBalanceRespVO>> getProcessOutputBalancePage(
            @Valid HcProcessOutputBalancePageReqVO pageReqVO) {
        PageResult<HcProcessOutputBalanceDO> pageResult = processOutputBalanceService.getProcessOutputBalancePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcProcessOutputBalanceRespVO.class));
    }

}

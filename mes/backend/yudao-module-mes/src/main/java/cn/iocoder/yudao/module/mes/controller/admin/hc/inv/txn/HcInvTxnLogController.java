package cn.iocoder.yudao.module.mes.controller.admin.hc.inv.txn;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.txn.vo.HcInvTxnLogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.txn.vo.HcInvTxnLogRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO;
import cn.iocoder.yudao.module.mes.service.hc.inv.txn.HcInvTxnLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 库存流水")
@RestController
@RequestMapping("/mes/hc/inv/txn")
@Validated
public class HcInvTxnLogController {

    @Resource
    private HcInvTxnLogService hcInvTxnLogService;

    @GetMapping("/page")
    @Operation(summary = "查询库存流水分页")
    public CommonResult<PageResult<HcInvTxnLogRespVO>> getInvTxnLogPage(@Valid HcInvTxnLogPageReqVO pageReqVO) {
        PageResult<HcInvTxnLogDO> pageResult = hcInvTxnLogService.getInvTxnLogPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcInvTxnLogRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出库存流水 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportInvTxnLogExcel(@Valid HcInvTxnLogPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcInvTxnLogDO> list = hcInvTxnLogService.getInvTxnLogList(pageReqVO);
        ExcelUtils.write(response, "库存流水.xls", "数据", HcInvTxnLogRespVO.class, BeanUtils.toBean(list, HcInvTxnLogRespVO.class));
    }

}

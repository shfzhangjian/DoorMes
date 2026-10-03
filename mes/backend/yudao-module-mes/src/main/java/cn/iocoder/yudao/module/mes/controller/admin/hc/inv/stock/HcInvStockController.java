package cn.iocoder.yudao.module.mes.controller.admin.hc.inv.stock;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.stock.vo.HcInvStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.stock.vo.HcInvStockRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.HcInvStockService;
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

@Tag(name = "管理后台 - 实时库存余额")
@RestController
@RequestMapping("/mes/hc/inv/stock")
@Validated
public class HcInvStockController {

    @Resource
    private HcInvStockService hcInvStockService;

    @GetMapping("/page")
    @Operation(summary = "查询实时库存余额分页")
    public CommonResult<PageResult<HcInvStockRespVO>> getInvStockPage(@Valid HcInvStockPageReqVO pageReqVO) {
        PageResult<HcInvStockDO> pageResult = hcInvStockService.getInvStockPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcInvStockRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出实时库存余额 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportInvStockExcel(@Valid HcInvStockPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcInvStockDO> list = hcInvStockService.getInvStockList(pageReqVO);
        ExcelUtils.write(response, "实时库存余额.xls", "数据", HcInvStockRespVO.class, BeanUtils.toBean(list, HcInvStockRespVO.class));
    }

}

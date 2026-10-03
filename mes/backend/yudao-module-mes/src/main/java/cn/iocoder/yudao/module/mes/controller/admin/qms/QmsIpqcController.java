package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcStandardRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcOrderDO;
import cn.iocoder.yudao.module.mes.service.qms.QmsIpqcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - IPQC过程抽检")
@RestController
@RequestMapping("/mes/quality/ipqc")
@Validated
public class QmsIpqcController {

    @Resource
    private QmsIpqcService qmsIpqcService;

    @PostMapping("/create")
    @Operation(summary = "创建IPQC过程抽检单")
    public CommonResult<Long> createIpqc(@Valid @RequestBody QmsIpqcSaveReqVO createReqVO) {
        return success(qmsIpqcService.createIpqc(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新IPQC过程抽检单")
    public CommonResult<Boolean> updateIpqc(@Valid @RequestBody QmsIpqcSaveReqVO updateReqVO) {
        qmsIpqcService.updateIpqc(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交IPQC过程抽检结果")
    public CommonResult<Boolean> submitIpqc(@Valid @RequestBody QmsIpqcSaveReqVO submitReqVO) {
        qmsIpqcService.submitIpqc(submitReqVO);
        return success(true);
    }

    @PutMapping("/suspend")
    @Operation(summary = "挂起IPQC过程抽检单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> suspendIpqc(@RequestParam("id") Long id) {
        qmsIpqcService.suspendIpqc(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除IPQC过程抽检单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteIpqc(@RequestParam("id") Long id) {
        qmsIpqcService.deleteIpqc(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除IPQC过程抽检单")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteIpqcList(@RequestParam("ids") List<Long> ids) {
        qmsIpqcService.deleteIpqcList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取IPQC过程抽检详情")
    public CommonResult<QmsIpqcRespVO> getIpqc(@RequestParam("id") Long id) {
        return success(qmsIpqcService.getIpqcResp(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取IPQC过程抽检分页")
    public CommonResult<PageResult<QmsIpqcRespVO>> getIpqcPage(@Valid QmsIpqcPageReqVO pageReqVO) {
        PageResult<QmsIpqcOrderDO> pageResult = qmsIpqcService.getIpqcPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsIpqcRespVO.class));
    }

    @GetMapping("/pending-list")
    @Operation(summary = "获取IPQC待巡检/挂起队列")
    public CommonResult<List<QmsIpqcRespVO>> getPendingIpqcList() {
        return success(qmsIpqcService.getPendingIpqcList());
    }

    @GetMapping("/standard-by-machine")
    @Operation(summary = "按物料与工序带出IPQC检验标准")
    public CommonResult<QmsIpqcStandardRespVO> getIpqcStandardByMachine(@RequestParam("materialCode") String materialCode,
                                                                        @RequestParam(value = "operationCode", required = false) String operationCode,
                                                                        @RequestParam(value = "operationName", required = false) String operationName) {
        return success(qmsIpqcService.getIpqcStandard(materialCode, operationCode, operationName));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出IPQC过程抽检 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportIpqcExcel(@Valid QmsIpqcPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsIpqcOrderDO> list = qmsIpqcService.getIpqcPage(pageReqVO).getList();
        ExcelUtils.write(response, "IPQC过程抽检.xls", "数据", QmsIpqcRespVO.class,
                BeanUtils.toBean(list, QmsIpqcRespVO.class));
    }
}

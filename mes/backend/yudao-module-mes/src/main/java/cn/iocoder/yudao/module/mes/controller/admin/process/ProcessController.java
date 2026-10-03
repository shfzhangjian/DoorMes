// 完整路径: cn.iocoder.yudao.module.mes.controller.admin.process.ProcessController.java
package cn.iocoder.yudao.module.mes.controller.admin.process;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.mes.controller.admin.process.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
// [新增] 引入 DO
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessStationDO;
import cn.iocoder.yudao.module.mes.service.process.ProcessService;

@Tag(name = "管理后台 - MES标准工序")
@RestController
@RequestMapping("/mes/base/process")
@Validated
public class ProcessController {

    @Resource
    private ProcessService processService;

    @PostMapping("/create")
    @Operation(summary = "创建MES标准工序")
    public CommonResult<Long> createProcess(@Valid @RequestBody ProcessSaveReqVO createReqVO) {
        return success(processService.createProcess(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新MES标准工序")
    public CommonResult<Boolean> updateProcess(@Valid @RequestBody ProcessSaveReqVO updateReqVO) {
        processService.updateProcess(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除MES标准工序")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteProcess(@RequestParam("id") Long id) {
        processService.deleteProcess(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除MES标准工序")
    public CommonResult<Boolean> deleteProcessList(@RequestParam("ids") List<Long> ids) {
        processService.deleteProcessListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得MES标准工序")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<ProcessRespVO> getProcess(@RequestParam("id") Long id) {
        // 1. 获得主表信息
        ProcessDO process = processService.getProcess(id);
        ProcessRespVO respVO = BeanUtils.toBean(process, ProcessRespVO.class);

        // 2. [新增] 获得工位/设备矩阵列表并拼装
        List<ProcessStationDO> stationDOs = processService.getProcessStationList(id);
        respVO.setStations(BeanUtils.toBean(stationDOs, ProcessRespVO.ProcessStation.class));

        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得MES标准工序分页")
    public CommonResult<PageResult<ProcessRespVO>> getProcessPage(@Valid ProcessPageReqVO pageReqVO) {
        PageResult<ProcessDO> pageResult = processService.getProcessPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ProcessRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出MES标准工序 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProcessExcel(@Valid ProcessPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ProcessDO> list = processService.getProcessPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "MES标准工序.xls", "数据", ProcessRespVO.class,
                BeanUtils.toBean(list, ProcessRespVO.class));
    }

}

// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.tooling.ToolingLedgerController.java
package cn.iocoder.yudao.module.mes.controller.admin.tooling;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.tooling.vo.ToolingLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.tooling.vo.ToolingLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.tooling.ToolingLedgerDO;
import cn.iocoder.yudao.module.mes.service.tooling.ToolingLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工装模具台账与寿命管理")
@RestController
@RequestMapping("/mes/tooling-ledger")
@Validated
public class ToolingLedgerController {

    @Resource
    private ToolingLedgerService toolingLedgerService;

    @PostMapping("/create")
    @Operation(summary = "创建工装治具台账")
    public CommonResult<Long> createToolingLedger(@Valid @RequestBody ToolingLedgerSaveReqVO createReqVO) {
        return success(toolingLedgerService.createToolingLedger(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改工装治具台账")
    public CommonResult<Boolean> updateToolingLedger(@Valid @RequestBody ToolingLedgerSaveReqVO updateReqVO) {
        toolingLedgerService.updateToolingLedger(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工装治具台账")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteToolingLedger(@RequestParam("id") Long id) {
        toolingLedgerService.deleteToolingLedger(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工装治具详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<ToolingLedgerDO> getToolingLedger(@RequestParam("id") Long id) {
        return success(toolingLedgerService.getToolingLedger(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工装治具分页")
    public CommonResult<PageResult<ToolingLedgerDO>> getToolingLedgerPage(@Valid ToolingLedgerPageReqVO pageVO) {
        return success(toolingLedgerService.getToolingLedgerPage(pageVO));
    }
}

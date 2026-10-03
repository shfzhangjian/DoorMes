package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRoleScopeAddReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRoleScopePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRoleScopeRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsQualityStandardRoleScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 角色检验标准范围")
@RestController
@RequestMapping("/mes/quality/base/standard-role-scope")
@Validated
public class QmsQualityStandardRoleScopeController {

    @Resource
    private QmsQualityStandardRoleScopeService roleScopeService;

    @GetMapping("/page")
    @Operation(summary = "获取角色已分配检验标准范围分页")
    public CommonResult<PageResult<QmsQualityStandardRoleScopeRespVO>> getScopePage(
            @Valid QmsQualityStandardRoleScopePageReqVO pageReqVO) {
        return success(roleScopeService.getScopePage(pageReqVO));
    }

    @GetMapping("/candidate-page")
    @Operation(summary = "获取可加入角色范围的候选检验标准分页")
    public CommonResult<PageResult<QmsQualityStandardRoleScopeRespVO>> getCandidatePage(
            @Valid QmsQualityStandardRoleScopePageReqVO pageReqVO) {
        return success(roleScopeService.getCandidatePage(pageReqVO));
    }

    @GetMapping("/count")
    @Operation(summary = "获取角色各检验标准范围数量")
    @Parameter(name = "roleId", description = "角色ID", required = true)
    public CommonResult<Map<String, Long>> getScopeCount(@RequestParam("roleId") Long roleId) {
        return success(roleScopeService.getScopeCount(roleId));
    }

    @PostMapping("/add")
    @Operation(summary = "批量增加角色检验标准范围")
    public CommonResult<Boolean> addScopes(@Valid @RequestBody QmsQualityStandardRoleScopeAddReqVO reqVO) {
        roleScopeService.addScopes(reqVO);
        return success(true);
    }

    @DeleteMapping("/remove")
    @Operation(summary = "批量移出角色检验标准范围")
    @Parameter(name = "ids", description = "范围记录ID列表", required = true)
    public CommonResult<Boolean> removeScopes(@RequestParam("ids") List<Long> ids) {
        roleScopeService.removeScopes(ids);
        return success(true);
    }
}

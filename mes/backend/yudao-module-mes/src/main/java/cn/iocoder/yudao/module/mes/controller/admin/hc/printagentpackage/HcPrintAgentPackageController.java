package cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo.HcPrintAgentPackageLatestRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo.HcPrintAgentPackagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo.HcPrintAgentPackageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo.HcPrintAgentPackageSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.printagentpackage.HcPrintAgentPackageDO;
import cn.iocoder.yudao.module.mes.service.hc.printagentpackage.HcPrintAgentPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 安装包版本")
@RestController
@RequestMapping("/mes/hc/print-agent-package")
@Validated
public class HcPrintAgentPackageController {

    @Resource
    private HcPrintAgentPackageService hcPrintAgentPackageService;

    @PostMapping("/create")
    @Operation(summary = "创建安装包版本")
    public CommonResult<Long> createHcPrintAgentPackage(@Valid @RequestBody HcPrintAgentPackageSaveReqVO createReqVO) {
        return success(hcPrintAgentPackageService.createHcPrintAgentPackage(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新安装包版本")
    public CommonResult<Boolean> updateHcPrintAgentPackage(@Valid @RequestBody HcPrintAgentPackageSaveReqVO updateReqVO) {
        hcPrintAgentPackageService.updateHcPrintAgentPackage(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除安装包版本")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteHcPrintAgentPackage(@RequestParam("id") Long id) {
        hcPrintAgentPackageService.deleteHcPrintAgentPackage(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除安装包版本")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteHcPrintAgentPackageList(@RequestParam("ids") List<Long> ids) {
        hcPrintAgentPackageService.deleteHcPrintAgentPackageListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得安装包版本")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcPrintAgentPackageRespVO> getHcPrintAgentPackage(@RequestParam("id") Long id) {
        HcPrintAgentPackageDO entity = hcPrintAgentPackageService.getHcPrintAgentPackage(id);
        return success(BeanUtils.toBean(entity, HcPrintAgentPackageRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得安装包版本分页")
    public CommonResult<PageResult<HcPrintAgentPackageRespVO>> getHcPrintAgentPackagePage(@Valid HcPrintAgentPackagePageReqVO pageReqVO) {
        PageResult<HcPrintAgentPackageDO> pageResult = hcPrintAgentPackageService.getHcPrintAgentPackagePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcPrintAgentPackageRespVO.class));
    }

    @PutMapping("/publish")
    @Operation(summary = "发布为当前安装包版本")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> publishHcPrintAgentPackage(@RequestParam("id") Long id) {
        hcPrintAgentPackageService.publishHcPrintAgentPackage(id);
        return success(true);
    }

    @GetMapping("/latest")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "获得当前发布的安装包版本")
    public CommonResult<HcPrintAgentPackageLatestRespVO> getLatestHcPrintAgentPackage(
            @RequestParam(value = "packageCode", required = false) String packageCode) {
        HcPrintAgentPackageDO entity = hcPrintAgentPackageService.getLatestReleasedPackage(packageCode);
        return success(entity == null ? null : BeanUtils.toBean(entity, HcPrintAgentPackageLatestRespVO.class));
    }

    @GetMapping("/latest/download")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "下载当前发布的安装包版本")
    public void downloadLatestHcPrintAgentPackage(
            @RequestParam(value = "packageCode", required = false) String packageCode,
            HttpServletResponse response) throws IOException {
        HcPrintAgentPackageDO entity = hcPrintAgentPackageService.getLatestReleasedPackage(packageCode);
        if (entity == null || StrUtil.isBlank(entity.getPackageUrl())) {
            writeNotFound(response, "当前没有已发布的安装包");
            return;
        }
        response.sendRedirect(normalizeRedirectUrl(entity.getPackageUrl()));
    }

    private void writeNotFound(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }

    private String normalizeRedirectUrl(String url) {
        String normalized = url.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("/")) {
            return normalized;
        }
        return "/" + normalized;
    }

}

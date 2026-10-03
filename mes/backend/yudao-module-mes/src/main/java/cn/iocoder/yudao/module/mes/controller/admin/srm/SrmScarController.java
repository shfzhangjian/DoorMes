package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmScarPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmScarRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmScarSaveReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmScarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
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

@Tag(name = "管理后台 - SRM供方异常与整改台账")
@RestController
@RequestMapping("/mes/srm/scar")
@Validated
public class SrmScarController {

    @Resource
    private SrmScarService scarService;

    @PostMapping("/create")
    @Operation(summary = "创建供方异常与整改台账")
    public CommonResult<Long> createScar(@Valid @RequestBody SrmScarSaveReqVO reqVO) {
        return success(scarService.createScar(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供方异常与整改台账")
    public CommonResult<Boolean> updateScar(
            @Validated({Default.class, SrmScarSaveReqVO.Update.class})
            @RequestBody SrmScarSaveReqVO reqVO) {
        scarService.updateScar(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供方异常与整改台账")
    public CommonResult<Boolean> deleteScar(@RequestParam("id") Long id) {
        scarService.deleteScar(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供方异常与整改台账")
    public CommonResult<SrmScarRespVO> getScar(@RequestParam("id") Long id) {
        return success(scarService.getScar(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供方异常与整改台账分页")
    public CommonResult<PageResult<SrmScarRespVO>> getScarPage(@Valid SrmScarPageReqVO reqVO) {
        return success(scarService.getScarPage(reqVO));
    }

}

package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmDocumentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmDocumentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmDocumentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmDocumentDO;
import cn.iocoder.yudao.module.mes.service.srm.SrmService;
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

@Tag(name = "管理后台 - SRM通用业务单据")
@RestController
@RequestMapping("/mes/srm/document")
@Validated
public class SrmDocumentController {

    @Resource
    private SrmService srmService;

    @PostMapping("/create")
    @Operation(summary = "创建SRM通用业务单据")
    public CommonResult<Long> createDocument(@Valid @RequestBody SrmDocumentSaveReqVO reqVO) {
        return success(srmService.createDocument(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新SRM通用业务单据")
    public CommonResult<Boolean> updateDocument(
            @Validated({Default.class, SrmDocumentSaveReqVO.Update.class})
            @RequestBody SrmDocumentSaveReqVO reqVO) {
        srmService.updateDocument(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除SRM通用业务单据")
    public CommonResult<Boolean> deleteDocument(@RequestParam("id") Long id) {
        srmService.deleteDocument(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得SRM通用业务单据")
    public CommonResult<SrmDocumentRespVO> getDocument(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(srmService.getDocument(id), SrmDocumentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得SRM通用业务单据分页")
    public CommonResult<PageResult<SrmDocumentRespVO>> getDocumentPage(@Valid SrmDocumentPageReqVO reqVO) {
        PageResult<SrmDocumentDO> pageResult = srmService.getDocumentPage(reqVO);
        return success(BeanUtils.toBean(pageResult, SrmDocumentRespVO.class));
    }

}

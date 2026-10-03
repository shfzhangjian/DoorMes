package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmAttachmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmAttachmentSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmAttachmentVersionUpdateReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM通用附件")
@RestController
@RequestMapping("/mes/srm/attachment")
@Validated
public class SrmAttachmentController {

    @Resource
    private SrmService srmService;

    @PostMapping("/create")
    @Operation(summary = "创建SRM通用附件")
    public CommonResult<Long> createAttachment(@Valid @RequestBody SrmAttachmentSaveReqVO reqVO) {
        return success(srmService.createAttachment(reqVO));
    }

    @PostMapping("/version-update")
    @Operation(summary = "更新SRM附件版本")
    public CommonResult<Long> updateAttachmentVersion(
            @Valid @RequestBody SrmAttachmentVersionUpdateReqVO reqVO) {
        return success(srmService.updateAttachmentVersion(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除SRM通用附件")
    public CommonResult<Boolean> deleteAttachment(@RequestParam("id") Long id) {
        srmService.deleteAttachment(id);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获得SRM通用附件列表")
    public CommonResult<List<SrmAttachmentRespVO>> getAttachmentList(@RequestParam("bizType") String bizType,
            @RequestParam("bizId") Long bizId,
            @RequestParam(value = "includeHistory", defaultValue = "false") Boolean includeHistory) {
        return success(BeanUtils.toBean(
                srmService.getAttachmentList(bizType, bizId, Boolean.TRUE.equals(includeHistory)),
                SrmAttachmentRespVO.class));
    }

}

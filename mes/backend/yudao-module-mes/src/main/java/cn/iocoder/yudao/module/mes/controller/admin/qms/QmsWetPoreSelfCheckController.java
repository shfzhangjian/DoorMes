package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsWetPoreSelfCheckPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsWetPoreSelfCheckPhotoReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsWetPoreSelfCheckRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsWetPoreSelfCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 湿法泡孔自检")
@RestController
@RequestMapping("/mes/quality/wet-pore-self-check")
@Validated
public class QmsWetPoreSelfCheckController {

    @Resource
    private QmsWetPoreSelfCheckService qmsWetPoreSelfCheckService;

    @GetMapping("/page")
    @Operation(summary = "获取湿法泡孔自检分页")
    public CommonResult<PageResult<QmsWetPoreSelfCheckRespVO>> getPage(@Valid QmsWetPoreSelfCheckPageReqVO pageReqVO) {
        return success(qmsWetPoreSelfCheckService.getPage(pageReqVO));
    }

    @PutMapping("/photo")
    @Operation(summary = "保存湿法泡孔自检图片")
    public CommonResult<QmsWetPoreSelfCheckRespVO> updatePhoto(@Valid @RequestBody QmsWetPoreSelfCheckPhotoReqVO reqVO) {
        return success(qmsWetPoreSelfCheckService.updatePhoto(reqVO));
    }

}

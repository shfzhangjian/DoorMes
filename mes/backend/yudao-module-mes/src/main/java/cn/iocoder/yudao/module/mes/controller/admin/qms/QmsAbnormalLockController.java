package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsAbnormalLockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsAbnormalLockRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsAbnormalLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - QMS异常锁定")
@RestController
@RequestMapping("/mes/quality/abnormal-lock")
@Validated
public class QmsAbnormalLockController {

    @Resource
    private QmsAbnormalLockService abnormalLockService;

    @GetMapping("/page")
    @Operation(summary = "分页查询检验NG异常锁定索引")
    public CommonResult<PageResult<QmsAbnormalLockRespVO>> page(@Valid QmsAbnormalLockPageReqVO reqVO) {
        return success(abnormalLockService.getPage(reqVO));
    }
}

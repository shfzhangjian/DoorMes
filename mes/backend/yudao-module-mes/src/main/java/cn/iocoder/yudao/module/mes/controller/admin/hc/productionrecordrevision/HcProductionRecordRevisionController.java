package cn.iocoder.yudao.module.mes.controller.admin.hc.productionrecordrevision;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionrecordrevision.vo.HcProductionRecordRevisionCreateReqVO;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.UPDATE;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 生产记录展示修订")
@RestController
@RequestMapping("/mes/hc/production-record-revision")
@Validated
public class HcProductionRecordRevisionController {

    @Resource
    private HcProductionRecordRevisionService productionRecordRevisionService;

    @PostMapping("/create")
    @Operation(summary = "创建生产记录展示修订")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> create(@Valid @RequestBody HcProductionRecordRevisionCreateReqVO reqVO) {
        productionRecordRevisionService.createRevision(reqVO);
        return success(true);
    }

}

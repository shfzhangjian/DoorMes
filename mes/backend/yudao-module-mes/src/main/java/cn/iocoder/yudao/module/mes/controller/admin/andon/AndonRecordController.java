// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.andon.AndonRecordController.java
package cn.iocoder.yudao.module.mes.controller.admin.andon;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.andon.vo.AndonRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.andon.vo.AndonRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.andon.AndonRecordDO;
import cn.iocoder.yudao.module.mes.service.andon.AndonRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 安灯异常呼叫")
@RestController
@RequestMapping("/mes/andon-record")
@Validated
public class AndonRecordController {

    @Resource
    private AndonRecordService andonRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建安灯呼叫")
    public CommonResult<Long> createAndonRecord(@Valid @RequestBody AndonRecordSaveReqVO createReqVO) {
        return success(andonRecordService.createAndonRecord(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新安灯呼叫(如分配处理人/修改状态)")
    public CommonResult<Boolean> updateAndonRecord(@Valid @RequestBody AndonRecordSaveReqVO updateReqVO) {
        andonRecordService.updateAndonRecord(updateReqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得安灯呼叫分页")
    public CommonResult<PageResult<AndonRecordDO>> getAndonRecordPage(@Valid AndonRecordPageReqVO pageVO) {
        return success(andonRecordService.getAndonRecordPage(pageVO));
    }
}

package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCodeDO;
import cn.iocoder.yudao.module.mes.service.qms.QmsDefectCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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

@Tag(name = "管理后台 - 缺陷代码库")
@RestController
@RequestMapping("/mes/quality/base/defect-code")
@Validated
public class QmsDefectCodeController {

    @Resource
    private QmsDefectCodeService qmsDefectCodeService;

    @PostMapping("/create")
    @Operation(summary = "创建缺陷代码")
    public CommonResult<Long> createDefectCode(@Valid @RequestBody QmsDefectCodeSaveReqVO createReqVO) {
        return success(qmsDefectCodeService.createDefectCode(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新缺陷代码")
    public CommonResult<Boolean> updateDefectCode(@Valid @RequestBody QmsDefectCodeSaveReqVO updateReqVO) {
        qmsDefectCodeService.updateDefectCode(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除缺陷代码")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteDefectCode(@RequestParam("id") Long id) {
        qmsDefectCodeService.deleteDefectCode(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取缺陷代码详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsDefectCodeRespVO> getDefectCode(@RequestParam("id") Long id) {
        return success(qmsDefectCodeService.getDefectCodeResp(id));
    }

    @GetMapping("/list")
    @Operation(summary = "获取缺陷代码列表")
    public CommonResult<List<QmsDefectCodeRespVO>> getDefectCodeList(@Valid QmsDefectCodeListReqVO reqVO) {
        List<QmsDefectCodeDO> list = qmsDefectCodeService.getDefectCodeList(reqVO);
        return success(BeanUtils.toBean(list, QmsDefectCodeRespVO.class));
    }
}

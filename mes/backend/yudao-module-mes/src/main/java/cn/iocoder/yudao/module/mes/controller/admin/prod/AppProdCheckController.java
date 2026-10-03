// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.prod.AppProdCheckController.java
package cn.iocoder.yudao.module.mes.controller.admin.prod;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.prod.vo.ProdCheckPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.prod.vo.ProdCheckSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.prod.ProdCheckDO;
import cn.iocoder.yudao.module.mes.service.prod.ProdCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "移动端 - 生产质检打卡")
@RestController
@RequestMapping("/mes/app/prod-check") // 🚨 App 端路由规范
@Validated
public class AppProdCheckController {

    @Resource
    private ProdCheckService prodCheckService;

    @PostMapping("/create")
    @Operation(summary = "提交现场质检记录")
    public CommonResult<Long> createProdCheck(@Valid @RequestBody ProdCheckSaveReqVO createReqVO) {
        return success(prodCheckService.createProdCheck(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "查询质检历史")
    public CommonResult<PageResult<ProdCheckDO>> getProdCheckPage(@Valid ProdCheckPageReqVO pageVO) {
        return success(prodCheckService.getProdCheckPage(pageVO));
    }
}

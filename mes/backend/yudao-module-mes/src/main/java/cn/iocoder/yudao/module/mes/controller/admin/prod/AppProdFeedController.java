// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.prod.AppProdFeedController.java
package cn.iocoder.yudao.module.mes.controller.admin.prod;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.prod.vo.ProdFeedVOs.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.prod.ProdFeedDO;
import cn.iocoder.yudao.module.mes.service.prod.ProdFeedServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "移动端 - 现场生产投料防呆")
@RestController
@RequestMapping("/mes/app/prod-feed") // 🚨 App 端路由
public class AppProdFeedController {
    @Resource
    private ProdFeedServiceImpl prodFeedService;

    @PostMapping("/create")
    @Operation(summary = "提交物料投料动作")
    public CommonResult<Long> createProdFeed(@Valid @RequestBody SaveReqVO createReqVO) {
        return success(prodFeedService.createProdFeed(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "投料历史记录")
    public CommonResult<PageResult<ProdFeedDO>> getProdFeedPage(@Valid PageReqVO pageVO) {
        return success(prodFeedService.getProdFeedPage(pageVO));
    }
}

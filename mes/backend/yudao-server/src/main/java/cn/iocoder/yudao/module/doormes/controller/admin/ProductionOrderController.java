package cn.iocoder.yudao.module.doormes.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.doormes.order.ProductionOrderService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.doormes.order.ProductionOrderModels.*;

@RestController @Profile("doormes") @RequestMapping("/doormes/production-orders")
public class ProductionOrderController {
    private final ProductionOrderService service;
    public ProductionOrderController(ProductionOrderService service){this.service=service;}
    @GetMapping("/page") @PreAuthorize("@ss.hasAnyPermissions('doormes:production:query','doormes:changes:query')")
    public CommonResult<PageResult<Summary>> page(@RequestParam(required=false) String keyword,@RequestParam(required=false) String type,
        @RequestParam(required=false) String status,@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize){return success(service.page(keyword,type,status,pageNo,pageSize));}
    @GetMapping("/get") @PreAuthorize("@ss.hasAnyPermissions('doormes:production:query','doormes:changes:query')")
    public CommonResult<Order> get(@RequestParam String id,@RequestParam(required=false) Integer revision){return success(service.get(id,revision));}
    @GetMapping("/versions") @PreAuthorize("@ss.hasAnyPermissions('doormes:production:query','doormes:changes:query')")
    public CommonResult<List<Version>> versions(@RequestParam String id){return success(service.versions(id));}
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('doormes:orders:create')")
    public CommonResult<Order> create(@Valid @RequestBody Create input){return success(service.create(input));}
    @PutMapping("/update") @PreAuthorize("@ss.hasPermission('doormes:orders:update')")
    public CommonResult<Order> update(@RequestParam String id,@Valid @RequestBody Update input){return success(service.update(id,input));}
    @PostMapping("/submit") @PreAuthorize("@ss.hasPermission('doormes:orders:submit')")
    public CommonResult<Order> submit(@RequestParam String id,@Valid @RequestBody Action input){return success(service.submit(id,input));}
    @PostMapping("/claim") @PreAuthorize("@ss.hasPermission('doormes:design:claim')")
    public CommonResult<Order> claim(@RequestParam String id,@Valid @RequestBody Action input){return success(service.claim(id,input));}
    @PostMapping("/open-drawing") @PreAuthorize("@ss.hasPermission('doormes:design:drawing-save')")
    public CommonResult<OpenResult> open(@RequestParam String id,@Valid @RequestBody Action input){return success(service.openDrawing(id,input));}
    @PostMapping("/bind-drawing") @PreAuthorize("@ss.hasPermission('doormes:orders:bind')")
    public CommonResult<Order> bind(@RequestParam String id,@Valid @RequestBody Bind input){return success(service.bind(id,input));}
    @PostMapping("/changes/request") @PreAuthorize("@ss.hasPermission('doormes:orders:change-request')")
    public CommonResult<Order> request(@RequestParam String id,@Valid @RequestBody RequestChange input){return success(service.requestChange(id,input));}
    @PostMapping("/changes/review") @PreAuthorize("@ss.hasPermission('doormes:orders:change-review')")
    public CommonResult<Order> review(@RequestParam String id,@Valid @RequestBody Review input){return success(service.review(id,input));}
    @PostMapping("/changes/apply") @PreAuthorize("@ss.hasPermission('doormes:orders:change-apply')")
    public CommonResult<Order> apply(@RequestParam String id,@Valid @RequestBody Apply input){return success(service.apply(id,input));}
}

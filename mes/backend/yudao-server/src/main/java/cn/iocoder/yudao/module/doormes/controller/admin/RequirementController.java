package cn.iocoder.yudao.module.doormes.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.doormes.requirement.RequirementService;
import cn.iocoder.yudao.module.doormes.order.ProductionOrderService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.doormes.requirement.RequirementModels.*;

@RestController
@Profile("doormes")
@RequestMapping("/doormes/requirements")
public class RequirementController {
    private final RequirementService service;
    private final ProductionOrderService orders;
    public RequirementController(RequirementService service,ProductionOrderService orders) {this.service=service;this.orders=orders;}
    @GetMapping("/page")
    @PreAuthorize("@ss.hasAnyPermissions('doormes:orders:query','doormes:design:query')")
    public CommonResult<PageResult<Summary>> page(@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="") String status,
        @RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize) {return success(service.page(keyword,status,pageNo,pageSize));}
    @GetMapping("/get")
    @PreAuthorize("@ss.hasAnyPermissions('doormes:orders:query','doormes:design:query')")
    public CommonResult<Document> get(@RequestParam String id,@RequestParam(required=false) Integer revision) {return success(service.get(id,revision));}
    @GetMapping("/versions")
    @PreAuthorize("@ss.hasAnyPermissions('doormes:orders:query','doormes:design:query')")
    public CommonResult<List<Version>> versions(@RequestParam String id) {return success(service.versions(id));}
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('doormes:orders:create')")
    public CommonResult<Document> create(@Valid @RequestBody Mutation input) {return success(service.create(input));}
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('doormes:orders:update')")
    public CommonResult<Document> update(@RequestParam String id,@Valid @RequestBody Mutation input) {orders.assertIndependentRequirement(id);return success(service.update(id,input));}
    @PostMapping("/submit")
    @PreAuthorize("@ss.hasPermission('doormes:orders:submit')")
    public CommonResult<Document> submit(@RequestParam String id,@Valid @RequestBody Action input) {orders.assertIndependentRequirement(id);return success(service.submit(id,input));}
    @PostMapping("/claim")
    @PreAuthorize("@ss.hasPermission('doormes:design:claim')")
    public CommonResult<Document> claim(@RequestParam String id,@Valid @RequestBody Action input) {orders.assertIndependentRequirement(id);return success(service.claim(id,input));}
}

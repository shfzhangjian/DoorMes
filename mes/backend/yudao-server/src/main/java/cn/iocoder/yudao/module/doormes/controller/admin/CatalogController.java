package cn.iocoder.yudao.module.doormes.controller.admin;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.doormes.catalog.CatalogService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.doormes.catalog.CatalogModels.*;

@RestController @Profile("doormes") @RequestMapping("/doormes/catalog")
public class CatalogController {
    private final CatalogService service;public CatalogController(CatalogService service){this.service=service;}
    @GetMapping("/page") @PreAuthorize("@ss.hasAnyPermissions('doormes:catalog:query','doormes:design:query','doormes:orders:query','doormes:drawings:query')")
    public CommonResult<CatalogService.Page> page(@RequestParam(required=false) String category,@RequestParam(required=false) String keyword,@RequestParam(defaultValue="false") boolean published,@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize){return success(service.page(category,keyword,published,pageNo,pageSize));}
    @GetMapping("/get") @PreAuthorize("@ss.hasAnyPermissions('doormes:catalog:query','doormes:design:query','doormes:orders:query','doormes:drawings:query')")
    public CommonResult<Snapshot> get(@RequestParam String id,@RequestParam(required=false) Integer revision){return success(service.get(id,revision));}
    @GetMapping("/versions") @PreAuthorize("@ss.hasAnyPermissions('doormes:catalog:query','doormes:design:query','doormes:orders:query','doormes:drawings:query')")
    public CommonResult<List<Version>> versions(@RequestParam String id){return success(service.versions(id));}
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('doormes:catalog:edit')")
    public CommonResult<Snapshot> create(@Valid @RequestBody Mutation input){return success(service.create(input));}
    @PutMapping("/revise") @PreAuthorize("@ss.hasPermission('doormes:catalog:edit')")
    public CommonResult<Snapshot> revise(@RequestParam String id,@Valid @RequestBody Mutation input){return success(service.revise(id,input));}
    @PostMapping("/publish") @PreAuthorize("@ss.hasPermission('doormes:catalog:publish')")
    public CommonResult<Snapshot> publish(@RequestParam String id,@Valid @RequestBody Publish input){return success(service.publish(id,input));}
}

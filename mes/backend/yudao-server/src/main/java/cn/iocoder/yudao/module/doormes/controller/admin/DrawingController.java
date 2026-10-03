package cn.iocoder.yudao.module.doormes.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.doormes.drawing.DrawingService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.doormes.drawing.DrawingModels.*;

@RestController @Profile("doormes") @RequestMapping("/doormes/drawings")
public class DrawingController {
    private final DrawingService service;public DrawingController(DrawingService service){this.service=service;}
    @GetMapping("/page") @PreAuthorize("@ss.hasAnyPermissions('doormes:orders:query','doormes:design:query','doormes:drawings:query')")
    public CommonResult<Page> page(@RequestParam(required=false) String keyword,@RequestParam(required=false) String source,
        @RequestParam(defaultValue="false") boolean mine,@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize){
        return success(service.page(keyword,source,mine,pageNo,pageSize));
    }
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('doormes:design:drawing-create')")
    public CommonResult<Snapshot> create(@Valid @RequestBody Create input){return success(service.create(input));}
    @GetMapping("/find") @PreAuthorize("@ss.hasAnyPermissions('doormes:orders:query','doormes:design:query','doormes:drawings:query')")
    public CommonResult<Snapshot> find(@RequestParam String requirementId,@RequestParam String lineId){return success(service.find(requirementId,lineId));}
    @GetMapping("/get") @PreAuthorize("@ss.hasAnyPermissions('doormes:orders:query','doormes:design:query','doormes:drawings:query')")
    public CommonResult<Snapshot> get(@RequestParam String id,@RequestParam(required=false) Integer revision){return success(service.get(id,revision));}
    @GetMapping("/versions") @PreAuthorize("@ss.hasAnyPermissions('doormes:orders:query','doormes:design:query','doormes:drawings:query')")
    public CommonResult<List<Version>> versions(@RequestParam String id){return success(service.versions(id));}
    @PostMapping("/open") @PreAuthorize("@ss.hasPermission('doormes:design:drawing-save')")
    public CommonResult<Snapshot> open(@Valid @RequestBody Open input){return success(service.open(input));}
    @PutMapping("/save") @PreAuthorize("@ss.hasPermission('doormes:design:drawing-save')")
    public CommonResult<Snapshot> save(@RequestParam String id,@Valid @RequestBody Save input){return success(service.save(id,input));}
}

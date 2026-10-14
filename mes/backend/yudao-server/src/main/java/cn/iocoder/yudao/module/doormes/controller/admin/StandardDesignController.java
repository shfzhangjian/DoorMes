package cn.iocoder.yudao.module.doormes.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.doormes.drawing.DrawingModels.Snapshot;
import cn.iocoder.yudao.module.doormes.drawing.StandardDesignService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController @Profile("doormes") @RequestMapping("/doormes/standard-designs")
public class StandardDesignController {
    public record Disable(int revision,@NotBlank String reason) {}
    private final StandardDesignService service;
    public StandardDesignController(StandardDesignService service){this.service=service;}
    @GetMapping("/page") @PreAuthorize("@ss.hasAnyPermissions('doormes:design:query','doormes:design:standard-review','doormes:design:standard-reuse')")
    public CommonResult<StandardDesignService.Page> page(@RequestParam(defaultValue="false") boolean publishedOnly,@RequestParam(required=false) String keyword,
        @RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize){return success(service.page(publishedOnly,keyword,pageNo,pageSize));}
    @PostMapping("/submit") @PreAuthorize("@ss.hasPermission('doormes:design:standard-submit')")
    public CommonResult<StandardDesignService.Entry> submit(@RequestBody StandardDesignService.Submit input){return success(service.submit(input));}
    @PostMapping("/review") @PreAuthorize("@ss.hasPermission('doormes:design:standard-review')")
    public CommonResult<StandardDesignService.Entry> review(@RequestParam String drawingId,@RequestBody StandardDesignService.Decision input){return success(service.review(drawingId,input));}
    @PostMapping("/disable") @PreAuthorize("@ss.hasPermission('doormes:design:standard-review')")
    public CommonResult<StandardDesignService.Entry> disable(@RequestParam String drawingId,@Valid @RequestBody Disable input){return success(service.disable(drawingId,input.revision(),input.reason()));}
    @PostMapping("/reuse") @PreAuthorize("@ss.hasPermission('doormes:design:standard-reuse')")
    public CommonResult<Snapshot> reuse(@RequestBody StandardDesignService.Reuse input){return success(service.reuse(input));}
}

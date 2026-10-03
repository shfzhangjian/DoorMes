package cn.iocoder.yudao.module.doormes.controller.admin;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.doormes.asset.AssetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.doormes.asset.AssetModels.*;

@RestController @Profile("doormes") @RequestMapping("/doormes/visual-assets")
public class AssetController {
    private final AssetService service;private final Semaphore uploadSlots=new Semaphore(2);public AssetController(AssetService service){this.service=service;}
    @PostMapping("/authorize") @ApiAccessLog(requestEnable=false)
    @PreAuthorize("@ss.hasPermission('doormes:catalog:asset-upload')")
    public CommonResult<Grant> authorize(@Valid @RequestBody Authorize input){return success(service.authorize(input));}
    @PutMapping("/uploads/{grantId}") @ApiAccessLog(requestEnable=false,responseEnable=false)
    @PreAuthorize("@ss.hasPermission('doormes:catalog:asset-upload')")
    public CommonResult<Evidence> upload(@PathVariable String grantId,HttpServletRequest request)throws IOException {
        if(!uploadSlots.tryAcquire())throw new ServiceException(503,"资源上传繁忙，请稍后重试");
        try{return success(service.upload(grantId,request.getContentType(),request.getInputStream()));}finally{uploadSlots.release();}
    }
    @GetMapping("/page") @PreAuthorize("@ss.hasAnyPermissions('doormes:catalog:query','doormes:design:query','doormes:orders:query','doormes:drawings:query')")
    public CommonResult<AssetService.Page> page(@RequestParam(required=false)String kind,@RequestParam(required=false)String keyword,@RequestParam(defaultValue="1")int pageNo,@RequestParam(defaultValue="20")int pageSize){return success(service.page(kind,keyword,pageNo,pageSize));}
    @GetMapping("/get") @PreAuthorize("@ss.hasAnyPermissions('doormes:catalog:query','doormes:design:query','doormes:orders:query','doormes:drawings:query')")
    public CommonResult<Descriptor> get(@RequestParam String assetId){return success(service.get(assetId));}
    @GetMapping("/content") @ApiAccessLog(requestEnable=false,responseEnable=false)
    @PreAuthorize("@ss.hasAnyPermissions('doormes:catalog:query','doormes:design:query','doormes:orders:query','doormes:drawings:query')")
    public ResponseEntity<byte[]> content(@RequestParam String assetId,@RequestParam String contentHash){
        Descriptor descriptor=service.get(assetId);byte[] bytes=service.read(assetId,contentHash);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(descriptor.mediaType())).header("X-Content-Type-Options","nosniff").header("Content-Disposition","attachment; filename=\"asset.bin\"").cacheControl(CacheControl.noStore()).body(bytes);
    }
}

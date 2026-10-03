package cn.iocoder.yudao.module.oa.controller.admin.ecology;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologyConfigStatusRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologySendMessageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologySendMessageRespVO;
import cn.iocoder.yudao.module.oa.service.ecology.OaEcologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - OA 泛微消息 MVP")
@RestController
@RequestMapping("/oa/ecology")
public class OaEcologyMessageController {

    @Resource
    private OaEcologyService oaEcologyService;

    @GetMapping("/config-status")
    @Operation(summary = "获得泛微 OA 对接配置状态")
    public CommonResult<OaEcologyConfigStatusRespVO> getConfigStatus() {
        return success(oaEcologyService.getConfigStatus());
    }

    @PostMapping("/messages/send-test")
    @Operation(summary = "发送泛微 OA 测试消息")
    public CommonResult<OaEcologySendMessageRespVO> sendTestMessage(@Valid @RequestBody OaEcologySendMessageReqVO reqVO) {
        return success(oaEcologyService.sendMessage(reqVO));
    }

}

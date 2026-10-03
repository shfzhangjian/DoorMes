package cn.iocoder.yudao.module.oa.api.ecology;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.oa.api.ecology.dto.OaEcologyMessageSendReqDTO;
import cn.iocoder.yudao.module.oa.api.ecology.dto.OaEcologyMessageSendRespDTO;
import cn.iocoder.yudao.module.oa.api.ecology.dto.OaEcologyUserMatchRespDTO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologySendMessageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologySendMessageRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologyUserRespVO;
import cn.iocoder.yudao.module.oa.service.ecology.OaEcologyService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 泛微 OA 消息 API 实现。
 */
@Service
public class OaEcologyMessageApiImpl implements OaEcologyMessageApi {

    @Resource
    private OaEcologyService oaEcologyService;

    @Override
    public OaEcologyMessageSendRespDTO sendMessage(OaEcologyMessageSendReqDTO reqDTO) {
        OaEcologySendMessageReqVO reqVO = BeanUtils.toBean(reqDTO, OaEcologySendMessageReqVO.class);
        OaEcologySendMessageRespVO respVO = oaEcologyService.sendMessage(reqVO);
        return BeanUtils.toBean(respVO, OaEcologyMessageSendRespDTO.class);
    }

    @Override
    public OaEcologyUserMatchRespDTO findEmployeeBySystemUser(String username, String nickname, String mobile) {
        OaEcologyUserRespVO respVO = oaEcologyService.findEmployeeBySystemUser(username, nickname, mobile);
        return BeanUtils.toBean(respVO, OaEcologyUserMatchRespDTO.class);
    }

}

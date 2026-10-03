package cn.iocoder.yudao.module.oa.service.ecology;

import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologyConfigStatusRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologySendMessageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologySendMessageRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologyUserRespVO;

public interface OaEcologyService {

    OaEcologyConfigStatusRespVO getConfigStatus();

    OaEcologySendMessageRespVO sendMessage(OaEcologySendMessageReqVO reqVO);

    OaEcologyUserRespVO findEmployeeBySystemUser(String username, String nickname, String mobile);

}

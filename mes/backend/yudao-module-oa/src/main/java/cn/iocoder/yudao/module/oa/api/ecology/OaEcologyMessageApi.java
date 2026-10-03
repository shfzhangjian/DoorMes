package cn.iocoder.yudao.module.oa.api.ecology;

import cn.iocoder.yudao.module.oa.api.ecology.dto.OaEcologyMessageSendReqDTO;
import cn.iocoder.yudao.module.oa.api.ecology.dto.OaEcologyMessageSendRespDTO;
import cn.iocoder.yudao.module.oa.api.ecology.dto.OaEcologyUserMatchRespDTO;

/**
 * 泛微 OA 消息 API。
 */
public interface OaEcologyMessageApi {

    /**
     * 发送泛微 OA 工作消息。
     *
     * @param reqDTO 消息请求
     * @return 发送结果
     */
    OaEcologyMessageSendRespDTO sendMessage(OaEcologyMessageSendReqDTO reqDTO);

    /**
     * 按 MES 系统用户信息匹配泛微 OA 人员。
     *
     * @param username 系统账号
     * @param nickname 系统昵称
     * @param mobile 手机号
     * @return 匹配到的 OA 人员；未匹配返回 null
     */
    OaEcologyUserMatchRespDTO findEmployeeBySystemUser(String username, String nickname, String mobile);

}

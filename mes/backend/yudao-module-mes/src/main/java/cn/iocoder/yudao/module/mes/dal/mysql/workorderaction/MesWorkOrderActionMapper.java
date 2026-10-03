package cn.iocoder.yudao.module.mes.dal.mysql.workorderaction;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderActionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产工单动作执行实绩 Mapper
 * 对应表: mes_work_order_action
 *
 * @author 芋道源码
 */
@Mapper
public interface MesWorkOrderActionMapper extends BaseMapperX<MesWorkOrderActionDO> {

    // 基础 CRUD 由 BaseMapperX 提供
    // 由于启用了 autoResultMap = true，MyBatis-Plus 会自动处理 JSON 字段的 TypeHandler

}

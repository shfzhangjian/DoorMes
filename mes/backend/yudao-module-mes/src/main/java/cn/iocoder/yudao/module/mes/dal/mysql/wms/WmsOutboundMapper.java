// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/wms/WmsOutboundMapper.java
package cn.iocoder.yudao.module.mes.dal.mysql.wms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsOutboundDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库/领料单 Mapper
 *
 * @author 资深后端架构智能体
 */
@Mapper
public interface WmsOutboundMapper extends BaseMapperX<WmsOutboundDO> {
}

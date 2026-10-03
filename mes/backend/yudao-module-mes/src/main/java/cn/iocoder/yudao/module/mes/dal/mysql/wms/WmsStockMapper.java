// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/wms/WmsStockMapper.java
package cn.iocoder.yudao.module.mes.dal.mysql.wms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsStockDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 实时库存表 Mapper
 *
 * @author 资深后端架构智能体
 */
@Mapper
public interface WmsStockMapper extends BaseMapperX<WmsStockDO> {
}

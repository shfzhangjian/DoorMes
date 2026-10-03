// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/trace/TraceabilityRecordMapper.java
package cn.iocoder.yudao.module.mes.dal.mysql.trace;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.trace.TraceabilityRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 全链路批次追溯谱系 Mapper
 *
 * @author 资深后端架构智能体
 */
@Mapper
public interface TraceabilityRecordMapper extends BaseMapperX<TraceabilityRecordDO> {
}

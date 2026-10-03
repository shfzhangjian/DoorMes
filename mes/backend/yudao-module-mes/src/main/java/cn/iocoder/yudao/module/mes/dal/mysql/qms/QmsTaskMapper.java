// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/qms/QmsTaskMapper.java
package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsTaskDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 质量检验任务 Mapper
 *
 * @author 资深后端架构智能体
 */
@Mapper
public interface QmsTaskMapper extends BaseMapperX<QmsTaskDO> {
}

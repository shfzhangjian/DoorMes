// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/andon/AndonRecordMapper.java
package cn.iocoder.yudao.module.mes.dal.mysql.andon;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.andon.AndonRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 现场Andon异常响应与呼叫 Mapper
 *
 * @author 资深后端架构智能体
 */
@Mapper
public interface AndonRecordMapper extends BaseMapperX<AndonRecordDO> {
    // 基础 CRUD 已通过 BaseMapperX 继承，此处后续可扩展自定义复杂查询
}

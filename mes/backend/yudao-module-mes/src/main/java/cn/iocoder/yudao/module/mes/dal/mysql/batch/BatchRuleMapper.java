// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/batch/BatchRuleMapper.java
package cn.iocoder.yudao.module.mes.dal.mysql.batch;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.batch.BatchRuleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES批次号生成规则 Mapper
 *
 * @author 资深后端架构智能体
 */
@Mapper
public interface BatchRuleMapper extends BaseMapperX<BatchRuleDO> {
}

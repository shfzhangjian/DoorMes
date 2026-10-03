// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/job/JobActionHistoryMapper.java
package cn.iocoder.yudao.module.mes.dal.mysql.job;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.job.JobActionHistoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动态SOP作业执行历史 Mapper
 *
 * @author 资深后端架构智能体
 */
@Mapper
public interface JobActionHistoryMapper extends BaseMapperX<JobActionHistoryDO> {
}

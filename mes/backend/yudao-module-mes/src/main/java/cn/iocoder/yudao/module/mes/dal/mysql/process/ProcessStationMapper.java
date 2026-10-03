package cn.iocoder.yudao.module.mes.dal.mysql.process;

import java.util.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessStationDO;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Mapper
public interface ProcessStationMapper extends BaseMapperX<ProcessStationDO> {

    default List<ProcessStationDO> selectListByProcessId(Long processId) {
        return selectList(ProcessStationDO::getProcessId, processId);
    }

    default int deleteByProcessId(Long processId) {
        return delete(new LambdaQueryWrapper<ProcessStationDO>()
                .eq(ProcessStationDO::getProcessId, processId));
    }
}

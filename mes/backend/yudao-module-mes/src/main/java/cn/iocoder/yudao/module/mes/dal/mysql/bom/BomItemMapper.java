package cn.iocoder.yudao.module.mes.dal.mysql.bom;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.bomitem.BomItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工艺BOM子项 Mapper
 *
 * @author 演示管理员
 */
@Mapper
public interface BomItemMapper extends BaseMapperX<BomItemDO> {

    default List<BomItemDO> selectListByBomId(Long bomId) {
        return selectList(BomItemDO::getBomId, bomId);
    }

    default int deleteByBomId(Long bomId) {
        return delete(BomItemDO::getBomId, bomId);
    }

	default int deleteByBomIds(List<Long> bomIds) {
	    return deleteBatch(BomItemDO::getBomId, bomIds);
	}

}

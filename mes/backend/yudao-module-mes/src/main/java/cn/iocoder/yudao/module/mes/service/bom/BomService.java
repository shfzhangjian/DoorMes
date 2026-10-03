package cn.iocoder.yudao.module.mes.service.bom;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.mes.controller.admin.bom.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.bom.BomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.bomitem.BomItemDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

/**
 * 工艺BOM主表 Service 接口
 *
 * @author 演示管理员
 */
public interface BomService {

    /**
     * 创建工艺BOM主表
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createBom(@Valid BomSaveReqVO createReqVO);

    /**
     * 更新工艺BOM主表
     *
     * @param updateReqVO 更新信息
     */
    void updateBom(@Valid BomSaveReqVO updateReqVO);

    /**
     * 删除工艺BOM主表
     *
     * @param id 编号
     */
    void deleteBom(Long id);

    /**
    * 批量删除工艺BOM主表
    *
    * @param ids 编号
    */
    void deleteBomListByIds(List<Long> ids);

    /**
     * 获得工艺BOM主表
     *
     * @param id 编号
     * @return 工艺BOM主表
     */
    BomDO getBom(Long id);

    /**
     * 获得工艺BOM主表分页
     *
     * @param pageReqVO 分页查询
     * @return 工艺BOM主表分页
     */
    PageResult<BomDO> getBomPage(BomPageReqVO pageReqVO);

    // ==================== 子表（工艺BOM子项） ====================

    /**
     * 获得工艺BOM子项列表
     *
     * @param bomId BOM主表ID
     * @return 工艺BOM子项列表
     */
    List<BomItemDO> getBomItemListByBomId(Long bomId);

}

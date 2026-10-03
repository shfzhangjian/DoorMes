package cn.iocoder.yudao.module.mes.service.bom;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.mes.controller.admin.bom.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.bom.BomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.bomitem.BomItemDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.mes.dal.mysql.bom.BomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.bom.BomItemMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * 工艺BOM主表 Service 实现类
 *
 * @author 演示管理员
 */
@Service
@Validated
public class BomServiceImpl implements BomService {

    @Resource
    private BomMapper bomMapper;
    @Resource
    private BomItemMapper bomItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBom(BomSaveReqVO createReqVO) {
        // 插入
        BomDO bom = BeanUtils.toBean(createReqVO, BomDO.class);
        bomMapper.insert(bom);


        // 插入子表
        createBomItemList(bom.getId(), createReqVO.getBomItems());
        // 返回
        return bom.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBom(BomSaveReqVO updateReqVO) {
        // 校验存在
        validateBomExists(updateReqVO.getId());
        // 更新
        BomDO updateObj = BeanUtils.toBean(updateReqVO, BomDO.class);
        bomMapper.updateById(updateObj);

        // 更新子表
        updateBomItemList(updateReqVO.getId(), updateReqVO.getBomItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBom(Long id) {
        // 校验存在
        validateBomExists(id);
        // 删除
        bomMapper.deleteById(id);

        // 删除子表
        deleteBomItemByBomId(id);
    }

    @Override
        @Transactional(rollbackFor = Exception.class)
    public void deleteBomListByIds(List<Long> ids) {
        // 删除
        bomMapper.deleteByIds(ids);

    // 删除子表
            deleteBomItemByBomIds(ids);
    }


    private void validateBomExists(Long id) {
        if (bomMapper.selectById(id) == null) {
            throw exception(BOM_NOT_EXISTS);
        }
    }

    @Override
    public BomDO getBom(Long id) {
        return bomMapper.selectById(id);
    }

    @Override
    public PageResult<BomDO> getBomPage(BomPageReqVO pageReqVO) {
        return bomMapper.selectPage(pageReqVO);
    }

    // ==================== 子表（工艺BOM子项） ====================

    @Override
    public List<BomItemDO> getBomItemListByBomId(Long bomId) {
        return bomItemMapper.selectListByBomId(bomId);
    }

    private void createBomItemList(Long bomId, List<BomItemDO> list) {
        list.forEach(o -> o.setBomId(bomId).clean());
        bomItemMapper.insertBatch(list);
    }

    private void updateBomItemList(Long bomId, List<BomItemDO> list) {
	    list.forEach(o -> o.setBomId(bomId).clean());
	    List<BomItemDO> oldList = bomItemMapper.selectListByBomId(bomId);
	    List<List<BomItemDO>> diffList = diffList(oldList, list, (oldVal, newVal) -> {
            boolean same = ObjectUtil.equal(oldVal.getId(), newVal.getId());
            if (same) {
                newVal.setId(oldVal.getId()).clean(); // 解决更新情况下：updateTime 不更新
            }
            return same;
	    });

	    // 第二步，批量添加、修改、删除
	    if (CollUtil.isNotEmpty(diffList.get(0))) {
	        bomItemMapper.insertBatch(diffList.get(0));
	    }
	    if (CollUtil.isNotEmpty(diffList.get(1))) {
	        bomItemMapper.updateBatch(diffList.get(1));
	    }
	    if (CollUtil.isNotEmpty(diffList.get(2))) {
	        bomItemMapper.deleteByIds(convertList(diffList.get(2), BomItemDO::getId));
	    }
    }

    private void deleteBomItemByBomId(Long bomId) {
        bomItemMapper.deleteByBomId(bomId);
    }

	private void deleteBomItemByBomIds(List<Long> bomIds) {
        bomItemMapper.deleteByBomIds(bomIds);
	}

}

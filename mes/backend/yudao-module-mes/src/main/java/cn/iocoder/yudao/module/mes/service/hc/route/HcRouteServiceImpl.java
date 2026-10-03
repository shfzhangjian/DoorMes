package cn.iocoder.yudao.module.mes.service.hc.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRoutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.workcenter.HcWorkCenterDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.route.HcRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.route.HcRouteOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.workcenter.HcWorkCenterMapper;
import jakarta.annotation.Resource;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCROUTE_ROUTECODE_EXISTS;

@Service
@Validated
public class HcRouteServiceImpl implements HcRouteService {

    @Resource
    private HcRouteMapper hcRouteMapper;

    @Resource
    private HcRouteOperationMapper hcRouteOperationMapper;

    @Resource
    private HcWorkCenterMapper hcWorkCenterMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcRoute(HcRouteSaveReqVO createReqVO) {
        validateRouteCodeUnique(null, createReqVO.getRouteCode());
        HcRouteDO entity = BeanUtils.toBean(createReqVO, HcRouteDO.class);
        hcRouteMapper.insert(entity);
        createHcRouteOperationList(entity.getId(), entity.getRouteCode(), createReqVO.getRouteOperations());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcRoute(HcRouteSaveReqVO updateReqVO) {
        validateHcRouteExists(updateReqVO.getId());
        validateRouteCodeUnique(updateReqVO.getId(), updateReqVO.getRouteCode());
        HcRouteDO updateObj = BeanUtils.toBean(updateReqVO, HcRouteDO.class);
        hcRouteMapper.updateById(updateObj);
        updateHcRouteOperationList(updateReqVO.getId(), updateReqVO.getRouteCode(), updateReqVO.getRouteOperations());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcRoute(Long id) {
        validateHcRouteExists(id);
        hcRouteMapper.deleteById(id);
        hcRouteOperationMapper.deleteByParentId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcRouteListByIds(List<Long> ids) {
        hcRouteMapper.deleteByIds(ids);
        hcRouteOperationMapper.deleteByParentIds(ids);
    }

    private void validateHcRouteExists(Long id) {
        if (hcRouteMapper.selectById(id) == null) {
            throw exception(HCROUTE_NOT_EXISTS);
        }
    }

    private void validateRouteCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcRouteDO entity = hcRouteMapper.selectOne(new LambdaQueryWrapperX<HcRouteDO>().eq(HcRouteDO::getRouteCode, value).neIfPresent(HcRouteDO::getId, id));
        if (entity != null) {
            throw exception(HCROUTE_ROUTECODE_EXISTS);
        }
    }

    @Override
    public HcRouteDO getHcRoute(Long id) {
        return hcRouteMapper.selectById(id);
    }

    @Override
    public List<HcRouteDO> getHcRouteSimpleList() {
        LambdaQueryWrapperX<HcRouteDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcRouteDO::getStatus, 1);
        queryWrapper.orderByAsc(HcRouteDO::getRouteCode);
        queryWrapper.orderByDesc(HcRouteDO::getId);
        return hcRouteMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcRouteDO> getHcRouteSimpleListByMaterialId(Long materialId) {
        if (materialId == null) {
            return List.of();
        }
        LambdaQueryWrapperX<HcRouteDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcRouteDO::getStatus, 1);
        queryWrapper.and(wrapper -> wrapper
                .eq(HcRouteDO::getProductMaterialId, materialId)
                .or()
                .eq(HcRouteDO::getApplicableScope, "GLOBAL")
                .or()
                .isNull(HcRouteDO::getProductMaterialId));
        queryWrapper.orderByAsc(HcRouteDO::getRouteCode);
        queryWrapper.orderByDesc(HcRouteDO::getId);
        return hcRouteMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcRouteDO> getHcRouteList(HcRoutePageReqVO reqVO) {
        return hcRouteMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcRouteDO> getHcRoutePage(HcRoutePageReqVO pageReqVO) {
        return hcRouteMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcRouteOperationDO> getHcRouteOperationListByParentId(Long parentId) {
        return hcRouteOperationMapper.selectListByParentId(parentId);
    }

    private void createHcRouteOperationList(Long parentId, String routeCode, List<HcRouteOperationDO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list = normalizeRouteOperationOrder(list);
        list.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setRouteId(parentId);
            item.setRouteCode(routeCode);
            normalizeRouteOperationUnit(item);
        });
        hcRouteOperationMapper.insertBatch(list);
    }

    private void updateHcRouteOperationList(Long parentId, String routeCode, List<HcRouteOperationDO> list) {
        List<HcRouteOperationDO> dbList = hcRouteOperationMapper.selectListByParentId(parentId);
        if (list == null) {
            list = List.of();
        }
        list = normalizeRouteOperationOrder(list);

        Set<Long> reqIds = list.stream()
                .map(HcRouteOperationDO::getId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> deleteIds = dbList.stream()
                .map(HcRouteOperationDO::getId)
                .filter(id -> !reqIds.contains(id))
                .collect(Collectors.toSet());
        if (!deleteIds.isEmpty()) {
            hcRouteOperationMapper.deleteBatch(HcRouteOperationDO::getId, deleteIds);
        }

        List<HcRouteOperationDO> updateList = list.stream()
                .filter(item -> item.getId() != null && item.getId() > 0)
                .peek(item -> {
                    item.clean();
                    item.setRouteId(parentId);
                    item.setRouteCode(routeCode);
                    normalizeRouteOperationUnit(item);
                })
                .toList();
        if (!updateList.isEmpty()) {
            hcRouteOperationMapper.updateBatch(updateList);
        }

        List<HcRouteOperationDO> createList = list.stream()
                .filter(item -> item.getId() == null || item.getId() <= 0)
                .peek(item -> {
                    item.clean();
                    item.setId(null);
                    item.setRouteId(parentId);
                    item.setRouteCode(routeCode);
                    normalizeRouteOperationUnit(item);
                })
                .toList();
        if (!createList.isEmpty()) {
            hcRouteOperationMapper.insertBatch(createList);
        }
    }

    private List<HcRouteOperationDO> normalizeRouteOperationOrder(List<HcRouteOperationDO> list) {
        List<HcRouteOperationDO> orderedList = list.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(this::resolveRouteOperationSeqNo))
                .toList();
        for (int i = 0; i < orderedList.size(); i++) {
            HcRouteOperationDO item = orderedList.get(i);
            if (item.getSeqNo() == null || item.getSeqNo() <= 0) {
                item.setSeqNo(i + 1);
            }
        }
        return orderedList;
    }

    private int resolveRouteOperationSeqNo(HcRouteOperationDO item) {
        return item.getSeqNo() == null || item.getSeqNo() <= 0 ? Integer.MAX_VALUE : item.getSeqNo();
    }

    private void normalizeRouteOperationUnit(HcRouteOperationDO item) {
        normalizeRouteOperationWorkCenter(item);
        if (item.getOutputUom() == null || item.getOutputUom().isBlank()) {
            item.setOutputUom(item.getOutputUnitCode());
        }
    }

    private void normalizeRouteOperationWorkCenter(HcRouteOperationDO item) {
        HcWorkCenterDO workCenter = null;
        if (item.getWorkCenterId() != null) {
            workCenter = hcWorkCenterMapper.selectById(item.getWorkCenterId());
        }
        if (workCenter == null && item.getWorkCenterCode() != null && !item.getWorkCenterCode().isBlank()) {
            workCenter = hcWorkCenterMapper.selectOne(new LambdaQueryWrapperX<HcWorkCenterDO>()
                    .eq(HcWorkCenterDO::getWcCode, item.getWorkCenterCode()));
        }
        if (workCenter == null) {
            return;
        }
        String operationCode = firstNotBlank(workCenter.getWcCode(), item.getWorkCenterCode(), item.getOperationCode());
        String operationName = firstNotBlank(workCenter.getProcessName(), workCenter.getProcessStage(), item.getOperationName(), workCenter.getWcName());
        item.setWorkCenterId(workCenter.getId());
        item.setWorkCenterCode(workCenter.getWcCode());
        item.setWorkCenterName(workCenter.getWcName());
        item.setOperationCode(operationCode);
        item.setOperationName(operationName);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

}

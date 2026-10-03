package cn.iocoder.yudao.module.mes.service.hc.formtemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate.HcFormTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate.HcFormTemplateVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.formtemplate.HcFormTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.formtemplate.HcFormTemplateVersionMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFORMTEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFORMTEMPLATE_TEMPLATECODE_EXISTS;

@Service
@Validated
public class HcFormTemplateServiceImpl implements HcFormTemplateService {

    @Resource
    private HcFormTemplateMapper hcFormTemplateMapper;

    @Resource
    private HcFormTemplateVersionMapper hcFormTemplateVersionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcFormTemplate(HcFormTemplateSaveReqVO createReqVO) {
        validateTemplateCodeUnique(null, createReqVO.getTemplateCode());
        HcFormTemplateDO entity = BeanUtils.toBean(createReqVO, HcFormTemplateDO.class);
        hcFormTemplateMapper.insert(entity);
        createHcFormTemplateVersionList(entity.getId(), createReqVO.getTemplateVersions());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcFormTemplate(HcFormTemplateSaveReqVO updateReqVO) {
        validateHcFormTemplateExists(updateReqVO.getId());
        validateTemplateCodeUnique(updateReqVO.getId(), updateReqVO.getTemplateCode());
        HcFormTemplateDO updateObj = BeanUtils.toBean(updateReqVO, HcFormTemplateDO.class);
        hcFormTemplateMapper.updateById(updateObj);
        updateHcFormTemplateVersionList(updateReqVO.getId(), updateReqVO.getTemplateVersions());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcFormTemplate(Long id) {
        validateHcFormTemplateExists(id);
        hcFormTemplateMapper.deleteById(id);
        hcFormTemplateVersionMapper.deleteByParentId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcFormTemplateListByIds(List<Long> ids) {
        hcFormTemplateMapper.deleteByIds(ids);
        hcFormTemplateVersionMapper.deleteByParentIds(ids);
    }

    private void validateHcFormTemplateExists(Long id) {
        if (hcFormTemplateMapper.selectById(id) == null) {
            throw exception(HCFORMTEMPLATE_NOT_EXISTS);
        }
    }

    private void validateTemplateCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcFormTemplateDO entity = hcFormTemplateMapper.selectOne(new LambdaQueryWrapperX<HcFormTemplateDO>().eq(HcFormTemplateDO::getTemplateCode, value).neIfPresent(HcFormTemplateDO::getId, id));
        if (entity != null) {
            throw exception(HCFORMTEMPLATE_TEMPLATECODE_EXISTS);
        }
    }

    @Override
    public HcFormTemplateDO getHcFormTemplate(Long id) {
        return hcFormTemplateMapper.selectById(id);
    }

    @Override
    public List<HcFormTemplateDO> getHcFormTemplateSimpleList() {
        LambdaQueryWrapperX<HcFormTemplateDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcFormTemplateDO::getStatus, "启用");
        queryWrapper.orderByAsc(HcFormTemplateDO::getTemplateCode);
        queryWrapper.orderByDesc(HcFormTemplateDO::getId);
        return hcFormTemplateMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcFormTemplateDO> getHcFormTemplateList(HcFormTemplatePageReqVO reqVO) {
        return hcFormTemplateMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcFormTemplateDO> getHcFormTemplatePage(HcFormTemplatePageReqVO pageReqVO) {
        return hcFormTemplateMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcFormTemplateVersionDO> getHcFormTemplateVersionListByParentId(Long parentId) {
        return hcFormTemplateVersionMapper.selectListByParentId(parentId);
    }

    private void createHcFormTemplateVersionList(Long parentId, List<HcFormTemplateVersionDO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(item -> item.setTemplateId(parentId));
        hcFormTemplateVersionMapper.insertBatch(list);
    }

    private void updateHcFormTemplateVersionList(Long parentId, List<HcFormTemplateVersionDO> list) {
        hcFormTemplateVersionMapper.deleteByParentId(parentId);
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(item -> { item.clean(); item.setTemplateId(parentId); });
        hcFormTemplateVersionMapper.insertBatch(list);
    }

}
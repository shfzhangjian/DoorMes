package cn.iocoder.yudao.module.mes.service.hc.ocaptemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.ocaptemplate.HcOcapTemplateDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.ocaptemplate.HcOcapTemplateMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOCAPTEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOCAPTEMPLATE_OCAPCODE_EXISTS;

@Service
@Validated
public class HcOcapTemplateServiceImpl implements HcOcapTemplateService {

    @Resource
    private HcOcapTemplateMapper hcOcapTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcOcapTemplate(HcOcapTemplateSaveReqVO createReqVO) {
        validateOcapCodeUnique(null, createReqVO.getOcapCode());
        HcOcapTemplateDO entity = BeanUtils.toBean(createReqVO, HcOcapTemplateDO.class);
        hcOcapTemplateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcOcapTemplate(HcOcapTemplateSaveReqVO updateReqVO) {
        validateHcOcapTemplateExists(updateReqVO.getId());
        validateOcapCodeUnique(updateReqVO.getId(), updateReqVO.getOcapCode());
        HcOcapTemplateDO updateObj = BeanUtils.toBean(updateReqVO, HcOcapTemplateDO.class);
        hcOcapTemplateMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcOcapTemplate(Long id) {
        validateHcOcapTemplateExists(id);
        hcOcapTemplateMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcOcapTemplateListByIds(List<Long> ids) {
        hcOcapTemplateMapper.deleteByIds(ids);
    }

    private void validateHcOcapTemplateExists(Long id) {
        if (hcOcapTemplateMapper.selectById(id) == null) {
            throw exception(HCOCAPTEMPLATE_NOT_EXISTS);
        }
    }

    private void validateOcapCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcOcapTemplateDO entity = hcOcapTemplateMapper.selectOne(new LambdaQueryWrapperX<HcOcapTemplateDO>().eq(HcOcapTemplateDO::getOcapCode, value).neIfPresent(HcOcapTemplateDO::getId, id));
        if (entity != null) {
            throw exception(HCOCAPTEMPLATE_OCAPCODE_EXISTS);
        }
    }

    @Override
    public HcOcapTemplateDO getHcOcapTemplate(Long id) {
        return hcOcapTemplateMapper.selectById(id);
    }

    @Override
    public List<HcOcapTemplateDO> getHcOcapTemplateSimpleList() {
        LambdaQueryWrapperX<HcOcapTemplateDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcOcapTemplateDO::getStatus, "启用");
        queryWrapper.orderByAsc(HcOcapTemplateDO::getOcapCode);
        queryWrapper.orderByDesc(HcOcapTemplateDO::getId);
        return hcOcapTemplateMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcOcapTemplateDO> getHcOcapTemplateList(HcOcapTemplatePageReqVO reqVO) {
        return hcOcapTemplateMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcOcapTemplateDO> getHcOcapTemplatePage(HcOcapTemplatePageReqVO pageReqVO) {
        return hcOcapTemplateMapper.selectPage(pageReqVO);
    }

}
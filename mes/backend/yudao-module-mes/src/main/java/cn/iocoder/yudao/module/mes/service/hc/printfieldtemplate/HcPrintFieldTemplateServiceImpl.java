package cn.iocoder.yudao.module.mes.service.hc.printfieldtemplate;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateItemSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.printfieldtemplate.HcPrintFieldTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.printfieldtemplate.HcPrintFieldTemplateItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.printfieldtemplate.HcPrintFieldTemplateItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.printfieldtemplate.HcPrintFieldTemplateMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRINTFIELDTEMPLATE_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRINTFIELDTEMPLATE_NOT_EXISTS;

@Service
@Validated
public class HcPrintFieldTemplateServiceImpl implements HcPrintFieldTemplateService {

    @Resource
    private HcPrintFieldTemplateMapper hcPrintFieldTemplateMapper;
    @Resource
    private HcPrintFieldTemplateItemMapper hcPrintFieldTemplateItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcPrintFieldTemplate(HcPrintFieldTemplateSaveReqVO createReqVO) {
        validateTemplateCodeUnique(null, createReqVO.getTemplateCode());
        HcPrintFieldTemplateDO entity = BeanUtils.toBean(createReqVO, HcPrintFieldTemplateDO.class);
        normalizeTemplate(entity);
        hcPrintFieldTemplateMapper.insert(entity);
        replaceItems(entity.getId(), createReqVO.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcPrintFieldTemplate(HcPrintFieldTemplateSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateTemplateCodeUnique(updateReqVO.getId(), updateReqVO.getTemplateCode());
        HcPrintFieldTemplateDO updateObj = BeanUtils.toBean(updateReqVO, HcPrintFieldTemplateDO.class);
        normalizeTemplate(updateObj);
        hcPrintFieldTemplateMapper.updateById(updateObj);
        replaceItems(updateReqVO.getId(), updateReqVO.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcPrintFieldTemplate(Long id) {
        validateExists(id);
        hcPrintFieldTemplateItemMapper.physicalDeleteByTemplateId(id);
        hcPrintFieldTemplateMapper.deleteById(id);
    }

    @Override
    public HcPrintFieldTemplateDO getHcPrintFieldTemplate(Long id) {
        return hcPrintFieldTemplateMapper.selectById(id);
    }

    @Override
    public HcPrintFieldTemplateRespVO getHcPrintFieldTemplateDetail(Long id) {
        HcPrintFieldTemplateDO entity = validateExists(id);
        HcPrintFieldTemplateRespVO respVO = BeanUtils.toBean(entity, HcPrintFieldTemplateRespVO.class);
        respVO.setItems(getItemsByTemplateId(id));
        return respVO;
    }

    @Override
    public PageResult<HcPrintFieldTemplateDO> getHcPrintFieldTemplatePage(HcPrintFieldTemplatePageReqVO pageReqVO) {
        return hcPrintFieldTemplateMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcPrintFieldTemplateItemRespVO> getItemsByTemplateId(Long templateId) {
        validateExists(templateId);
        return BeanUtils.toBean(hcPrintFieldTemplateItemMapper.selectListByTemplateId(templateId),
                HcPrintFieldTemplateItemRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItems(Long templateId, List<HcPrintFieldTemplateItemSaveReqVO> items) {
        validateExists(templateId);
        replaceItems(templateId, items);
    }

    @Override
    public HcPrintFieldTemplateRespVO getActiveTemplate(String templateCode) {
        HcPrintFieldTemplateDO entity = hcPrintFieldTemplateMapper.selectActiveByTemplateCode(templateCode);
        if (entity == null) {
            return null;
        }
        HcPrintFieldTemplateRespVO respVO = BeanUtils.toBean(entity, HcPrintFieldTemplateRespVO.class);
        respVO.setItems(BeanUtils.toBean(hcPrintFieldTemplateItemMapper.selectVisibleListByTemplateId(entity.getId()),
                HcPrintFieldTemplateItemRespVO.class));
        return respVO;
    }

    private HcPrintFieldTemplateDO validateExists(Long id) {
        HcPrintFieldTemplateDO entity = hcPrintFieldTemplateMapper.selectById(id);
        if (entity == null) {
            throw exception(HCPRINTFIELDTEMPLATE_NOT_EXISTS);
        }
        return entity;
    }

    private void validateTemplateCodeUnique(Long id, String templateCode) {
        HcPrintFieldTemplateDO entity = hcPrintFieldTemplateMapper.selectByTemplateCode(templateCode);
        if (entity != null && !entity.getId().equals(id)) {
            throw exception(HCPRINTFIELDTEMPLATE_CODE_EXISTS);
        }
    }

    private void normalizeTemplate(HcPrintFieldTemplateDO entity) {
        entity.setTemplateCode(StrUtil.trim(entity.getTemplateCode()));
        entity.setTemplateName(StrUtil.trim(entity.getTemplateName()));
        entity.setProcessCode(StrUtil.trim(entity.getProcessCode()));
        entity.setProcessName(StrUtil.trim(entity.getProcessName()));
        entity.setDocumentType(StrUtil.trim(entity.getDocumentType()));
        entity.setDocumentName(StrUtil.trim(entity.getDocumentName()));
        entity.setStatus(entity.getStatus() == null ? 0 : entity.getStatus());
    }

    private void replaceItems(Long templateId, List<HcPrintFieldTemplateItemSaveReqVO> items) {
        List<HcPrintFieldTemplateItemSaveReqVO> orderedItems = normalizeItems(items);
        hcPrintFieldTemplateItemMapper.physicalDeleteByTemplateId(templateId);
        if (orderedItems.isEmpty()) {
            return;
        }
        for (HcPrintFieldTemplateItemSaveReqVO item : orderedItems) {
            HcPrintFieldTemplateItemDO entity = BeanUtils.toBean(item, HcPrintFieldTemplateItemDO.class);
            entity.setId(null);
            entity.setTemplateId(templateId);
            hcPrintFieldTemplateItemMapper.insert(entity);
        }
    }

    private List<HcPrintFieldTemplateItemSaveReqVO> normalizeItems(List<HcPrintFieldTemplateItemSaveReqVO> items) {
        if (CollUtil.isEmpty(items)) {
            return List.of();
        }
        List<HcPrintFieldTemplateItemSaveReqVO> orderedItems = new ArrayList<>(items);
        orderedItems.sort(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()));
        Set<String> fieldKeys = new HashSet<>();
        for (int i = 0; i < orderedItems.size(); i++) {
            HcPrintFieldTemplateItemSaveReqVO item = orderedItems.get(i);
            item.setFieldKey(StrUtil.trim(item.getFieldKey()));
            item.setFieldLabel(StrUtil.trim(item.getFieldLabel()));
            item.setValueKey(StrUtil.trim(item.getValueKey()));
            item.setDefaultValue(StrUtil.trim(item.getDefaultValue()));
            item.setFormatType(StrUtil.blankToDefault(StrUtil.trim(item.getFormatType()), "TEXT"));
            item.setSuffix(StrUtil.trim(item.getSuffix()));
            item.setRemark(StrUtil.trim(item.getRemark()));
            item.setSort(item.getSort() == null ? i + 1 : item.getSort());
            item.setVisible(item.getVisible() == null || item.getVisible());
            if (StrUtil.isBlank(item.getFieldKey())) {
                throw invalidParamException("字段明细第 {} 行字段编码不能为空", i + 1);
            }
            if (StrUtil.isBlank(item.getFieldLabel())) {
                throw invalidParamException("字段明细第 {} 行显示名称不能为空", i + 1);
            }
            if (StrUtil.isBlank(item.getValueKey())) {
                throw invalidParamException("字段明细第 {} 行取值字段不能为空", i + 1);
            }
            if (!fieldKeys.add(item.getFieldKey())) {
                throw invalidParamException("字段编码重复：{}", item.getFieldKey());
            }
        }
        return orderedItems;
    }

}

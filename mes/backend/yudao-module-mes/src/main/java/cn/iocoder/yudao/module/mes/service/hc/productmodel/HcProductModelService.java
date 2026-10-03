package cn.iocoder.yudao.module.mes.service.hc.productmodel;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelSegmentDO;
import java.util.List;

public interface HcProductModelService {

    Long createHcProductModel(HcProductModelSaveReqVO createReqVO);

    void updateHcProductModel(HcProductModelSaveReqVO updateReqVO);

    void deleteHcProductModel(Long id);

    void deleteHcProductModelListByIds(List<Long> ids);

    HcProductModelDO getHcProductModel(Long id);

    HcProductModelDO getHcProductModelByCode(String modelCode);

    PageResult<HcProductModelDO> getHcProductModelPage(HcProductModelPageReqVO pageReqVO);

    List<HcProductModelDO> getHcProductModelList(HcProductModelPageReqVO reqVO);

    List<HcProductModelSegmentDO> getSegmentListByModelId(Long modelId);

    List<HcProductModelMaterialDO> getMaterialListByModelId(Long modelId);

    String generateModelCode(HcProductModelGenerateReqVO reqVO);

    List<HcProductModelSelectOptionRespVO> getSelectOptions(String keyword, String modelLevel);

}

package cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision;

import cn.iocoder.yudao.module.mes.controller.admin.hc.productionrecordrevision.vo.HcProductionRecordRevisionCreateReqVO;
import java.util.List;

public interface HcProductionRecordRevisionService {

    void createRevision(HcProductionRecordRevisionCreateReqVO reqVO);

    /**
     * 将指定模块的最新展示修订覆盖到查询结果中。
     * 原始业务表不参与更新。
     */
    <T> List<T> applyRevisions(String moduleCode, List<T> rows);

}

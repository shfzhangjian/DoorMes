package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierMaskFieldRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierMaskFieldSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierScopePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierScopeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierScopeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.supplier.vo.MesSupplierPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.supplier.vo.MesSupplierRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;

public interface SrmSupplierScopeService {

    String PERMISSION_MASKED = "MASKED";
    String PERMISSION_FULL = "FULL";
    String PERMISSION_EDIT = "EDIT";

    Long createScope(@Valid SrmSupplierScopeSaveReqVO reqVO);

    void updateScope(@Valid SrmSupplierScopeSaveReqVO reqVO);

    void deleteScope(Long id);

    SrmSupplierScopeRespVO getScope(Long id);

    PageResult<SrmSupplierScopeRespVO> getScopePage(SrmSupplierScopePageReqVO reqVO);

    List<SrmSupplierScopeRespVO> getSimpleScopeList();

    List<SrmSupplierMaskFieldRespVO> getMaskFields();

    void updateMaskFields(@Valid SrmSupplierMaskFieldSaveReqVO reqVO);

    void applySupplierScopeFilter(MesSupplierPageReqVO reqVO);

    Collection<Long> getCurrentAccessibleScopeIds();

    MesSupplierRespVO buildSupplierResp(MesSupplierDO supplier);

    void assertSupplierVisible(MesSupplierDO supplier);

    void assertSupplierEditable(MesSupplierDO supplier);

    boolean isSupplierSuperAdmin(Long userId);

}

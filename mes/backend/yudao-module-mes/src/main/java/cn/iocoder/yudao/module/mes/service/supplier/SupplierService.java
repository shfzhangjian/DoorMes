package cn.iocoder.yudao.module.mes.service.supplier;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.mes.controller.admin.supplier.vo.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 供应商主数据 Service 接口
 *
 * @author 演示管理员
 */
public interface SupplierService {

    /**
     * 创建供应商主数据
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSupplier(@Valid MesSupplierSaveReqVO createReqVO);

    /**
     * 更新供应商主数据
     *
     * @param updateReqVO 更新信息
     */
    void updateSupplier(@Valid MesSupplierSaveReqVO updateReqVO);

    /**
     * 删除供应商主数据
     *
     * @param id 编号
     */
    void deleteSupplier(Long id);

    /**
    * 批量删除供应商主数据
    *
    * @param ids 编号
    */
    void deleteSupplierListByIds(List<Long> ids);

    /**
     * 获得供应商主数据
     *
     * @param id 编号
     * @return 供应商主数据
     */
    MesSupplierRespVO getSupplier(Long id);

    /**
     * 获得供应商主数据分页
     *
     * @param pageReqVO 分页查询
     * @return 供应商主数据分页
     */
    PageResult<MesSupplierRespVO> getSupplierPage(MesSupplierPageReqVO pageReqVO);

    /**
     * 构建供应商名录导入模板。导入日期由使用者按 {@code yyyy/M/d} 填写。
     *
     * @return 模板数据行
     */
    List<MesSupplierImportExcelVO> buildImportTemplate();

    /**
     * 批量导入供应商名录。
     *
     * @param file Excel 文件
     * @return 导入结果
     */
    MesSupplierImportRespVO importSupplierExcel(MultipartFile file) throws IOException;

}

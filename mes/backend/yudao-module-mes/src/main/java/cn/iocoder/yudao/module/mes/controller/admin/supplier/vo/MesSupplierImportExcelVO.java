package cn.iocoder.yudao.module.mes.controller.admin.supplier.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.ExcelColumnSelect;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import lombok.Data;

/**
 * 供应商名录导入 Excel 行。
 *
 * <p>导入日期必须按 {@code yyyy/M/d} 填写，例如 {@code 2021/5/20}。</p>
 */
@Data
@ExcelIgnoreUnannotated
public class MesSupplierImportExcelVO {

    @ExcelProperty(value = "使用部门", index = 0)
    private String usingDepartment;

    @ExcelProperty(value = "供应商名称", index = 1)
    private String supplierName;

    @ExcelProperty(value = "供应商代码", index = 2)
    private String supplierCode;

    @ExcelProperty(value = "联系人", index = 3)
    private String contactPerson;

    @ExcelProperty(value = "联系电话", index = 4)
    private String contactPhone;

    @ExcelProperty(value = "供应商地址", index = 5)
    private String address;

    @ExcelProperty(value = "企业性质", index = 6)
    private String companyNature;

    @ExcelProperty(value = "产地", index = 7)
    private String originPlace;

    @ExcelProperty(value = "原厂信息", index = 8)
    private String originalFactoryInfo;

    @ExcelProperty(value = "提供/协作产品", index = 9)
    private String providedProduct;

    @ExcelProperty(value = "型号", index = 10)
    private String model;

    @ExcelProperty(value = "物料代码", index = 11)
    private String materialCode;

    @ExcelProperty(value = "适用产品", index = 12)
    private String applicableProduct;

    @ExcelProperty(value = "导入日期（yyyy/M/d，例如2021/5/20）", index = 13)
    @DateTimeFormat("yyyy/M/d")
    private String importDate;

    @ExcelProperty(value = "物料等级", index = 14)
    @ExcelColumnSelect(dictType = "mes_supplier_material_grade")
    private String materialGrade;

    @ExcelProperty(value = "供应商状态", index = 15)
    @ExcelColumnSelect(dictType = "mes_supplier_status")
    private String status;

}

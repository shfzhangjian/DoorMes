package cn.iocoder.yudao.module.doormes.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class CatalogModels {
    private CatalogModels() {}
    public static final String SCHEMA="doormes-material-catalog.v1";
    public record Input(@Pattern(regexp="finish|glass") @NotNull String category,
        @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,59}") @NotNull String code,
        @NotBlank @Size(max=160) String name,@NotBlank @Size(max=500) String specification,@NotNull @Size(max=1000) String note,
        @Pattern(regexp="metal|glass") @NotNull String materialFamily,@Pattern(regexp="#[0-9a-fA-F]{6}") @NotNull String baseColor,
        @DecimalMin("0") @DecimalMax("1") double metalness,@DecimalMin("0") @DecimalMax("1") double roughness,@DecimalMin("0") @DecimalMax("1") double opacity,
        Double thicknessMm,@NotNull @Size(max=30) List<@NotBlank @Size(max=60) String> compatibleProfileSystemIds,
        @Pattern(regexp="MES-TEXTURE-[A-Za-z0-9._-]{1,88}") String textureSetId,
        @Pattern(regexp="sha256:[a-f0-9]{64}") String textureContentHash,
        @DecimalMin("0.001") @DecimalMax("1000") Double textureRepeatX,
        @DecimalMin("0.001") @DecimalMax("1000") Double textureRepeatY) {
        public Input(String category,String code,String name,String specification,String note,String materialFamily,String baseColor,double metalness,double roughness,double opacity,Double thicknessMm,List<String> compatibleProfileSystemIds) {
            this(category,code,name,specification,note,materialFamily,baseColor,metalness,roughness,opacity,thicknessMm,compatibleProfileSystemIds,null,null,null,null);
        }
    }
    public record Mutation(@Min(0) int expectedRevision,@NotBlank @Size(max=1000) String changeNote,@Valid @NotNull Input item) {}
    public record Publish(@Min(1) int expectedRevision,@NotBlank @Size(max=1000) String note) {}
    public record Snapshot(String schemaVersion,String id,long tenantId,int revision,String status,long changedBy,String updatedAt,String changeNote,Input item,JsonNode data) {}
    public record Version(int revision,String status,String changeNote,long changedBy,String updatedAt) {}
}

package cn.iocoder.yudao.module.doormes.asset;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

public final class AssetModels {
    private AssetModels() {}
    public record Authorize(
        @NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,99}") String assetId,
        @NotBlank @Pattern(regexp="sha256:[a-f0-9]{64}") String expectedContentHash,
        @NotBlank @Pattern(regexp="model/gltf-binary|model/gltf\\+json|image/png|image/jpeg") String mediaType,
        @Min(2) @Max(26214400) int byteLength) {}
    public record Grant(String grantId,String expiresAtIso) {}
    public record Descriptor(String assetId,String kind,String mediaType,String contentHash,int byteLength,String storedAtIso) {}
    public record Evidence(Descriptor asset,JsonNode inspection,String inspectedAtIso) {}
    public record Metadata(String schemaVersion,String blobId,long tenantId,long uploadedBy,Evidence evidence) {}
}

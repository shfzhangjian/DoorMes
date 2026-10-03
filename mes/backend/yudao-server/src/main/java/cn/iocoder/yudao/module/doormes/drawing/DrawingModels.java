package cn.iocoder.yudao.module.doormes.drawing;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.List;

/** Business revision is separate from the engine's local command revision. */
public final class DrawingModels {
    private DrawingModels() {}
    public static final String SCHEMA="doormes-drawing-snapshot.v1";
    public record Open(@NotBlank String requirementId,@NotBlank String lineId,@Min(1) int expectedRequirementRevision) {}
    public record Metadata(@Size(max=60) String number,@NotBlank @Size(max=200) String name,
                           @Size(max=1000) String note,String source) {
        public Metadata(String number,String name,String note){this(number,name,note,null);}
    }
    public record Create(@Size(max=60) String number,@NotBlank @Size(max=200) String name,
        @Size(max=1000) String note,@NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,39}") String mark,
        @Min(1) @Max(1000) int quantity,@DecimalMin("1") @DecimalMax("50000") double widthMm,
        @DecimalMin("1") @DecimalMax("50000") double heightMm,@Size(max=36) String clientRequestId) {
        public Create(String number,String name,String note,String mark,int quantity,double widthMm,double heightMm){
            this(number,name,note,mark,quantity,widthMm,heightMm,null);
        }
    }
    public record Save(@Min(1) int expectedRevision,@NotBlank @Size(max=1000) String changeNote,
                       @NotNull JsonNode document,@Valid Metadata metadata) {
        public Save(int expectedRevision,String changeNote,JsonNode document){this(expectedRevision,changeNote,document,null);}
    }
    public record Snapshot(String schemaVersion,String id,long tenantId,String requirementId,int requirementRevision,
        String lineId,int revision,String status,long createdBy,long changedBy,String createdAt,String updatedAt,
        String changeNote,JsonNode document,Metadata metadata,boolean editable) {
        public Snapshot(String schemaVersion,String id,long tenantId,String requirementId,int requirementRevision,
            String lineId,int revision,String status,long createdBy,long changedBy,String createdAt,String updatedAt,
            String changeNote,JsonNode document){
            this(schemaVersion,id,tenantId,requirementId,requirementRevision,lineId,revision,status,createdBy,changedBy,createdAt,updatedAt,changeNote,document,null,false);
        }
    }
    public record Summary(String id,int revision,String status,String number,String name,String note,String source,
                          String requirementId,int requirementRevision,String lineId,long createdBy,String updatedAt,boolean editable) {}
    public record Page(List<Summary> list,long total) {}
    public record Version(int revision,String changeNote,long changedBy,String updatedAt) {}
}

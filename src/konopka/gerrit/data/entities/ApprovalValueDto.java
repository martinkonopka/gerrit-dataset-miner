package konopka.gerrit.data.entities;

public class ApprovalValueDto {
    public final short value;
    public final String description;

    public ApprovalValueDto(short value, String description) {
        this.value = value;
        this.description = description;
    }

    public ApprovalValueDto(String value, String description) {
        this.value = Short.parseShort(value.trim());
        this.description = description;
    }
}

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
/**
 * Class for the structure of metadata. Simply stores attributes and provides getters and setters.
 */
public class Result {

    @SerializedName("metadata")
    @Expose
    private Metadata metadata;

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

}

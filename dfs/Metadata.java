import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
/**
 * Class for the structure of metadata. Simply stores attributes and provides getters and setters.
 */
public class Metadata {

    @SerializedName("files")
    @Expose
    private List<File> files = null;

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public void addFile(File file) {
      files.add(file);
    }

    public void removeFile(File file) {
      files.remove(file);
    }
}

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class File {

    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("numberOfPages")
    @Expose
    private String numberOfPages;
    @SerializedName("pageSize")
    @Expose
    private String pageSize;
    @SerializedName("size")
    @Expose
    private String size;
    @SerializedName("pages")
    @Expose
    private List<Page> pages = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(String numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public void addPage(Page page) {
      pages.add(page);
    }

    public void removePage(Page page) {
      pages.remove(page);
    }

}

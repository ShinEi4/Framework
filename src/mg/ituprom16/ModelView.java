package mg.ituprom16;

import java.util.HashMap;

public class ModelView {
    private String url;
    private HashMap<String, Object> data = new HashMap<>();

    public ModelView(String url) {
        this.url = url;
    }

    public ModelView()
    {}

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void addObject(String key, Object value) {
        this.getData().put(key, value);
    }
}

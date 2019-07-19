package digital.hoodoo.aemcloud.packagetool.models;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Set;

public class GroupModel {

    public static String ALL_PACKAGES = "all";
    public static String DEFAULT_EXCLUDED_PACKAGES = "day,adobe,com.adobe.cq.inbox";

    private String name;
    private String title;
    private boolean selected;

    public GroupModel(String path, ResourceResolver resourceResolver, Set<String> selectedGroups) {
        if (path == null || "".equals(path)) {
            this.name = ALL_PACKAGES;
            this.title = "All packages";
        } else {
            this.name = path.indexOf("/") > 0 ? path.substring(0, path.indexOf("/")) : path;
            this.title = !"".equals(this.name) && resourceResolver.getResource("/etc/packages/" + this.name) != null ?
                    resourceResolver.getResource("/etc/packages/" + this.name).getValueMap().get("jcr:title", this.name) : this.name;
        }
        this.selected = selectedGroups.contains(this.name);
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public boolean isSelected() {
        return selected;
    }
}

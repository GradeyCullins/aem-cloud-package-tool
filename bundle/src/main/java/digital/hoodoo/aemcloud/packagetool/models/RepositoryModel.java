package digital.hoodoo.aemcloud.packagetool.models;

public class RepositoryModel {

    private String name;
    private String title;
    private boolean selected;

    public RepositoryModel(String name, String title, boolean selected) {
        this.name = name;
        this.title = title;
        this.selected = selected;
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

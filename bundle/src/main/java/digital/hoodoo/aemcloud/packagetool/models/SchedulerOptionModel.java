package digital.hoodoo.aemcloud.packagetool.models;

import java.util.Calendar;
import java.util.Collection;

public class SchedulerOptionModel {

    public static String ALL_OPTIONS = "all";

    private String name;
    private String value;
    private boolean selected;
    private Collection<String> packages;
    private Calendar lastSync;
    private Calendar nextSync;

    public SchedulerOptionModel(String name, String value, String selected, Collection<String> packages, Calendar lastSync, long syncInterval) {
        this.name = name;
        this.value = value;
        this.selected = this.value.equals(selected);
        this.packages = packages;
        this.lastSync = lastSync;
        this.nextSync = lastSync != null ? (Calendar)lastSync.clone() : null;
        if (this.nextSync != null) {
            this.nextSync.add(Calendar.HOUR, (int)syncInterval);
        }
    }

    public SchedulerOptionModel(String name, String value) {
        this.name = name;
        this.value = value;
        this.selected = false;
        this.packages = null;
    }

    public SchedulerOptionModel() {
        this.name = "Not scheduled";
        this.value = "not-scheduled";
        this.selected = false;
        this.packages = null;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean isSelected() {
        return selected;
    }

    public Collection<String> getPackages() {
        return packages;
    }

    public Calendar getLastSync() {
        return lastSync;
    }

    public Calendar getNextSync() {
        return nextSync;
    }
}

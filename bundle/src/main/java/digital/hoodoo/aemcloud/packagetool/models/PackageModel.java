package digital.hoodoo.aemcloud.packagetool.models;

import com.day.cq.commons.Externalizer;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class PackageModel {

    private static final String PACKAGE_MANAGER_PATH = "/crx/packmgr/index.jsp";
    private static final String PACKAGE_MANAGER_THUMBNAIL_PATH = "/crx/packmgr/thumbnail.jsp?path=";
    private String id;
    private String name;
    private String lastModifiedDate;
    private String path;
    private String link;
    private String thumbnailSrc;
    private String repository;
    private String branch;
    private String buildId;
    private String info;
    private String description;
    private SchedulerOptionModel scheduled;
    private String nextScheduledRun;
    private String packageDate;


    public PackageModel(String id, String name, Date lastModifiedDate, String path, String repository, String branch,
            String buildId, String link) {
        this(id, name, "", path, repository, branch, buildId, link);
        this.lastModifiedDate = lastModifiedDate != null ? String.valueOf(lastModifiedDate.getTime()) : "";
    }

    public PackageModel(String id, String name, String lastModifiedDate, String path, String repository, String branch,
            String buildId, String link) {
        this.id = id;
        this.name = name;
        this.lastModifiedDate = lastModifiedDate;
        this.path = path;
        this.repository = repository;
        this.branch = branch;
        this.buildId = buildId;
        this.link = link;
    }

    public PackageModel(JcrPackage jcrPackage, ResourceResolver resourceResolver, Map<String, SchedulerOptionModel> schedulerOptions) throws RepositoryException, UnsupportedEncodingException {

        this.name = jcrPackage.getNode().getName();
        this.id = "local-package_" + jcrPackage.getNode().getIdentifier();
        this.lastModifiedDate = jcrPackage.getDefinition().getLastModified() != null
                ? jcrPackage.getDefinition().getLastModified().getTime().toString()
                : "";

        Externalizer externalizer = resourceResolver.adaptTo(Externalizer.class);
        this.path = jcrPackage.getNode().getPath();
        this.link = externalizer.authorLink(resourceResolver, PACKAGE_MANAGER_PATH + "#" + URLEncoder.encode(this.path, "UTF-8"));
        this.thumbnailSrc = externalizer.authorLink(resourceResolver, PACKAGE_MANAGER_THUMBNAIL_PATH + URLEncoder.encode(this.path, "UTF-8"));

        this.repository = "";
        this.branch = "";
        this.buildId = "";

        String version = jcrPackage.getDefinition().get(JcrPackageDefinition.PN_VERSION);
        this.info = (version != null && !version.isEmpty()) ? "Version: " + version : "";

        String build = jcrPackage.getDefinition().get(JcrPackageDefinition.PN_BUILD_COUNT);
        this.info = (!this.info.isEmpty() && build != null && !build.isEmpty()) ? this.info + " | " : this.info;
        this.info = (build != null && !build.isEmpty()) ? this.info + "Build: " + build : this.info;

        if (jcrPackage.getDefinition().isModified()) {
            Date lastModified = jcrPackage.getDefinition().getLastModified() != null
                    ? jcrPackage.getDefinition().getLastModified().getTime()
                    : null;

            this.packageDate = lastModified != null ? String.valueOf(lastModified.getTime()) : null;

            this.info = (!this.info.isEmpty() && lastModified != null) ? this.info + " | " : this.info;
            this.info = lastModified != null ? this.info + "Last modified: " : this.info;
        } else {
            Date lastInstalled = jcrPackage.getDefinition().getLastUnpacked() != null
                    ? jcrPackage.getDefinition().getLastUnpacked().getTime()
                    : null;
            Date lastBuilt = jcrPackage.getDefinition().getLastWrapped() != null
                    ? jcrPackage.getDefinition().getLastWrapped().getTime()
                    : null;
            if (lastBuilt != null || lastInstalled != null) {
                this.info = !this.info.isEmpty() ? this.info + " | " : this.info;
                if (lastBuilt != null && lastInstalled != null) {
                    this.info = lastBuilt.after(lastInstalled) ? this.info + "Last built: "
                            : this.info + "Last installed: ";

                    this.packageDate = lastBuilt.after(lastInstalled) ? String.valueOf(lastBuilt.getTime()) : String.valueOf(lastInstalled.getTime());

                } else {
                    this.info = lastBuilt != null ? this.info + "Last built: "
                            : this.info + "Last installed: ";

                    this.packageDate = lastBuilt != null ? String.valueOf(lastBuilt.getTime()) : String.valueOf(lastInstalled.getTime());
                }
            }
        }

        this.description = jcrPackage.getDefinition().getDescription() != null
                ? jcrPackage.getDefinition().getDescription()
                : "";

        this.scheduled = null;
        for (SchedulerOptionModel schedulerOptionItem : schedulerOptions.values()) {
            if (schedulerOptionItem.getPackages() != null && schedulerOptionItem.getPackages().contains(jcrPackage.getNode().getIdentifier())) {
                this.scheduled = new SchedulerOptionModel(schedulerOptionItem.getName(), schedulerOptionItem.getValue());
                this.nextScheduledRun = schedulerOptionItem.getNextSync() != null ? String.valueOf(schedulerOptionItem.getNextSync().getTimeInMillis()) : "";
                break;
            }
        }
        if (this.scheduled == null) {
            this.scheduled = new SchedulerOptionModel();
            this.nextScheduledRun = "";
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getPath() {
        return path;
    }

    public String getLink() {
        return link;
    }

    public String getThumbnailSrc() {
        return thumbnailSrc;
    }

    public String getRepository() {
        return repository;
    }

    public String getBranch() {
        return branch;
    }

    public String getBuildId() {
        return buildId;
    }

    public String getInfo() {
        return info;
    }

    public String getDescription() {
        return description;
    }

    public SchedulerOptionModel getScheduled() {
        return scheduled;
    }

    public String getNextScheduledRun() {
        return nextScheduledRun;
    }

    public String getDeepLinkUserPackages(){
        return this.link + "/" + this.name;
    }

    public String getPackageDate(){
        return this.packageDate;
    }
}

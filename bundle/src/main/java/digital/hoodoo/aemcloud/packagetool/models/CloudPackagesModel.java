package digital.hoodoo.aemcloud.packagetool.models;

import digital.hoodoo.aemcloud.packagetool.AEMCloudAPIClient;
import digital.hoodoo.aemcloud.packagetool.AEMCloudException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.*;

@Model(adaptables = SlingHttpServletRequest.class)
public class CloudPackagesModel {

    private static Logger log = LoggerFactory.getLogger(CloudPackagesModel.class);

    @ScriptVariable
    private SlingHttpServletRequest request;

    private List<PackageModel> userPackages;
    private List<PackageModel> buildPackages;

    private String userNameFilter;
    private String buildNameFilter;
    private Map<String, RepositoryModel> buildRepositories;
    private String buildRepositoryFilter;
    private String buildBranchFilter;
    private String buildBuildIdFilter;

    @PostConstruct
    private void initModel() {
        try {

            AEMCloudAPIClient client = new AEMCloudAPIClient(request.getResourceResolver());

            //Get filters
            this.userNameFilter = request.getRequestParameter("user-name") != null ? request.getRequestParameter("user-name").getString() : "";
            this.buildNameFilter = request.getRequestParameter("build-name") != null ? request.getRequestParameter("build-name").getString() : "";
            this.buildRepositoryFilter = request.getRequestParameter("build-repository") != null ? request.getRequestParameter("build-repository").getString() : "";
            this.buildBranchFilter = request.getRequestParameter("build-branch") != null ? request.getRequestParameter("build-branch").getString() : "";
            this.buildBuildIdFilter = request.getRequestParameter("build-buildid") != null ? request.getRequestParameter("build-buildid").getString() : "";

            this.buildRepositories = new HashMap<String, RepositoryModel>();
            this.userPackages = client.getUserPackages(this.userNameFilter);
            this.buildPackages = client.getBuildPackages(this.buildNameFilter, this.buildRepositories, this.buildRepositoryFilter, this.buildBranchFilter, this.buildBuildIdFilter);

        } catch (AEMCloudException e) {
            log.error(e.getMessage());
            this.buildPackages = new ArrayList<PackageModel>();
            this.userPackages = new ArrayList<PackageModel>();
        }
    }

    public List<PackageModel> getBuildPackages() {
        return this.buildPackages;
    }

    public List<PackageModel> getUserPackages() {
        return this.userPackages;
    }

    public String getUserNameFilter() {
        return userNameFilter;
    }

    public String getBuildNameFilter() {
        return buildNameFilter;
    }

    public String getBuildRepositoryFilter() {
        return buildRepositoryFilter;
    }

    public String getBuildBranchFilter() {
        return buildBranchFilter;
    }

    public String getBuildBuildIdFilter() {
        return buildBuildIdFilter;
    }

    public Collection<RepositoryModel> getBuildRepositories() {
        return this.buildRepositories.values();
    }
}

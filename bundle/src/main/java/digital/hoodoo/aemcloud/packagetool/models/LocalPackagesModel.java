package digital.hoodoo.aemcloud.packagetool.models;

import digital.hoodoo.aemcloud.packagetool.Utils;
import digital.hoodoo.aemcloud.packagetool.scheduler.PackageSyncUtil;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.*;
import java.io.UnsupportedEncodingException;
import java.util.*;

@Model(adaptables = SlingHttpServletRequest.class)
public class LocalPackagesModel {

    private static Logger log = LoggerFactory.getLogger(LocalPackagesModel.class);

    @ScriptVariable
    private SlingHttpServletRequest request;

    @Inject
    private Packaging packaging;

    private List<PackageModel> packages;
    private Map<String, GroupModel> groups;
    private Map<String, SchedulerOptionModel> schedulerOptions;

    private Set<String> selectedGroups;
    private String nameFilter;
    private String scheduleFilter;

    @PostConstruct
    private void initModel() {

        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Session session = resourceResolver.adaptTo(Session.class);
            JcrPackageManager jcrPackageManager = packaging.getPackageManager(session);

            // Get filters
            boolean addDefaultGroups;
            Set<String> excludedGroups = new HashSet<String>(Arrays.asList(GroupModel.DEFAULT_EXCLUDED_PACKAGES.split(",")));
            if (request.getRequestParameter("local-group") == null) {
                this.selectedGroups = new HashSet<String>();
                addDefaultGroups = true;
            } else {
                this.selectedGroups = new HashSet<String>(Arrays.asList(request.getRequestParameter("local-group").getString().split(",")));
                addDefaultGroups = false;
            }
            this.nameFilter = request.getRequestParameter("local-name") != null
                    ? request.getRequestParameter("local-name").getString()
                    : "";
            this.scheduleFilter = request.getRequestParameter("local-schedule") != null
                    ? request.getRequestParameter("local-schedule").getString()
                    : SchedulerOptionModel.ALL_OPTIONS;

            // Get scheduler options
            Node schedulerOptionsNode = resourceResolver.getResource(PackageSyncUtil.SCHEDULER_OPTIONS_PATH).adaptTo(Node.class);
            this.schedulerOptions = new HashMap<String, SchedulerOptionModel>();
            if (schedulerOptionsNode != null && schedulerOptionsNode.hasNodes()) {
                NodeIterator schedulerOptions = schedulerOptionsNode.getNodes();
                while (schedulerOptions.hasNext()) {
                    Node schedulerOption = schedulerOptions.nextNode();
                    String optionName = schedulerOption.hasProperty("name") ? schedulerOption.getProperty("name").getString() : "";
                    String optionValue = schedulerOption.getName();
                    long optionSyncInterval = schedulerOption.hasProperty("syncInterval") ? schedulerOption.getProperty("syncInterval").getLong() : 0;
                    Calendar optionLastSync = schedulerOption.hasProperty("lastSync") ? schedulerOption.getProperty("lastSync").getDate() : null;
                    Collection<String> optionPackages = new ArrayList<String>();
                    if (schedulerOption.hasProperty("packages")) {
                        Value[] values = schedulerOption.getProperty("packages").getValues();
                        for (Value value : values) {
                            optionPackages.add(value.getString());
                        }
                    }
                    if (!optionName.isEmpty() && !optionValue.isEmpty()) {
                        SchedulerOptionModel option = new SchedulerOptionModel(optionName, optionValue, this.scheduleFilter, optionPackages, optionLastSync, optionSyncInterval);
                        this.schedulerOptions.putIfAbsent(optionValue, option);
                    }
                }
            }
            SchedulerOptionModel notScheduledOption = new SchedulerOptionModel("Not scheduled", "not-scheduled", this.scheduleFilter, null, null, 0);
            this.schedulerOptions.putIfAbsent(notScheduledOption.getValue(), notScheduledOption);
            SchedulerOptionModel allScheduledOptions = new SchedulerOptionModel("All", SchedulerOptionModel.ALL_OPTIONS, this.scheduleFilter, null, null, 0);
            this.schedulerOptions.putIfAbsent(allScheduledOptions.getValue(), allScheduledOptions);

            // Get list of local packages and groups
            List<JcrPackage> localPackages = jcrPackageManager.listPackages();
            List<PackageModel> packagesList = new ArrayList<PackageModel>();
            this.groups = new HashMap<String, GroupModel>();
            for (JcrPackage localPackage : localPackages) {
                if (localPackage.getDefinition() != null) {
                    String groupPath = localPackage.getDefinition().get(JcrPackageDefinition.PN_GROUP);
                    if (addDefaultGroups) {
                        String groupName = groupPath.indexOf("/") > 0 ? groupPath.substring(0, groupPath.indexOf("/")) : groupPath;
                        if (!excludedGroups.contains(groupName)) {
                            this.selectedGroups.add(groupName);
                        }
                    }
                    GroupModel group = new GroupModel(groupPath, resourceResolver, this.selectedGroups);
                    this.groups.putIfAbsent(group.getName(), group);
                    PackageModel packageModel = new PackageModel(localPackage, resourceResolver, schedulerOptions);
                    if ((this.selectedGroups.contains(GroupModel.ALL_PACKAGES) || this.selectedGroups.contains(group.getName()))
                            && (this.nameFilter.isEmpty() || localPackage.getNode().getName().matches(".*" + this.nameFilter + ".*"))
                            && (this.scheduleFilter.equals(SchedulerOptionModel.ALL_OPTIONS) || packageModel.getScheduled().getValue().equals(this.scheduleFilter))) {
                        packagesList.add(packageModel);
                    }
                }
            }
            this.packages = Utils.getInverseList(packagesList);
            //this.groups.putIfAbsent(GroupModel.ALL_PACKAGES, new GroupModel("", resourceResolver, this.selectedGroups));

        } catch (RepositoryException|UnsupportedEncodingException e) {
            log.error(e.getMessage());
            this.packages = new ArrayList<PackageModel>();
        }
    }

    public List<PackageModel> getPackages() {
        return packages;
    }

    public Collection<GroupModel> getGroups() {
        return groups.values();
    }

    public String getNameFilter() {
        return nameFilter;
    }

    public Collection<SchedulerOptionModel> getSchedulerOptions() {
        return schedulerOptions.values();
    }
}

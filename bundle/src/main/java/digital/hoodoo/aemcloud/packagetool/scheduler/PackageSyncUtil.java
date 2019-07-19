package digital.hoodoo.aemcloud.packagetool.scheduler;

import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;

public class PackageSyncUtil {

    public static final String SCHEDULER_OPTIONS_PATH = "/conf/aem-cloud/settings/package-tool/scheduler/options";
    public static final String PACKAGES = "packages";

    protected final static Logger log = LoggerFactory.getLogger(PackageSyncUtil.class);

    public static boolean addToSchedulerOptions(String packageId, String schedulerOptionId, ResourceResolver resourceResolver) {
        try {
            Node schedulerOptionsNode = resourceResolver.getResource(PackageSyncUtil.SCHEDULER_OPTIONS_PATH).adaptTo(Node.class);
            if (schedulerOptionsNode == null || !schedulerOptionsNode.hasNodes()) {
                log.error("AEM Cloud package scheduler: No scheduler options found.");
                return false;
            } else {
                NodeIterator schedulerOptions = schedulerOptionsNode.getNodes();
                while (schedulerOptions.hasNext()) {
                    Node schedulerOption = schedulerOptions.nextNode();

                    Value[] packageIds = schedulerOption.hasProperty(PackageSyncUtil.PACKAGES) ? schedulerOption.getProperty(PackageSyncUtil.PACKAGES).getValues() : null;
                    if (schedulerOption.getName().equals(schedulerOptionId)) {
                        //If the option is schedulerOptionId -> add the package if not there yet
                        if (packageIds != null) {
                            boolean found = false;
                            for (Value packageIdValue : packageIds) {
                                if (packageIdValue.getString().equals(packageId)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                ValueFactory valueFactory = schedulerOption.getSession().getValueFactory();
                                packageIds = (Value[]) ArrayUtils.add(packageIds, valueFactory.createValue(packageId));
                                schedulerOption.setProperty(PackageSyncUtil.PACKAGES, packageIds);
                                resourceResolver.commit();
                            }
                        } else {
                            ValueFactory valueFactory = schedulerOption.getSession().getValueFactory();
                            packageIds = new Value[1];
                            packageIds[0] = valueFactory.createValue(packageId);
                            schedulerOption.setProperty(PackageSyncUtil.PACKAGES, packageIds);
                            resourceResolver.commit();
                        }
                    } else {
                        //If the option is not schedulerOptionId -> remove the package if it is there
                        if (packageIds != null) {
                            boolean found = false;
                            Value[] updatedPackageIds = packageIds.length > 1 ? new Value[packageIds.length - 1] : new Value[1];
                            int index = 0;
                            for (int i = 0; i < packageIds.length; i++) {
                                if (packageIds[i].getString().equals(packageId)) {
                                    found = true;
                                } else if (index < packageIds.length - 1) {
                                    updatedPackageIds[index] = packageIds[i];
                                    index++;
                                }
                            }
                            if (found) {
                                if (packageIds.length > 1) {
                                    schedulerOption.setProperty(PackageSyncUtil.PACKAGES, updatedPackageIds);
                                } else {
                                    schedulerOption.setProperty(PackageSyncUtil.PACKAGES, (Value) null);
                                }
                                resourceResolver.commit();
                            }
                        }
                    }
                }
                return true;
            }
        } catch (RepositoryException e) {
            log.error("AEM Cloud package scheduler: Repository exception.", e);
            return false;
        } catch (PersistenceException e) {
            log.error("AEM Cloud package scheduler: Error modifying scheduler options nodes.", e);
            return false;
        }
    }
}

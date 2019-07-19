package digital.hoodoo.aemcloud.packagetool.scheduler;

import digital.hoodoo.aemcloud.packagetool.AEMCloudAPIClient;
import digital.hoodoo.aemcloud.packagetool.AEMCloudException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.resource.LoginException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

@Component
@Service(value = Runnable.class)
@Property(name = "scheduler.period", longValue = 3600)
public class PackageSync implements Runnable {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Packaging packaging;

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String SYNC_INTERVAL = "syncInterval";
    private static final String LAST_SYNC = "lastSync";
    private static final String NAME = "name";

    public void run() {
        log.info("AEM Cloud sync scheduler: Package sync scheduler start...");
        try {
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            AEMCloudAPIClient client = new AEMCloudAPIClient(resourceResolver);
            JcrPackageManager packageManager = packaging.getPackageManager(resourceResolver.adaptTo(Session.class));
            if (packageManager == null) {
                log.error("AEM Cloud sync scheduler error: Package manager is null.");
            } else {

                //Get scheduler options
                Node schedulerOptionsNode = resourceResolver.getResource(PackageSyncUtil.SCHEDULER_OPTIONS_PATH).adaptTo(Node.class);
                if (schedulerOptionsNode == null || !schedulerOptionsNode.hasNodes()) {
                    log.error("AEM Cloud sync scheduler: No scheduler options found.");
                } else {
                    NodeIterator schedulerOptions = schedulerOptionsNode.getNodes();
                    while (schedulerOptions.hasNext()) {
                        Node schedulerOption = schedulerOptions.nextNode();

                        //Check if the scheduler option should be fired
                        long interval = schedulerOption.hasProperty(SYNC_INTERVAL) ? schedulerOption.getProperty(SYNC_INTERVAL).getLong() : 1;
                        Calendar lastSync = schedulerOption.hasProperty(LAST_SYNC) ? schedulerOption.getProperty(LAST_SYNC).getDate() : null;
                        Calendar currentDate = Calendar.getInstance();

                        if (lastSync == null || (currentDate.getTimeInMillis() - lastSync.getTimeInMillis() - 1) / 3600000 >= interval - 1) {
                            String schedulerOptionName = schedulerOption.hasProperty(NAME) ? schedulerOption.getProperty(NAME).getString() : "";
                            log.info("AEM Cloud sync scheduler: " + schedulerOptionName + " scheduler option started at " + currentDate.getTime().toString());
                            schedulerOption.setProperty(LAST_SYNC, currentDate);
                            resourceResolver.commit();

                            //Get packages to send
                            Value[] packageIds = schedulerOption.hasProperty(PackageSyncUtil.PACKAGES) ? schedulerOption.getProperty(PackageSyncUtil.PACKAGES).getValues() : null;
                            if (packageIds == null || packageIds.length == 0) {
                                log.info("AEM Cloud sync scheduler: No packages to sync.");
                            } else {
                                for (Value packageId : packageIds) {
                                    Resource packageResource = resourceResolver.getResource(packageId.getString());
                                    if (packageResource == null) {
                                        //TODO: remove this package id from the scheduler options.
                                    } else {
                                        JcrPackage localPackage = packageManager.open(packageResource.adaptTo(Node.class));
                                        packageManager.assemble(localPackage, null);
                                        client.uploadPackage(packageResource.adaptTo(InputStream.class), localPackage.getNode().getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            resourceResolver.close();
        } catch (RepositoryException e) {
            log.error("AEM Cloud sync scheduler: Repository exception.", e);
        } catch (LoginException e) {
            log.error("AEM Cloud sync scheduler: Error getting administrative resource resolver.", e);
        } catch (PersistenceException e) {
            log.error("AEM Cloud sync scheduler: Error updating scheduler options node.", e);
        } catch (AEMCloudException e) {
            log.error("AEM Cloud sync scheduler: Error sending package to AEM Cloud.", e);
        } catch (PackageException e) {
            log.error("AEM Cloud sync scheduler: Error building the package.", e);
        } catch (IOException e) {
            log.error("AEM Cloud sync scheduler: Error building the package.", e);
        }
    }

    protected void activate(ComponentContext ctx) {
        log.info("AEM Cloud Sync Scheduler activate...");
    }
}

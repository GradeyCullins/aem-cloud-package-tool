package digital.hoodoo.aemcloud.packagetool.servlets;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import digital.hoodoo.aemcloud.packagetool.AEMCloudAPIClient;
import digital.hoodoo.aemcloud.packagetool.scheduler.PackageSyncUtil;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

@SlingServlet(methods = "POST", paths = "/bin/aem-cloud/package-tool", extensions = "json")
public class PackageToolServlet extends SlingAllMethodsServlet {

    @Reference
    private Packaging packaging;

    @Reference
    private Replicator replicator;

    private static Logger log = LoggerFactory.getLogger(PackageToolServlet.class);

    private static String PACKAGE_TOOL_PATH = "/apps/hoodoo-digital/aem-cloud/package-tool/content/package-tool.html";
    private static String BUILD_PACKAGE_PREFIX = "build-package_";
    private static String USER_PACKAGE_PREFIX = "user-package_";
    private static String LOCAL_PACKAGE_PREFIX = "local-package_";
    private static String FILTER_PREFIX = "filter_";
    private static String SCHEDULE_ITEMS_PARAMETER = "scheduleItems";
    private static String IMPORT_ITEMS_PARAMETER = "importItems";
    private static String SEND_ITEMS_PARAMETER = "sendItems";

    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

        String selector = request.getRequestPathInfo().getSelectorString();
        log.info("Package tool servlet: " + selector);

        List<RequestParameter> parameters = request.getRequestParameterList();

        //If packages are to be added/removed to/from the scheduled job
        if ("schedule".equals(selector)) {
            for (RequestParameter parameter : parameters) {
                if (parameter.getName().equals(SCHEDULE_ITEMS_PARAMETER)) {
                    JsonArray items = new Gson().fromJson(parameter.getString(), JsonObject.class).get("items").getAsJsonArray();
                    int updatedItems = 0;
                    for (int i = 0; i < items.size(); i++) {
                        String packageId = items.get(i).getAsJsonObject().get("itemId").getAsString().replace("local-package_","");
                        String period = items.get(i).getAsJsonObject().get("period").getAsString();
                        if (PackageSyncUtil.addToSchedulerOptions(packageId, period, request.getResourceResolver())) {
                            updatedItems++;
                        }
                    }
                    response.setContentType("text/html;charset=utf-8");
                    response.setStatus(200);
                    response.getWriter().write(updatedItems + " package(s) updated on AEM Cloud Package scheduler.");
                }
            }
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(200);
        } else {
            try {
                AEMCloudAPIClient client = new AEMCloudAPIClient(request.getResourceResolver());
                Session session = request.getResourceResolver().adaptTo(Session.class);
                JcrPackageManager jcrPackageManager = packaging.getPackageManager(session);

                //If packages are to be imported from AEM Cloud
                if ("import".equals(selector)) {
                    for (RequestParameter parameter : parameters) {
                        if (parameter.getName().equals(IMPORT_ITEMS_PARAMETER)) {
                            JsonArray items = new Gson().fromJson(parameter.getString(), JsonObject.class).get("items").getAsJsonArray();
                            int updatedItems = 0;
                            for (int i = 0; i < items.size(); i++) {
                                String itemId = items.get(i).getAsJsonObject().get("itemId").getAsString();
                                boolean install = items.get(i).getAsJsonObject().get("install").getAsBoolean();
                                boolean replicate = items.get(i).getAsJsonObject().get("replicate").getAsBoolean();
                                if (itemId.startsWith(BUILD_PACKAGE_PREFIX) || itemId.startsWith(USER_PACKAGE_PREFIX)) {
                                    String path = itemId.startsWith(BUILD_PACKAGE_PREFIX) ? itemId.substring(BUILD_PACKAGE_PREFIX.length()) : itemId.substring(USER_PACKAGE_PREFIX.length());
                                    log.info("Importing AEM Cloud Package: " + path);
                                    String downloadUrl = client.getPackageDownloadLink(path);
                                    log.info("Download link = " + downloadUrl);
                                    JcrPackage uploadedPackage = jcrPackageManager.upload(new URL(downloadUrl).openStream(), true);

                                    //Install the package
                                    if (install) {
                                        ImportOptions opts = new ImportOptions();
                                        opts.setAccessControlHandling(AccessControlHandling.OVERWRITE);
                                        opts.setImportMode(ImportMode.UPDATE);
                                        uploadedPackage.install(opts);
                                    }

                                    //Replicate the package
                                    if (replicate) {
                                        replicator.replicate(session, ReplicationActionType.ACTIVATE, uploadedPackage.getNode().getPath());
                                    }

                                    updatedItems++;
                                }
                            }
                            response.setContentType("text/html;charset=utf-8");
                            response.setStatus(200);
                            response.getWriter().write(updatedItems + " package(s) imported from AEM Cloud.");
                        }
                    }
                    response.setContentType("text/html;charset=utf-8");
                    response.setStatus(200);
                }

                //If packages are to be sent to AEM Cloud
                if ("send".equals(selector)) {
                    for (RequestParameter parameter : parameters) {
                        if (parameter.getName().equals(SEND_ITEMS_PARAMETER)) {
                            JsonArray items = new Gson().fromJson(parameter.getString(), JsonObject.class).get("items").getAsJsonArray();
                            int updatedItems = 0;
                            for (int i = 0; i < items.size(); i++) {
                                String itemId = items.get(i).getAsJsonObject().get("itemId").getAsString();
                                if (itemId.startsWith(LOCAL_PACKAGE_PREFIX)) {
                                    String id = itemId.substring(LOCAL_PACKAGE_PREFIX.length());
                                    log.info("Sending package to AEM Cloud: " + id);
                                    JcrPackage jcrPackage = jcrPackageManager.open(request.getResourceResolver().getResource(id).adaptTo(Node.class));
                                    log.info("JcrPackage = " + jcrPackage.toString());
                                    log.info("File: " + jcrPackage.getNode().getPath());
                                    Resource packageResource = request.getResourceResolver().getResource(id);
                                    client.uploadPackage(packageResource.adaptTo(InputStream.class), jcrPackage.getNode().getName());
                                    updatedItems++;
                                }
                            }
                            response.setContentType("text/html;charset=utf-8");
                            response.setStatus(200);
                            response.getWriter().write(updatedItems + " package(s) sent to AEM Cloud.");
                        }
                    }
                    response.setContentType("text/html;charset=utf-8");
                    response.setStatus(200);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }
}

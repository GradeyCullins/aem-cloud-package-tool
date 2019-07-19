package digital.hoodoo.aemcloud.connect;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class ConfigUtil {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String LOG_REPOSITORY_ERROR_MSG = "Something went wrong while working with repository";
    private static final String LOG_INCOMPLETE_CONFIG = "AEM Cloud Config could not be retrieved";
    private static final String AEM_CLOUD_API_PATH = "/api/v1";

    private boolean internal;
    private String aemCloudUrl;
    private String aemCloudAccount;
    private String aemCloudKeyName;
    private String aemCloudKeyValue;
    private String aemCloudAPIUrl;

    public boolean isInternal() {
        return this.internal;
    }

    public String getAemCloudUrl() {
        return this.aemCloudUrl;
    }

    public void setAemCloudUrl(String aemCloudUrl) {
        this.aemCloudUrl = aemCloudUrl;
    }

    public String getAemCloudAPIUrl() {
        return this.aemCloudAPIUrl;
    }

    public String getAemCloudAccount() {
        return this.aemCloudAccount;
    }

    public String getAemCloudKeyName() {
        return this.aemCloudKeyName;
    }

    public String getAemCloudKeyValue() {
        return this.aemCloudKeyValue;
    }

    public ConfigUtil(ResourceResolver resourceResolver) {

        Node jcrContentNode = getCloudServiceJcrContentNode(resourceResolver);

        try {
            if (jcrContentNode != null) {
                //EXTERNAL CONNECTION CONFIGURED THROUGH CLOUD SERVICES
                this.internal = false;
                this.aemCloudUrl = jcrContentNode.hasProperty("aemCloudUrl") ? getUrlWithProtocol(jcrContentNode.getProperty("aemCloudUrl").getString()) : "";
                this.aemCloudAPIUrl = this.aemCloudUrl + AEM_CLOUD_API_PATH;
                this.aemCloudAccount = jcrContentNode.hasProperty("aemCloudAccount") ? jcrContentNode.getProperty("aemCloudAccount").getString() : "";
                this.aemCloudKeyName = jcrContentNode.hasProperty("aemCloudKeyName") ? jcrContentNode.getProperty("aemCloudKeyName").getString() : "";
                this.aemCloudKeyValue = jcrContentNode.hasProperty("aemCloudKeyValue") ? jcrContentNode.getProperty("aemCloudKeyValue").getString() : "";
            } else {
                this.aemCloudUrl = System.getenv("AEMCLOUD_URL");
                if (this.aemCloudUrl != null && !this.aemCloudUrl.isEmpty()) {
                    //EXTERNAL CONNECTION CONFIGURED THROUGH ENVIRONMENT VARIABLES
                    this.internal = false;
                    this.aemCloudAPIUrl = this.aemCloudUrl + AEM_CLOUD_API_PATH;
                    this.aemCloudAccount = System.getenv("AEMCLOUD_ACCOUNT");
                    this.aemCloudKeyName = System.getenv("AEMCLOUD_KEY_NAME");
                    this.aemCloudKeyValue = System.getenv("AEMCLOUD_KEY_VALUE");
                } else {
                    //INTERNAL CONNECTION CONFIGURED THROUGH ENVIRONMENT VARIABLES
                    this.internal = true;
                    this.aemCloudUrl = System.getenv("INTERNAL_API_ADDRESS");
                    this.aemCloudAPIUrl = this.aemCloudUrl;
                    this.aemCloudAccount = System.getenv("INTERNAL_API_USERNAME");
                    this.aemCloudKeyName = System.getenv("INTERNAL_API_USERNAME");
                    this.aemCloudKeyValue = System.getenv("INTERNAL_API_PASSWORD");
                }
            }

            //verify that the config is complete
            if (this.aemCloudUrl.isEmpty() || this.aemCloudAPIUrl.isEmpty() || this.aemCloudAccount.isEmpty() || this.aemCloudKeyName.isEmpty() || this.aemCloudKeyValue.isEmpty()) {
                logger.error(LOG_INCOMPLETE_CONFIG);
            }
        } catch (RepositoryException e) {
            logger.error(LOG_REPOSITORY_ERROR_MSG, e);
        }
    }

    private Node getCloudServiceJcrContentNode(ResourceResolver resourceResolver) {
        Node jcrContentNode = null;

        try {
            //Get touch UI cloud service
            Resource touchUIResource = resourceResolver.getResource("/conf/aem-cloud/settings/cloudconfigs");
            if (touchUIResource != null) {
                Node node = touchUIResource.adaptTo(Node.class);
                NodeIterator niter = null;
                niter = node.getNodes();
                while (niter.hasNext()) {
                    Node childNode = niter.nextNode();
                    // we are only interested in the first configuration we find
                    if ("cq:Page".equals(childNode.getProperty("jcr:primaryType").getString()) && "aemcloudconnect".equals(childNode.getName())) {
                        jcrContentNode = childNode.getNode("jcr:content");
                        break;
                    }
                }
            }
        } catch (RepositoryException e) {
            logger.error(LOG_REPOSITORY_ERROR_MSG, e);
        }
        return jcrContentNode;
    }

    private String getUrlWithProtocol(String url) {
        return (url.startsWith("http://") || url.startsWith("https://")) ? url : "https://" + url;
    }

}

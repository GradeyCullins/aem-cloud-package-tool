package digital.hoodoo.aemcloud.connect.servlet;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

@SlingServlet(methods = "POST", resourceTypes = "sling/servlet/default", extensions = "json", selectors = "aemcloud-cloudconfig")
public class CloudConfigServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConfigServlet.class);

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        Resource resource = request.getResource();
        Session session = (Session)resource.getResourceResolver().adaptTo(Session.class);
        ResourceResolver resourceResolver = resource.getResourceResolver();

        Resource configResource = resource.getPath().endsWith("aemcloudconnect") ? resourceResolver.getResource(resource.getPath()) : createConfigResource(resource.getPath() + "/settings/cloudconfigs", resourceResolver);
        if (configResource != null) {
            Map<String, Object> props = new HashMap();
            readRequestParameters(request, props);
            updateConfigs(session, resourceResolver, configResource, props);
        }

        response.setHeader("Location", getRedirectPath(configResource));
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(302);
    }

    private Resource createConfigResource(String parentPath, ResourceResolver resolver)
    {
        Resource parent = resolver.getResource(parentPath);
        Page page = null;
        if (parent != null) {
            try
            {
                page = ((PageManager)resolver.adaptTo(PageManager.class)).create(parent.getPath(), "aemcloudconnect", "/apps/hoodoo-digital/aem-cloud/connect/templates/aemcloudconfig", "");
            }
            catch (WCMException localWCMException) {}
        }
        if (page != null) {
            return (Resource)page.adaptTo(Resource.class);
        }
        return null;
    }

    private void readRequestParameters(SlingHttpServletRequest request, Map<String, Object> prop)
    {
        if (request != null)
        {
            LOGGER.debug("reading Request Parameters");
            prop.put("jcr:title", request.getRequestParameter("jcr:title").getString());

            //AEM Cloud specific cloud config properties
            prop.put("aemCloudUrl", request.getRequestParameter("aemCloudUrl") != null ? request.getRequestParameter("aemCloudUrl").getString() : "");
            prop.put("aemCloudAccount", request.getRequestParameter("aemCloudAccount") != null ? request.getRequestParameter("aemCloudAccount").getString() : "");
            prop.put("aemCloudKeyName", request.getRequestParameter("aemCloudKeyName") != null ? request.getRequestParameter("aemCloudKeyName").getString() : "");
            prop.put("aemCloudKeyValue", request.getRequestParameter("aemCloudKeyValue") != null ? request.getRequestParameter("aemCloudKeyValue").getString() : "");
        }
    }

    private void updateConfigs(Session session, ResourceResolver resolver, Resource resource, Map<String, Object> prop)
    {
        try
        {
            Node configNode = (Node)resource.adaptTo(Node.class);

            Resource jcrContentResource = resource.getChild("jcr:content");
            if (jcrContentResource != null)
            {
                Node jcrContentNode = configNode.getNode("jcr:content");

                for (Map.Entry<String, Object> property : prop.entrySet())
                {
                    String key = (String)property.getKey();
                    if ("".equals(property.getValue())){
                        try {
                            jcrContentNode.getProperty(key).remove();
                        } catch (PathNotFoundException localPathNotFoundException) {
                            //If the property didn't exist
                        }
                    } else {
                        JcrUtil.setProperty(jcrContentNode, key, property.getValue());
                    }
                }
                session.save();
            }
        }
        catch (PathNotFoundException localPathNotFoundException) {

        }catch (RepositoryException localRepositoryException) {

        }catch (Exception localException) {}
    }

    private String getRedirectPath(Resource configResource)
    {
        String configPath = "/apps/hoodoo-digital/aem-cloud/connect/content/configurations.html/conf/aem-cloud";
        if (configResource != null)
        {
            Resource cloudconfigResource = configResource.getParent();
            if (cloudconfigResource != null)
            {
                Resource settingsResource = cloudconfigResource.getParent();
                if (settingsResource != null) {
                    return "/apps/hoodoo-digital/aem-cloud/connect/content/configurations.html" + settingsResource.getParent().getPath();
                }
            }
        }
        return configPath;
    }
}

package digital.hoodoo.aemcloud.connect.impl;

import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.Template;
import digital.hoodoo.aemcloud.connect.Configuration;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

@Model(adaptables={SlingHttpServletRequest.class}, adapters={Configuration.class})
public class ConfigurationImpl implements Configuration {

    @Self(injectionStrategy=InjectionStrategy.REQUIRED)
    private SlingHttpServletRequest request;
    @SlingObject(injectionStrategy=InjectionStrategy.REQUIRED)
    private ResourceResolver resourceResolver;
    @SlingObject(injectionStrategy=InjectionStrategy.REQUIRED)
    private Resource resource;
    @RequestAttribute(injectionStrategy=InjectionStrategy.OPTIONAL)
    private Resource useResource;
    @RequestAttribute(injectionStrategy=InjectionStrategy.OPTIONAL)
    private String uniqueConfigName;

    public String getTitle()
    {
        return (String)getResource().getValueMap().get("jcr:content/jcr:title", getResource().getValueMap().get("jcr:title", getResource().getName()));
    }

    public boolean hasChildren()
    {
        if (getResource().hasChildren()) {
            for (Resource child : getResource().getChildren())
            {
                boolean isContainer = ResourceHelper.isConfigurationContainer(child);
                boolean hasSetting = ResourceHelper.hasSetting(child, "cloudconfigs/");
                if ((isContainer) || (hasSetting)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getThumbnail()
    {
        if (ResourceHelper.isConfiguration(getResource()))
        {
            Page page = (Page)getResource().adaptTo(Page.class);
            if (page != null)
            {
                Template template = page.getTemplate();
                if (template != null)
                {
                    String templateThubnail = template.getThumbnailPath();
                    if (templateThubnail != null && !templateThubnail.isEmpty()) {
                        return templateThubnail;
                    }
                }
            }
            return "/libs/cq/ui/widgets/themes/default/icons/240x180/page.png";
        }
        return null;
    }

    public Calendar getLastModifiedDate()
    {
        Page page = (Page)getResource().adaptTo(Page.class);
        if (page != null) {
            return page.getLastModified();
        }
        ValueMap props = (ValueMap)getResource().adaptTo(ValueMap.class);
        if (props != null) {
            return (Calendar)props.get("jcr:lastModified", Calendar.class);
        }
        return null;
    }

    public String getLastModifiedBy()
    {
        Page page = (Page)getResource().adaptTo(Page.class);
        if (page != null) {
            return page.getLastModifiedBy();
        }
        ValueMap props = (ValueMap)getResource().adaptTo(ValueMap.class);
        if (props != null) {
            return (String)props.get("jcr:lastModifiedBy", String.class);
        }
        return null;
    }

    public Calendar getLastPublishedDate()
    {
        ReplicationStatus replicationStatus = (ReplicationStatus)getResource().adaptTo(ReplicationStatus.class);
        if (replicationStatus != null) {
            return replicationStatus.getLastPublished();
        }
        return null;
    }

    public Set<String> getQuickactionsRels()
    {
        Set<String> quickactions = new LinkedHashSet();
        if (ResourceHelper.isConfiguration(getResource()))
        {
            if (Permissions.hasPermission(this.resourceResolver, getResource().getPath(), "jcr:modifyProperties")) {
                quickactions.add("cq-confadmin-actions-properties-activator");
            }
            if (Permissions.hasPermission(this.resourceResolver, getResource().getPath(), "crx:replicate"))
            {
                quickactions.add("cq-confadmin-actions-publish-activator");
                quickactions.add("cq-confadmin-actions-unpublish-activator");
            }
            if (Permissions.hasPermission(this.resourceResolver, getResource().getPath(), "jcr:removeChildNodes")) {
                quickactions.add("cq-confadmin-actions-delete-activator");
            }
        }
        else if (hasChildren())
        {
            boolean uniqueConfAlreadyExists = false;
            if (getUniqueConfigName() != null)
            {
                uniqueConfAlreadyExists = ResourceHelper.checkConfNodeExists(getResource(), getUniqueConfigName());
                if (!uniqueConfAlreadyExists) {
                    quickactions.add("cq-confadmin-actions-createconfig-activator");
                } else {
                    quickactions.add("none");
                }
            }
        }
        return quickactions;
    }

    public String getUrl()
    {
        return getResource().getPath() + ".aemcloud-cloudconfig.json";
    }

    private Resource getResource()
    {
        if (this.useResource != null) {
            return this.useResource;
        }
        return this.resource;
    }

    private String getUniqueConfigName()
    {
        if (this.uniqueConfigName != null) {
            return this.uniqueConfigName;
        }
        return null;
    }

    //AEM Cloud specific cloud config properties
    public String getAemCloudUrl()
    {
        return (String)getResource().getValueMap().get("jcr:content/aemCloudUrl", "");
    }

    public String getAemCloudAccount()
    {
        return (String)getResource().getValueMap().get("jcr:content/aemCloudAccount", "");
    }

    public String getAemCloudKeyName()
    {
        return (String)getResource().getValueMap().get("jcr:content/aemCloudKeyName", "");
    }

    public String getAemCloudKeyValue()
    {
        return (String)getResource().getValueMap().get("jcr:content/aemCloudKeyValue", "");
    }
}

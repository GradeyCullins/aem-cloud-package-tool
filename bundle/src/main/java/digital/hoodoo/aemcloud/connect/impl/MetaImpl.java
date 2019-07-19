package digital.hoodoo.aemcloud.connect.impl;

import digital.hoodoo.aemcloud.connect.Meta;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.Set;

@Model(adaptables={SlingHttpServletRequest.class}, adapters={Meta.class})
public class MetaImpl implements Meta {

    @Self(injectionStrategy=InjectionStrategy.REQUIRED)
    private SlingHttpServletRequest request;
    @SlingObject(injectionStrategy=InjectionStrategy.REQUIRED)
    private ResourceResolver resourceResolver;
    private Resource resource;

    @PostConstruct
    protected void postConstruct()
    {
        String suffix = this.request.getRequestPathInfo().getSuffix();
        this.resource = this.resourceResolver.getResource(suffix);
    }

    public boolean isFolder()
    {
        return ResourceHelper.isResourceType(this.resource, new String[] { "nt:folder", "sling:Folder", "sling:OrderedFolder" });
    }

    public String getTitle()
    {
        if (this.resource != null) {
            return (String)this.resource.getValueMap().get("jcr:content/jcr:title", this.resource.getValueMap().get("jcr:title", this.resource.getName()));
        }
        return null;
    }

    public Set<String> getActionsRels()
    {
        Set<String> actions = new LinkedHashSet();
        if ((this.resource != null) && (this.request != null))
        {
            Resource contentResource = this.request.getResource();
            String uniqueConfName = null;
            boolean uniqueConfAlreadyExists = false;
            if (contentResource != null)
            {
                if (contentResource.getValueMap().get("uniqueConfigName") != null) {
                    uniqueConfName = contentResource.getValueMap().get("uniqueConfigName").toString();
                }
                boolean isRoot = "/conf".equals(this.resource.getPath());
                boolean hasCapability = this.resource.getChild("settings/cloudconfigs") != null;
                boolean hasSetting = ResourceHelper.hasSetting(this.resource, "settings/cloudconfigs/");
                if ((uniqueConfName != null) && (!uniqueConfName.isEmpty())) {
                    uniqueConfAlreadyExists = ResourceHelper.checkConfNodeExists(this.resource, uniqueConfName);
                }
                if ((!isRoot) && (hasCapability) && (!hasSetting) && (!uniqueConfAlreadyExists) &&
                        (Permissions.hasPermission(this.resourceResolver, this.resource.getPath(), "{http://www.jcp.org/jcr/1.0}addChildNodes"))) {
                    actions.add("cq-confadmin-actions-createconfig-activator");
                }
            }
        }
        return actions;
    }
}

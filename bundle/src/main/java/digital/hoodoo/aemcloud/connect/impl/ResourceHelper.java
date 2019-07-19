package digital.hoodoo.aemcloud.connect.impl;

import org.apache.sling.api.resource.Resource;

public final class ResourceHelper
{
    public static boolean isResourceType(Resource resource, String... resourceTypes)
    {
        if ((resource != null) && (resourceTypes != null)) {
            for (String resourceType : resourceTypes)
            {
                Resource child = resource.getChild("jcr:content");
                if (child != null) {
                    resource = child;
                }
                if (resource.isResourceType(resourceType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isConfigurationContainer(Resource resource)
    {
        return (resource != null) &&
                resource.getPath().startsWith("/conf") &&
                ((resource.isResourceType("sling:Folder")) || (resource.isResourceType("sling:OrderedFolder"))) &&
                (resource.getChild("settings") != null);
    }

    public static boolean hasSetting(Resource resource, String settingPath)
    {
        return (resource != null) && (resource.getChild(settingPath) != null);
    }

    public static boolean isConfiguration(Resource resource)
    {
        if (resource != null)
        {
            Resource parent = resource;
            do
            {
                if ("cloudconfigs".equals(parent.getName())) {
                    return true;
                }
                parent = parent.getParent();
            } while (parent != null);
        }
        return false;
    }

    public static boolean checkConfNodeExists(Resource resource, String uniqueConfName)
    {
        if ((resource != null) && (isConfigurationContainer(resource)))
        {
            Resource settingResource = resource.getChild("settings");
            if (settingResource != null)
            {
                Resource cloudconfigResource = settingResource.getChild("cloudconfigs");
                if (cloudconfigResource != null)
                {
                    Resource child = cloudconfigResource.getChild(uniqueConfName);
                    if (child != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}


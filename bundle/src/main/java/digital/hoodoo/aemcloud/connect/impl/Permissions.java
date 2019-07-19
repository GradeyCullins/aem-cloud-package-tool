package digital.hoodoo.aemcloud.connect.impl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Permissions
{
    private static final Logger LOG = LoggerFactory.getLogger(Permissions.class);

    public static boolean hasPermission(ResourceResolver resourceResolver, String path, String privilege)
    {
        try
        {
            Session session = (Session)resourceResolver.adaptTo(Session.class);
            AccessControlManager acm = session != null ? session.getAccessControlManager() : null;
            if ((acm != null) && (path != null && !path.isEmpty()) && (privilege != null && !privilege.isEmpty()))
            {
                Privilege p = acm.privilegeFromName(privilege);
                return acm.hasPrivileges(path, new Privilege[] { p });
            }
        }
        catch (RepositoryException e)
        {
            LOG.error("Unable to verify privilege " + privilege + " for path " + path, e);
        }
        return false;
    }
}

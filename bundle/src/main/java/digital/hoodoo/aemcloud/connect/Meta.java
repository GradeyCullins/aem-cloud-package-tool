package digital.hoodoo.aemcloud.connect;

import java.util.Set;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public abstract interface Meta
{
    public abstract boolean isFolder();

    public abstract String getTitle();

    public abstract Set<String> getActionsRels();
}

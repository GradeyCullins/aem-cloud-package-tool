package digital.hoodoo.aemcloud.connect;

import java.util.Calendar;
import java.util.Set;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public abstract interface Configuration
{
    public abstract String getTitle();

    public abstract boolean hasChildren();

    public abstract String getThumbnail();

    public abstract Calendar getLastModifiedDate();

    public abstract String getLastModifiedBy();

    public abstract Calendar getLastPublishedDate();

    public abstract Set<String> getQuickactionsRels();

    public abstract String getUrl();

    //AEM Cloud specific cloud config properties
    public abstract String getAemCloudUrl();

    public abstract String getAemCloudAccount();

    public abstract String getAemCloudKeyName();

    public abstract String getAemCloudKeyValue();
}

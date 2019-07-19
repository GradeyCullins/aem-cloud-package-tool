package digital.hoodoo.aemcloud.packagetool;

import digital.hoodoo.aemcloud.packagetool.models.PackageModel;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static List<PackageModel> getInverseList(List<PackageModel> packages) {
        List<PackageModel> result = new ArrayList<PackageModel>();
        for (int i = packages.size() - 1; i >= 0; i--) {
            result.add(packages.get(i));
        }
        return result;
    }
}

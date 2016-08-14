package ogp.com.gpstoggler3.apps;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppEnumerator {
    private Context context;


    public AppEnumerator(Context context) {
        this.context = context.getApplicationContext();
    }


    public ListAppStore execute() {
        final PackageManager 	pm 		 = context.getPackageManager();
        List<ApplicationInfo>   packages = pm.getInstalledApplications (PackageManager.GET_META_DATA);
        ListAppStore            apps = new ListAppStore();
        boolean 				empty    = true;

        for (ApplicationInfo applicationInfo : packages)
        {
            try
            {
                PackageInfo packageInfo = pm.getPackageInfo (applicationInfo.packageName,
                        PackageManager.GET_PERMISSIONS);

                if (null != packageInfo.requestedPermissions
                        &&
                        !applicationInfo.packageName.contains ("gpstoggler"))		// Self not included!
                {
                    for (int i = 0; i < packageInfo.requestedPermissions.length; i++)
                    {
                        if (packageInfo.requestedPermissions[i].equals (Manifest.permission.ACCESS_FINE_LOCATION))
                        {
                            String label = (String)applicationInfo.loadLabel (pm);

                            apps.add (new AppStore(label.replaceAll("\\n", " "), applicationInfo.packageName));
                            empty = false;
                            break;
                        }

                    }
                }
            }
            catch (PackageManager.NameNotFoundException e)
            {
                e.printStackTrace();
            }
        }


        if (!empty)
        {
            Collections.sort (apps, new Comparator<Object>()
            {
                public int compare (Object o1,
                                    Object o2)
                {
                    AppStore p1 = (AppStore)o1;
                    AppStore p2 = (AppStore)o2;

                    return p1.friendlyName.compareToIgnoreCase (p2.friendlyName);
                }
            });
        }

        return apps;
    }
}

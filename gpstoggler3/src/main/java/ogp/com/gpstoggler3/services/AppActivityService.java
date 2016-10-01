package ogp.com.gpstoggler3.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;


public class AppActivityService extends AccessibilityService {
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        Log.v(Constants.TAG, "AppActivityService::onServiceConnected. Entry...");

        AccessibilityServiceInfo serviceInfo = new AccessibilityServiceInfo();
        serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        serviceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            serviceInfo.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        }

        setServiceInfo(serviceInfo);
        Log.i(Constants.TAG, "AppActivityService::onServiceConnected. 'setServiceInfo' called.");

        Log.v(Constants.TAG, "AppActivityService::onServiceConnected. Exit.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.v(Constants.TAG, "AppActivityService::onAccessibilityEvent. Entry...");

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (null != event.getPackageName() && null != event.getClassName()) {
                ComponentName componentName = new ComponentName(event.getPackageName().toString(), event.getClassName().toString());

                if (null != tryGetActivity(componentName)) {
                    Log.d(Constants.TAG, "AppActivityService::onAccessibilityEvent. Window stack changed. Pass event to Automation thread.");

                    Intent intent = new Intent(Broadcasters.WINDOW_STACK_CHANGED);
                    sendBroadcast(intent);

                    Log.i(Constants.TAG, "AppActivityService::onAccessibilityEvent. Accessibility broadcast called.");
                }
            }
        }

        Log.v(Constants.TAG, "AppActivityService::onAccessibilityEvent. Exit.");
    }


    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }


    @Override
    public void onInterrupt() {
    }
}

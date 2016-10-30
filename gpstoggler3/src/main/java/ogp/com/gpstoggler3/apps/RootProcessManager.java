package ogp.com.gpstoggler3.apps;

class RootProcessManager {
    static class AndroidAppProcess {
        private String packageName;
        private boolean foreground;


        AndroidAppProcess(String packageName, boolean foreground) {
            this.packageName = packageName;
            this.foreground = foreground;
        }


        public String getPackageName() {
            return packageName;
        }


        boolean isForeground() {
            return foreground;
        }
    }
}

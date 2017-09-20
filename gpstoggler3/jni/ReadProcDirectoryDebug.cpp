#include <unistd.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/param.h>
#include <vector>
#include <sstream>
#include <iterator>

#include "debug.h"
#include "ReadProcDirectoryDebug.h"

#define PROC_DIR                    "/proc"
#define CMDLINE                     "cmdline"
#define CGROUP                      "cgroup"
#define STAT                        "stat"
#define CPUSET                      "cpuset"

#define LOOK_CPUSTAT_FOREGROUND     "foreground"
#define LOOK_CPUSTAT_TOP_APP        "top-app"
#define LOOK_BG_NON_INTERACTIVE     "cpu:/bg_non_interactive"
#define FOREGROUND                  'F'
#define BACKGROUND                  'B'
#define MAXFILEDATA                 4096
#define FOREGROUND_INDEX            40
#define FOREGROUND_VALUE            '0'


const char *CReadProcDirectoryDebug::COMMAND = "read_proc_debug";

#define DEBUG_PACKAGE "com.waze"


CReadProcDirectoryDebug::CReadProcDirectoryDebug(void) : CReadProcDirectory() {
}


CReadProcDirectoryDebug::~CReadProcDirectoryDebug() {
}


bool CReadProcDirectoryDebug::execute (std::string &output) {
    LOGV("CReadProcDirectoryDebug::execute. Entry...");

    DIR *pDir;
    struct dirent *pDirEntry;
    struct stat sStat;
    char appData[MAXPATHLEN + 1];
    char appCmd[MAXFILEDATA +  1];
    char appStat[MAXFILEDATA +  1];
    bool success = false;
    bool useCPUSet = true;
    bool useCGroup = true;
    int counted = 0;

    output = "";

    pDir = opendir (PROC_DIR);
    if (NULL != pDir) {
        while ((pDirEntry = readdir (pDir)) != NULL) {
            if (1 > strlen (pDirEntry->d_name) || '0' > pDirEntry->d_name[0] || '9' < pDirEntry->d_name[0] || DT_DIR != pDirEntry->d_type) {
                continue;
            }

            //LOGV("CReadProcDirectory::execute. Found name: %s.", pDirEntry->d_name);

            sprintf (appData, "%s/%s/%s", PROC_DIR, pDirEntry->d_name, CMDLINE);
            if (!readFile (appData, appCmd, MAXFILEDATA)) {         // Find application package
                continue;
            }

            // Simple sanity checks.
            if (NULL != strchr (appCmd, '/') || NULL != strchr (appCmd, ':') || NULL == strchr (appCmd, '.')) {
                continue;
            }

            //LOGV("Package name: %s", appCmd);

            bool bForeground = true;
            bool bDebugProc = NULL != strstr(appCmd, DEBUG_PACKAGE);

            if (useCPUSet) {
                sprintf (appData, "%s/%s/%s", PROC_DIR, pDirEntry->d_name, CPUSET);
                if (readFile (appData, appStat, MAXFILEDATA)) {     // Find application 'cpuset'.
                    if (NULL == strstr (appStat, LOOK_CPUSTAT_FOREGROUND) && NULL == strstr (appStat, LOOK_CPUSTAT_TOP_APP)) {
                        bForeground = false;

                        if (bDebugProc) {
                            LOGI("CReadProcDirectoryDebug::execute. [%s] is background [1].", appCmd);
                        } else {
                            LOGV("CReadProcDirectoryDebug::execute. [%s] is background [1].", appCmd);
                        }
                    }
                } else {
                    useCPUSet = false;
                }
            }

            if (useCGroup) {
                sprintf (appData, "%s/%s/%s", PROC_DIR, pDirEntry->d_name, CGROUP);
                if (readFile (appData, appStat, MAXFILEDATA)) {     // Find application 'cgroup'.
                    if (NULL != strstr (appStat, LOOK_BG_NON_INTERACTIVE)) {
                        bForeground = false;

                        if (bDebugProc) {
                            LOGI("CReadProcDirectoryDebug::execute. [%s] is background [2].", appCmd);
                        } else {
                            LOGV("CReadProcDirectoryDebug::execute. [%s] is background [2].", appCmd);
                        }
                    }
                } else {
                    useCGroup = false;
                }
            }

            if (!useCGroup) {
                sprintf (appData, "%s/%s/%s", PROC_DIR, pDirEntry->d_name, STAT);
                if (readFile (appData, appStat, MAXFILEDATA)) {     // Find application 'stat'.
                    std::istringstream buffer(appStat);
                    std::vector<std::string> split;

                    std::copy(std::istream_iterator<std::string>(buffer), std::istream_iterator<std::string>(), std::back_inserter(split));

                    if (FOREGROUND_VALUE != split[FOREGROUND_INDEX][0]) {
                        bForeground = false;

                        if (bDebugProc) {
                            LOGI("CReadProcDirectoryDebug::execute. [%s] is background [3].", appCmd);
                        } else {
                            LOGV("CReadProcDirectoryDebug::execute. [%s] is background [3].", appCmd);
                        }
                    }
                } else {
                    continue;
                }
            }

            if (bForeground) {
                if (bDebugProc) {
                    LOGI("CReadProcDirectoryDebug::execute. [%s] is foreground [0].", appCmd);
                } else {
                    LOGV("CReadProcDirectoryDebug::execute. [%s] is foreground [0].", appCmd);
                }
            }

            sprintf (appData, "%c%s\n", bForeground ? FOREGROUND : BACKGROUND, appCmd);
            output += appData;
            counted++;
        }

        closedir(pDir);
        success = true;
    } else {
        LOGE("CReadProcDirectoryDebug::execute. Failed to open '/proc' directory.");
    }

    LOGD("CReadProcDirectoryDebug::execute. Encountered %d application(s).", counted);
    LOGV("CReadProcDirectoryDebug::execute. Exit: %s.", output.c_str());
    return success;
}

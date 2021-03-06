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
#include "ReadProcDirectory.h"

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


const char *CReadProcDirectory::COMMAND = "read_proc";

CReadProcDirectory::CReadProcDirectory(void) : CCommand() {
}


CReadProcDirectory::~CReadProcDirectory() {
}


bool CReadProcDirectory::execute (std::string &output) {
    LOGV("CReadProcDirectory::execute. Entry...");

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

            if (useCPUSet) {
                sprintf (appData, "%s/%s/%s", PROC_DIR, pDirEntry->d_name, CPUSET);
                if (readFile (appData, appStat, MAXFILEDATA)) {     // Find application 'cpuset'.
                    if (NULL == strstr (appStat, LOOK_CPUSTAT_FOREGROUND) && NULL == strstr (appStat, LOOK_CPUSTAT_TOP_APP)) {
                        bForeground = false;
                        LOGV("CReadProcDirectory::execute. [%s] is background [1].", appCmd);
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
                        LOGV("CReadProcDirectory::execute. [%s] is background [2].", appCmd);
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
                        LOGV("CReadProcDirectory::execute. [%s] is background [3].", appCmd);
                    }
                } else {
                    continue;
                }
            }

            if (bForeground) {
                LOGV("CReadProcDirectory::execute. [%s] is foreground [0].", appCmd);
            }

            sprintf (appData, "%c%s\n", bForeground ? FOREGROUND : BACKGROUND, appCmd);
            output += appData;
            counted++;
        }

        closedir(pDir);
        success = true;
    } else {
        LOGE("CReadProcDirectory::execute. Failed to open '/proc' directory.");
    }

    LOGD("CReadProcDirectory::execute. Encountered %d application(s).", counted);
    LOGV("CReadProcDirectory::execute. Exit: %s.", output.c_str());
    return success;
}


bool CReadProcDirectory::readFile (const char *pcszFilePath, char *pszData, int nMaxData) {
    bool success = false;

    FILE *pFile = fopen(pcszFilePath, "rb");
    if (NULL != pFile) {
        int actually = 0;
        if (0 < (actually = fread (pszData, 1, nMaxData, pFile))) {
            success = true;
            pszData[actually] = '\0';
        }

        fclose(pFile);
    }

    return success;
}

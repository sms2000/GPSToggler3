#include <jni.h>
#include <android/log.h>

#include <sys/mount.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/reboot.h>
#include <linux/stat.h>
#include <errno.h>


#include "main.h"
#include "CommandFactory.h"


#define LOG_TAG    			"GPSToggler3"

#define LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,   LOG_TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,    LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,    LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,   LOG_TAG, __VA_ARGS__)


int main (int argc, char *argv[]) {
    int ret = EXIT_SUCCESS;

    LOGV("Native::main. Entry...");

    LOGI("Native::main. Number of parameters beyond the own path is %d", argc - 1);
    printf("Native::main. Number of parameters beyond the own path is %d\n", argc - 1);
    for (int i = 1; i < argc; i++) {
        printf("Native::main. Argument %d is %s\n", i, argv[i]);
    	LOGI("Native::main. Argument %d is %s", i, argv[i]);
    }

    if (argc > 2) {
        CCommand *pCommand = CCommandFactory::create(argv[2]);
        if (NULL != pCommand) {
            std::string output;
            if (pCommand->execute(output)) {
            	LOGI("Native::main. Command [%s] executed. Output: %s.", argv[2], output.c_str());
            } else {
            	LOGE("Native::main. Command [%s] failed.", argv[2]);
            }
        } else {
          	LOGE("Native::main. Command [%s] not found in the Factory.", argv[2]);
        }
    }

    if (argc > 1) {
        printf ("%s\n", argv[1]);           // Finalizer
        LOGI("Native::main. Finalazing...");
    }

    LOGV("Native::main. Exit.");
    return ret;
}

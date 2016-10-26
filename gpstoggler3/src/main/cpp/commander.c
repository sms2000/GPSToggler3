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


#include "commander.h"

#define LOG_TAG    			"GPSToggler3"

#define LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,   LOG_TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,    LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,    LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,   LOG_TAG, __VA_ARGS__)


int main (int argc, char *argv[]) {
    printf("Obsession data 1\n"); bbbbbbbbbbbb

    int ret = EXIT_SUCCESS;
    printf("Obsession data 2\n");

    LOGV("Native::main. Entry...");
    printf("Obsession data 3\n");

    LOGI("Native::main. Number of parameters beyond the own path is %d", argc - 1);
    printf("Obsession data 4\n");
    for (int i = 1; i < argc; i++) {
        printf("Obsession data 5\n");
    	LOGI("Native::  %s", argv[i]);
    }

    printf("Obsession data 6\n");
    LOGV("Native::main. Exit.");
    printf("Obsession data 7\n");
    return ret;
}

#include <jni.h>

#include <sys/mount.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/reboot.h>
#include <linux/stat.h>
#include <errno.h>


#include "main.h"
#include "debug.h"
#include "CommandFactory.h"


int main (int argc, char *argv[]) {
    LOGV("Native::main. Entry...");

    LOGD("Native::main. Number of parameters beyond the own path is %d", argc - 1);
    for (int i = 1; i < argc; i++) {
    	LOGD("Native::main. Argument %d is %s", i, argv[i]);
    }

    if (argc > 2) {
        CCommand *pCommand = CCommandFactory::create(argv[2]);
        if (NULL != pCommand) {
            std::string output;
            if (pCommand->execute(output)) {
            	LOGI("Native::main. Command [%s] executed.", argv[2]);
            	LOGV("Native::main. Output: %s.", output.c_str());
            	print_string(output.c_str());
            } else {
            	LOGE("Native::main. Command [%s] failed.", argv[2]);
            	print_error(ERR_FAILED);
            }
        } else {
          	LOGE("Native::main. Command [%s] not found in the Factory.", argv[2]);
          	print_error(ERR_UNKNOWN_COMMAND);
        }
    }

    if (argc > 1) {
        print_string(argv[1]);
        LOGI("Native::main. Finished.");
    }

    LOGV("Native::main. Exit.");
    return 0;
}


void print_string(const char *pszString) {
    printf ("%s\n", pszString);
}


void print_error(int error_value) {
    printf("ERROR %d\n", error_value);
}

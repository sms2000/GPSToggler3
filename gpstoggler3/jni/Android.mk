LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

TARGET_OUT                      := ${NDK_PROJECT_PATH}/src/main/assets/${TARGET_ARCH_ABI}/
TARGET_SONAME_EXTENSION	        := ~~so~~

LOCAL_MODULE    		        := commander
LOCAL_MODULE_FILENAME           := commander
LOCAL_SRC_FILES 		        := main.cpp Command.cpp ReadProcDirectory.cpp CommandFactory.cpp
LOCAL_LDLIBS    		        := -llog
LOCAL_FORCE_STATIC_EXECUTABLE 	:= true

include $(BUILD_EXECUTABLE)

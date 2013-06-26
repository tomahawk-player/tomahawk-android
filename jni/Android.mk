LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := spotify
LOCAL_SRC_FILES := libspotify.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := libspotifywrapper
LOCAL_SRC_FILES := run_loop.cpp tasks.cpp jni_glue.cpp logger.cpp
LOCAL_LDLIBS += -llog
LOCAL_SHARED_LIBRARIES := libspotify
LOCAL_CPPFLAGS = -std=c++0x -D__STDC_INT64__
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)

include $(BUILD_SHARED_LIBRARY)

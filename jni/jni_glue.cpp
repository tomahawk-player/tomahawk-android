/*
 Copyright (c) 2012, Spotify AB
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of Spotify AB nor the names of its contributors may
 be used to endorse or promote products derived from this software
 without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL SPOTIFY AB BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Wraps the JNI calls into tasks that will be running in the libspotify thread
 */

#include <stdlib.h>
#include <pthread.h>
#include <string>

#include <api.h>

#include "jni_glue.h"
#include "tasks.h"
#include "run_loop.h"
#include "logger.h"
#include "sound_driver.h"

using namespace std;

static JavaVM *s_java_vm;
static jobject s_java_class_loader = NULL;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
	// Save a reference to the java virtual machine to be able to call java from native thread
	s_java_vm = jvm;
	return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeinit(JNIEnv * je, jclass jc, jobject class_loader, jstring j_storage_path) {
	static pthread_t tid;
	static const char *storage_path;

	storage_path = je->GetStringUTFChars(j_storage_path, 0);

	// Save a reference to the java LibSpotify classloader to be able to retreive it from a native thread.
	if (s_java_class_loader != NULL)
		je->DeleteGlobalRef(s_java_class_loader);
	s_java_class_loader = je->NewGlobalRef(class_loader);

	pthread_setname_np(tid, "Spotify Thread");
	pthread_create(&tid, NULL, start_spotify, (void *) storage_path);

	init_audio_player();
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativelogin(JNIEnv *je, jclass jc, jstring j_username, jstring j_password, jstring j_blob) {

	const char *username = je->GetStringUTFChars(j_username, 0);
	const char *password = je->GetStringUTFChars(j_password, 0);
	const char *blob = je->GetStringUTFChars(j_blob, 0);

	list<string> string_params;
	string_params.push_back(username);
	string_params.push_back(password);
	string_params.push_back(blob);
	addTask(login, "login", string_params);

	je->ReleaseStringUTFChars(j_username, username);
	je->ReleaseStringUTFChars(j_password, password);
	je->ReleaseStringUTFChars(j_blob, blob);
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativerelogin(JNIEnv *je, jclass jc) {
	addTask(relogin, "relogin");
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativelogout(JNIEnv *je, jclass jc) {
	addTask(logout, "logout");
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeprepare(JNIEnv *je, jclass jc, jstring j_uri) {
	const char *uri = je->GetStringUTFChars(j_uri, 0);

	list<string> string_params;
	string_params.push_back(uri);
	log("preparing uri %s", string_params.front().c_str());
	addTask(prepare, "prepare", string_params);

	je->ReleaseStringUTFChars(j_uri, uri);
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeplay(JNIEnv *je, jclass jc) {
	addTask(play, "play");
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativepause(JNIEnv *je, jclass jc) {
	addTask(pause, "pause");
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativestar(JNIEnv *je, jclass jc) {
	addTask(star, "star");
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeunstar(JNIEnv *je, jclass jc) {
	addTask(unstar, "unstar");
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeseek(JNIEnv *je, jclass jc, jint position) {
	list<int> int_params;
	int_params.push_back(position);

	addTask(seek, "seek", int_params);
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativedestroy(JNIEnv *je, jclass jc) {
	log("Cleanup native code");
	addTask(destroy, "destroy");
}

JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeresolve(JNIEnv *je, jclass jc, jstring j_qid, jstring j_query) {
    const char *qid = je->GetStringUTFChars(j_qid, 0);
    const char *query = je->GetStringUTFChars(j_query, 0);

	list<string> string_params;
	string_params.push_back(qid);
	string_params.push_back(query);
	addTask(resolve, "resolve", string_params);

	je->ReleaseStringUTFChars(j_qid, qid);
	je->ReleaseStringUTFChars(j_query, query);
}

// Helper for calling the specified static void/void java-method
void call_static_void_method(const char *method_name) {
	JNIEnv *env;
	jclass classLibSpotify = find_class_from_native_thread(&env);

	jmethodID methodId = env->GetStaticMethodID(classLibSpotify, method_name, "()V");
	env->CallStaticVoidMethod(classLibSpotify, methodId);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
	env->DeleteLocalRef(classLibSpotify);
}

// Makes it possible to do callbacks from native threads
jclass find_class_from_native_thread(JNIEnv **envSetter) {
	JNIEnv * env;
	int status = s_java_vm->GetEnv((void **) &env, JNI_VERSION_1_6);

	if (status < 0) {
		log("Failed to get JNI environment, assuming native thread");
		status = s_java_vm->AttachCurrentThread(&env, NULL);
		if (status < 0)
			exitl("callback_handler: failed to attach current thread");
	}
	*envSetter = env;

	if (s_java_class_loader == NULL)
		exitl("Could not find classloader");

	jclass cls = env->FindClass("java/lang/ClassLoader");
	jmethodID methodLoadClass = env->GetMethodID(cls, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");

	jstring className = env->NewStringUTF("org/tomahawk/libtomahawk/resolver/spotify/LibSpotifyWrapper");
	jclass result = (jclass) env->CallObjectMethod(s_java_class_loader, methodLoadClass, className);
	if (!result) {
		exitl("Cant find the LibSpotify class");
	}
	if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

	env->DeleteLocalRef(className);
	env->DeleteLocalRef(cls);
	return result;
}


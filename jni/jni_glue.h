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

#include <jni.h>

jclass find_class_from_native_thread(JNIEnv **envSetter);
void call_static_void_method(const char *method_name);

extern "C" {
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeinit(JNIEnv *je, jclass jc, jobject loader, jstring j_storage_path);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativedestroy(JNIEnv *je, jclass jc);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeresolve(JNIEnv *je, jclass jc, jstring j_qid, jstring j_query);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativelogin(JNIEnv *je, jclass jc, jstring j_username, jstring j_password, jstring j_blob);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativerelogin(JNIEnv *je, jclass jc);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativelogout(JNIEnv *je, jclass jc);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeprepare(JNIEnv *je, jclass jc, jstring j_uri);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeplay(JNIEnv *je, jclass jc);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativepause(JNIEnv *je, jclass jc);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeseek(JNIEnv *je, jclass jc, jint position);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativestar(JNIEnv *je, jclass jc);
JNIEXPORT void JNICALL Java_org_tomahawk_libtomahawk_resolver_spotify_LibSpotifyWrapper_nativeunstar(JNIEnv *je, jclass jc);

}

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
 * Tasks that can be added to the queue of tasks running on the libspotify thread
 */
#include "tasks.h"
#include "run_loop.h"
#include "jni_glue.h"
#include "logger.h"
#include "sound_driver.h"

static int s_player_position = 0;
static string s_current_uri;
static bool s_is_playing = false;
static bool s_is_waiting_for_metadata = false;
static bool s_play_after_loaded = false;

static void on_pause();
static void on_play();
static void on_starred();
static void on_unstarred();
static void set_star(bool is_starred, sp_session *session, sp_track *track);

void login(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	if (session == NULL)
		exitl("Logged in before session was initialized");
	string username = string_params.front();
	string_params.pop_front();
	string password = string_params.front();
	string_params.pop_front();
	string blob = string_params.front();
	log ("login %s",username.c_str());
	sp_error error;
	if (password.empty() && !blob.empty()){
	    error = sp_session_login(session, username.c_str(), NULL, true, blob.c_str());
	}
	else if (!password.empty() && blob.empty()){
        error = sp_session_login(session, username.c_str(), password.c_str(), true, NULL);
    }
    if (error != SP_ERROR_OK)
        log ("!!!login error occurred: %s",sp_error_message(error));
}

void relogin(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
    if (session == NULL)
        exitl("Logged in before session was initialized");
    sp_error error = sp_session_relogin(session);
    if (error != SP_ERROR_OK)
        log ("!!!relogin error occurred: %s",sp_error_message(error));
}

void logout(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
    if (session == NULL)
        exitl("Logged out before session was initialized");
    sp_error error = sp_session_logout(session);
    if (error != SP_ERROR_OK)
        log ("!!!logout error occurred: %s",sp_error_message(error));
}

static void SP_CALLCONV search_complete(sp_search *search, void *userdata) {
    JNIEnv *env;
    jclass classLibspotify = find_class_from_native_thread(&env);
	bool success = (sp_search_error(search) == SP_ERROR_OK) ? true : false;
	int count = sp_search_num_tracks(search);
    jstring j_trackname;
    jstring j_trackuri;
    jstring j_albumname;
    jstring j_artistname;
	sp_track *track;
	for (int i=0;i< count;i++){
	    track = sp_search_track(search, i);
        if (track != 0 && sp_track_error(track) == SP_ERROR_OK){
            const char *temp = sp_track_name(track);
            if (temp != 0 && strlen(temp) != 0){
                j_trackname = env->NewStringUTF(temp);
            }
            int trackDuration = sp_track_duration(track);
            if (sp_track_error(track) != SP_ERROR_OK)
                log("sp_track_error: %s",sp_error_message(sp_track_error(track)));
            int trackDiscnumber = sp_track_disc(track);
            if (sp_track_error(track) != SP_ERROR_OK)
                log("sp_track_error: %s",sp_error_message(sp_track_error(track)));
            int trackIndex = sp_track_index(track);
            if (sp_track_error(track) != SP_ERROR_OK)
                log("sp_track_error: %s",sp_error_message(sp_track_error(track)));
            char buffer [64];
            sp_link *link = sp_link_create_from_track(track, 0);
            if (link != 0){
                sp_link_as_string(link, buffer, 64);
            }
            j_trackuri = env->NewStringUTF(buffer);
            sp_album *album = sp_track_album(track);
            if (sp_track_error(track) != SP_ERROR_OK)
                log("sp_track_error: %s",sp_error_message(sp_track_error(track)));
            int albumYear = 0;
            if (album != 0){
                temp = sp_album_name(album);
                albumYear = sp_album_year(album);
                if (temp != 0 && strlen(temp) != 0){
                    j_albumname = env->NewStringUTF(temp);
                }
            }
            sp_artist *artist = sp_track_artist(track,0);
            if (sp_track_error(track) != SP_ERROR_OK)
                log("sp_track_error: %s",sp_error_message(sp_track_error(track)));
            if (artist != 0){
                temp = sp_artist_name(artist);
                if (temp != 0 && strlen(temp) != 0){
                    j_artistname = env->NewStringUTF(temp);
                }
            }
            jmethodID methodIdAddResult = env->GetStaticMethodID(classLibspotify, "addResult",
                "(Ljava/lang/String;IIILjava/lang/String;Ljava/lang/String;ILjava/lang/String;)V");
            env->CallStaticVoidMethod(classLibspotify, methodIdAddResult, j_trackname,
                trackDuration, trackDiscnumber, trackIndex, j_trackuri,
                j_albumname, albumYear, j_artistname);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
            env->DeleteLocalRef(j_trackname);
            env->DeleteLocalRef(j_trackuri);
            env->DeleteLocalRef(j_artistname);
            env->DeleteLocalRef(j_albumname);
	        j_trackname = NULL;
	        j_trackuri = NULL;
	        j_artistname = NULL;
	        j_albumname = NULL;
        }
	}
	string &qid = *static_cast<string*>(userdata);
    jmethodID methodIdOnResolved = env->GetStaticMethodID(classLibspotify, "onResolved",
	    "(Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;)V");
    env->CallStaticVoidMethod(classLibspotify, methodIdOnResolved, env->NewStringUTF(qid.c_str()), success,
        env->NewStringUTF(sp_error_message(sp_search_error(search))),
        env->NewStringUTF(sp_search_did_you_mean(search)));
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
	env->DeleteLocalRef(classLibspotify);

    log("Finished resolving query:'%s', success'%s', track count:'%d', qid:'%s'", sp_search_query(search),
        (success?"true":"false"), count, qid.c_str());
    sp_search_release(search);
    delete &qid;
}

void resolve(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	if (session == NULL)
		exitl("Tried to resolve before session was initialized");
    string *qid = new string(string_params.front());
	string query = string_params.back();
	log("resolve| session is %s, query:'%s' qid:'%s'", session==0?"null":"not null", query.c_str(), qid->c_str());
    sp_search_create(session, query.c_str(), 0, 100, 0, 100, 0, 100, 0, 100, SP_SEARCH_STANDARD, &search_complete, qid);
    log("Beginning to resolve query:'%s', qid:'%s'", query.c_str(), qid->c_str());
}

void setbitrate(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	if (session == NULL)
		exitl("Tried to setbitrate before session was initialized");
	int bitratemode = int_params.front();
	sp_bitrate bitrate;
	switch (bitratemode) {
        case 0:
            bitrate = SP_BITRATE_96k;
            break;
        case 1:
            bitrate = SP_BITRATE_160k;
            break;
        case 2:
            bitrate = SP_BITRATE_320k;
            break;
	}
	sp_error error = sp_session_preferred_bitrate(session, bitrate);
	log ("setbitrate set to mode " + bitratemode);
    if (error != SP_ERROR_OK)
        log ("!!!setbitrate error occurred: %s",sp_error_message(error));
}

static void play_track(sp_session *session, sp_track *track) {
	//unmute(opensl);
	sp_session_player_play(session, true);
	s_is_playing = true;
	on_play();
}

// Loads a track and assumes that the metadata is available
static void load_and_play_track(sp_session *session, sp_track *track) {
	sp_session_player_load(session, track);
	if (s_play_after_loaded)
		play_track(session, track);
	(sp_track_is_starred(session, track)) ? on_starred() : on_unstarred();
	call_static_void_method("onPrepared");
}

// Load the track if the metadata update was concerning the track
void load_and_play_track_after_metadata_updated(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	if (s_is_waiting_for_metadata && sp_track_is_loaded(track)) {
		s_is_waiting_for_metadata = false;
		load_and_play_track(session, track);
	}
}

// Loads track if metadata exists, otherwise load the metadata
static void load_track_or_metadata(sp_session *session, sp_track *track, const char *uri) {
	if (track != NULL) {
		if (s_is_playing)
			sp_session_player_play(session, false);
		sp_session_player_unload(session);
		sp_track_release(track);
	}
	track = sp_link_as_track(sp_link_create_from_string(uri));
	set_track(track);
	sp_track_add_ref(track);
	s_player_position = 0;
	s_current_uri = uri;

	// either the track is already cached and can be used or we need to wait for the metadata callback
	if (sp_track_is_loaded(track))
		load_and_play_track(session, track);
	else
		s_is_waiting_for_metadata = true;
}

// Play the song with the given uri. Only playing when it was previously playing.
void prepare(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	string uri = string_params.front();

	s_play_after_loaded = s_is_playing;
	load_track_or_metadata(session, track, uri.c_str());
}

void play(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	if (!s_is_playing) {
		s_is_playing = true;
		sp_session_player_play(session, true);
	}
}

void pause(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	if (s_is_playing) {
		s_is_playing = false;
		sp_session_player_play(session, false);
	}
}

void star(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	set_star(true, session, track);
	on_starred();
}

void unstar(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	set_star(false, session, track);
	on_unstarred();
}

void seek(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	int pos_ms = int_params.front();

	if (s_is_playing)
		sp_session_player_play(session, false);
	sp_session_player_seek(session, pos_ms);
	if (s_is_playing)
		sp_session_player_play(session, true);
	s_player_position = pos_ms;
}

void on_player_position_changed(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	s_player_position+=1000;

	JNIEnv *env;
	jclass classLibSpotify = find_class_from_native_thread(&env);

	jmethodID methodId = env->GetStaticMethodID(classLibSpotify,"onPlayerPositionChanged","(I)V");
	env->CallStaticVoidMethod(classLibSpotify, methodId, s_player_position);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
	env->DeleteLocalRef(classLibSpotify);
}

void on_end_of_track(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	call_static_void_method("onEndOfTrack");
}

void on_logged_in(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	sp_error error = (sp_error)int_params.front();
	bool success = (SP_ERROR_OK == error) ? true : false;

	JNIEnv *env;
	jclass class_libspotify = find_class_from_native_thread(&env);

	jmethodID methodId = env->GetStaticMethodID(class_libspotify, "onLogin", "(ZLjava/lang/String;Ljava/lang/String;)V");
	log("on_logged_in: success:%s, error %s, sp_error_message(error) %s, session %s, sp_session_user_name(session) %s",
	    success?"true":"false", error==0?"null":"not null", sp_error_message(error)==0?"null":"not null",
	    session==0?"null":"not null", sp_session_user_name(session)==0?"null":"not null");
	env->CallStaticVoidMethod(class_libspotify, methodId, success, env->NewStringUTF(sp_error_message(error)),
	    env->NewStringUTF(sp_session_user_name(session)));
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
	env->DeleteLocalRef(class_libspotify);
}

void on_logged_out(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	call_static_void_method("onLogout");
}

void on_credentials_blob_updated(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track){
    string blob = string_params.front();

	JNIEnv *env;
	jclass class_libspotify = find_class_from_native_thread(&env);

	jmethodID methodId = env->GetStaticMethodID(class_libspotify, "onCredentialsBlobUpdated", "(Ljava/lang/String;)V");
	env->CallStaticVoidMethod(class_libspotify, methodId, env->NewStringUTF(blob.c_str()));
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
	env->DeleteLocalRef(class_libspotify);
}

void on_player_pause(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	on_pause();
}

void on_player_end_of_track(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	call_static_void_method("onPlayerEndOfTrack");
}

void destroy(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	sp_session_release(session);
	destroy_audio_player();
}

static void on_pause() {
	call_static_void_method("onPlayerPause");
}

static void on_play() {
	call_static_void_method("onPlayerPlay");
}

static void on_starred() {
	call_static_void_method("onTrackStarred");
}

static void on_unstarred() {
	log("Unstarred now");
	call_static_void_method("onTrackUnStarred");
}

static void set_star(bool is_starred, sp_session *session, sp_track *track) {
	if (sp_track_set_starred(session, &track, 1, is_starred) != SP_ERROR_OK)
		exitl("Could not star/unstar the track");
}

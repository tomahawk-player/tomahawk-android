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

static int s_player_position = 0;
static string s_current_uri;
static bool s_is_playing = false;
static bool s_is_waiting_for_metadata = false;
static bool s_play_after_loaded = false;
static string qid;

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
	log ("login %s, %s, %s",username.c_str(),password.c_str(),blob.c_str());
	if (password.empty() && !blob.empty()){
	    sp_session_login(session, username.c_str(), NULL, true, blob.c_str());
	}
	else if (!password.empty() && blob.empty()){
        sp_session_login(session, username.c_str(), password.c_str(), true, NULL);
    }
}

void relogin(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
    if (session == NULL)
        exitl("Logged in before session was initialized");
    sp_session_relogin(session);
}

static void SP_CALLCONV search_complete(sp_search *search, void *userdata) {
	bool success = (sp_search_error(search) == SP_ERROR_OK) ? true : false;

	JNIEnv *env;
	jclass class_libspotify = find_class_from_native_thread(&env);

	int count = sp_search_num_tracks(search);
	jmethodID methodId = env->GetStaticMethodID(class_libspotify, "addResult",
	    "(Ljava/lang/String;IIILjava/lang/String;Ljava/lang/String;ILjava/lang/String;)V");
	sp_track *track;
	for (int i=0;i< count;i++){
	    track = sp_search_track(search, i);
        if (track != 0 && sp_track_error(track) == SP_ERROR_OK){
            const char *trackName = "";
            const char *temp = sp_track_name(track);
            if (temp != 0){
                trackName = temp;
            }
            int trackDuration = sp_track_duration(track);
            int trackDiscnumber = sp_track_disc(track);
            int trackIndex = sp_track_index(track);
            char buffer [64];
            sp_link *link = sp_link_create_from_track(track, 0);
            if (link != 0)
                sp_link_as_string(link, buffer, 64);
            const char *trackUri = "";
            trackUri = buffer;
            sp_album *album = sp_track_album(track);
            const char *albumName = "";
            int albumYear = 0;
            if (album != 0){
                temp = sp_album_name(album);
                albumYear = sp_album_year(album);
                if (temp != 0){
                    albumName = temp;
                }
            }
            sp_artist *artist = sp_track_artist(track,0);
            const char *artistName = "";
            if (artist != 0){
                temp = sp_artist_name(artist);
                if (temp != 0){
                    artistName = temp;
                }
            }
            jstring j_trackname = env->NewStringUTF(trackName);
            jstring j_trackuri = env->NewStringUTF(trackUri);
            jstring j_albumname = env->NewStringUTF(albumName);
            jstring j_artistname = env->NewStringUTF(artistName);
            env->CallStaticVoidMethod(class_libspotify, methodId, j_trackname,
                trackDuration, trackDiscnumber, trackIndex, j_trackuri,
                j_albumname, albumYear, j_artistname);
            env->DeleteLocalRef(j_trackname);
            env->DeleteLocalRef(j_trackuri);
            env->DeleteLocalRef(j_albumname);
            env->DeleteLocalRef(j_artistname);
            sp_album_release(album);
            sp_artist_release(artist);
        }
	}
	sp_track_release(track);

	methodId = env->GetStaticMethodID(class_libspotify, "onResolved",
	    "(Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;)V");
    env->CallStaticVoidMethod(class_libspotify, methodId, env->NewStringUTF(qid.c_str()), success,
        env->NewStringUTF(sp_error_message(sp_search_error(search))),
        env->NewStringUTF(sp_search_did_you_mean(search)));
	env->DeleteLocalRef(class_libspotify);

    log("Finished resolving query:'%s', success'%s'. track count:'%d'", sp_search_query(search),
        (success?"true":"false"), count);
    sp_search_release(search);
}

void resolve(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	if (session == NULL)
		exitl("Tried to resolve before session was initialized");
	qid = string_params.front();
	string query = string_params.back();
    sp_search_create(session, query.c_str(), 0, 100, 0, 100, 0, 100, 0, 100, SP_SEARCH_STANDARD, &search_complete, NULL);
    log("Beginning to resolve query:'%s', qid:'%s'", query.c_str(), qid.c_str());
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
}

// Load the track if the metadata update was concerning the track
void load_and_play_track_after_metadata_updated(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	if (s_is_waiting_for_metadata == true && sp_track_is_loaded(track)) {
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
	if (sp_track_is_loaded(track)) {
		load_and_play_track(session, track);
	}
	else
		s_is_waiting_for_metadata = true;
}

// Play a new track. It will only play the song if the previous song was playing
void play_next(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	string uri = string_params.front();
	s_play_after_loaded = s_is_playing;
	load_track_or_metadata(session, track, uri.c_str());
}

// Play or resume the song
void toggle_play(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	string uri = string_params.front();
    s_play_after_loaded = true;
    load_track_or_metadata(session, track, uri.c_str());
}

void pause(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	if (s_is_playing) {
		s_is_playing = false;
		sp_session_player_play(session, false);
		//mute(opensl);
		on_player_pause(int_params, string_params, session, track);
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
	s_player_position++;

	JNIEnv *env;
	jclass classLibSpotify = find_class_from_native_thread(&env);

	jmethodID methodId = env->GetStaticMethodID(classLibSpotify,"onPlayerPositionChanged","(I)V");
	env->CallStaticVoidMethod(classLibSpotify, methodId, s_player_position);
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
	env->CallStaticVoidMethod(class_libspotify, methodId, success, env->NewStringUTF(sp_error_message(error)),
	    env->NewStringUTF(sp_session_user_name(session)));
	env->DeleteLocalRef(class_libspotify);
}

void on_logged_out(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	call_static_void_method("onLogout");
}

void on_connectionstate_updated(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
    log ("on_connection_state_update");
    int j_connectionstate;
    sp_connectionstate connectionstate = sp_session_connectionstate(session);
    if (connectionstate == SP_CONNECTION_STATE_LOGGED_IN){
        j_connectionstate = 1;
    }
    else{
        j_connectionstate = 0;
    }

	JNIEnv *env;
	jclass class_libspotify = find_class_from_native_thread(&env);

	jmethodID methodId = env->GetStaticMethodID(class_libspotify, "onConnectionStateUpdated", "(I)V");
	env->CallStaticVoidMethod(class_libspotify, methodId, j_connectionstate);
	env->DeleteLocalRef(class_libspotify);
}

void on_credentials_blob_updated(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track){
    string blob = string_params.front();

	JNIEnv *env;
	jclass class_libspotify = find_class_from_native_thread(&env);

	jmethodID methodId = env->GetStaticMethodID(class_libspotify, "onCredentialsBlobUpdated", "(Ljava/lang/String;)V");
	env->CallStaticVoidMethod(class_libspotify, methodId, env->NewStringUTF(blob.c_str()));
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

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
 * The main runloop handling libspotify events and posting tasks to libspotify.
 * There is a queue of tasks that will be processed in order. The loop will sleep
 * when there is no work to do.
 */
#include <pthread.h>
#include <errno.h>
#include <list>
#include <string>

#include "run_loop.h"
#include "logger.h"
#include "key.h"
#include "tasks.h"
#include "base64.h"

using namespace std;

// Defined in the sound_driver to keep the buffer logic together
int music_delivery(sp_session *sess, const sp_audioformat *format, const void *frames, int num_frames);

// There is always only one track that can be played/paused
static sp_track *s_track = NULL;

// A task contains the function, parameters and a task name
struct Task {
	task_fptr fptr;
	list<int> int_params;
	list<string> string_params;
	string name;

	Task(task_fptr _fptr, string _name, list<int> _int_params, list<string> _string_params) :
			fptr(_fptr), name(_name), int_params(_int_params), string_params(_string_params) {
	}
};

// The queue of tasks, thread safeness is important!
static list<Task> s_tasks;
// Mutex and condition for tasks and processing of tasks
static pthread_mutex_t s_notify_mutex;
static pthread_cond_t s_notify_cond;

// Keep track of next time sp_session_process_events should run
static int s_next_timeout = 0;

void set_track(sp_track *track) {
	s_track = track;
}

void addTask(task_fptr fptr, string name, list<int> int_params, list<string> string_params) {
	log("Add task <%s> to the queue", name.c_str());
	pthread_mutex_lock(&s_notify_mutex);
	Task task(fptr, name, int_params, string_params);
	s_tasks.push_back(task);
	pthread_cond_signal(&s_notify_cond);
	pthread_mutex_unlock(&s_notify_mutex);
}

void addTask(task_fptr fptr, string name, list<string> string_params) {
	list<int> int_params;
	addTask(fptr, name, int_params, string_params);
}

void addTask(task_fptr fptr, string name, list<int> int_params) {
	list<string> string_params;
	addTask(fptr, name, int_params, string_params);
}

void addTask(task_fptr fptr, string name) {
	list<int> int_params;
	list<string> string_params;
	addTask(fptr, name, int_params, string_params);
}

static void connection_error(sp_session *session, sp_error error) {
	log("------------- Connection error: %s\n -------------", sp_error_message(error));
}

static void logged_out(sp_session *session) {
    addTask(on_logged_out, "logout_callback");
}

static void log_message(sp_session *session, const char *data) {
	log("************* Message: %s *************", data);
}

static void play_token_lost(sp_session *session) {
	addTask(pause, "token_lost:pause");
}

// Tell java about the end of track
static void end_of_track(sp_session *sess) {
	addTask(on_player_end_of_track, "on_end_of_track");
}

static void logged_in(sp_session *session, sp_error error) {
	list<int> int_params;
	int_params.push_back(error);

	addTask(on_logged_in, "login_callback", int_params);
}

static void process_events(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track) {
	do {
		sp_session_process_events(session, &s_next_timeout);
	} while (s_next_timeout == 0);
}

// run process_events on the libspotify thread
static void notify_main_thread(sp_session *session) {
	addTask(process_events, "Notify main thread: process_events");
}

// Try to start the track again if it was loaded
static void metadata_updated(sp_session *session) {
	addTask(load_and_play_track_after_metadata_updated, "load_and_play_track_after_metadata_updated");
}

static void credentials_blob_updated(sp_session *session, const char *blob) {
    list<string> string_params;
    string_params.push_back(blob);

    addTask(on_credentials_blob_updated, "on_credentials_blob_updated", string_params);
}

static sp_session_callbacks callbacks = {
	&logged_in,
	&logged_out,
	&metadata_updated,
	&connection_error,
	NULL,
	&notify_main_thread,
	&music_delivery,
	&play_token_lost,
	&log_message,
	&end_of_track,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    &credentials_blob_updated
};

// The main loop takes care of executing tasks on the libspotify thread.
static void libspotify_loop(sp_session *session) {

	while (true) {
		pthread_mutex_lock(&s_notify_mutex);

		// If no tasks then sleep until there is, or a timeout happens
		if (s_tasks.size() == 0) {
			struct timespec ts;
			clock_gettime(CLOCK_REALTIME, &ts);
			struct timespec timer_start = ts;
			ts.tv_sec += s_next_timeout / 1000;
			ts.tv_nsec += (s_next_timeout % 1000) * 1000000;

			log("Wait for new task or until %d ms", s_next_timeout);
			int reason = pthread_cond_timedwait(&s_notify_cond, &s_notify_mutex, &ts);
			// If timeout then process_events should be added to the queue
			if (reason == ETIMEDOUT) {
				pthread_mutex_unlock(&s_notify_mutex);
				addTask(process_events, "Timeout: process events");
				pthread_mutex_lock(&s_notify_mutex);
			} else { // calculate a new timeout (assuming the tasks below takes 0 time)
				struct timespec timer_end;
				clock_gettime(CLOCK_REALTIME, &timer_end);
				int delta = 0;
				delta += (timer_end.tv_sec - timer_start.tv_sec) * 1000;
				delta += (timer_end.tv_nsec - timer_start.tv_nsec) / 1000000;
				s_next_timeout -= delta;
			}
		}
		// create a copy of the list of operations since other thread can manipulate it and clear the real one
		list<Task> tasks_copy = s_tasks;
		s_tasks.clear();
		pthread_mutex_unlock(&s_notify_mutex);

		for (list<Task>::iterator it = tasks_copy.begin(); it != tasks_copy.end(); it++) {
			Task task = (*it);
			log("Running task: %s", task.name.c_str());
			task.fptr(task.int_params, task.string_params, session, s_track);
		}
	}
}

void* start_spotify(void *storage_path) {
    string path = (char *)storage_path;

    pthread_mutex_init(&s_notify_mutex, NULL);
    pthread_cond_init(&s_notify_cond, NULL);

    sp_session *session;
    sp_session_config config;

    // Libspotify does not guarantee that the structures are freshly initialized
    memset(&config, 0, sizeof(config));

    string cache_location = path + "/cache";
    string settings_location = path = "/settings";

    config.api_version = SPOTIFY_API_VERSION;
    config.cache_location = cache_location.c_str();
    config.settings_location = settings_location.c_str();
    int plain_g_appkey_size;
    unsigned char *plain_g_appkey = unbase64(g_appkey, g_appkey_size, &plain_g_appkey_size);
    config.application_key = plain_g_appkey;
    config.application_key_size = plain_g_appkey_size;
    config.user_agent = "TomahawkAndroid";
    config.callbacks = &callbacks;
    config.tracefile = NULL;

    sp_error error = sp_session_create(&config, &session);
    log("Libspotify was initiated");

    if (SP_ERROR_OK != error)
        exitl("failed to create session: %s\n", sp_error_message(error));

    // start the libspotify loop
    libspotify_loop(session);
}

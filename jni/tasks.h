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

/*
 * tasks.h
 *
 *  Created on: Nov 9, 2012
 *      Author: johanneskaehlare
 */

#ifndef TASKS_H_
#define TASKS_H_

#include <list>
#include <string>
#include <sstream>

#include <api.h>

using namespace std;

void login(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void relogin(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void logout(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void prepare(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void play(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void pause(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void star(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void unstar(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void seek(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void load_and_play_track_after_metadata_updated(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void destroy(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void resolve(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void setbitrate(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);

// Callbacks to java
void on_init(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void on_logged_in(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void on_logged_out(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void on_credentials_blob_updated(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void on_player_position_changed(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void on_player_end_of_track(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);
void on_player_pause(list<int> int_params, list<string> string_params, sp_session *session, sp_track *track);

#endif /* TASKS_H_ */

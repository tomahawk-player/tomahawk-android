/*
 *   Copyright 2014, Uwe L. Korn <uwelk@xhochy.com>
 *
 *   The MIT License (MIT)
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *   FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *   COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *   IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

#include <microhttpd.h>

#include <cstring>
#include <iostream>
#include <mutex>

// Typedef the pointers for better readability.
typedef struct MHD_Connection* connection_ptr;
typedef struct MHD_Daemon* daemon_ptr;
typedef struct MHD_Response* response_ptr ;

std::mutex exit_mutex;

int handle_exit(const connection_ptr connection)
{
    // Shutdown requested, unlock the relevant mutex.
    response_ptr response = MHD_create_response_from_data( strlen( "OK" ), (void*)"OK", MHD_NO, MHD_NO );
    int ret = MHD_queue_response( connection, MHD_HTTP_OK, response );
    MHD_destroy_response( response );
    // FIXME: Add a settle time so that we can actually send the response.
    exit_mutex.unlock();
    return ret;
}

static int ahc_echo( void* /*cls*/, connection_ptr connection, const char* url,
                     const char* method, const char* /*version*/, const char* /*upload_data*/,
                     size_t* upload_data_size, void** ptr)
{
    static int header_test;

    // We only expect GET requests.
    if ( strcmp( method, "GET" ) )
    {
        return MHD_NO;
    }

    // On the first call only the headers are valid. Reply in the second round.
    if ( &header_test != *ptr )
    {
        *ptr = &header_test;
        return MHD_YES;
    }

    // There should be no data uploaded in the GET request.
    if ( *upload_data_size )
    {
        return MHD_NO;
    }

    int ret;
    if ( !strcmp( url, "/exit" ) )
    {
        ret = handle_exit(connection);
    }
    else
    {
        response_ptr response = MHD_create_response_from_data( strlen(url), (void*)url, MHD_NO, MHD_NO );
        ret = MHD_queue_response( connection, MHD_HTTP_OK, response );
        MHD_destroy_response( response );
    }

    return ret;
}

int main( int argc, char* argv[] )
{
    // TODO: Kill all other instances on startup.

    if ( argc != 2 ) {
        std::cout << "Usage:" << std::endl;
        std::cout << "\t" << argv[0] << " <port>" << std::endl;
        return EXIT_FAILURE;
    }

    daemon_ptr daemon = MHD_start_daemon( MHD_USE_THREAD_PER_CONNECTION,
                                          atoi(argv[1]), nullptr, nullptr,
                                          &ahc_echo, nullptr, MHD_OPTION_END);
    if ( daemon == nullptr )
    {
        return EXIT_FAILURE;
    }

    // Lock mutex twice. The second call will hang until we receive an unlock
    // command from a different. This will initiate the shutdown process.
    exit_mutex.lock();
    exit_mutex.lock();

    // (void) getc ();

    MHD_stop_daemon(daemon);

    return EXIT_SUCCESS;
}

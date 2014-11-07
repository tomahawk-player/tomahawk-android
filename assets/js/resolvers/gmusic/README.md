
This is a Tomahawk resolver for Google Music.

[Tomahawk] is a music player which decouples your library and playlists
from individual sources and presents a unified view. When you play a
track Tomahawk will use the best version available from its configured
sources. You can build playlists and share tracks without thinking
about which of your services and libraries have a particular track.

This is a [resolver], a plugin for Tomahawk responsible for interfacing
with an external music source, in this case Google Music. It can stream
tracks from your uploaded library. When you search for tracks 
you'll get results from Google Music. In the future you'll also be able 
to browse your library and sync playlists.

[tomahawk]: http://www.tomahawk-player.org/
[resolver]: http://www.tomahawk-player.org/resolvers.html 

Installing
==========

In Tomahawk's Services configuration dialog, click "Install from File"
and choose `content/contents/code/gmusic.js`. Click the gear icon and
enter your Google email and password. Check the Tomahawk log file to
make sure the login succeeded. Unfortunately there's no way to provide
status information through the Tomahawk user interface.

Copying
=======

Written in 2013 by Sam Hanes <sam@maltera.com>
Heavily modified in 2014 by Lalit Maganti <lalitmaganti@gmail.com>

To the extent possible under law, the author(s) have dedicated all
copyright and related and neighboring rights to this software to
the public domain worldwide. This software is distributed without
any warranty.

You should have received a copy of the CC0 Public Domain Dedication
along with this software. If not, see:
http://creativecommons.org/publicdomain/zero/1.0/


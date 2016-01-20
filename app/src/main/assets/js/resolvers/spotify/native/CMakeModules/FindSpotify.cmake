find_package(PkgConfig)
pkg_check_modules(PC_SPOTIFY libspotify)

find_path(SPOTIFY_INCLUDE_DIR libspotify/api.h
    HINTS
        ${PC_SPOTIFY_INCLUDEDIR}
        ${PC_SPOTIFY_INCLUDE_DIRS}
)

find_library(SPOTIFY_LIBRARY NAMES spotify
    HINTS
    ${PC_SPOTIFY_LIBDIR}
    ${PC_SPOTIFY_LIBRARY_DIRS}
)

set(SPOTIFY_VERSION ${PC_SPOTIFY_VERSION})

find_package_handle_standard_args(Spotify
  REQUIRED_VARS SPOTIFY_INCLUDE_DIR SPOTIFY_LIBRARY
  VERSION_VAR SPOTIFY_VERSION
)


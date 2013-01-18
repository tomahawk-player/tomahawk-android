Tomahawk.resolverData =
    function () {
        return JSON.parse(Tomahawk.resolverDataString());
    };

Tomahawk.addTrackResults =
    function (results) {
        Tomahawk.addTrackResultsString(JSON.stringify(results));
    }
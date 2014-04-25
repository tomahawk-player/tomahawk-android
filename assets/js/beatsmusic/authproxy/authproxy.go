package authproxy

import (
    "html/template"
    "net/http"
)

func init() {
    http.HandleFunc("/json", jsonHandler)
    http.HandleFunc("/callback", handler)
}

const tokenTemplateHTML = `
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en-us">
<head>
    <meta http-equiv="content-type" content="text/html; charset=utf-8" />
    <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css">
    <title>Tomahawk Beats Login</title>
</head>
<body>
    <div class="container">
        <div class="row">
            <div class="col-md-4 col-md-offset-4">
                <h1 class="page-header">Token obtained</h1>
                <p class="lead">
                    Please copy the following token to Tomahawk to enable Beats support.
                </p>
                <div class="input-group">
                <input class="form-control" type="text" id="beatsmusic-token" value="{{.}}" disabled />
                    <span class="input-group-btn">
                        <button id="copy-button" class="btn btn-primary"><span class="glyphicon glyphicon-share"></span></button>
                    </span>
                </div>
            </div>
        </div>
    </div>
    <script src="//code.jquery.com/jquery-1.11.0.min.js"></script>
    <script type="text/javascript" src="/js/jquery.clipboard.js"></script>
    <script type="text/javascript">
        $(document).ready(function () {
            $("#copy-button").on("click", function (e) {
                e.preventDefault();
            });
            $("#copy-button").clipboard({
                path: '/jquery.clipboard.swf',
                copy: function () {
                    return $("#beatsmusic-token").val();
                }
            });
        });
    </script>
</body>
</html>
`

var tokenTemplate = template.Must(template.New("name").Parse(tokenTemplateHTML))

func handler(w http.ResponseWriter, r *http.Request) {
    err := tokenTemplate.Execute(w, r.FormValue("access_token"))
    if err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
    }
}

func jsonHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    w.Write([]byte("{ \"access_token\": \"" + r.FormValue("access_token") + "\" }"))
}

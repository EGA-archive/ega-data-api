import io.vertx.core.http.HttpHeaders

// Example replies for '/file'
if (context.request.uri ==~ /(.*)\/file([^\/]*)$/) {
    switch (context.request.method) {
        case 'GET':
            if(context.request.headers.get(HttpHeaders.AUTHORIZATION) == "AUTH_HEADER") {
                respond {
                    withStatusCode 204
                }
            } else {
                respond {
                    withFile 'example-file.txt'
                }
            }
            break
        default:
            respond {
                usingDefaultBehaviour()
            }
            break
    }
}

// Example replies for '/file/archive/{id}'
if (context.request.uri ==~ /(.*)\/file\/archive\/([^\/]*)$/) {
    switch (context.request.method) {
        case 'GET':
            if(context.request.headers.get(HttpHeaders.AUTHORIZATION) == "AUTH_HEADER") {
                respond {
                    withStatusCode 204
                }
            } else {
                respond {
                    withFile 'example-file.txt'
                }
            }
            break
        default:
            respond {
                usingDefaultBehaviour()
            }
            break
    }
}

// Example replies for '/file/archive/{id}/size'
if (context.request.uri ==~ /(.*)\/file\/archive\/(.*)\/size([^\/]*)$/) {
    switch (context.request.method) {
        case 'GET':
            if(context.request.headers.get(HttpHeaders.AUTHORIZATION) == "AUTH_HEADER") {
                respond {
                    withStatusCode 204
                }
            } else {
                respond {
                    withHeader('content-type:', 'text/plain')
                    withData '719'
                }
            }
            break
        default:
            respond {
                usingDefaultBehaviour()
            }
            break
    }
}

// Example replies for '/file/test1/size'
if (context.request.uri ==~ /(.*)\/file\/test1\/(.*)$/) {
    switch (context.request.method) {
        case 'GET':
            if(context.request.headers.get(HttpHeaders.AUTHORIZATION) == "AUTH_HEADER") {
                respond {
                    withStatusCode 204
                }
            } else {
                respond {
                    withHeader('content-type:', 'text/plain')
                    withData '551'
                }
            }
            break
        default:
            respond {
                usingDefaultBehaviour()
            }
            break
    }
}

// Example replies for '/file/test2/size'
if (context.request.uri ==~ /(.*)\/file\/test2\/(.*)$/) {
    switch (context.request.method) {
        case 'GET':
            if(context.request.headers.get(HttpHeaders.AUTHORIZATION) == "AUTH_HEADER") {
                respond {
                    withStatusCode 204
                }
            } else {
                respond {
                    withHeader('content-type:', 'text/plain')
                    withData '184'
                }
            }
            break
        default:
            respond {
                usingDefaultBehaviour()
            }
            break
    }
}

// Example replies for 'stats/load'
if (context.request.uri ==~ /(.*)\/stats\/load(.*)$/) {
    switch (context.request.method) {
        case 'GET':
            if(context.request.headers.get(HttpHeaders.AUTHORIZATION) == "AUTH_HEADER") {
                respond {
                    withStatusCode 204
                }
            } else {
                respond {
                    withHeader('content-type:', 'text/plain')
                    withData '1029'
                }
            }
            break
        default:
            respond {
                usingDefaultBehaviour()
            }
            break
    }
}

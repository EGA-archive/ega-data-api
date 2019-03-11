import io.vertx.core.http.HttpHeaders

// METADATA CONTROLLER

// Example replies for '/metadata/'.
if (context.request.uri ==~ /(.*)\/metadata\/(.*)/) {
    respond {
        usingDefaultBehaviour()
    }
}

// TICKETS CONTROLLER

// Example replies for GET 'tickets/**'
else if (context.request.uri ==~ /(.*)\/tickets\/variants\/(.*)/) {
    switch (context.request.method) {
        case 'OPTIONS':
            respond {
                withData('{' +
                         '  "body": {},' +
                         '  "statusCode": "100",' +
                         '  "statusCodeValue": 0' +
                         '}')
            }
            break
        case 'GET':
            respond {
                usingDefaultBehaviour()
            }
            break
        default:
            respond {
                usingDefaultBehaviour()
            }
    }
}
else if (context.request.uri ==~ /(.*)\/tickets\/files\/(.+)/) {
    switch (context.request.method) {
        case 'OPTIONS':
            respond {
                withData('{' +
                         '  "body": {},' +
                         '  "statusCode": "100",' +
                         '  "statusCodeValue": 0' +
                         '}')
            }
            break
        case 'GET':
            respond {
                usingDefaultBehaviour()
            }
            break
        default:
            respond {
                usingDefaultBehaviour()
            }
    }
}
else if (context.request.uri ==~ /(.*)\/tickets\/(.*)/) {
    switch (context.request.method) {
        case 'OPTIONS':
            respond {
                withHeader('Content-type:', 'application/json')
                withData('{' +
                         '  "body": {},' +
                         '  "statusCode": "100",' +
                         '  "statusCodeValue": 0' +
                         '}')
            }
            break
        default:
            respond {
                usingDefaultBehaviour()
            }
    }
}

// Example replies for '/files/**'
else if (context.request.uri ==~ /(.*)\/files(.*)$/) {
    switch (context.request.method) {
        case 'OPTIONS':
            if (context.request.uri ==~ /(.*)\/files\/variant\/byid\/(.*)$/) {
                respond {
                    withData('{' +
                                '  "body": {},' +
                                '  "statusCode": "100",' +
                                '  "statusCodeValue": 0' +
                                '}')
                }
            }
            else if (context.request.uri ==~ /(.*)\/files\/byid\/(.*)$/) {
                respond {
                    withData('{' +
                                '  "body": {},' +
                                '  "statusCode": "100",' +
                                '  "statusCodeValue": 0' +
                                '}')
                }
            }
            else {
                respond {
                    withData('{' +
                                '  "body": {},' +
                                '  "statusCode": "100",' +
                                '  "statusCodeValue": 0' +
                                '}')
                }
            }

            break
        case 'GET':
            // Example replies for GET /files/{fileId}
            if (context.request.uri ==~ /(.*)\/files\/([^\/]+)/) {
                respond {
                    withFile 'example-file.txt'
                }
            }
            // Example replies for GET /files/{fileId}/header
            else if (context.request.uri ==~ /(.*)\/files\/([^\/]+)\/header/) {
                respond {
                    usingDefaultBehaviour()
                }
            }
            // Example replies for GET /files/variant/byid/{type}
            else if (context.request.uri ==~ /(.*)\/files\/variant\/byid\/([^\/]+)/) {
                respond {
                    withFile 'example-file.txt'
                }
            }
            // Example replies for GET /files/byid/{type}
            else if (context.request.uri ==~ /(.*)\/files\/byid\/([^\/]+)/) {
                respond {
                    withFile 'example-file.txt'
                }
            }
            else {
                respond {
                    usingDefaultBehaviour()
                }
            }
            break
        case 'HEAD':
            // Example replies for HEAD /files/byid/{type}
            if (context.request.uri ==~ /(.*)\/files\/byid\/(.*)/) {
                respond {
                    withFile 'example-file.txt'
                }
            }
            // Example replies for HEAD /files/{fileId}
            else if (context.request.uri ==~ /(.*)\/files\/(.*)/) {
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

// STATS CONTROLLER

// Example replies for '/stats/'.
else if (context.request.uri ==~ /(.*)\/stats\/(.*)/) {
    if (context.request.uri ==~ /(.*)\/stats\/load/) {
        switch (context.request.method) {
            case 'OPTIONS':
                respond {
                    usingDefaultBehaviour()
                }
            break
            default:
                respond {
                    usingDefaultBehaviour()
                }
        }
    }
    else  if (context.request.uri ==~ /(.*)\/stats\/testme/) {
        switch (context.request.method) {
            case 'OPTIONS':
                respond {
                    usingDefaultBehaviour()
                }
            break
            case 'POST':
                respond {
                    usingDefaultBehaviour()
                }
            break
            default:
                respond {
                    usingDefaultBehaviour()
                }
        }
    }
    else {
        respond {
            usingDefaultBehaviour()
        }
    }
}

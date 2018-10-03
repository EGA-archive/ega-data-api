#!/usr/bin/env bash

OSS_MODULES=( ega-data-api-netflix/ega-data-api-config
              ega-data-api-netflix/ega-data-api-eureka
              ega-data-api-netflix/ega-data-api-hystrix )

EGA_API_MODULES=( ega-data-api-dataedge
                  ega-data-api-filedatabase
                  ega-data-api-key
                  ega-data-api-res )

push_images () {
    tag=$1
    if [ "$2" == "oss" ]; then
      maven_push "$tag" "${OSS_MODULES[@]}"
    elif [ "$2" == "api" ]; then
      maven_push "$tag" "${EGA_API_MODULES[@]}"
    else
      printf 'Option not recognized.'
      exit 1
    fi
}

maven_push () {
  tag=$1
  shift
  modules=( "$@" )
  for module in "${modules[@]}"; do
    printf 'Pushing EGA-DATA-API image for module: %s\n' "$module with tag $tag"
    mvn package -DskipTests docker:build -pl "$module" -DdockerRegistry="${DOCKER_REGISTRY}" -DpushImageTag -DdockerImageTags="$tag"
  done
}


printf '%s\n' "$DOCKER_PASSWORD" |
docker login -u "$DOCKER_USER" --password-stdin

## Travis run on master branch and not a PR (this is after a PR has been approved)
if  [ "$TRAVIS_BRANCH" = "master" ] &&
    [ "$TRAVIS_PULL_REQUEST" = "false" ]
then
    push_images latest api
    push_images latest oss
else
    mvn clean install -DskipDockerPush
fi

#!/bin/bash

# ---------------------------------------------------------
# docker check
# ---------------------------------------------------------
if ! docker version &> /dev/null; then
  echo "please install docker."
  echo "See: https://docs.docker.com/engine/install/"
  exit 1
fi

if ! docker compose version &> /dev/null; then
  echo "please install docker compose."
  echo "See: https://docs.docker.com/compose/install/"
  exit 1
fi

# ---------------------------------------------------------
# down
# ---------------------------------------------------------
function down()
{
  if [ -f compose.yml ]; then
    docker compose stop
    docker compose logs mc > last.log
    docker compose down
  fi
}

# ---------------------------------------------------------
# clean
# ---------------------------------------------------------
if [ "${1}" = "clean" ]; then
  echo "clean..."
  down
  rm -rf data-* plugins
  rm -f .env compose.yml last.log
  echo "done."
  exit
fi

# ---------------------------------------------------------
# default
# ---------------------------------------------------------
if [ -f /etc/timezone ]; then
  DEFAULT_TZ="$(cat /etc/timezone)"
else
  DEFAULT_TZ="Asia/Tokyo"
fi
DEFAULT_TAG="latest"
DEFAULT_NAME="sandbox"
DEFAULT_TYPE="paper"
DEFAULT_VERSION="latest"
DEFAULT_MODRINTH="viaversion,viabackwards,worldedit:beta"
DEFAULT_PORT="25565"

# ---------------------------------------------------------
# make compose.yml
# ---------------------------------------------------------
if [ ! -f compose.yml ]; then
  echo "make ... compose.yml"
  cat - << COMPOSE_EOF > compose.yml
services:
  mc:
    image: "itzg/minecraft-server:\${S_TAG:-${DEFAULT_TAG}}"
    container_name: "\${S_NAME:-${DEFAULT_NAME}}"
    environment:
      TZ: "\${S_TZ:-${DEFAULT_TZ}}"
      EULA: "true"
      TYPE: "\${S_TYPE:-${DEFAULT_TYPE}}"
      VERSION: "\${S_VERSION:-${DEFAULT_VERSION}}"
      MODE: "creative"
      FORCE_GAMEMODE: "true"
      MODRINTH_PROJECTS: "\${S_MODRINTH:-${DEFAULT_MODRINTH}}"
      RCON_CMDS_STARTUP: "\${S_OPERATOR}"
    ports:
      - "\${S_PORT:-${DEFAULT_PORT}}:25565"
    volumes:
      - ./data-\${S_TYPE:-${DEFAULT_TYPE}}-\${S_VERSION:-${DEFAULT_VERSION}}:/data
      - ./plugins:/plugins:ro
COMPOSE_EOF
fi

# ---------------------------------------------------------
# make .env
# ---------------------------------------------------------
if [ ! -f .env ]; then
  echo "make ... .env"
  cat - << ENV_EOF > .env
S_TAG="${DEFAULT_TAG}"
S_NAME="${DEFAULT_NAME}"
S_TZ="${DEFAULT_TZ}"
S_TYPE="${DEFAULT_TYPE}"
S_VERSION="${DEFAULT_VERSION}"
S_MODRINTH="${DEFAULT_MODRINTH}"
S_PORT="${DEFAULT_PORT}"
S_OPERATOR="op Muscle_p"
ENV_EOF
  echo "please, check and edit your .env file."
  exit
fi

# ---------------------------------------------------------
# update plugins
# ---------------------------------------------------------
mkdir -p plugins
SRCS=($(ls -t ../target/worldedit-selection-viewer-*.jar))
if [ ${#SRCS} -gt 0 ]; then
  cp -auv ${SRCS[0]} plugins/worldedit-selection-viewer.jar
else
  echo "worldedit-selection-viewer-*.jar was not found."
  echo "Run \"(cd ..; mvn clean package)\", if you need."
fi

# ---------------------------------------------------------
# launch minecraft server (CTRL+C to down)
# ---------------------------------------------------------
trap 'down' SIGINT
docker compose up


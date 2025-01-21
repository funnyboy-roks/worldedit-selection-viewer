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
# check MCID
# ---------------------------------------------------------
function checkMCID()
{
  if [[ "${1}" =~ ^\. ]]; then
    echo "Bedrock player"
    return 0
  fi
  local -r JSON=$(curl -s "https://api.mojang.com/users/profiles/minecraft/${1}")
  if [[ "${JSON}" =~ "\"id\" :" ]]; then
    echo "Java player"
    return 0
  fi
  echo "Player \"${1}\" does not found."
  return 1
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
# default parameters (do not edit)
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
DEFAULT_MODRINTH="viaversion,viabackwards,worldedit:beta,geyser:beta"
DEFAULT_MODS="https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot"
DEFAULT_PORT="25565"
DEFAULT_PORT_BE="19132"

# ---------------------------------------------------------
# generate compose.yml file
# ---------------------------------------------------------
if [ ! -f compose.yml ]; then
  echo "Making ... compose.yml"
  cat - << COMPOSE_EOF > compose.yml
# This is an automatically generated file.
# You can edit it as needed.
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
      MODS: "\${S_MODS:-${DEFAULT_MODS}}"
      RCON_CMDS_STARTUP: "\${S_OPERATOR}"
    ports:
      - "\${S_PORT:-${DEFAULT_PORT}}:25565"
      - "\${S_PORT_BE:-${DEFAULT_PORT_BE}}:19132/udp"
    volumes:
      - ./data-\${S_TYPE:-${DEFAULT_TYPE}}-\${S_VERSION:-${DEFAULT_VERSION}}:/data
      - ./plugins:/plugins:ro
COMPOSE_EOF
fi

# ---------------------------------------------------------
# generate .env file
# ---------------------------------------------------------
if [ ! -f .env ]; then
  echo "Making ... .env"
  cat - << ENV_EOF > .env
# Please edit it according to your environment. 

# https://hub.docker.com/r/itzg/minecraft-server/tags
# S_TAG="java8"  # S_VERSION >= 1.7.10 && S_VERSION <= 1.16.5
# S_TAG="java11" # S_VERSION >= 1.7.10 && S_VERSION <= 1.16.5
# S_TAG="java16" # S_VERSION >= 1.17 && S_VERSION <= 1.17.1
# S_TAG="java17" # S_VERSION >= 1.17 && S_VERSION <= 1.20.4
# S_TAG="java21" # S_VERSION >= 1.20.5
S_TAG="${DEFAULT_TAG}"

# Container Name
S_NAME="${DEFAULT_NAME}"

# Time Zone
S_TZ="${DEFAULT_TZ}"

# Server Type "spigot", "paper"
S_TYPE="${DEFAULT_TYPE}"

# Server Version "1.21.4", "latest"
S_VERSION="${DEFAULT_VERSION}"

# Modrinth Projects
S_MODRINTH="${DEFAULT_MODRINTH}"

# Download Plugins URLs
S_MODS="${DEFAULT_MODS}"

# TCP 25565 for Java
S_PORT="${DEFAULT_PORT}"

# UDP 19132 for Bedrock
S_PORT_BE="${DEFAULT_PORT_BE}"

# Command on Startup
S_OPERATOR="op <player-name>"
ENV_EOF
fi

# ---------------------------------------------------------
# check operator
# ---------------------------------------------------------
while [ -n "$(sed -ne '/S_OPERATOR="op <player-name>"/p' .env)" ]; do
  while read -p "Input player name: " MCID; do
    if [ -z "${MCID}" ]; then
      MCID="<player-name>"
      break
    fi
    if checkMCID "${MCID}"; then
      break
    fi
  done
  sed -re "s/^S_OPERATOR=.*$/S_OPERATOR=\"op ${MCID}\"/" -i .env
  sed -ne '/S_OPERATOR/p' .env
done

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
docker compose up && down

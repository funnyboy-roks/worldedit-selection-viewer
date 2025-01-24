#!/bin/bash
# ---------------------------------------------------------
# Define variables
# ---------------------------------------------------------
COMPOSE_YML="compose.yml"
INIT_ENV=".init"
DOT_ENV=".env"
LOG_FILE="last.log"
WORK_FILES=("${COMPOSE_YML}" "${INIT_ENV}" "${DOT_ENV}" "${LOG_FILE}")
if [ -f /etc/timezone ]; then
  DEFAULT_TZ="$(cat /etc/timezone)"
else
  DEFAULT_TZ="Asia/Tokyo"
fi

# ---------------------------------------------------------
# Down function
# ---------------------------------------------------------
function down()
{
  if [ -f "${COMPOSE_YML}" ] && docker compose version &> /dev/null; then
    docker compose stop
    docker compose logs mc > ${LOG_FILE}
    docker compose down
  fi
}

# ---------------------------------------------------------
# Check MCID function
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
# Parse command args: run.sh [clean|init|help]
# ---------------------------------------------------------
case "${1}" in
"")
  ;;
"clean")
  echo "Clean..."
  down
  rm -rf data-* plugins
  rm -f "${WORK_FILES[@]}"
  echo "done."
  exit 0
  ;;
"init")
  echo "Check update plugins..."
  export ENV_FILE="${INIT_ENV}"
  ;;
*)
  cat - << USAGE_EOF
Usage: $(basename ${0}) [options]
options:
    clean ... Clean all sandbox data.
    init  ... Check update plugins.
USAGE_EOF
  exit 0
  ;;
esac

# ---------------------------------------------------------
# Generate the compose.yml file
# ---------------------------------------------------------
if [ ! -f "${COMPOSE_YML}" ]; then
  echo "Generate ... ${COMPOSE_YML}"
  cat - << COMPOSE_EOF > "${COMPOSE_YML}"
services:
  mc:
    image: "itzg/minecraft-server:\${TAG}"
    container_name: "sandbox"
    environment:
      TZ: "${DEFAULT_TZ}"
      EULA: "true"
      SERVER_NAME: "Sandbox"
      MOTD: "for worldedit-selection-viewer"
      TYPE: "\${TYPE}"
      VERSION: "\${VERSION}"
      MODE: "creative"
      FORCE_GAMEMODE: "true"
    env_file:
      - "\${ENV_FILE:-${DOT_ENV}}"
    ports:
      - "25565:25565"
      - "19132:19132/udp"
    volumes:
      - "./data-\${TYPE}-\${VERSION}:/data"
      - "./plugins:/plugins:ro"
COMPOSE_EOF
fi

# ---------------------------------------------------------
# Generate the .init file. Used only on first startup.
# ---------------------------------------------------------
if [ ! -f "${INIT_ENV}" ]; then
  echo "Generate ... ${INIT_ENV}"
  cat - << INIT_EOF > "${INIT_ENV}"
# Used only for the first time.

# Download plugins
MODRINTH_PROJECTS="viaversion,viabackwards,worldedit:beta"
PLUGINS="
https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot
https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot
"

# Configure convenient settings
RCON_CMDS_STARTUP="op <player-name>
gamerule doDaylightCycle false
gamerule doWeatherCycle false
time set noon
weather clear"
INIT_EOF
fi

# ---------------------------------------------------------
# Generate the .env file. Used on subsequent startups.
# ---------------------------------------------------------
if [ ! -f "${DOT_ENV}" ]; then
  echo "Generate ... ${DOT_ENV}"
  cat - << ENV_EOF > "${DOT_ENV}"
# The .env file will be automatically loaded from docker compose.
# From the second time onwards, it is also referenced as an environment variable within the container.

# Server type: "paper", "spigot"
TYPE="paper"

# Server version: "latest", "1.21.4"
VERSION="latest"

# image-tags: https://hub.docker.com/r/itzg/minecraft-server/tags
# "java11" version 1.7.10 ~ 1.16.5
# "java17" version 1.17 ~ 1.20.4
# "java21" version 1.20.5 ~
TAG="latest"
ENV_EOF
fi

# ---------------------------------------------------------
# Load env_file ".init" only for the first time. 
# ---------------------------------------------------------
source "${DOT_ENV}"

if [ ! -d "./data-${TYPE}-${VERSION}" ]; then
  echo "Detect new version. [${TYPE}-${VERSION}]"
  export ENV_FILE="${INIT_ENV}"
fi

# ---------------------------------------------------------
# Update operator player-name in env_file ".init".
# ---------------------------------------------------------
while [ -n "$(sed -ne '/op <player-name>/p' ${INIT_ENV})" ]; do
  while read -p "Input operator player name: " MCID; do
    if [ -z "${MCID}" ]; then
      MCID="<player-name>"
      break
    fi
    if checkMCID "${MCID}"; then
      break
    fi
  done
  sed -re "s/op <player-name>/op ${MCID}/" -i "${INIT_ENV}"
done

# ---------------------------------------------------------
# Update local plugins
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
# Check if docker is enabled
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
# launch minecraft server (CTRL+C to down)
# ---------------------------------------------------------
trap 'down' SIGINT
docker compose up && down

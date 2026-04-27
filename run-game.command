#!/bin/zsh
cd "/Users/herbertwu/Downloads/RhythemGameLevels" || exit 1
if [ -x "target/app/bin/app" ]; then
  exec "target/app/bin/app"
fi

./mvnw -q -DskipTests javafx:jlink || exit 1
exec "target/app/bin/app"

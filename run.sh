#!/bin/sh
# コンパイルして実行する。引数はそのままアプリに渡る（例: ./run.sh add 牛乳を買う）
set -e
cd "$(dirname "$0")"
javac -encoding UTF-8 -d out $(find src/main/java -name '*.java')
java -cp out todo.Main "$@"

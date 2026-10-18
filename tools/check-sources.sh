#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" "$ROOT/tools/ParseJavaSources.java"
java -cp "$TMP" ParseJavaSources "$ROOT"/app/src/main/java/io/github/pingjing/app/*.java
"$ROOT/tools/test-matcher.sh"
python3 - "$ROOT" <<'PY'
import pathlib,sys
from lxml import etree
root=pathlib.Path(sys.argv[1]); docs=list((root/'app/src/main').rglob('*.xml'))
for path in docs: etree.parse(str(path))
print('PASS: parsed',len(docs),'Android XML resources')
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
assert 'android.permission.INTERNET' not in manifest
assert not etree.parse(str(root/'app/src/main/AndroidManifest.xml')).xpath('//*[local-name()="uses-permission"][@*[local-name()="name"]="android.permission.QUERY_ALL_PACKAGES"]')
assert 'android.permission.BIND_ACCESSIBILITY_SERVICE' in manifest
print('PASS: minimum-permission sanity checks')
PY

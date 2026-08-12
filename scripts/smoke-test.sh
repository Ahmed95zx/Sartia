#!/usr/bin/env bash
#
# End-to-end smoke test against a running deployment.
#
# Drives the real JSF screens the way a browser does - fetching each page,
# extracting its view state and generated field names, and posting the form
# back - so it exercises the whole stack rather than the service layer alone.
#
# Usage:  ./scripts/smoke-test.sh [base-url]
# Default base-url: http://localhost:8080/sartia
#
set -uo pipefail

BASE="${1:-http://localhost:8080/sartia}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

PASS=0
FAIL=0

pass() { printf '  \033[32mPASS\033[0m  %s\n' "$1"; PASS=$((PASS + 1)); }
fail() { printf '  \033[31mFAIL\033[0m  %s\n' "$1"; FAIL=$((FAIL + 1)); }

check() {
    local label="$1" expected="$2" actual="$3"
    if [ "$expected" = "$actual" ]; then pass "$label"; else fail "$label (expected $expected, got $actual)"; fi
}

contains() {
    local label="$1" file="$2" needle="$3"
    if grep -q "$needle" "$file"; then pass "$label"; else fail "$label (missing: $needle)"; fi
}

# The view state is generated per render and must be echoed back with the POST.
view_state() { sed -n 's/.*name="jakarta\.faces\.ViewState"[^>]*value="\([^"]*\)".*/\1/p' "$1" | head -1; }

# Finds the submit button's field name. Matches on type="submit" rather than on
# the first j_idt-looking name: components without an explicit id are numbered
# at render time, so anything added above the button shifts those numbers and a
# positional match silently picks up the wrong field.
button_name() {
    grep -oE '<input[^>]*type="submit"[^>]*' "$1" \
        | sed -n 's/.*name="\([^"]*\)".*/\1/p' | head -1
}

echo
echo "Sartia smoke test - $BASE"
echo "================================================================"

# ---------------------------------------------------------------- public pages
echo
echo "Public pages"
for page in catalog.xhtml login.xhtml register.xhtml "movie.xhtml?id=1"; do
    code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/$page")
    check "GET $page" "200" "$code"
done

curl -s "$BASE/catalog.xhtml" -o "$WORK/catalog.html"
contains "catalogue lists films" "$WORK/catalog.html" "movie-card"
contains "category sidebar populated" "$WORK/catalog.html" "cat-list"

# ---------------------------------------------------------------- REST API
echo
echo "REST API"
code=$(curl -s -o "$WORK/cats.json" -w '%{http_code}' "$BASE/api/categories")
check "GET /api/categories" "200" "$code"
contains "categories carry counts" "$WORK/cats.json" "movieCount"

code=$(curl -s -o "$WORK/movies.json" -w '%{http_code}' "$BASE/api/movies?size=5")
check "GET /api/movies" "200" "$code"
contains "results are paged" "$WORK/movies.json" "totalPages"

# Lower-case and mid-word on purpose: it proves the search is a case-insensitive
# substring match rather than a whole-word or prefix match.
curl -s "$BASE/api/movies?q=atrix" -o "$WORK/search.json"
if grep -q '"totalItems":0' "$WORK/search.json"; then
    fail "keyword search matches inside a word"
else
    pass "keyword search matches inside a word"
fi

code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/movies/999999")
check "unknown film answers 404" "404" "$code"

# ---------------------------------------------------------------- access control
echo
echo "Access control"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/my-rentals.xhtml")
check "my-rentals redirects when signed out" "302" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/admin/movies.xhtml")
check "admin area redirects when signed out" "302" "$code"

# ---------------------------------------------------------------- customer flow
echo
echo "Customer flow"
JAR="$WORK/cookies.txt"

curl -s -c "$JAR" "$BASE/login.xhtml" -o "$WORK/login.html"
VS=$(view_state "$WORK/login.html")
BTN=$(button_name "$WORK/login.html")

curl -s -b "$JAR" -c "$JAR" -L -o "$WORK/after-login.html" \
     --data-urlencode "loginForm=loginForm" \
     --data-urlencode "loginForm:username=david" \
     --data-urlencode "loginForm:password=david123" \
     --data-urlencode "$BTN=login" \
     --data-urlencode "jakarta.faces.ViewState=$VS" \
     "$BASE/login.xhtml"

contains "customer is signed in" "$WORK/after-login.html" "my-rentals"

code=$(curl -s -b "$JAR" -o "$WORK/rentals.html" -w '%{http_code}' "$BASE/my-rentals.xhtml")
check "my-rentals reachable once signed in" "200" "$code"
contains "rental history is shown" "$WORK/rentals.html" "Rental history"

# ---------------------------------------------------------------- admin flow
echo
echo "Administrator flow"
AJAR="$WORK/admin-cookies.txt"

curl -s -c "$AJAR" "$BASE/login.xhtml" -o "$WORK/alogin.html"
AVS=$(view_state "$WORK/alogin.html")
ABTN=$(button_name "$WORK/alogin.html")

curl -s -b "$AJAR" -c "$AJAR" -L -o "$WORK/after-admin.html" \
     --data-urlencode "loginForm=loginForm" \
     --data-urlencode "loginForm:username=admin" \
     --data-urlencode "loginForm:password=admin123" \
     --data-urlencode "$ABTN=login" \
     --data-urlencode "jakarta.faces.ViewState=$AVS" \
     "$BASE/login.xhtml"

contains "administrator sees management navigation" "$WORK/after-admin.html" "admin/movies"

code=$(curl -s -b "$AJAR" -o "$WORK/admin-movies.html" -w '%{http_code}' "$BASE/admin/movies.xhtml")
check "catalogue management reachable" "200" "$code"

code=$(curl -s -b "$AJAR" -o "$WORK/admin-returns.html" -w '%{http_code}' "$BASE/admin/returns.xhtml")
check "loans screen reachable" "200" "$code"
contains "open loans are listed" "$WORK/admin-returns.html" "Open Rentals"

# ---------------------------------------------------------------- wrong password
echo
echo "Authentication"
BJAR="$WORK/bad-cookies.txt"
curl -s -c "$BJAR" "$BASE/login.xhtml" -o "$WORK/blogin.html"
BVS=$(view_state "$WORK/blogin.html")
BBTN=$(button_name "$WORK/blogin.html")

curl -s -b "$BJAR" -c "$BJAR" -L -o "$WORK/after-bad.html" \
     --data-urlencode "loginForm=loginForm" \
     --data-urlencode "loginForm:username=david" \
     --data-urlencode "loginForm:password=wrong-password" \
     --data-urlencode "$BBTN=login" \
     --data-urlencode "jakarta.faces.ViewState=$BVS" \
     "$BASE/login.xhtml"

if grep -q "my-rentals" "$WORK/after-bad.html"; then
    fail "a wrong password is rejected"
else
    pass "a wrong password is rejected"
fi

# ---------------------------------------------------------------- summary
echo
echo "================================================================"
echo "  $PASS passed, $FAIL failed"
echo
[ "$FAIL" -eq 0 ]

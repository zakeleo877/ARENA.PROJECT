#!/usr/bin/env bash
# Commits whatever is on disk, even when I am not around to do it.
#
# The interruption I hit loses whatever is not committed: I edit three files,
# the turn ends, and the next turn starts from a clean tree. Saving the work
# every thirty seconds means an interruption costs at most half a minute.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 0

git add -A 2>/dev/null || exit 0
git diff --cached --quiet 2>/dev/null && exit 0

{
  printf 'chore(wip): save the work in progress\n\n'
  printf 'Written by .arena-work/auto-commit.sh on %s.\n' "$(date -u '+%Y-%m-%d %H:%M:%S UTC')"
} > .arena-work/.msg

git -c user.name="arena-agent" -c user.email="agent@arena.local" \
    commit -q -F .arena-work/.msg 2>/dev/null
rm -f .arena-work/.msg

#!/usr/bin/env bash
# Re-enable method access transformers, then iteratively drop only those that
# NeoForge's Minecraft recompile rejects because a subclass override would be
# narrowed. Converges on the largest set of method ATs that actually apply.
set -u
AT=src/main/resources/META-INF/accesstransformer.cfg
METHODS=src/main/resources/META-INF/accesstransformer-methods.cfg.disabled
BASE=/tmp/at_base.cfg
cp "$AT" "$BASE"

# start from base + all method entries
grep -vE '^#|^$' "$METHODS" > /tmp/at_methods.txt
cp /tmp/at_methods.txt /tmp/at_keep.txt

for round in $(seq 1 12); do
  { cat "$BASE"; echo; cat /tmp/at_keep.txt; } > "$AT"
  echo "round $round: $(grep -c . /tmp/at_keep.txt) method entries"
  ./gradlew createMinecraftArtifacts --no-daemon --console=plain > /tmp/at_round.log 2>&1
  if grep -q 'BUILD SUCCESSFUL' /tmp/at_round.log; then
    echo "converged: $(grep -c . /tmp/at_keep.txt) method ATs apply cleanly"
    exit 0
  fi
  # methods the recompile rejected
  grep -oE 'ERROR Line: [0-9]+, ([a-zA-Z0-9_]+)\(' /tmp/at_round.log \
    | sed -E 's/.*, ([a-zA-Z0-9_]+)\(/\1/' | sort -u > /tmp/at_bad.txt
  if [ ! -s /tmp/at_bad.txt ]; then
    echo "failed for a reason other than override narrowing:"
    grep -iE 'ERROR|error:' /tmp/at_round.log | head -5
    exit 1
  fi
  before=$(grep -c . /tmp/at_keep.txt)
  grep -vFf <(sed 's/^/ /;s/$/(/' /tmp/at_bad.txt) /tmp/at_keep.txt > /tmp/at_keep.new || true
  mv /tmp/at_keep.new /tmp/at_keep.txt
  after=$(grep -c . /tmp/at_keep.txt)
  echo "  dropped $((before-after)) entries for: $(tr '\n' ' ' < /tmp/at_bad.txt)"
  [ "$before" = "$after" ] && { echo "no progress, stopping"; exit 1; }
done
echo "did not converge in 12 rounds"; exit 1

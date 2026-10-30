#!/bin/sh
set -eu

if [ "$#" -gt 1 ]; then
  echo 'Usage: ./scripts/install-skills.sh [existing-task-directory]' >&2
  exit 2
fi

kitRoot=$(CDPATH= cd "$(dirname "$0")/.." && pwd -P)
taskInput=${1:-$kitRoot}
case "$taskInput" in
  /*) ;;
  *) taskInput=$PWD/$taskInput ;;
esac
if [ ! -d "$taskInput" ]; then
  echo "Task directory does not exist: $taskInput" >&2
  exit 2
fi
taskRoot=$(CDPATH= cd "$taskInput" && pwd -P)
skillRoot=$taskRoot/.agents/skills

for parentDir in "$taskRoot/.agents" "$skillRoot"; do
  if [ -L "$parentDir" ] || { [ -e "$parentDir" ] && [ ! -d "$parentDir" ]; }; then
    echo "Skill parent must be a real directory: $parentDir" >&2
    exit 1
  fi
done

# Preflight both destinations before copying either skill.
for skillName in fde-discovery fde-planning; do
  sourceDir=$kitRoot/skills/$skillName
  destinationDir=$skillRoot/$skillName
  if [ ! -f "$sourceDir/SKILL.md" ]; then
    echo "Missing source skill: $sourceDir" >&2
    exit 1
  fi
  if [ -e "$destinationDir" ] || [ -L "$destinationDir" ]; then
    if [ -L "$destinationDir" ] || [ ! -d "$destinationDir" ] || ! diff -qr "$sourceDir" "$destinationDir" >/dev/null 2>&1; then
      echo "Existing skill differs; nothing copied. Compare it manually: $destinationDir" >&2
      exit 1
    fi
  fi
done

mkdir -p "$skillRoot"
for skillName in fde-discovery fde-planning; do
  destinationDir=$skillRoot/$skillName
  if [ -d "$destinationDir" ]; then
    echo "Already identical: $destinationDir"
  else
    cp -R "$kitRoot/skills/$skillName" "$destinationDir"
    echo "Installed: $destinationDir"
  fi
done
echo 'Select fde-discovery / fde-planning in the task Codex. If unavailable, explicitly read the kit SKILL.md files.'

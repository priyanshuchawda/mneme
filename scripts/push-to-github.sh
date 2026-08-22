#!/usr/bin/env bash
# Create private GitHub repo and push Mneme / iQOO hackathon project.
# Author: Priyanshu Chawda (priyanshuchawda)
#
# Usage:
#   ./scripts/push-to-github.sh
#   ./scripts/push-to-github.sh my-custom-repo-name

set -euo pipefail

AUTHOR="Priyanshu Chawda"
GITHUB_USER="priyanshuchawda"
DEFAULT_REPO_NAME="mneme-iqoo-hackathon"
REPO_NAME="${1:-$DEFAULT_REPO_NAME}"

# Resolve project root (parent of scripts/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

log() {
  printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

die() {
  log "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "'$1' not found."
}

main() {
  require_command git
  require_command gh

  log "Project root: ${PROJECT_ROOT}"
  log "GitHub user:  ${GITHUB_USER}"
  log "Repo name:    ${REPO_NAME}"

  gh auth status >/dev/null 2>&1 || die "GitHub CLI not authenticated. Run: gh auth login"

  cd "${PROJECT_ROOT}"

  if [[ ! -f idea.md ]]; then
    die "idea.md not found in ${PROJECT_ROOT}"
  fi

  if [[ ! -d .git ]]; then
    log "Initializing git repository..."
    git init -b main
  fi

  if ! git remote get-url origin >/dev/null 2>&1; then
    log "Creating private GitHub repo: ${GITHUB_USER}/${REPO_NAME}"
    gh repo create "${REPO_NAME}" \
      --private \
      --source=. \
      --remote=origin \
      --description "Mneme — offline personal memory assistant for iQOO Hackathon 2026 by ${AUTHOR}" \
      || die "Failed to create repo (it may already exist — set remote manually or pick another name)"
  else
    log "Remote 'origin' already exists: $(git remote get-url origin)"
  fi

  log "Staging files..."
  git add idea.md README.md HACKATHON_REFERENCE.md .gitignore scripts/push-to-github.sh

  if git diff --cached --quiet; then
    log "No changes to commit."
  else
    git commit -m "$(cat <<EOF
Add Mneme project ideation and iQOO hackathon reference docs.

Author: ${AUTHOR} (${GITHUB_USER})
EOF
)"
    log "Commit created."
  fi

  log "Pushing to origin main..."
  git push -u origin main

  REPO_URL="https://github.com/${GITHUB_USER}/${REPO_NAME}"
  log "Done."
  log "Private repo: ${REPO_URL}"
}

main "$@"

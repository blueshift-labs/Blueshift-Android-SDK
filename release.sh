#!/bin/bash

# Exit if any command fails
set -e

# Define text formatting variables
GREEN=$(tput setaf 2)
RED=$(tput setaf 1)
YELLOW=$(tput setaf 3)
CYAN=$(tput setaf 6)
BOLD=$(tput bold)
RESET=$(tput sgr0)

# Check if version argument is provided
if [ -z "$1" ]; then
  printf "${RED}❌ Error: Missing version argument.${RESET}\n"
  printf "${YELLOW}Usage: ./release.sh <version>${RESET}\n"
  exit 1
fi

VERSION=$1
BRANCH=$(git rev-parse --abbrev-ref HEAD) # pick the current branch
BUILD_GRADLE="android-sdk/build.gradle"
AAR_DEST="dist/"
M2_REPO="$HOME/.m2/repository/com/blueshift/android-sdk-x/$VERSION/"
TAG_NAME="v${VERSION}"

printf "\n${CYAN}🚀 Releasing version: ${BOLD}${VERSION}${RESET}\n"
printf "🔍 Current branch: ${BOLD}${BRANCH}${RESET}\n"

# Step 1: Update PUBLISH_VERSION in build.gradle
printf "\n${YELLOW}➡️ Step 1: Updating PUBLISH_VERSION in build.gradle...${RESET}\n"
sed -i '' "s/PUBLISH_VERSION = '[0-9]*\.[0-9]*\.[0-9]*'/PUBLISH_VERSION = '$VERSION'/" "$BUILD_GRADLE"
printf "${GREEN}✅  PUBLISH_VERSION updated successfully.${RESET}\n"

# Step 2: Build the AAR file
printf "\n${YELLOW}➡️ Step 2: Building release AAR...${RESET}\n"
./gradlew assembleRelease publishToMavenLocal
printf "${GREEN}✅  Build completed successfully.${RESET}\n"

# Step 3: Clear old files in /dist and copy the new AAR
printf "\n${YELLOW}➡️ Step 3: Cleaning old files in /dist/...${RESET}\n"
rm -rf "$AAR_DEST"/*

AAR_SOURCE=$(find "$M2_REPO" -name "*.aar" | head -n 1)

if [ -f "$AAR_SOURCE" ]; then
  mkdir -p "$AAR_DEST"
  cp "$AAR_SOURCE" "$AAR_DEST"
  printf "${GREEN}✅  Copied new AAR to /dist.${RESET}\n"
else
  printf "${RED}❌  Error: AAR file not found in $M2_REPO${RESET}\n"
  exit 1
fi

# Step 4: Commit the changes (Including the AAR)
printf "\n${YELLOW}➡️ Step 4: Committing changes...${RESET}\n"
git add "$BUILD_GRADLE" "$AAR_DEST"
git commit -m "Published ${TAG_NAME} via Maven Central"
printf "${GREEN}✅  Changes committed.${RESET}\n"

# Step 5: Tag the release
printf "\n${YELLOW}➡️ Step 5: Tagging release...${RESET}\n"
git tag "$TAG_NAME"
printf "${GREEN}✅  Created tag: ${TAG_NAME}${RESET}\n"

# Step 6: Push changes to repository
printf "\n${YELLOW}➡️ Step 6: Pushing changes to ${BOLD}${BRANCH}${RESET}...${RESET}\n"
git push origin "$BRANCH"
git push origin "$TAG_NAME"
printf "${GREEN}✅  Pushed code and tag to repository.${RESET}\n"

# Step 7: Publish to Maven Central
printf "\n${YELLOW}➡️ Step 7: Publishing to Maven Central...${RESET}\n"
./gradlew publish
printf "${GREEN}✅  Library published successfully!${RESET}\n"

# Reminder to manually complete the release on Sonatype
printf "\n${BOLD}${CYAN}⚠️ Final Step Required: Complete the release process on Sonatype!${RESET}\n"
printf "👉 Go to ${BOLD}https://oss.sonatype.org/${RESET}, log in, and publish the release.\n"
printf "📌 Navigate to 'Staging Repositories', find your release, and click 'Close' and then 'Release'.\n"
printf "🔔 This step is required to make the library publicly available on Maven Central.\n\n"

printf "${BOLD}${GREEN}🎉 Release process completed successfully! 🚀${RESET}\n"

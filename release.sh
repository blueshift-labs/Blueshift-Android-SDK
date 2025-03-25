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
  printf "%s❌ Error: Missing version argument.%s\n" "$RED" "$RESET"
  printf "%sUsage: ./release.sh <version>%s\n" "$YELLOW" "$RESET"
  exit 1
fi

VERSION=$1
BRANCH=$(git rev-parse --abbrev-ref HEAD) # pick the current branch
BUILD_GRADLE="android-sdk/build.gradle"
AAR_DEST="dist/"
M2_REPO="$HOME/.m2/repository/com/blueshift/android-sdk-x/$VERSION/"
TAG_NAME="v${VERSION}"

printf "\n%s🚀 Releasing version: %s%s\n" "$CYAN" "$BOLD" "$VERSION"
printf "🔍 Current branch: %s%s%s\n" "$BOLD" "$BRANCH" "$RESET"

# Step 1: Update PUBLISH_VERSION in build.gradle
printf "\n%s📌 Step 1: Updating PUBLISH_VERSION in build.gradle...%s\n" "$YELLOW" "$RESET"
sed -i '' "s/PUBLISH_VERSION = '[0-9]*\.[0-9]*\.[0-9]*'/PUBLISH_VERSION = '$VERSION'/" "$BUILD_GRADLE"
printf "%s✅  PUBLISH_VERSION updated successfully.%s\n" "$GREEN" "$RESET"

# Step 2: Build the AAR file
printf "\n%s📌 Step 2: Building release AAR...%s\n" "$YELLOW" "$RESET"
./gradlew assembleRelease publishToMavenLocal
printf "%s✅  Build completed successfully.%s\n" "$GREEN" "$RESET"

# Step 3: Clear old files in /dist and copy the new AAR
printf "\n%s📌 Step 3: Cleaning old files in /dist/...%s\n" "$YELLOW" "$RESET"
rm -rf "$AAR_DEST"/*

AAR_SOURCE=$(find "$M2_REPO" -name "*.aar" | head -n 1)

if [ -f "$AAR_SOURCE" ]; then
  mkdir -p "$AAR_DEST"
  cp "$AAR_SOURCE" "$AAR_DEST"
  printf "%s✅  Copied new AAR to /dist.%s\n" "$GREEN" "$RESET"
else
  printf "%s❌ Error: AAR file not found in %s%s\n" "$RED" "$M2_REPO" "$RESET"
  exit 1
fi

# Step 4: Commit the changes (Including the AAR)
printf "\n%s📌 Step 4: Committing changes...%s\n" "$YELLOW" "$RESET"
git add "$BUILD_GRADLE" "$AAR_DEST"
git commit -m "Published ${TAG_NAME} via Maven Central"
printf "%s✅  Changes committed.%s\n" "$GREEN" "$RESET"

# Step 5: Tag the release
printf "\n%s📌 Step 5: Tagging release...%s\n" "$YELLOW" "$RESET"
git tag "$TAG_NAME"
printf "%s✅  Created tag: %s%s\n" "$GREEN" "$TAG_NAME" "$RESET"

# Step 6: Push changes to repository
printf "\n%s📌 Step 6: Pushing changes to %s%s...%s\n" "$YELLOW" "$BOLD" "$BRANCH" "$RESET"
git push origin "$BRANCH"
git push origin "$TAG_NAME"
printf "%s✅  Pushed code and tag to repository.%s\n" "$GREEN" "$RESET"

# Step 7: Publish to Maven Central
printf "\n%s📌 Step 7: Publishing to Maven Central...%s\n" "$YELLOW" "$RESET"
./gradlew publish
printf "%s✅  Library published successfully!%s\n" "$GREEN" "$RESET"

# Reminder to manually complete the release on Sonatype
printf "\n%s⚠️  Final Step Required: Complete the release process on Sonatype!%s\n" "$BOLD" "$RESET"
printf "👉 Go to %shttps://oss.sonatype.org/%s, log in, and publish the release.\n" "$BOLD" "$RESET"
printf "📌 Navigate to 'Staging Repositories', find your release, and click 'Close' and then 'Release'.\n"
printf "🔔 This step is required to make the library publicly available on Maven Central.\n\n"

printf "%s🎉 Release process completed successfully! 🚀%s\n" "$BOLD" "$GREEN"

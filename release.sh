#!/bin/bash

# Exit if any command fails
set -e

# Check if version argument is provided
if [ -z "$1" ]; then
  printf "❌ Error: Missing version argument.\n"
  printf "Usage: ./release.sh <version>\n"
  exit 1
fi

VERSION=$1
BRANCH="main" # pick the current branch
BUILD_GRADLE="android-sdk/build.gradle"
AAR_DEST="dist/"
# Construct the expected path pattern within the local Maven repository
# Using $HOME is generally reliable in most CI/CD environments
M2_REPO_BASE="$HOME/.m2/repository/com/blueshift/android-sdk-x"
M2_REPO_VERSION_DIR="$M2_REPO_BASE/$VERSION"
TAG_NAME="v${VERSION}"

printf "\n🚀 Releasing version: %s\n" "$VERSION"
printf "🔍 Current branch: %s\n" "$BRANCH"

# --- Step 1: Update PUBLISH_VERSION in build.gradle ---
printf "\n📌 Step 1: Updating PUBLISH_VERSION in build.gradle...\n"
# Use a temporary file for sed to ensure cross-platform compatibility (macOS vs Linux sed -i)
sed "s/PUBLISH_VERSION = '[0-9]*\.[0-9]*\.[0-9]*'/PUBLISH_VERSION = '$VERSION'/" "$BUILD_GRADLE" > "$BUILD_GRADLE.tmp" && mv "$BUILD_GRADLE.tmp" "$BUILD_GRADLE"
if [ $? -ne 0 ]; then
    printf "❌ Error: Failed to update version in %s.\n" "$BUILD_GRADLE"
    rm -f "$BUILD_GRADLE.tmp" # Clean up temp file on error
    exit 1
fi
printf "✅ PUBLISH_VERSION updated successfully.\n"

# --- Step 2: Build the AAR file and publish locally ---
printf "\n📌 Step 2: Building release AAR and publishing locally...\n"
./gradlew assembleRelease publishToMavenLocal
printf "✅ Build and local publish completed successfully.\n"

# --- Step 3: Clear old files in /dist and copy the new AAR ---
printf "\n📌 Step 3: Cleaning old files in %s and copying new AAR...\n" "$AAR_DEST"
# Create dist directory if it doesn't exist
mkdir -p "$AAR_DEST"
# Remove potentially existing old AARs matching a pattern
rm -f "$AAR_DEST"/android-sdk-x-*.aar

# Find the newly published AAR file in the local Maven repo
# Use find with -maxdepth 1 to avoid searching too deep unnecessarily
AAR_SOURCE=$(find "$M2_REPO_VERSION_DIR" -maxdepth 1 -name "android-sdk-x-$VERSION.aar" -print -quit)

if [ -f "$AAR_SOURCE" ]; then
  cp "$AAR_SOURCE" "$AAR_DEST"
  printf "✅ Copied new AAR (%s) to %s.\n" "$(basename "$AAR_SOURCE")" "$AAR_DEST"
else
  printf "❌ Error: AAR file not found in %s\n" "$M2_REPO_VERSION_DIR"
  printf "🔍 Searched for pattern: android-sdk-x-%s.aar\n" "$VERSION"
  # Optional: List contents for debugging
  # ls -l "$M2_REPO_VERSION_DIR"
  exit 1
fi

# --- Step 4: Commit the changes (Including the AAR) ---
printf "\n📌 Step 4: Committing changes...\n"
git add "$BUILD_GRADLE" "$AAR_DEST"
git commit -m "Published ${TAG_NAME} via Maven Central" || echo "ℹ️ No changes to commit"
printf "✅ Changes committed.\n"

# --- Step 5: Tag the release ---
printf "\n📌 Step 5: Tagging release...\n"
git tag "$TAG_NAME"
printf "✅ Created tag: %s\n" "$TAG_NAME"

# --- Step 6: Push changes to repository ---
printf "\n📌 Step 6: Pushing changes to %s...\n" "$BRANCH"
git push origin "$BRANCH"
git push origin "$TAG_NAME"
printf "✅ Pushed code and tag to repository.\n"

# --- Step 7: Publish to Maven Central ---
printf "\n📌 Step 7: Publishing to Maven Central...\n"
# Ensure necessary credentials (SONATYPE_USERNAME, SONATYPE_PASSWORD, SIGNING_KEY_ID, etc.)
# are available as environment variables in the GitHub Actions environment.
./gradlew publishToMavenCentral
printf "✅ Library publish task submitted successfully!\n"

# --- Final Manual Step Reminder ---
printf "\n⚠️ Final Step Required: Complete the release process on Sonatype!\n"
printf "👉 Go to https://oss.sonatype.org/, log in, and publish the release.\n"
printf "📌 Navigate to 'Staging Repositories', find your release, and click 'Close' and then 'Release'.\n"
printf "🔔 This step is required to make the library publicly available on Maven Central.\n\n"

printf "🎉 Release script completed successfully! 🚀\n"

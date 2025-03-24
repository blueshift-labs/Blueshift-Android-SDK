#!/bin/bash

# Exit if any command fails
set -e

# Check if version argument is provided
if [ -z "$1" ]; then
  echo "Usage: ./release.sh <version>"
  exit 1
fi

VERSION=$1
BRANCH="main"
BUILD_GRADLE="android-sdk/build.gradle"
AAR_DEST="dist/"
M2_REPO="$HOME/.m2/repository/com/blueshift/android-sdk-x/$VERSION/"

echo "Releasing version: $VERSION"

# Step 1: Update PUBLISH_VERSION in build.gradle
echo "Updating PUBLISH_VERSION in build.gradle..."
sed -i '' "s/PUBLISH_VERSION = '[0-9]*\.[0-9]*\.[0-9]*'/PUBLISH_VERSION = '$VERSION'/" "$BUILD_GRADLE"

# Verify the change
grep "PUBLISH_VERSION" "$BUILD_GRADLE"

# Step 2: Build the AAR file
echo "Building release AAR..."
./gradlew assembleRelease publishToMavenLocal

# Step 3: Clear old files in /dist and copy the new AAR
echo "Cleaning old files in /dist/..."
rm -rf "$AAR_DEST"/*

AAR_SOURCE=$(find "$M2_REPO" -name "*.aar" | head -n 1)

if [ -f "$AAR_SOURCE" ]; then
  mkdir -p "$AAR_DEST"
  cp "$AAR_SOURCE" "$AAR_DEST"
  echo "Copied new AAR to /dist."
else
  echo "Error: AAR file not found in $M2_REPO"
  exit 1
fi

# Step 4: Commit the changes (Including the AAR)
git add "$BUILD_GRADLE" "$AAR_DEST"
git commit -m "Published v${VERSION} via Maven Central"

# Step 5: Tag the release
git tag "$VERSION"

# Step 6: Push to the main branch
git push origin "$BRANCH"
git push origin "$VERSION"

# Step 7: Publish to Maven Central
echo "Publishing to Maven Central..."
./gradlew publish

echo "Release process completed!"

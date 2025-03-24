#!/bin/bash

# Exit if any command fails
set -e

# Check if version argument is provided
if [ -z "$1" ]; then
  echo "Usage: ./release.sh <version>"
  exit 1
fi

VERSION=$1
BRANCH="automate_release"

echo "Releasing version: $VERSION"

# Step 1: Update PUBLISH_VERSION in build.gradle
echo "Updating PUBLISH_VERSION in build.gradle..."
sed -i '' "s/ext.PUBLISH_VERSION = \".*\"/ext.PUBLISH_VERSION = \"$VERSION\"/" android-sdk/build.gradle

# Step 2: Build the AAR file
echo "Building release AAR..."
./gradlew assembleRelease

# Step 3: Copy the AAR file to /dist
AAR_SOURCE=$(find ~/.m2 -name "*.aar" | tail -n 1)
AAR_DEST="dist/"
mkdir -p $AAR_DEST
cp "$AAR_SOURCE" "$AAR_DEST"

echo "Copied AAR to /dist."

# Step 4: Commit the changes
git add .
git commit -m "Released ${VERSION}"

# Step 5: Tag the release
git tag "$VERSION"

# Step 6: Push to the main branch
git push origin "$BRANCH"
git push origin "$VERSION"

# Step 7: Publish to Maven Central
echo "Publishing to Maven Central..."
./gradlew publish

echo "Release process completed!"

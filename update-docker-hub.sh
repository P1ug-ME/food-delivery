#!/bin/bash

# TTB Spark Food Delivery - Docker Hub Update Script
# Updates Docker Hub with latest code changes

set -e

echo "🚀 TTB Spark Food Delivery - Docker Hub Update"
echo "=============================================="

# Configuration
DOCKER_HUB_USERNAME="p1ug-me"
IMAGE_NAME="ttb-order-service"
VERSION_TAG="v1.0.0"
LATEST_TAG="latest"

echo "📋 Build Configuration:"
echo "   Docker Hub Username: ${DOCKER_HUB_USERNAME}"
echo "   Image Name: ${IMAGE_NAME}"
echo "   Version Tag: ${VERSION_TAG}"
echo "   Latest Tag: ${LATEST_TAG}"
echo ""

# Step 1: Run tests to ensure code quality
echo "🧪 Running tests..."
./gradlew test
if [ $? -ne 0 ]; then
    echo "❌ Tests failed! Cannot proceed with Docker build."
    exit 1
fi
echo "✅ Tests passed!"

# Step 2: Clean build
echo "📦 Building application..."
./gradlew clean build -x test
if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi
echo "✅ Build successful!"

# Step 3: Build Docker images
echo "🐳 Building Docker images..."
VERSIONED_IMAGE="${DOCKER_HUB_USERNAME}/${IMAGE_NAME}:${VERSION_TAG}"
LATEST_IMAGE="${DOCKER_HUB_USERNAME}/${IMAGE_NAME}:${LATEST_TAG}"

docker build -t ${VERSIONED_IMAGE} -t ${LATEST_IMAGE} .
if [ $? -ne 0 ]; then
    echo "❌ Docker build failed!"
    exit 1
fi
echo "✅ Docker images built successfully!"

# Step 4: Login to Docker Hub (if not already logged in)
echo "🔐 Checking Docker Hub login..."
docker info | grep Username || {
    echo "Please login to Docker Hub:"
    docker login --username ${DOCKER_HUB_USERNAME}
}

# Step 5: Push images to Docker Hub
echo "📤 Pushing images to Docker Hub..."
echo "   Pushing: ${VERSIONED_IMAGE}"
docker push ${VERSIONED_IMAGE}

echo "   Pushing: ${LATEST_IMAGE}"
docker push ${LATEST_IMAGE}

if [ $? -eq 0 ]; then
    echo "✅ Successfully pushed to Docker Hub!"
    echo ""
    echo "📊 Image Information:"
    docker images ${DOCKER_HUB_USERNAME}/${IMAGE_NAME}
    echo ""
    echo "🔗 Docker Hub URLs:"
    echo "   Repository: https://hub.docker.com/r/${DOCKER_HUB_USERNAME}/${IMAGE_NAME}"
    echo "   Versioned: docker pull ${VERSIONED_IMAGE}"
    echo "   Latest: docker pull ${LATEST_IMAGE}"
    echo ""
    echo "🚀 Usage:"
    echo "   docker run -p 8080:8080 ${LATEST_IMAGE}"
    echo "   docker-compose up -d"
else
    echo "❌ Failed to push to Docker Hub!"
    exit 1
fi

echo ""
echo "🎉 Docker Hub update completed successfully!"

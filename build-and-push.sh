#!/bin/bash

# TTB Spark Food Delivery - Order Service Build and Push Script
# This script builds the Docker image and pushes it to Docker Hub

set -e

# Configuration
IMAGE_NAME="ttb-order-service"
DOCKER_HUB_USERNAME="warrunyou1"  # Your Docker Hub username
TAG="latest"
FULL_IMAGE_NAME="${DOCKER_HUB_USERNAME}/${IMAGE_NAME}:${TAG}"

echo "🚀 TTB Spark Food Delivery - Order Service Build & Deploy"
echo "=================================================="

# Build the application first
echo "📦 Building application with Gradle..."
./gradlew clean build -x test

# Build Docker image
echo "🐳 Building Docker image: ${FULL_IMAGE_NAME}"
docker build -t ${FULL_IMAGE_NAME} .

# Tag with additional version if provided
if [ ! -z "$1" ]; then
    VERSION_TAG="${DOCKER_HUB_USERNAME}/${IMAGE_NAME}:$1"
    echo "🏷️  Tagging image with version: ${VERSION_TAG}"
    docker tag ${FULL_IMAGE_NAME} ${VERSION_TAG}
fi

# Push to Docker Hub
echo "📤 Pushing image to Docker Hub..."
docker push ${FULL_IMAGE_NAME}

if [ ! -z "$1" ]; then
    docker push ${VERSION_TAG}
fi

echo "✅ Build and push completed successfully!"
echo "🐳 Image available at: ${FULL_IMAGE_NAME}"

# Display image info
echo "📊 Image Information:"
docker images ${DOCKER_HUB_USERNAME}/${IMAGE_NAME}

echo ""
echo "🚀 To run the container:"
echo "docker run -p 8080:8080 ${FULL_IMAGE_NAME}"
echo ""
echo "🐳 To run with Docker Compose:"
echo "docker-compose up -d"

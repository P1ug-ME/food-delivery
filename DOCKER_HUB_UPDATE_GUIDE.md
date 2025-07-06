# Docker Hub Update Guide - TTB Spark Food Delivery

## 🎯 Current Status

Your Docker configuration has been updated:
- ✅ **Username**: `warrunyou1` 
- ✅ **Application**: Built and tested successfully
- ✅ **Docker Images**: Built locally and ready for push
- ⏳ **Docker Hub Login**: Required to push images

## 🔐 Step 1: Login to Docker Hub

Run this command and enter your credentials:

```bash
docker login --username warrunyou1
```

**Enter your Docker Hub password when prompted**

**Alternative - Using Personal Access Token (Recommended):**
1. Go to [Docker Hub Settings](https://app.docker.com/settings)
2. Create a Personal Access Token (PAT)
3. Use the PAT as your password when logging in

## 🚀 Step 2: Complete Docker Hub Update

After successful login, run:

```bash
./update-docker-hub.sh
```

This will:
- ✅ Run tests to ensure code quality
- ✅ Build the application with Gradle  
- ✅ Create Docker images (both versioned and latest)
- ✅ Push images to Docker Hub
- ✅ Display deployment information

## 🐳 Expected Docker Hub Images

After successful push, you'll have:

| **Image** | **Docker Hub URL** |
|-----------|-------------------|
| **Latest** | `docker pull warrunyou1/ttb-order-service:latest` |
| **Versioned** | `docker pull warrunyou1/ttb-order-service:v1.0.0` |

## 📊 Docker Hub Repository

Your TTB Spark Food Delivery images will be available at:
**https://hub.docker.com/r/warrunyou1/ttb-order-service**

## 🧪 Test Your Deployed Images

After successful push, test your images:

```bash
# Pull and run the latest image
docker pull warrunyou1/ttb-order-service:latest
docker run -p 8080:8080 warrunyou1/ttb-order-service:latest

# Test the application
curl http://localhost:8080/actuator/health
```

## 📋 TTB Spark Submission Information

For the TTB Spark submission, you can now provide:

### GitHub Repository
```
https://github.com/P1ug-ME/food-delivery
```

### Docker Hub Images
```
Latest: docker pull warrunyou1/ttb-order-service:latest
Versioned: docker pull warrunyou1/ttb-order-service:v1.0.0
Repository: https://hub.docker.com/r/warrunyou1/ttb-order-service
```

### Quick Deployment Commands
```bash
# Using Docker
docker run -p 8080:8080 warrunyou1/ttb-order-service:latest

# Using Docker Compose (from repository)
git clone https://github.com/P1ug-ME/food-delivery.git
cd food-delivery
docker-compose up -d
```

## 🔧 Manual Push (If Automated Script Fails)

If the automated script has issues, you can manually push:

```bash
# Login to Docker Hub
docker login --username warrunyou1

# Build images
docker build -t p1ug-me/ttb-order-service:v1.0.0 -t p1ug-me/ttb-order-service:latest .

# Push images
docker push p1ug-me/ttb-order-service:v1.0.0
docker push p1ug-me/ttb-order-service:latest
```

## ✅ Success Verification

After successful Docker Hub update, you should see:
- ✅ Images available on Docker Hub
- ✅ Repository publicly accessible
- ✅ Images can be pulled and run
- ✅ Health check endpoints working
- ✅ Ready for TTB Spark evaluation

---

**🎉 Once completed, your TTB Spark Food Delivery Platform will be fully deployed and ready for submission!**

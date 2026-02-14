# MongoDB Atlas Connection Setup

## Current Issue

The application is failing to connect to MongoDB Atlas with the error:
```
javax.net.ssl.SSLException: Received fatal alert: internal_error
```

This error typically means your **IP address is NOT whitelisted** in MongoDB Atlas.

## Solution: Whitelist Your IP Address

### Step 1: Log in to MongoDB Atlas
Go to [https://cloud.mongodb.com](https://cloud.mongodb.com) and log in.

### Step 2: Select Your Project
Select the project containing `Cluster0`.

### Step 3: Go to Network Access
1. Click on **"Network Access"** in the left sidebar (under Security)
2. Click the **"+ ADD IP ADDRESS"** button

### Step 4: Add Your IP Address
You have two options:

**Option A: Add your current IP (Recommended for development)**
- Click **"ADD CURRENT IP ADDRESS"**
- This adds your current public IP

**Option B: Allow access from anywhere (For testing only)**
- Click **"ALLOW ACCESS FROM ANYWHERE"**
- This adds `0.0.0.0/0` which allows all IPs
- ⚠️ **WARNING**: Only use this for testing, not production!

### Step 5: Confirm and Wait
1. Click **"Confirm"**
2. Wait 1-2 minutes for the changes to propagate

### Step 6: Test the Connection
Run your application again:
```bash
JAVA_HOME=/usr/lib/jvm/jdk-21-oracle-x64 ./mvnw spring-boot:run
```

## Other Possible Issues

### 1. MongoDB Cluster is Paused
- Free tier clusters pause after 60 days of inactivity
- Go to MongoDB Atlas Dashboard and check if your cluster shows "PAUSED"
- Click "Resume" if it's paused

### 2. Wrong Credentials
Verify your credentials in `.env`:
```
MONGODB_URI=mongodb+srv://narwalnitesh14_db_user:<password>@cluster0.jijkv5j.mongodb.net/shopapi...
```
Make sure the password is correct.

### 3. Database User Not Created
1. Go to MongoDB Atlas > Database Access
2. Ensure the user `narwalnitesh14_db_user` exists
3. Ensure it has `readWriteAnyDatabase` role or specific access to `shopapi` database

## Connection String Format

Your connection string should look like:
```
mongodb+srv://username:password@cluster0.jijkv5j.mongodb.net/shopapi?retryWrites=true&w=majority&appName=Cluster0
```

## Running with Java 21

For best compatibility, run the application with Java 21:
```bash
JAVA_HOME=/usr/lib/jvm/jdk-21-oracle-x64 ./mvnw spring-boot:run
```

## Testing MongoDB Connection

You can test the connection using mongosh (MongoDB Shell):
```bash
mongosh "mongodb+srv://cluster0.jijkv5j.mongodb.net/shopapi" --username narwalnitesh14_db_user
```
Enter your password when prompted.


# 🚀 Deploy Rafeeq to Render

## One-Click Deploy

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/omarlhlbwy441-netizen/dtr1-n)

## Manual Steps

### Step 1: Create PostgreSQL Database
1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click **New +** → **PostgreSQL**
3. Name: `rafeeq-db`
4. Plan: **Free**
5. Click **Create Database**
6. Copy the **Internal Database URL**

### Step 2: Create Web Service
1. Click **New +** → **Web Service**
2. Connect your GitHub repo: `omarlhlbwy441-netizen/dtr1-n`
3. Configure:
   - **Name**: `rafeeq-api`
   - **Runtime**: Python 3
   - **Build Command**: `pip install -r backend/requirements.txt`
   - **Start Command**: `cd backend && uvicorn app.main:app --host 0.0.0.0 --port $PORT --workers 1`

### Step 3: Add Environment Variables
```
DATABASE_URL=postgresql://rafeeq:password@host:5432/rafeeq
GEMINI_API_KEY=your-gemini-key
AI_PROVIDER=gemini
SECRET_KEY=your-secret-key
ENVIRONMENT=production
```

### Step 4: Deploy
Click **Create Web Service** and wait!

## URLs After Deployment
- API: `https://rafeeq-api.onrender.com`
- Health: `https://rafeeq-api.onrender.com/api/v1/health`
- Docs: `https://rafeeq-api.onrender.com/docs`

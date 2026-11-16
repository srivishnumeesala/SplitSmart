# SplitSmart 🧾

> **Split expenses. Settle fairly. Stay friends.**

A full-stack expense-splitting app built with **pure Java 21** (no frameworks) and a **vanilla HTML/CSS/JS** frontend.

---

## 🚀 Quick Start (Local)

```bash
# 1. Compile
javac -d out -sourcepath src src/splitsmart/Main.java

# 2. Run desktop GUI
java -cp out splitsmart.Main

# 3. Run web server  →  open http://localhost:8080
java -cp out splitsmart.WebMain
```

Or use the scripts:
```bash
# Windows
run.bat

# Linux / Mac
chmod +x run.sh && ./run.sh
```

---

## 🌐 Deploy to Vercel + Railway

### Step 1 — Push to GitHub
```bash
git remote add origin https://github.com/YOUR_USERNAME/splitsmart.git
git push -u origin main
```

### Step 2 — Deploy Backend to Railway (free)
1. Go to [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub repo**
2. Select your `splitsmart` repository
3. Railway auto-detects the `Dockerfile` and builds it
4. After deploy, copy your app URL: `https://splitsmart-xxxx.up.railway.app`

### Step 3 — Deploy Frontend to Vercel
1. Go to [vercel.com](https://vercel.com) → **New Project** → import your GitHub repo
2. Set **Root Directory** to `web`
3. Framework preset: **Other** (static)
4. Add environment variable:
   - Key: `SPLITSMART_API_URL`
   - Value: `https://splitsmart-xxxx.up.railway.app` (your Railway URL)
5. Update `vercel.json` → replace `YOUR-RAILWAY-APP` with your actual Railway hostname
6. Click **Deploy** ✅

---

## 📁 Project Structure

```
SplitSmart/
├── src/splitsmart/
│   ├── Main.java              # Swing desktop entry point
│   ├── WebMain.java           # Web server entry point
│   ├── model/                 # User, Group, Member, Expense, Transfer, enums
│   ├── logic/                 # BalanceCalculator, SettlementPlanner, SplitCalculator, Money, Validator
│   ├── storage/               # DataStore (CSV persistence), PasswordUtil
│   ├── ui/                    # Swing UI panels
│   ├── web/                   # WebServer.java (REST API, built-in JDK HttpServer)
│   └── test/                  # SettlementTest.java (plain main() tests)
├── web/                       # Frontend (HTML + CSS + vanilla JS)
│   ├── index.html
│   ├── style.css
│   └── app.js
├── data/                      # Auto-created CSV files at runtime
├── Dockerfile                 # For Railway / Docker deployment
├── railway.json               # Railway config
├── vercel.json                # Vercel static + API proxy config
├── run.bat                    # Windows run script
└── run.sh                     # Linux/Mac run script
```

---

## ✨ Features

| Feature | Detail |
|---|---|
| **Auth** | Register/Login with salted SHA-256 passwords |
| **Groups** | Create groups, add/remove members |
| **Expenses** | Equal / Exact / Percentage splits |
| **Balances** | Auto-computed, never stored |
| **Settle-up** | Greedy min-transfers algorithm |
| **Dashboard** | Net balance, recent activity, spending chart |
| **Transactions** | Filter by group, category, search |
| **Analytics** | Bar charts by category and by week |
| **Settings** | Light / Dark theme, demo data, reset |
| **Storage** | Plain CSV files, auto-save, crash-safe |

---

## 🔧 Tech Stack

- **Backend**: Java 21 JDK — `com.sun.net.httpserver.HttpServer` (zero dependencies)
- **Frontend**: HTML5 + CSS3 + Vanilla JS (ES6)  
- **Storage**: Plain CSV files
- **Desktop GUI**: Java Swing

---

## 📝 Notes

- All money stored as **paise** (long integer) — no floating-point rounding errors
- Passwords hashed with **SHA-256 + SecureRandom salt**
- Settlement uses **greedy min-flow algorithm** (at most N-1 transfers)
- No Maven, Gradle, or external libraries used anywhere

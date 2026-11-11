# SplitSmart

> **Split expenses. Settle fairly. Stay friends.**

SplitSmart is a clean, dependency-free Java application built for roommates, travel groups, and friends to record shared expenses, dynamically calculate net balances, and compute the optimal settle-up plan with the absolute minimum number of transfers.

It is available in two modes:
1. **Web Browser Application**: Running locally at **[http://localhost:8080](http://localhost:8080)** and deployable to any cloud platform (Docker, Render, Railway, etc.).
2. **Desktop Swing Application**: Clean GUI with warm amber glossy styling.

Built strictly with **Core Java (Java 21, JDK only)** without third-party frameworks, build tools, or external libraries.

---

## 🌐 Localhost Web Link

The web application is live locally:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 🚀 How to Run

### Option 1: Web Application (Browser)

**On Windows:**
```cmd
run-web.bat
```
Or manually:
```cmd
javac -d out -sourcepath src src/splitsmart/WebMain.java
java -cp out splitsmart.WebMain
```
Then open **[http://localhost:8080](http://localhost:8080)** in your browser.

**On macOS / Linux:**
```bash
chmod +x run-web.sh
./run-web.sh
```

---

### Option 2: Desktop GUI Application (Swing)

**On Windows:**
```cmd
run.bat
```
Or manually:
```cmd
javac -d out -sourcepath src src/splitsmart/Main.java
java -cp out splitsmart.Main
```

**On macOS / Linux:**
```bash
chmod +x run.sh
./run.sh
```

---

### Option 3: Run the Automated Tests

**Core Settlement Logic Test Suite (`SettlementTest.java`):**
```cmd
javac -d out -sourcepath src src/splitsmart/test/SettlementTest.java
java -cp out splitsmart.test.SettlementTest
```
Or run `test.bat` on Windows.

**End-to-End Walkthrough Test Suite (`EndToEndTest.java`):**
```cmd
javac -d out -sourcepath src src/splitsmart/test/EndToEndTest.java
java -cp out splitsmart.test.EndToEndTest
```

---

## 📦 How to Upload to GitHub

Follow these steps to upload SplitSmart to your GitHub account:

1. **Create a new repository on GitHub:**
   - Go to [github.com/new](https://github.com/new)
   - Repository name: `SplitSmart`
   - Set to **Public** or **Private** (do **NOT** check "Initialize this repository with a README", as we already have one)
   - Click **Create repository**

2. **Push your code to GitHub from your terminal:**
   Open Command Prompt / PowerShell in this directory (`C:\Users\srivi\.gemini\antigravity\scratch\SplitSmart`):
   ```cmd
   git add .
   git commit -m "Initial commit: SplitSmart desktop and web application"
   git remote add origin https://github.com/YOUR_GITHUB_USERNAME/SplitSmart.git
   git branch -M main
   git push -u origin main
   ```
   *(Replace `YOUR_GITHUB_USERNAME` with your actual GitHub username)*

---

## ☁️ How to Deploy to the Cloud (Free)

SplitSmart has zero external dependencies, making it deployable on any platform:

### Method A: Deploy on Render (Free)
1. Go to [render.com](https://render.com) and connect your GitHub account.
2. Click **New +** -> **Web Service**.
3. Select your `SplitSmart` repository.
4. Settings:
   - **Environment**: `Docker` (SplitSmart includes a production `Dockerfile`)
   - **Region**: Choose the closest region.
   - **Plan**: `Free`
5. Click **Create Web Service**. Render will automatically build the multi-stage Docker image and provide you with a live `https://splitsmart-xxxx.onrender.com` URL!

### Method B: Deploy on Railway (Free)
1. Go to [railway.app](https://railway.app) and sign in with GitHub.
2. Click **New Project** -> **Deploy from GitHub repo**.
3. Select `SplitSmart`. Railway will detect the `Dockerfile` or `Procfile` and deploy automatically with an assigned public URL.

### Method C: Run Anywhere with Docker
```bash
docker build -t splitsmart .
docker run -p 8080:8080 splitsmart
```
Then visit `http://localhost:8080`.

---

## 💡 Settle-Up Algorithm Explanation

1. Each member's net balance is calculated as total paid minus total share (summing to zero).
2. Members are divided into two distinct groups: receivers (net > 0) and payers (net < 0).
3. The algorithm greedily pairs the member who is owed the most with the member who owes the most.
4. A transfer is scheduled for the smaller of the two amounts, clearing at least one member's debt.
5. Both participants' balances are updated, and any member whose balance reaches zero is removed.
6. The matching repeats until all balances reach zero, guaranteeing settlement in at most N - 1 transfers.

---

## 📂 Project Structure

```text
SplitSmart/
├── Dockerfile                          (Multi-stage production container build)
├── Procfile                            (Cloud dyno runner configuration)
├── README.md                           (Complete documentation)
├── run-web.bat                         (Windows web launcher: http://localhost:8080)
├── run-web.sh                          (Mac/Linux web launcher)
├── run.bat                             (Windows desktop Swing launcher)
├── run.sh                              (Mac/Linux desktop Swing launcher)
├── test.bat                            (Windows test runner)
├── data/                               (Auto-managed persistent CSV storage)
├── web/                                (Web browser frontend — vanilla HTML/CSS/JS)
│   ├── index.html                      (Responsive SPA structure & modals)
│   ├── style.css                       (Warm Amber Glossy Light & Dark CSS)
│   └── app.js                          (Dynamic client logic, charts & API client)
└── src/
    └── splitsmart/
        ├── Main.java                   (Desktop GUI application entry point)
        ├── WebMain.java                (Web application entry point with port binding)
        ├── model/                      (Core entities: User, Group, Member, Expense, Transfer, Category, SplitType)
        ├── logic/                      (BalanceCalculator, SettlementPlanner, SplitCalculator, Money, Validator)
        ├── storage/                    (DataStore with CSV persistence, PasswordUtil with salted SHA-256)
        ├── web/                        (Built-in JDK HttpServer REST API & SimpleJson)
        ├── ui/                         (Desktop Swing components with Warm Amber Theme & Java2D graphics)
        └── test/                       (SettlementTest console tests & EndToEndTest walkthrough)
```

---

## 🌟 Key Features

1. **Authentication & Multi-User Isolation**: Salted SHA-256 password hashing. Users only see their own groups.
2. **Expense Splitting**: Equal (with 1-paisa rounding fix), Exact amounts, and Percentage splits.
3. **Dynamic Balances**: Balances recompute dynamically from the expense ledger; net sum is always zero.
4. **Minimum Cash Flow Settlement**: Greedily matches largest debtors and creditors for minimum transfers.
5. **Interactive Mark as Paid**: Immediately records settlements and updates balances.
6. **Analytics & Dashboards**: High-DPI category and weekly spending charts.
7. **Warm Amber Theme**: Live Light and Dark mode toggling.
8. **Resilient CSV Persistence**: Auto-saves after modifications with error-tolerant CSV parsing.

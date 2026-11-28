# 🏠 Family Pantry App — Easy Setup Guide

This takes about **3 minutes** and you only have to do it once. When you're done, your wife will have a **Family Pantry** icon right on her Android home screen that updates your Google Sheet automatically.

> **Tip:** If you have the **1-click Copy Helper** page open, you can copy each file with a single click instead of highlighting text manually!

---

## Step 1: Open Apps Script inside your Google Sheet

1. Open your Google Sheet in Chrome:
   **[Family Food Rations & Pantry Tracker](https://docs.google.com/spreadsheets/d/11ySbLHJaZi3eF_1iffbmlhhK1O_4o-7aYrnbkfgjd_0/edit)**
2. In the top menu bar, click **Extensions** → **Apps Script**.
3. A new browser tab will open — this is where your app will live.

---

## Step 2: Paste `Code.gs` (Connects the App to Your Sheet)

1. You'll already see a file open called **`Code.gs`** with a few starter lines inside.
2. Click inside that text area, select everything (**`Cmd + A`**), and press **Delete** so it's completely empty.
3. Copy all the text from **`archive/google-apps-script/Code.gs`** (or click the button on the **1-click Copy Helper** page) and paste it in (**`Cmd + V`**).
4. Click the **Save** icon (the floppy disk at the top) or press **`Cmd + S`**.

---

## Step 3: Add the Mobile App Screen (`Index.html`)

1. On the left side, next to **Files**, click the **`+`** button → choose **HTML**.
2. Type **`Index`** and press **Enter**.
   - *Important:* Use a capital **`I`** and exact spelling (`Index`). Google automatically adds `.html` for you so it becomes `Index.html`.
3. Click inside the new `Index.html` file, select all the starter lines (**`Cmd + A`**), and press **Delete**.
4. Copy all the text from **`archive/google-apps-script/Index.html`** (or use the **1-click Copy Helper** page) and paste it in (**`Cmd + V`**).
5. Click the **Save** icon or press **`Cmd + S`**.

---

## Step 4: Publish as a Web App (Get Your Private Link)

1. Click the blue **Deploy** button (top-right corner) → click **New deployment**.
2. Next to *Select type*, click the **gear icon (`⚙️`)** → choose **Web app**.
3. Fill in these three boxes:
   - **Description:** `Family Pantry App`
   - **Execute as:** **Me** (`fawadmubashir@gmail.com`)
   - **Who has access:** **Anyone**
     - *Why "Anyone"?* So your wife never gets stuck on Google sign-in prompts on her phone. The link is a long, private, unguessable address that only people you send it to will have. *(If you prefer, you can pick **Anyone with a Google account** instead.)*
4. Click **Deploy**.

### ⚠️ One-Time Google Permission Screen (Totally Normal!)
The first time you publish, Google will ask you to approve your own personal app:
- Click **Authorize access** → pick your Gmail (`fawadmubashir@gmail.com`).
- If Google shows a warning saying **"Google hasn't verified this app"**, don't worry — this always appears for personal scripts you create for yourself.
- Click **Advanced** (bottom-left) → click **Go to [Project Name] (unsafe)** → click **Allow**.

5. On the next screen, click **Copy** next to your **Web app URL** (save it somewhere handy!), then click **Done**.

---

## Step 5: Pin the App Icon to Your Wife's Android Home Screen

1. Send your wife that **Web app URL** by text or WhatsApp.
2. Have her open the link in **Chrome** on her Android phone.
3. Tap the **3 dots (`⋮`)** in the top-right corner of Chrome.
4. Tap **Add to Home screen**.
5. Name it **`Family Pantry`** and tap **Add**.
6. **All done!** She now has a normal **Family Pantry** app icon on her phone and never has to open Google Sheets.

---

## 💡 Bonus Tips

- **Your existing data is completely safe:** The app automatically detects your existing columns and keeps everything intact (or creates a clean **`Pantry Inventory`** tab if the sheet is blank). Every change is also recorded automatically in a **`History Log`** tab so you can see what was updated and when.
- **New menu inside your Google Sheet (optional):** Go back to **Family Food Rations & Pantry Tracker** and refresh the page. You'll see a new **`🏠 Family Pantry`** menu at the top — including a 1-click option to create a synced Google Form if you ever want one.

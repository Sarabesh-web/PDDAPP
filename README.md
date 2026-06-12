# Smart Academic Productivity Suite — Mobile App

A native Android replica of the **Smart Academic Planner** application. Built using modern Android development practices: **Jetpack Compose** for UI styling, **Kotlin Coroutines** and **Flows** for state management, and **Retrofit** for real-time synchronization with the Node.js Express & MongoDB backend API.

---

## 📱 Features & Capabilities

* **Auth Screen**: Native Login & Registration connected to the Express backend (passwords are securely hashed using PBKDF2).
* **Dashboard (Home)**: Daily motivation, statistical performance metrics (study hours, completion rates, streaks), and upcoming deadlines.
* **Planner**: Horizontal tabbed schedule viewer to plan, check off, or delete daily study blocks.
* **Tasks**: Complete Task manager with search querying, priority badges (high, medium, low), and state filter tabs.
* **Focus Timer**: Custom Pomodoro study/break loop alert tool to keep students on track.
* **Notes**: Interactive Grid-layout note-taker with subject indicators.
* **Analytics**: Graph distributions mapping study habits and subject distributions.
* **Settings**: Dynamic dark/light theme switching, notification toggle switches, study timer configuration, and backend API server routing settings.

---

## 🛠️ Synchronization & Networking Architecture

```
                 ┌────────────────────────────────┐
                 │       Jetpack Compose UI       │
                 └───────────────┬────────────────┘
                                 │ Collects Flows
                 ┌───────────────▼────────────────┐
                 │        DataRepository          │
                 └───────────────┬────────────────┘
                                 │ Executes Retrofit calls
                 ┌───────────────▼────────────────┐
                 │           ApiService           │
                 └───────────────┬────────────────┘
                                 │ HTTP requests
                 ┌───────────────▼────────────────┐
                 │      Express Backend API       │
                 └────────────────────────────────┘
```

* **Session Management**: A helper `SessionManager` class utilizes secure `SharedPreferences` to cache your JWT authentication token, user information, and custom API base URL locally on the device.
* **Header Interceptor**: All outgoing requests are intercepted and appended with an `Authorization: Bearer <token>` header to authenticate resource queries.
* **Dynamic Server Routing**: You can point the app to any backend instance (localhost or cloud-deployed) directly from the Login/Register screen.

---

## 🚀 How to Run and Test

### Prerequisites
1. Open and start your backend server inside `PDD/backend/` (`npm run dev`).
2. Make sure your Android SDK and Android Studio are installed.

### Step 1: Open in Android Studio
1. Launch Android Studio.
2. Click **Open** and select the [PDDapp](file:///C:/Users/sarab/OneDrive/Desktop/PDDapp) directory.
3. Allow Gradle to sync and build project models.

### Step 2: Configure Backend Connection
When you launch the app, you will be presented with a Login / Signup screen. Enter your student account details and configure the **Backend Server URL**:

* **If running on the Android Emulator**:
  Use the default pre-filled URL: `http://10.0.2.2:3001` (this points to your host computer's localhost port `3001`).
* **If running on a Physical Android Device**:
  Ensure your phone and computer are connected to the same Wi-Fi network, and use your computer's local IP address (e.g., `http://192.168.1.50:3001`).
* **If deployed in Production (Render)**:
  Enter your Render web service URL (e.g., `https://your-backend.onrender.com`).

*Note: Cleartext traffic is enabled in the Manifest, allowing you to connect to local HTTP debug servers.*

# HealSphere 🩺  
A smart Android app for medicine reminders, health vitals tracking, and emergency assistance.

---

## 🚀 Features

### 👤 Profile Management
- Create and switch between multiple user profiles  
- Upload profile photo  
- Update personal details securely  
- Emergency contact stored and used for direct CALL & SOS SMS  

---

### 💊 Medicine Management
✔ Add medicines with  
   → Name, Dosage, Time, Notes, Prescription image  
✔ Automatic Alarm & Notification reminder  
✔ Logs taken/missed medicines  
✔ Daily Medicine Report (Pie Chart)  

---

### 🌡 Health Vitals Tracker
Log and visualize:
| Vital | Format | Chart Type |
|-------|--------|------------|
| Blood Pressure | 120/80 | Bar (stacked) |
| Weight | 65 kg | Line Chart |
| Glucose (Sugar) | 98 mg/dL | Line Chart |

📈 All vitals saved with dates in Firestore and shown in trends.

---

### 📊 Daily Health Report
- Medicine intake status (Taken vs Missed)  
- Water Intake progress  
- BP / Sugar / Weight Trends  
- Graphs using **MPAndroidChart**  

---

### 🚨 Emergency Support
- 📞 Direct Call to saved emergency contact  
- 📩 Send pre-filled SOS message with location info (optional)

---

### 💧 Water Tracker
- Track glasses taken daily  
- Persistent counter stored locally  
- Motivational feedback (goal-based)

---

## 🛠 Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java (Android) |
| UI | XML Views, Material Design |
| Database | Firebase Firestore |
| Storage | Firebase Storage |
| Image Loading | Glide |
| Notifications | AlarmManager, BroadcastReceiver |
| Charts | MPAndroidChart |
| Local Storage | SharedPreferences |

---

## 🔧 Firebase Structure

users

└── {userId}

├── name, age, contact, photoUrl, emergencyContact

├── medicines

│ └── {medicineId} → name, dosage, time, note, prescriptionUrl

├── logs

│ └── YYYY-MM-DD / entries / taken/missed

└── vitals

└── YYYY-MM-DD → systolic, diastolic, glucose, weight

---

## 📂 Project Structure

HealSphere/

└── app/

├── activities/

│ ├── MainActivity.java

│ ├── ProfileSelectionActivity.java

│ ├── DailyReportActivity.java

│ ├── ViewMedicinesActivity.java

├── receivers/

│ ├── ReminderReceiver.java

│ ├── HealthTipReceiver.java

├── xml layouts/

├── drawable/

├── manifest/

---

## 🚀 Installation & Setup

### 1️⃣ Clone the repo
```bash
git clone https://github.com/yourusername/HealSphere.git

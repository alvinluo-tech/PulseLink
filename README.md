# PulseLink 🏥

English | [简体中文](README_CN.md)

A comprehensive health monitoring and medication management application for senior care, built with Jetpack Compose and modern Android development practices.

## 📱 Overview

PulseLink is designed to help seniors and their caregivers manage health data, medication reminders, and monitor vital signs. The app provides an intuitive interface for tracking blood pressure, heart rate, and maintaining medication schedules.

## ✨ Features

### 👤 User Management
- **Dual Login System**: Separate login flows for seniors and caregivers
- **Role-based Access**: Tailored experiences based on user role
- **Profile Management**: Personalized user profiles with health summaries

### 🩺 Health Monitoring
- **Blood Pressure Tracking**: Record and monitor systolic/diastolic readings
- **Heart Rate Monitoring**: Track heart rate measurements
- **Health History**: View historical health records with color-coded status indicators
- **Data Visualization**: Easy-to-read health summary cards

### 💊 Medication Management
- **Smart Reminders**: Timely medication notifications
- **Reminder History**: View today's medication schedule with status tracking
- **Take/Miss Tracking**: Record medication adherence
- **Medication Details**: Dosage, timing, and medication name tracking

### 🤖 AI Voice Assistant
- **Conversational Interface**: Chat-based interaction for health queries
- **Voice Input Support**: Speak to ask questions
- **Health Guidance**: Get answers about health and medication

### 📊 Dashboard
- **Health Data Overview**: Quick access to vital health metrics
- **Quick Actions**: Navigate to key features from home screen
- **Daily Summary**: View reminder counts and health status at a glance

## 🏗️ Architecture

PulseLink follows **Clean Architecture** principles with **MVVM** pattern:

```
app/
├── data/                      # Data layer
├── domain/                    # Domain layer (business logic)
├── presentation/              # Presentation layer (UI)
│   ├── assistant/            # AI voice assistant feature
│   ├── health/               # Health data input
│   ├── history/              # Health history viewing
│   ├── home/                 # Main dashboard
│   ├── login/                # Authentication
│   ├── navigation/           # Navigation configuration
│   ├── profile/              # User profile
│   ├── reminder/             # Single medication reminder
│   ├── reminderlist/         # Medication reminder list
│   └── welcome/              # Welcome/onboarding
├── di/                       # Dependency Injection
└── ui/                       # UI theme and components
```

## 🛠️ Tech Stack

### Core
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern declarative UI toolkit
- **Material Design 3** - UI design system

### Architecture & Libraries
- **Hilt** - Dependency injection
- **Navigation Component** - In-app navigation
- **ViewModel** - UI state management
- **StateFlow** - Reactive state management
- **Kotlin Coroutines** - Asynchronous programming

### Build & Tools
- **Gradle Kotlin DSL** - Build configuration
- **Version Catalogs** - Dependency management
- **Android Studio** - IDE

## 📋 Prerequisites

- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 24 (Nougat) or higher
- Gradle 8.13.1

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/alvinluo-tech/PulseLink.git
cd PulseLink
```

### 2. Open in Android Studio
- Launch Android Studio
- Select "Open an Existing Project"
- Navigate to the cloned repository

### 3. Sync Dependencies
```bash
./gradlew build
```

### 4. Run the App
- Connect an Android device or start an emulator
- Click "Run" in Android Studio or use:
```bash
./gradlew installDebug
```

## 📱 App Screens

### Welcome & Authentication
- **Welcome Screen**: Choose between senior or caregiver login
- **Login Screen**: Authenticate using phone number

### Main Features
- **Home Dashboard**: Overview of health metrics and quick actions
- **Health Data Entry**: Input blood pressure and heart rate
- **Health History**: View past health records with status indicators
- **Profile**: User information and app settings
- **Reminders**: View and manage medication reminders
- **Voice Assistant**: AI-powered health assistant

## 🎨 Design Highlights

- **Senior-Friendly UI**: Large text, high contrast, simple navigation
- **Color-Coded Status**: 
  - 🟢 Green: Normal/Taken
  - 🔵 Blue: Pending
  - 🔴 Red: High/Missed
  - 🟡 Yellow: Low
- **Bottom Navigation**: Easy access to key features
- **Floating Action Button**: Quick access to voice assistant
- **Card-Based Design**: Clear separation of information

## 🔐 Security & Privacy

- User authentication required
- Role-based access control
- Secure data handling
- Privacy-first design

## 📦 Project Structure

```
PulseLink/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/alvin/pulselink/
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   ├── presentation/
│   │   │   │   ├── di/
│   │   │   │   └── ui/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── ARCHITECTURE.md
├── MIGRATION_GUIDE.md
└── README.md
```

## 🧪 Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- **Alvin Luo** - *Initial work* - [alvinluo-tech](https://github.com/alvinluo-tech)

## 🙏 Acknowledgments

- Material Design 3 guidelines
- Jetpack Compose community
- Android development best practices

## 📞 Support

For support, please open an issue in the GitHub repository or contact the development team.

## 🗺️ Roadmap

- [ ] Integration with wearable devices
- [ ] Real-time health monitoring
- [ ] Caregiver notification system
- [ ] Multi-language support
- [ ] Cloud data synchronization
- [ ] Emergency contact features
- [ ] Health report generation
- [ ] Integration with healthcare providers

## 📱 Screenshots

*(Add screenshots of your app here)*

## 🔧 Configuration

### Firebase Setup (Required)
This project uses Firebase services. To set it up:

1. **Create a Firebase Project**:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or use existing one

2. **Add Android App**:
   - Register your app with package name: `com.alvin.pulselink`
   - Download `google-services.json`

3. **Add Configuration File**:
   ```bash
   # Copy the downloaded file to app directory
   cp /path/to/google-services.json app/
   ```
   
   Or rename the example file:
   ```bash
   cp app/google-services.json.example app/google-services.json
   # Then update with your actual Firebase credentials
   ```

4. **Enable Firebase Services** (in Firebase Console):
   - Authentication (if needed)
   - Firestore Database (if needed)
   - Cloud Storage (if needed)

⚠️ **Important**: Never commit `google-services.json` to version control as it contains API keys!

### API Keys
Configure API keys in `local.properties`:
```properties
API_KEY=your_api_key_here
```

---

**Note**: This is an educational/demonstration project for senior health care management. Consult healthcare professionals for medical advice.

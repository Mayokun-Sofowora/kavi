# Kavi - Multi-variant Dice Game

| Virtual screen | Balut Board | Greed Board | Pig Board |
|--------------|------------|-------------|----------|
| ![virtual screen3](https://github.com/user-attachments/assets/fc3141ba-0b10-4931-8eaa-dca09073de80)| ![Balut Board](https://github.com/user-attachments/assets/fa2d1da7-56fc-442f-b002-55c76ef40208) | ![Greed Board](https://github.com/user-attachments/assets/e118826f-6be8-4fdd-b0b1-1add523a49bc) | ![Pig Board](https://github.com/user-attachments/assets/ecd50679-990b-424b-9a5c-5f944457f7ff) |



A modern Android dice game application implementing multiple classic and custom dice game variants with AI opponents. Built using Jetpack Compose and following clean architecture principles.

## Game Variants

### 1. Pig
A simple press-your-luck dice game where players:
- Roll a single die repeatedly to accumulate points
- Can bank points at any time
- Lose all accumulated points if they roll a 1
- First to reach 100 points wins

### 2. Greed
A complex dice game with multiple scoring combinations:
- Roll six dice and choose which to keep
- Score combinations like straights, sets, and single 1s/5s
- Must score at least 800 points to start banking
- First to reach 10,000 points wins

### 3. Balut
A strategic dice game similar to Yahtzee:
- Roll five dice up to three times per turn
- Score in various categories (pairs, straights, etc.)
- Each category can only be used once
- Highest total score wins after all categories are filled

## Features

- 🎲 Multiple game variants with different rule sets
- 🤖 Adaptive AI opponent with dynamic difficulty
- 📊 Comprehensive statistics tracking
- 🎮 Intuitive touch and shake controls
- 🌟 Customizable interface themes
- 📱 Modern Material Design 3 UI

## Technical Details

### Architecture
- MVVM architecture with Clean Architecture principles
- Dependency injection using Hilt
- Kotlin Coroutines and Flow for reactive programming
- Jetpack Compose for modern UI implementation

### Key Components
- GameManagers: Handle game logic and state management
- ScoreCalculator: Processes dice combinations and scoring
- StatisticsManager: Tracks game statistics and player behavior
- AI Implementation: Adaptive opponent with strategic decision making

## Setup and Installation

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on an Android device/emulator (min SDK 30)

## API Configuration (Roboflow)

If the Roboflow API key is ignored in the release build, you can retrieve and provide it manually:

1. **Sign in to Roboflow** at [https://roboflow.com/](https://roboflow.com/).
2. Navigate to your **Project Dashboard**.
3. Locate the **API Key** under the "Settings" or "Security" section.
4. Add the key manually to your local `gradle.properties` file:
   ```properties
   ROBOFLOW_API_KEY=your_api_key_here
   ```
5. In your `local.properties` (not committed to version control), reference it in `build.gradle`:
   ```gradle
   buildConfigField "String", "ROBOFLOW_API_KEY", '"' + ROBOFLOW_API_KEY + '"'
   ```
6. Access the API key in your code safely using:
   ```kotlin
   val apiKey = BuildConfig.ROBOFLOW_API_KEY
   ```

## Development

### Building
```bash
./gradlew build
```

### Testing
```bash
./gradlew test
```

### Release Testing
To test the release version of the app:
1. Connect an Android device with USB debugging enabled
2. Run the following command from the project root:
```bash
./gradlew installRelease
```
This will install the release version of the app on your device for testing.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Material Design 3 for UI components
- Jetpack Compose for modern UI toolkit
- Android Architecture Components

## About
For detailed documentation, including the project description, installation guidelines, user manual, and additional technical insights, refer to the accompanying thesis available in the [Mayokun-Sofowora/kavi-docs](https://github.com/Mayokun-Sofowora/kavi-docs) repository.


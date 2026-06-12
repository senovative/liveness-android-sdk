# Contributing to Liveness Android SDK

First off, thank you for considering contributing to the Liveness Android SDK! It's people like you that make open-source SDKs a great tool for developers.

## Where do I go from here?

If you've noticed a bug or have a feature request, please make sure to check the [Issues](issues) to see if someone else has already reported it or requested it. If not, feel free to open a new issue.

## Setting up the development environment

1. **Fork** the repository on GitHub.
2. **Clone** the repository to your local machine.
3. Open the project in **Android Studio**.
4. The project contains two main modules:
   - `liveness-sdk`: The core SDK library.
   - `sample`: A sample app demonstrating how to use the SDK.
5. Build the `sample` module to test your changes locally.

## Making a Pull Request

1. Create a new branch for your feature or bugfix (`git checkout -b feature/your-feature-name`).
2. Make your changes in the `liveness-sdk` module.
3. If applicable, update the `sample` app to demonstrate your changes.
4. Ensure all builds and tests pass successfully (`./gradlew assembleDebug`).
5. Commit your changes (`git commit -m 'Add some feature'`).
6. Push to the branch (`git push origin feature/your-feature-name`).
7. Open a Pull Request from your branch to our `main` branch.

## Code Style

- We follow standard Kotlin coding conventions.
- Keep the code clean, well-documented, and modular.
- Please do not include any unnecessary changes in your Pull Request.

Thank you for your contributions!

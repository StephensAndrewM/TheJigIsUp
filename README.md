# The Jig Is Up

## Overview

## Installation Instructions

*Note: The Android OS and its components are updated frequently. The steps given below have been tested and work on Ubuntu as of May 2016, but may be different in the future.*

1. Install [Android Studio](http://developer.android.com/sdk/index.html) using the appropriate installation method for your system. Instructions for doing so can be found on the [Android Studio documentation](http://developer.android.com/sdk/installing/index.html). *Note: If you have never developed Java on your computer, you will also need to install the JDK from Oracle using [these instructions](http://www.webupd8.org/2012/09/install-oracle-java-8-in-ubuntu-via-ppa.html) (for Ubuntu).*

2. Start Android Studio and follow the Setup Wizard. You may be prompted to locate the Android SDK, at which point you should cancel the prompt, then open Tools > Android > SDK Manager to download and install the SDK.

3. Use Git to clone this repository into a local working directory.

4. Open the project in Android Studio. Build the project using Build > Make Project. You may then deploy to your phone or use the Android emulator for testing. *Note: If this is your first time developing for Android, you will need to enable Developer Mode to allow Android Studio to communicate. These instructions are different depending on your Android version. *

### Notes

* We have had the best experience installing and configuring Android Studio on Ubuntu. 

* Android Studio's frequent changes are quite frustrating. There's a good chance that by the time you build this, some breaking change will have been made in the software (through no fault of our own). Prepare to spend some time troubleshooting.

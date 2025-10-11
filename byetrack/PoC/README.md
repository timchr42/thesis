
# Proof-Of-Concept: Capabilities as a Solution against Tracking

- This repository contains the code for two HyTrack apps, modified with the mitigation framework, as well as a test app to demonstrate additional functionality, a malicious app (Evil) trying to impersonate the test app (inclused a respective local evil library) and a custom installer.
- The byetrack library providing the app-side mitigaiton functionality is inluded in its own repository, such as the modified androidx browser library, which uses the token enhanced launchUrl method instead of the vulnerable one.
- Similarly, the modified Firefox Fenix browser is in its own repository, which is used as the browser in this demo.

The same setup steps were used as outlined by the authors of HyTrack. For reference see `README_setup.md`.

** Additional Information: **
- Throughout the changes, I refere to the mitigation framework as "byetrack".
- When looking for changes in the codebase, search for `// byetrack` comments.
- This mitigation framework is only implemented for Custom Tabs (CTs), as Trusted Web Activities (TWAs) are not supported by Firefox, nonetheless, the mitigation framework would work the same way for TWAs.

## Webserver Set-Up
- Via docker: cd into the web server's folder and run `docker-compose up`. 
  - if you have a new docker version, use `docker compose up` instead.
  - ad.com/`examplecorp.de` is hosted on localhost:80
  - the other web tracking host / `schnellnochraviolimachen.de` is hosted on `localhost:8080`
- Alternatively: Host it on a real webserver.
  - Host `webapp/examplecorp` folder with PHP under a domain, this acts as `ad.com`
  - We have used apache and PHP 8.2
  - Exchange the encryption key in L2 of `util.php` 
  - Configure TLS in your web server
- Optionally: To test web tracking, host webapp/ravioli under a different domain

## App Set-Up:

### Preamble
- The HyTrack apps need to be configured for a specific host acting as `ad.com`, as specified above.
- If you are running our webserver via Docker and the Android Emulator, you can just use our defaults:
  - examplecorp.de --> `localhost:80` on host --> maps to `10.0.2.2` in the emulator
  - schnellnochraviolimachen.de --> localhost:8080 on host --> maps to `10.0.2.2:8080` in the emulator
- If you are running on a real Android device you have to adjust the values accordingly and must ensure that the device can reach the webserver.
  - For this, search in the code base and replace all occurrences of `10.0.2.2` with your respective host. Replace `http` with `https` if needed.
- **Generating release keys** and enabling **chrome debug mode** like the docs of HyTrack describe is not necessary for this demo, as this is only necessary for Trusted Web Activities, which are not used since they are not supported by firefox.

> [!NOTE]
> The custom installer app installs the HyTrack apps with the default configuration. If you want the installer to install the apps with your custom configuration, you need to replace apk files in the installer's `assets` folder.

### Defense Library Set-Up
- By default, the apps are configured to use the latest release of the byetrack and modified androidx.browser library via jitpack. If you wish to build the library from source locally via gradle, you need to:
    - Clone the byetrack library from github: `git clone git@github.com:timchr42/byetrack.git`.
    - Clone the androidx.browser fork from github: `git clone git@github.com:timchr42/AndroidxBrowserByetrack.git`.
    - In both library folders, run `./gradlew publishToMavenLocal` or execute the task in Android Studio to build and publish the libraries to your local maven repository.
    - Make sure you add the following to the apps's settings.gradle's depenedyResolutionManagement:

      ```gradle
        ...
        repositories {
            ...
            mavenLocal()
        }
      ```
    - In the apps's build.gradle, replace the dependencies for the byetrack and androidx.browser library with:
      ```gradle
        dependencies {
            ...
            implementation("com.timchr42:AndroidxBrowserByetrack:0.1.0")
        }
      ```

### Building & Installing
- via cli: run `./gradlew assembleDebug` in the custom installer folder.
- Install the installer from the output folder (`customInstaller/app/build/outputs/apk/debug/app-debug.apk`)
- run `adb install app-debug.apk` to install the custom installer on your device/emulator
- OR: Open the custom installer project in Android Studion and run it in your device/emulator
- The installer allows you to install both HyTrack apps and the test app via buttons.

## Browser Set-Up:
- Clone the modified [firefox fork](https://github.com/timchr42/firefox) from git instead of the original one.
 - f.e. via ssh: `git clone git@github.com:timchr42/firefox.git`.
- Make sure you are on the correct branch (`capability-mods-clean`).
- Follow the instructions provided by the firefox [docs](https://firefox-source-docs.mozilla.org/mobile/android/fenix.html) to setup the build environment correctly depending on your OS.
- If everything is set up correctly, you can build and install the browser via `./mach build` and `./mach install --app fenix` respectively in the root of the firefox folder.


## Test
- Make sure Firefox (Fenix) is the default browser on the emuluator or device.
- Follow steps in the demo or play around.
- Feel free to try out different policy tailored to you own needs by changing the `policy.json` in the assets folder the respective app.
- Also, feel free to try out the modified `androidx.browser` [library](https://github.com/timchr42/AndroidxBrowserByetrack) including the necessary changes hosted on github via jitpack in your own apps.
 - Add maven repo to repositories in your app's settings.gradle:
   ```gradle
   repositories {
       ...
       mavenCentral()
       maven { url 'https://jitpack.io' }
   }
   ```
 - Add the dependency in your app's build.gradle:
   ```gradle
   dependencies {
       implementation 'com.github.timchr42:AndroidxBrowserByetrack:0.2.0'
   }
   ```
 - exclude the original androidx.browser dependency if you have it to avoid dependency conflicts:
   ```gradle
   configurations {
       all {
           exclude group: 'androidx.browser', module: 'browser'
       }
   }
   ```

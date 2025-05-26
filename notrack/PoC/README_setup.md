
# HyTrack Proof-Of-Concept
- this repository contains the code for two HyTrack TWA apps, as well as a PHP-backend for HyTrack
- It was used in the demo video.

**Terminology:**  
- HyTrack requires one tracking service. We call this service `ad.com` in the paper.
- For the Demo and testing purposes we hosted the PoC under different domains. Replace them with your own as needed.
- As `ad.com` *we* have used the domain `examplecorp.de`. For local testing, this repo uses `http://localhost` on port 80.
- Since HyTrack bridges the gap from native to web tracking, we have used another web site as an example.
- *We* have used `schnellonochraviolimachen.de`. For local testing, this repo uses `http://localhost:8080`
- Note that in the Demo we have used TLS and therefore HTTPS as protocol. For local testing this is infeasible, therefore we'll use http for localhost.

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
- The apps need to be configured for a specific host acting as `ad.com`, as specified above.
- If you are running our webserver via Docker and the Android Emulator, you can just use our defaults:
  - examplecorp.de --> `localhost:80` on host --> maps to `10.0.2.2` in the emulator
  - schnellnochraviolimachen.de --> localhost:8080 on host --> maps to `10.0.2.2:8080` in the emulator
- If you are running on a real Android device you have to adjust the values accordingly and must ensure that the device can reach the webserver.
  - For this, search in the code base and replace all occurrences of `10.0.2.2` with your respective host. Replace `http` with `https` if needed.


### Generate release keys
- either via Android Studio GUI
- or: `keytool -genkey -v -keystore /root/testdeploykey -keyalg RSA -alias key0`. 
  - use password `password` and empty key details for testing purposes
- If you want to establish a real DAL, without using chrome's debug mode, follow the next step.

### Establishing a real DAL
- Skip this for the Artifact Functional evaluation, as it requires full-control over a domain and a valid TLS chain
  - Use Chrome's debug mode for quick testing instead, detailed below.
- Adjust `build.gradle` with key details accordingly.
    - https://developer.android.com/studio/publish/app-signing
- Adjust domains in DALs in the app, at both `app/src/main/res/values/strings.xml`
- Adjust domains in source of the app, replace `examplecorp.de` at both `app/src/main/res/layout/fragment_first.xml`
- Adjust release keys for DALs in the `.well-known` folder of the webapp(s)
    - Refer to https://developer.android.com/training/app-links/verify-android-applinks#web-assoc 

### Chrome Debug Mode
- As Chrome requires a HTTPS page for DALs and therefore Trusted Web Activities, we want to use a less cumbersome way for local testing
    - Chrome src for that check: https://source.chromium.org/chromium/chromium/src/+/main:components/content_relationship_verification/android/java/src/org/chromium/components/content_relationship_verification/OriginVerifier.java;l=189;drc=1627d349077c2aee9500cbf21c1ec7f8d908b2be
- The downside is that Chrome will show a warning that testing flags are enabled.
- Enable debug mode
  - In the mobile Chrome navigate to `chrome://flags`
  - Search for and enable `Enable command line on non-rooted devices`
  - Restart Chrome
  - On the host system open a terminal and run `adb shell "echo '_ --disable-digital-asset-link-verification-for-url=\"http://10.0.2.2\"' > /data/local/tmp/chrome-command-line"`
  - Source: https://developer.chrome.com/docs/android/trusted-web-activity/integration-guide#debugging

### Building

#### Docker
- in the `PoC` directory run `docker run -it -v ./:/in --entrypoint=bash runmymind/docker-android-sdk:ubuntu-standalone-20250210`
- cd into `/in`
- in both app dirs, run `./gradlew assemble`

#### Regular 
- via cli: `./gradlew assemble` in the apps' folder
- OR: Open app in Android Studio and build
    - The `build.gradle` builds the regular (developer) build with release keys, i.e. DALs and therefore TWAs work without explicitly building a release


## Test
- Install apps from output folders (`/app/build/outputs/apk/release/app-release.apk`)
  - `adb install app-release.apk`
- Follow steps in demo or play around
- Opposed to the demo, we have enabled an input element to change the `ad.com` host in the app.
- The Launcher App / "ITWA" directly launches into the Trusted Web Activity
- The other app supports the restore-functionality. It can be toggled on and off.

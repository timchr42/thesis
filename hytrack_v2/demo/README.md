# DEMO

This folder contains our demonstration of HyTrack, a mobile tracking technique using Custom Tabs (CTs) and Trusted Web Activities (TWAs).
See `demo.mp4` to see HyTrack in action

## Description and Explantions

- **Setup** The left panel shows incoming requests for the web server. It acts as our fictious tracking provider "ad.com" and is hosted on examplecorp.de. Another website using HyTrack is hosted on another domain. The right panel shows an emulator Pixel 6A with Android 11, Play Store & Chrome 127. Besides tracking an ID, the demo and PoC track the names of events in the cookie itself for demonstration purposes. We cleared the storage of the second app.
- **0:00** We reset Chrome's browsing data.
- **0:06** ad.com is visited, assigning a new tracking ID. The request can be observed on the left.
- **0:11** The first HyTrack-TWA app was started. It starts directly into a TWA. The left shows the request to the `assetlinks.json` triggered by Chrome to verify the DAL. The app submits the event `TrackingEventA' via the `app' URL parameter. The previously assigned tracking cookie is transmitted as `crossapptracking'.
- **0:13** Clicking `Continue' takes the user seamlessly from the TWA to the native application.
- **0:29** The second app is started. It launches into a native activity. Clicking the button opens the TWA. A toggle is provided (for demonstration purposes only) that toggles the restoring behavior.
- **0:32** The TWA is rendered with a Chrome banner informing the user that they are running Chrome. The banner disappears automatically at 0:37. Note how it does not re-appear at the second usage of this TWA later on.
- **0:40** A click on `Continue' has taken us back to the native activity. The cookie data was transmitted to the app's local storage.
- **0:45** The web tracking scenario is executed. It tracks us via naive bounce tracking, i.e., by redirection.
- **0:58** The second app is used again. Note how the banner does not reappear. The local storage is updated with the recent event.
- **1:10** We reset Chrome's browsing data. Since the cookie is deleted, the server assigns a new tracking ID.
- **1:23** Enabling the restoring behavior reconstructs the tracking cookie and information. This can be observed by the `c' parameter in the URL request, which contains an encrypted blob containing the cookies.

## LICENSE

This document and the demo are licensed under the CC BY 4.0 license. Author: Malte Wessels

https://creativecommons.org/licenses/by/4.0/

# Home Cams (Android TV)

Shows Camera 01, Camera 02 and Camera 03 (one lens of Camera 02) in a grid, playing RTSP directly. No server needed.

## Get the APK (no Android Studio needed)
1. Create a new GitHub repo and upload everything in this folder, including the hidden `.github` folder.
2. Open the repo's **Actions** tab, choose **Build APK** and click **Run workflow** (it also runs on every push).
3. When it finishes (about 5 to 10 minutes), open the run and download the **HomeCams-apk** artifact. Unzip it to get `app-debug.apk`.

## Install on the box
- Easiest: copy `app-debug.apk` to a USB drive, plug it into the box and open it with a file manager app (allow "install unknown apps" when asked).
- Or from a PC on the same Wi-Fi: `adb connect <box-ip>:5555` then `adb install app-debug.apk`.

## Using it
Use the remote's arrows to move between cameras and OK to make one fullscreen. Back returns to the grid.

## Settings
Camera URLs and the lens settings are at the top of `app/src/main/java/com/home/cams/MainActivity.kt`.
- If Camera 03 shows the wrong half, change `LENSES_STACKED` and `LENS_INDEX`.
- If Camera 01 doesn't play, check its RTSP path (assumed to match Camera 02).
- Camera 03 opens a second connection to Camera 02. If the camera only allows one viewer at a time, Camera 03 or Camera 02 may drop out.

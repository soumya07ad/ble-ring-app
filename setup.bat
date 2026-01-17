@echo off
REM Download gradle wrapper jar if missing
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo Downloading Gradle wrapper...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('https://services.gradle.org/distributions/gradle-8.5-bin.zip', 'gradle-8.5-bin.zip')"
    echo Extracting Gradle...
    powershell -Command "Expand-Archive -Path 'gradle-8.5-bin.zip' -DestinationPath '.'"
    if exist "gradle-8.5\lib\gradle-wrapper.jar" (
        if not exist "gradle\wrapper" mkdir gradle\wrapper
        move /Y "gradle-8.5\lib\gradle-wrapper.jar" "gradle\wrapper\gradle-wrapper.jar"
        rmdir /s /q gradle-8.5
        del gradle-8.5-bin.zip
    )
)

echo Build successful!
echo Open FitnessAndroidApp in Android Studio to run the app.

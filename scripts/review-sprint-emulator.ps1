param([string]$Avd = 'Medium_Phone_API_36.1', [string[]]$Modes = @('setupsession', 'sprintui'))
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$sdk = 'C:\Users\ruanc\AppData\Local\Android\Sdk'
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$output = Join-Path $project 'releases\sprint-review'
New-Item -ItemType Directory -Force -Path $output | Out-Null
# This harness may install only on the emulator it starts, never on an attached TV.
$emulator = Start-Process -FilePath (Join-Path $sdk 'emulator\emulator.exe') -ArgumentList @('-avd', $Avd, '-port', '5556', '-no-window', '-no-audio', '-no-snapshot', '-gpu', 'swiftshader_indirect') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $output 'emulator.txt') -RedirectStandardError (Join-Path $output 'emulator-errors.txt')
try {
    $ready = $false
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        if ($emulator.HasExited) { throw 'Emulator exited before testing.' }
        $boot = & $adb -s emulator-5556 shell getprop sys.boot_completed 2>$null
        if ($boot -eq '1') { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'Emulator did not boot within the bounded startup window.' }
    & $adb -s emulator-5556 shell wm size 1920x1080
    & $adb -s emulator-5556 shell wm density 320
    & $adb -s emulator-5556 install -r (Join-Path $project 'app\build\outputs\apk\debug\app-debug.apk')
    if ($LASTEXITCODE -ne 0) { throw 'Emulator TV APK installation failed.' }
    & $adb -s emulator-5556 install -r -t (Join-Path $project 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')
    if ($LASTEXITCODE -ne 0) { throw 'Emulator test APK installation failed.' }
    $failures = @()
    foreach ($mode in $Modes) {
        $testRun = Start-Process -FilePath $adb -ArgumentList @('-s', 'emulator-5556', 'shell', 'am', 'instrument', '-w', '-e', 'mode', $mode, 'za.co.rc.tv.test/za.co.rc.tv.WallpaperImportCheck') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $output "$mode.txt") -RedirectStandardError (Join-Path $output "$mode-errors.txt")
        if (-not $testRun.WaitForExit(120000)) { Stop-Process -Id $testRun.Id -Force; throw "Instrumentation timed out: $mode" }
        Get-Content (Join-Path $output "$mode.txt")
        Get-Content (Join-Path $output "$mode-errors.txt")
        if (-not (Select-String -Path (Join-Path $output "$mode.txt") -Pattern 'INSTRUMENTATION_CODE: -1' -Quiet)) { $failures += $mode }
    }
    & $adb -s emulator-5556 pull /sdcard/Android/data/za.co.rc.tv/files (Join-Path $output 'captures')
    & $adb -s emulator-5556 shell dumpsys meminfo za.co.rc.tv | Set-Content (Join-Path $output 'meminfo.txt')
    & $adb -s emulator-5556 shell run-as za.co.rc.tv du -sk files cache code_cache | Set-Content (Join-Path $output 'storage.txt')
    if ($failures.Count -gt 0) { throw "Failed/incomplete emulator instrumentation: $($failures -join ', '). See captured outputs." }
} finally {
    if (-not $emulator.HasExited) { Stop-Process -Id $emulator.Id -Force }
}

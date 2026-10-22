param([Parameter(Mandatory=$true)][string]$SigningDirectory)
$ErrorActionPreference='Stop'
$taskKeyFolder=(Resolve-Path -LiteralPath $SigningDirectory).Path
$taskKeystore=Join-Path $taskKeyFolder 'upload-keystore.p12'
$taskPasswordFile=Join-Path $taskKeyFolder 'keystore-password.txt'
if(!(Test-Path -LiteralPath $taskKeystore) -or !(Test-Path -LiteralPath $taskPasswordFile)){throw 'Choose the private upload-key backup folder.'}
$taskNames=@('KOSHER_UPLOAD_KEYSTORE','KOSHER_UPLOAD_STORE_PASSWORD','KOSHER_UPLOAD_KEY_PASSWORD','KOSHER_UPLOAD_KEY_ALIAS')
$taskOld=@{}
foreach($taskName in $taskNames){$taskOld[$taskName]=[Environment]::GetEnvironmentVariable($taskName,'Process')}
Push-Location $PSScriptRoot
try {
 $env:KOSHER_UPLOAD_KEYSTORE=$taskKeystore
 $env:KOSHER_UPLOAD_STORE_PASSWORD=[IO.File]::ReadAllText($taskPasswordFile).Trim()
 $env:KOSHER_UPLOAD_KEY_PASSWORD=$env:KOSHER_UPLOAD_STORE_PASSWORD
 $env:KOSHER_UPLOAD_KEY_ALIAS='kosher-upload'
 & ./gradlew.bat bundleRelease
 if($LASTEXITCODE -ne 0){throw 'Release build failed.'}
 Write-Output 'Release bundle: app/build/outputs/bundle/release/app-release.aab'
} finally {
 foreach($taskName in $taskNames){[Environment]::SetEnvironmentVariable($taskName,$taskOld[$taskName],'Process')}
 Pop-Location
}

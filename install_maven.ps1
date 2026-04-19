$ErrorActionPreference = 'Stop'
$mavenVersion = "3.9.6"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$zipPath = "$env:TEMP\apache-maven.zip"
$installPath = "C:\maven"

Write-Host "Downloading Apache Maven $mavenVersion..."
Invoke-WebRequest -Uri $mavenUrl -OutFile $zipPath

Write-Host "Extracting to $installPath..."
if (!(Test-Path -Path $installPath)) {
    New-Item -ItemType Directory -Force -Path $installPath | Out-Null
}
Expand-Archive -Path $zipPath -DestinationPath $installPath -Force

$mavenBinPath = "$installPath\apache-maven-$mavenVersion\bin"

Write-Host "Adding Maven to User PATH..."
$userPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
if ($userPath -notmatch [regex]::Escape($mavenBinPath)) {
    $newUserPath = $userPath + ";$mavenBinPath"
    [Environment]::SetEnvironmentVariable("Path", $newUserPath, [EnvironmentVariableTarget]::User)
}

Write-Host "Updating Current Session PATH..."
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

Write-Host "Maven installed successfully!"
mvn -version

param(
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$User = "root",
    [Parameter(Mandatory = $true)]
    [string]$Password,
    [string]$SourceDatabase = "sensorhub",
    [string]$TargetDatabase = "sensorhub"
)

$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$mysqldump = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"

if (-not (Test-Path $mysql)) {
    throw "mysql.exe not found: $mysql"
}

if (-not (Test-Path $mysqldump)) {
    throw "mysqldump.exe not found: $mysqldump"
}

$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$dumpFile = Join-Path $env:TEMP "sensorhub-db-migration-$timestamp.sql"

Write-Host "Creating target database $TargetDatabase ..."
& $mysql --host=$DbHost --port=$DbPort --user=$User --password=$Password --execute="CREATE DATABASE IF NOT EXISTS $TargetDatabase DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create database $TargetDatabase"
}

Write-Host "Dumping $SourceDatabase to $dumpFile ..."
& $mysqldump --host=$DbHost --port=$DbPort --user=$User --password=$Password --single-transaction --routines --events --triggers --default-character-set=utf8mb4 --result-file=$dumpFile $SourceDatabase
if ($LASTEXITCODE -ne 0) {
    throw "Failed to dump database $SourceDatabase"
}

Write-Host "Importing dump into $TargetDatabase ..."
cmd /c "`"$mysql`" --host=$DbHost --port=$DbPort --user=$User --password=$Password $TargetDatabase < `"$dumpFile`""
if ($LASTEXITCODE -ne 0) {
    throw "Failed to import dump into $TargetDatabase"
}

Write-Host "Database migration completed: $SourceDatabase -> $TargetDatabase"
Write-Host "Dump file: $dumpFile"

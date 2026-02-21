# Migrate local Construction CMS PostgreSQL database to Supabase
# Usage:
#   .\migrate_to_supabase.ps1 -SupabaseConnectionUri "postgresql://postgres:PASSWORD@db.xxx.supabase.co:5432/postgres"
# Optional:
#   -LocalConnectionUri "postgresql://postgres:admin123@localhost:5432/construction_db"
#   -DumpPath ".\construction_dump.dump"

param(
    [Parameter(Mandatory = $true)]
    [string] $SupabaseConnectionUri,

    [Parameter(Mandatory = $false)]
    [string] $LocalConnectionUri = "postgresql://postgres:admin123@localhost:5432/construction_db",

    [Parameter(Mandatory = $false)]
    [string] $DumpPath = ".\construction_dump.dump"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not $ProjectRoot) { $ProjectRoot = (Get-Location).Path }
Set-Location $ProjectRoot

# Ensure sslmode for Supabase
if ($SupabaseConnectionUri -notmatch "[?&]sslmode=") {
    $SupabaseConnectionUri = $SupabaseConnectionUri + $(if ($SupabaseConnectionUri -match "\?") { "&" } else { "?" }) + "sslmode=require"
}

if (-not [System.IO.Path]::IsPathRooted($DumpPath)) {
    $DumpPath = Join-Path $ProjectRoot (Split-Path -Leaf $DumpPath)
}
if ((Split-Path -Parent $DumpPath) -eq "") {
    $DumpPath = Join-Path $ProjectRoot "construction_dump.dump"
}

Write-Host "=== Construction DB → Supabase migration ===" -ForegroundColor Cyan
Write-Host "Dump file: $DumpPath" -ForegroundColor Gray
Write-Host ""

# --- Parse local connection for pg_dump ---
if ($LocalConnectionUri -match "postgres(?:ql)?://([^:]+):([^@]+)@([^:]+):(\d+)/(.+)") {
    $localUser = $Matches[1]
    $localPass = $Matches[2]
    $localHost = $Matches[3]
    $localPort = $Matches[4]
    $localDb   = $Matches[5]
} else {
    Write-Host "Could not parse LocalConnectionUri. Using defaults." -ForegroundColor Yellow
    $localUser = "postgres"
    $localPass = "admin123"
    $localHost = "localhost"
    $localPort = "5432"
    $localDb   = "construction_db"
}

# --- Step 1: Dump local database ---
Write-Host "[1/2] Dumping local database..." -ForegroundColor Cyan

$dumpDone = $false

# Try Docker Compose first
try {
    $containerId = (docker compose -f (Join-Path $ProjectRoot "docker-compose.yml") ps -q db 2>$null)
    if ($containerId) {
        Write-Host "Using Docker Compose db service" -ForegroundColor Gray
        $tempDumpInContainer = "/tmp/construction_dump.dump"
        docker compose exec -T db pg_dump -U $localUser -d $localDb -F c -f $tempDumpInContainer 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            docker compose cp "db:$tempDumpInContainer" $DumpPath 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0 -and (Test-Path $DumpPath) -and (Get-Item $DumpPath).Length -gt 0) {
                $dumpDone = $true
            }
        }
    }
} catch {
    # fall through to pg_dump
}

if (-not $dumpDone) {
    # Fallback: local pg_dump
    $pgDump = Get-Command pg_dump -ErrorAction SilentlyContinue
    if ($pgDump) {
        $env:PGPASSWORD = $localPass
        & pg_dump -h $localHost -p $localPort -U $localUser -d $localDb -F c -f $DumpPath
        if ($LASTEXITCODE -eq 0 -and (Test-Path $DumpPath)) { $dumpDone = $true }
    }
}

if (-not $dumpDone) {
    Write-Host "Dump failed. Options:" -ForegroundColor Red
    Write-Host "  - Start local DB: docker compose up -d db" -ForegroundColor Yellow
    Write-Host "  - Or run manually: docker compose exec -T db pg_dump -U postgres -d construction_db -F c -f - > $DumpPath" -ForegroundColor Yellow
    exit 1
}

$size = (Get-Item $DumpPath).Length / 1KB
Write-Host "Dump saved: $DumpPath ($([math]::Round($size, 1)) KB)" -ForegroundColor Green
Write-Host ""

# --- Step 2: Restore to Supabase ---
Write-Host "[2/2] Restoring to Supabase..." -ForegroundColor Cyan

$pgRestore = Get-Command pg_restore -ErrorAction SilentlyContinue
if (-not $pgRestore) {
    Write-Host "pg_restore not found. Restore manually:" -ForegroundColor Yellow
    Write-Host "  pg_restore -d `"$SupabaseConnectionUri`" -v --no-owner --no-acl --clean --if-exists `"$DumpPath`"" -ForegroundColor White
    exit 0
}

# Parse Supabase URI for pg_restore (it doesn't accept URI for -d on all versions, so we use -h -p -U -d)
if ($SupabaseConnectionUri -match "postgres(?:ql)?://([^:]+):([^@]+)@([^/]+)/([^?]+)") {
    $supaUser = $Matches[1]
    $supaPass = $Matches[2]
    $supaHost = $Matches[3]
    $supaDb   = $Matches[4]
    if ($supaHost -match ":(\d+)$") {
        $supaPort = $Matches[1]
        $supaHost = $supaHost -replace ":\d+$", ""
    } else {
        $supaPort = "5432"
    }
} else {
    Write-Host "Could not parse SupabaseConnectionUri." -ForegroundColor Red
    exit 1
}

$env:PGPASSWORD = $supaPass
& pg_restore -h $supaHost -p $supaPort -U $supaUser -d $supaDb -v --no-owner --no-acl --clean --if-exists $DumpPath 2>&1
# pg_restore often exits 1 due to harmless errors (e.g. role "postgres" or extensions)
if ($LASTEXITCODE -ne 0) {
    Write-Host "pg_restore finished with exit code $LASTEXITCODE (some warnings are normal). Check that tables exist in Supabase." -ForegroundColor Yellow
} else {
    Write-Host "Restore completed." -ForegroundColor Green
}

Write-Host ""
Write-Host "Next: Set DATABASE_URL on Render to your Supabase connection string (with ?sslmode=require)." -ForegroundColor Cyan

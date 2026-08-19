param(
    [Parameter(Mandatory = $false)]
    [string]$MigrationName = "new-migration"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

supabase migration new $MigrationName
supabase db push

param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectRef
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

supabase link --project-ref $ProjectRef

# Requires PowerShell 7 and the default Mailpit configuration in .env.
param([string]$Api = 'http://localhost:8080', [string]$Mailbox = 'http://localhost:8025')
$ErrorActionPreference = 'Stop'

function Submit-Contact([string]$Name) {
    $body = @{ name = $Name; email = 'visitor@example.test'; message = 'Local contact flow verification' } | ConvertTo-Json
    $result = Invoke-WebRequest -Uri "$Api/api/contact" -Method Post -ContentType 'application/json' -Body $body -SkipHttpErrorCheck
    if ($result.StatusCode -ne 202) { throw "Expected 202; received $($result.StatusCode). Wait for the API or rate-limit window and retry." }
    return ($result.Content | ConvertFrom-Json).submissionId
}
function Has-Message([string]$Id) {
    $messages = Invoke-RestMethod -Uri "$Mailbox/api/v1/messages?limit=100"
    return @($messages.messages | Where-Object { $_.Subject -like "*$Id*" }).Count -gt 0
}
function Wait-Message([string]$Id) {
    $deadline = [DateTime]::UtcNow.AddSeconds(45)
    do {
        if (Has-Message $Id) { return }
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "No Mailpit email for submission $Id within 45 seconds. Check worker logs and the DLT."
}

$id = Submit-Contact 'Valid local submission'
Wait-Message $id
Write-Output "PASS: valid submission accepted and received in Mailpit ($id)."
$invalid = Invoke-WebRequest -Uri "$Api/api/contact" -Method Post -ContentType 'application/json' -Body '{"name":"","email":"invalid","message":""}' -SkipHttpErrorCheck
if ($invalid.StatusCode -ne 400) { throw "Expected invalid submission to return 400; received $($invalid.StatusCode)." }
Write-Output 'PASS: invalid submission rejected with 400.'

docker compose stop worker
if ($LASTEXITCODE -ne 0) { throw 'Could not stop worker.' }
try {
    $queued = Submit-Contact 'Queued while worker is stopped'
    Start-Sleep -Seconds 2
    if (Has-Message $queued) { throw 'Email arrived while the worker was stopped.' }
    Write-Output "PASS: queued submission accepted while worker was stopped ($queued)."
} finally {
    docker compose start worker
    if ($LASTEXITCODE -ne 0) { throw 'Could not restart worker.' }
}
Wait-Message $queued
Write-Output 'PASS: restarted worker delivered queued submission to Mailpit.'
Write-Output 'Real Gmail delivery was NOT tested by this script.'

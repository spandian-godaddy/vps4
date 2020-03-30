<#
This can be run from any directory on a Windows server with Panopta installed. It is safe to run over and over again.
It will print out the new server_key generated that you must update in the vps4 db with the new value.
#>

function Uninstall-Panopta {
    $app = Get-WmiObject -Class Win32_Product | Where-Object { 
        $_.Name -match "Panopta Agent" 
     }
     
     Write-Output "Uninstalling Panopta Agent..."
     $app.Uninstall();
}

function Edit-ManifestFile {
    Write-Output "Modifing $manifestFile..."
    if ([System.IO.File]::Exists($manifestFile)) {
        if (Select-String -Pattern "server_key" -Quiet $manifestFile) {
            Write-Output "server_key exists!"
            # Delete server_key
            (Get-Content $manifestFile) | Where-Object {$_ -notmatch 'server_key'} | Out-File $manifestFile
        }

        if (Select-String -Pattern "disable_server_match" -Quiet $manifestFile) {
            Write-Output "disable_server_match already exists"
        }
        else {
            Write-Output "Adding disable_server_match"
            "disable_server_match = true" >> $manifestFile
        }
    }
    else {
        Write-Output "$manifestFile does not exist"
    }
}

function Install-Panopta {
    if ([System.IO.File]::Exists($manifestFile)) {
        $ManifestProps = ConvertFrom-StringData (Get-Content $manifestFile -raw)
        $customerKey = $ManifestProps["customer_key"]
        Write-Output "Customer key: $customerKey"

        $installPanoptaScript = "C:\panopta_temp\panopta_agent_windows.ps1"
        $args = "-customer_key $customerKey"
        Write-Output "Installing Panopta $installPanoptaScript..."
        Start-Process powershell.exe -Wait -ArgumentList "-file $installPanoptaScript", $args
    }
    else {
        Write-Output "$manifestFile does not exist"
    }
}

function Read-ServerKey {
    param ([string]$serverKeyLabel = "")

    $agentConfig = "C:\Program Files (x86)\PanoptaAgent\Agent.config"
    if ([System.IO.File]::Exists($agentConfig)) {
        $XPath = "//add[@key='ServerKey']"
        $serverKey = Select-Xml -Path $agentConfig -XPath $XPath | Select-Object -ExpandProperty Node
        Write-Output "$serverKeyLabel server_key: $($serverKey.value)"
    }
    else {
        Write-Output "$agentConfig does not exist"
    }
}

$manifestFile = "C:\PanoptaAgent.manifest"
Write-Output "Starting..."

Read-ServerKey "Old"
Uninstall-Panopta
Edit-ManifestFile
Install-Panopta
Read-ServerKey "New"

Write-Output "Script Finished"
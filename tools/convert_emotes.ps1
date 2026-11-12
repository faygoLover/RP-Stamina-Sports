# Эмоции Emotecraft (emotes_src\<id>.emotecraft) → ресурсы аддона (assets\rpsports\sport_emotes\<id>.json).
# Нужен jar Emotecraft (только для перевода; аддону он не нужен). Путь — параметр или клиент DEPARTMENT_s5_client.
#   powershell -File tools\convert_emotes.ps1 [-Emotecraft путь\emotecraft.jar]
param(
    [string]$Emotecraft = "D:\ElyPrismLauncher\instances\DEPARTMENT_s5_client\minecraft\mods\emotecraft-for-MC1.20.1-2.2.7-b.build.50-forge.jar"
)
$root = Split-Path $PSScriptRoot -Parent
$pal = Get-ChildItem "$root\libs\player-animation-lib-forge-*.jar" | Select-Object -First 1
$gson = Get-ChildItem "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\com.google.code.gson\gson" -Recurse -Filter "gson-2.10.jar" | Select-Object -First 1
$out = "$root\src\main\resources\assets\rpsports\sport_emotes"
New-Item -ItemType Directory -Force $out | Out-Null
foreach ($f in Get-ChildItem "$root\emotes_src\*.emotecraft") {
    java -cp "$Emotecraft;$($pal.FullName);$($gson.FullName)" "$root\tools\EmoteConvert.java" $f.FullName "$out\$($f.BaseName).json"
}

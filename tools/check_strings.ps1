$files = Get-ChildItem -Path "E:\Datos\Descargas\Server Files\MSN Messenger Mobile\app\src\main\res" -Recurse -Filter "strings.xml" | ForEach-Object { $_.FullName }
foreach ($f in $files) {
    $c = Get-Content -Path $f -Raw
    $names = [regex]::Matches($c, '<string name="([^"]+)"')
    $groups = $names | Group-Object { $_.Groups[1].Value }
    $dupes = $groups | Where-Object { $_.Count -gt 1 }
    $main = [bool]([regex]::IsMatch($c, 'name="main_title"'))
    $sessionsClose = [bool]([regex]::IsMatch($c, 'name="sessions_close"'))
    $picker = [bool]([regex]::IsMatch($c, 'name="profile_error_photo_picker"'))
    $pickerCount = ([regex]::Matches($c, 'name="profile_error_photo_picker"')).Count
    "{0}  dupes={1}  main={2}  sessionsClose={3}  picker={4} pickerCount={5}" -f $f, ($dupes.Count), $main, $sessionsClose, $picker, $pickerCount
}

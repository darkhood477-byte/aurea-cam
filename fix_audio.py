import re
content = open("app/src/main/java/com/example/camera/SettingsSheet.kt", "r").read()
content = content.replace('listOf("Internal", "External", "Off")', 'listOf("Internal", "Bluetooth Mic", "Wired Mic", "Off")')
open("app/src/main/java/com/example/camera/SettingsSheet.kt", "w").write(content)

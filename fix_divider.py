import re
content = open("app/src/main/java/com/example/camera/SettingsSheet.kt", "r").read()
content = content.replace("Divider(", "HorizontalDivider(")
open("app/src/main/java/com/example/camera/SettingsSheet.kt", "w").write(content)

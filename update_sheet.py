content = open("app/src/main/java/com/example/camera/SettingsSheet.kt", "r").read()
content = content.replace('listOf("4K", "1080p", "720p")', 'listOf("1080p", "4K", "720p", "480p")')
open("app/src/main/java/com/example/camera/SettingsSheet.kt", "w").write(content)

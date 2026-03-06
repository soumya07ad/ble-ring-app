import os
import re
from pathlib import Path

def process_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content
    
    # Exact word replacements
    replacements = {
        "DarkCard": "MaterialTheme.colorScheme.surface",
        "DarkBackground": "MaterialTheme.colorScheme.background",
        "DarkSurfaceVariant": "MaterialTheme.colorScheme.surfaceVariant",
        "DarkSurface": "MaterialTheme.colorScheme.surface",
        "TextPrimary": "MaterialTheme.colorScheme.onSurface",
        "TextSecondary": "MaterialTheme.colorScheme.onSurfaceVariant",
        "TextMuted": "MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)",
        "AppColors.textPrimary": "MaterialTheme.colorScheme.onSurface",
        "AppColors.textSecondary": "MaterialTheme.colorScheme.onSurfaceVariant",
        "AppColors.textMuted": "MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)",
        "AppColors.cardBackground": "MaterialTheme.colorScheme.surface",
        "AppColors.background": "MaterialTheme.colorScheme.background",
        "AppColors.glassBorder": "MaterialTheme.colorScheme.outline",
        "AppColors.navBarBackground": "MaterialTheme.colorScheme.surface",
        "AppColors.navBarBorder": "MaterialTheme.colorScheme.outlineVariant",
        "Color.Black": "MaterialTheme.colorScheme.background",
        "Color.White": "MaterialTheme.colorScheme.onSurface"
    }
    
    for old, new in replacements.items():
        escaped_old = old.replace(".", "\.")
        content = re.sub(rf"\b{escaped_old}\b", new, content)

    # Specific Color(0xFF...) hardcodes replacement logic
    # The user wants to remove Color(0xFF...): background etc
    # We will do a few file-specific replaces since regexing all Color(0xFF..) might break intended gradients

    # DashboardScreen.kt specific:
    content = content.replace("Color(0xFF030303)", "MaterialTheme.colorScheme.background")
    content = content.replace("Color(0xFF080810)", "MaterialTheme.colorScheme.surface")
    content = content.replace("Color(0xFF1A1A2E)", "MaterialTheme.colorScheme.outline")
    content = content.replace("Color(0xFF1C1C1E)", "MaterialTheme.colorScheme.surface")

    if content != original:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {file_path.name}")

root_dir = Path(r"c:\Users\adhik\Downloads\DKGS LABS\Ring\FitnessAndroidApp\src\main\kotlin\com\fitness\app")

for p in root_dir.rglob("*.kt"):
    # Skip Theme.kt as it's the definition file
    if p.name == "Theme.kt" or p.name == "Color.kt":
        continue
    process_file(p)

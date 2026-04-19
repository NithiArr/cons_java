import os
import re

template_dir = r"E:\Personal\Con_java\src\main\resources\templates"

count = 0
for root, dirs, files in os.walk(template_dir):
    for file in files:
        if not file.endswith(".html") or file in ["login.html", "base.html"]:
            continue
        path = os.path.join(root, file)
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()

        original = content

        # Replace ALL remaining @{/...} patterns regardless of surrounding context
        # This covers: "@{/path}", '@{/path}', `@{/path}`, and unquoted @{/path}
        content = re.sub(r'@\{(/[^}]*)\}', r'\1', content)

        if content != original:
            with open(path, "w", encoding="utf-8") as f:
                f.write(content)
            count += 1
            print(f"Fixed: {os.path.relpath(path, template_dir)}")

print(f"\nDone. Fixed {count} files.")

import os
import re

template_dir = r"E:\Personal\Con_java\src\main\resources\templates"

for root, dirs, files in os.walk(template_dir):
    for file in files:
        if file.endswith(".html") and file not in ["login.html", "base.html"]:
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()

            # Skip if already decorated
            if "layout:decorate" in content:
                continue

            # We need to wrap the whole file content into the Layout block
            new_content = """<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org" xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout" layout:decorate="~{base}">
<head>
    <title>Elite Page</title>
</head>
<body>
    <div layout:fragment="content">
""" + content + """
    </div>
</body>
</html>"""

            with open(path, "w", encoding="utf-8") as f:
                f.write(new_content)
                
print("Decorated all inner templates with layout structure.")

import os
import re

template_dir = r"E:\Personal\Con_java\src\main\resources\templates"

for root, dirs, files in os.walk(template_dir):
    for file in files:
        if file.endswith(".html") and file != "login.html":
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()

            # 1. Replace {% load static %}
            content = re.sub(r'{%\s*load static\s*%}', '', content)

            # 2. Replace {% static 'path/to/file' %} -> @{/path/to/file}
            content = re.sub(r'{%\s*static\s*[\'"](.*?)[\'"]\s*%}', r'@{/\1}', content)

            # 3. Handle href="{% static ... %}" being replaced with th:href="@{...}"
            content = re.sub(r'href="\@{/(.*?)}"', r'th:href="@{/\1}"', content)
            content = re.sub(r'src="\@{/(.*?)}"', r'th:src="@{/\1}"', content)

            # 4. Replace {% url 'name' %} -> @{/name}
            content = re.sub(r'{%\s*url\s*[\'"](.*?)[\'"]\s*%}', r'@{/\1}', content)
            content = re.sub(r'href="\@{/(.*?)}"', r'th:href="@{/\1}"', content)

            # 5. Remove blocks
            content = re.sub(r'{%\s*extends.*?%}', '', content)
            content = re.sub(r'{%\s*block\s+.*?\s*%}', '', content)
            content = re.sub(r'{%\s*endblock\s*%}', '', content)

            # 6. Basic variable binding {{ var }} -> ${var}
            content = re.sub(r'\{\{\s*(.*?)\s*\}\}', r'${\1}', content)

            # 7. CSRF token
            content = re.sub(r'{%\s*csrf_token\s*%}', '', content)
            
            # Remove any generic django tags left over just to prevent UI clutter
            # Careful not to accidentally drop inline js brackets. Django uses {% %}
            content = re.sub(r'{%.*?%}', '', content)

            with open(path, "w", encoding="utf-8") as f:
                f.write(content)
                
print("Done converting templates to Thymeleaf base syntax.")

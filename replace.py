import os

def replace_in_file(filepath, old_str, new_str):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    if old_str in content:
        content = content.replace(old_str, new_str)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")

directory = 'liveness-sdk/src/main'
for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('.kt') or file.endswith('.xml') or file.endswith('.pro') or file.endswith('.java'):
            replace_in_file(os.path.join(root, file), 'io.senovative.liveness.core', 'io.senovative.liveness.sdk')

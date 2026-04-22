import os
import re

RES_DIR = r"c:\Users\LENOVO\AndroidStudioProjects\p2pbooking\app\src\main\res"
LAYOUT_DIR = os.path.join(RES_DIR, "layout")
DRAWABLE_DIR = os.path.join(RES_DIR, "drawable")

# Regex to find @color/c_XXXXXX
color_ref_regex = re.compile(r'@color/c_([0-9a-fA-F]+)', flags=re.IGNORECASE)

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace @color/c_FFFFFF with #FFFFFF
    new_content = color_ref_regex.sub(lambda match: f"#{match.group(1).upper()}", content)

    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)

for directory in [LAYOUT_DIR, DRAWABLE_DIR]:
    if not os.path.exists(directory): continue
    for filename in os.listdir(directory):
        if filename.endswith(".xml"):
            process_file(os.path.join(directory, filename))

# Remove all <color name="c_...">...</color> lines from colors.xml
def revert_colors_xml(filepath):
    if not os.path.exists(filepath): return
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = [line for line in lines if 'name="c_' not in line]
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)

VALUES_DIR = os.path.join(RES_DIR, "values")
VALUES_NIGHT_DIR = os.path.join(RES_DIR, "values-night")

revert_colors_xml(os.path.join(VALUES_DIR, "colors.xml"))
revert_colors_xml(os.path.join(VALUES_NIGHT_DIR, "colors.xml"))

print("Rollback complete.")

import os
import re
import xml.etree.ElementTree as ET
import colorsys

def hex_to_rgb(hex_str):
    hex_str = hex_str.lstrip('#')
    if len(hex_str) == 8:
        a = int(hex_str[0:2], 16)
        r = int(hex_str[2:4], 16)
        g = int(hex_str[4:6], 16)
        b = int(hex_str[6:8], 16)
        return a, r, g, b
    elif len(hex_str) == 6:
        r = int(hex_str[0:2], 16)
        g = int(hex_str[2:4], 16)
        b = int(hex_str[4:6], 16)
        return 255, r, g, b
    elif len(hex_str) == 4:
        a = int(hex_str[0]*2, 16)
        r = int(hex_str[1]*2, 16)
        g = int(hex_str[2]*2, 16)
        b = int(hex_str[3]*2, 16)
        return a, r, g, b
    elif len(hex_str) == 3:
        r = int(hex_str[0]*2, 16)
        g = int(hex_str[1]*2, 16)
        b = int(hex_str[2]*2, 16)
        return 255, r, g, b
    return 255, 0, 0, 0

def rgb_to_hex(a, r, g, b):
    if a == 255:
        return f"#{r:02x}{g:02x}{b:02x}".upper()
    return f"#{a:02x}{r:02x}{g:02x}{b:02x}".upper()

def invert_color(hex_str):
    if hex_str.upper() == "#FFFFFF" or hex_str.upper() == "#FFFFFFFF": return "#121212"
    if hex_str.upper() == "#000000" or hex_str.upper() == "#FF000000": return "#FFFFFF"
    if hex_str.upper() == "#121212" or hex_str.upper() == "#FF121212": return "#FFFFFF"
    
    a, r, g, b = hex_to_rgb(hex_str)
    # Convert RGB to HLS
    h, l, s = colorsys.rgb_to_hls(r / 255.0, g / 255.0, b / 255.0)
    
    # Invert lightness (1.0 - L) but preserve some saturation and hue
    new_l = 1.0 - l
    
    # Adjust heavily desaturated colors so they don't just become gray mud
    # If it's very bright (L > 0.8), dark mode should make it very dark (new_l < 0.2).
    # If it's a primary color (S > 0.5), we might not want to invert lightness fully, 
    # but for simplicity, we stick to standard inversion.
    
    new_r, new_g, new_b = colorsys.hls_to_rgb(h, new_l, s)
    return rgb_to_hex(a, int(new_r * 255), int(new_g * 255), int(new_b * 255))

RES_DIR = r"c:\Users\LENOVO\AndroidStudioProjects\p2pbooking\app\src\main\res"
LAYOUT_DIR = os.path.join(RES_DIR, "layout")
DRAWABLE_DIR = os.path.join(RES_DIR, "drawable")

all_colors = set()

# Regex for finding hex colors: #FFFFFF, #FFF, #FFFFFFFF
hex_regex = re.compile(r'#([0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{4}|[0-9a-fA-F]{3})\b')

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content = content
    matches = hex_regex.findall(content)
    
    for match in sorted(set(matches), key=len, reverse=True): 
        # Reverse length to process 8 chars before 6 chars if overlapping (regex handles it mostly, just safe)
        full_hex = f"#{match}".upper()
        # standardize color code format for naming
        clean_name = f"c_{match.lower()}"
        
        all_colors.add((full_hex, clean_name))
        
        # Replace only actual hex values, avoiding things that might overlap
        # Using a precise replace using regex
        new_content = re.sub(f"#{match}(?!\w)", f"@color/{clean_name}", new_content, flags=re.IGNORECASE)

    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)

for directory in [LAYOUT_DIR, DRAWABLE_DIR]:
    if not os.path.exists(directory): continue
    for filename in os.listdir(directory):
        if filename.endswith(".xml"):
            process_file(os.path.join(directory, filename))

# Create/Update colors.xml
def update_colors_xml(filepath, color_tuples, is_night=False):
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    tags = []
    
    # Read existing
    existing = set()
    if os.path.exists(filepath):
        try:
            tree = ET.parse(filepath)
            root = tree.getroot()
            for child in root:
                if child.tag == 'color':
                    existing.add(child.attrib['name'])
        except Exception:
            pass

    for full_hex, name in color_tuples:
        if name not in existing:
            mapped_hex = invert_color(full_hex) if is_night else full_hex
            tags.append(f'    <color name="{name}">{mapped_hex}</color>')

    if not tags: return

    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        if "</resources>" in content:
            new_content = content.replace("</resources>", "\n".join(tags) + "\n</resources>")
        else:
            new_content = "<resources>\n" + "\n".join(tags) + "\n</resources>"
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
    else:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n' + "\n".join(tags) + '\n</resources>')

VALUES_DIR = os.path.join(RES_DIR, "values")
VALUES_NIGHT_DIR = os.path.join(RES_DIR, "values-night")

update_colors_xml(os.path.join(VALUES_DIR, "colors.xml"), all_colors, is_night=False)
update_colors_xml(os.path.join(VALUES_NIGHT_DIR, "colors.xml"), all_colors, is_night=True)

print(f"Processed {len(all_colors)} unique hex colors.")

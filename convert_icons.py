import os
import glob
import xml.etree.ElementTree as ET
import re

svg_dir = r"C:\Users\atharva\AndroidStudioProjects\TrackFit\.vscode\phosphor_icons"
out_dir = r"C:\Users\atharva\AndroidStudioProjects\TrackFit\app\src\main\res\drawable"

NS = {'svg': 'http://www.w3.org/2000/svg'}
ET.register_namespace('', "http://www.w3.org/2000/svg")

svg_files = glob.glob(os.path.join(svg_dir, "*.svg"))

for svg_file in svg_files:
    base_name = os.path.basename(svg_file).replace(".svg", ".xml")
    out_file = os.path.join(out_dir, base_name)
    
    try:
        tree = ET.parse(svg_file)
        root = tree.getroot()
        
        viewBox = root.attrib.get('viewBox', '0 0 256 256').split()
        if len(viewBox) == 4:
            vw = viewBox[2]
            vh = viewBox[3]
        else:
            vw = "256"
            vh = "256"
            
        paths = []
        for elem in root.iter():
            # Handle elements with or without namespace string
            if elem.tag.endswith('path'):
                paths.append(elem.attrib.get('d'))
                
        # Generate Vector XML
        vector_xml = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="{vw}"
    android:viewportHeight="{vh}">
"""
        for d in paths:
            if d:
                # Add a solid #FFFFFF fill to all paths
                vector_xml += f'  <path\n      android:fillColor="#FFFFFFFF"\n      android:pathData="{d}" />\n'
        vector_xml += "</vector>"
        
        with open(out_file, "w", encoding="utf-8") as f:
            f.write(vector_xml)
            
        print(f"Converted {os.path.basename(svg_file)} -> {base_name}")
            
    except Exception as e:
        print(f"Error converting {svg_file}: {e}")

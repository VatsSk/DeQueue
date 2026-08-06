import os
import re

src_dir = r"D:\DeQueue\src\main\java\com\dequeue"
output_lines = []

output_lines.append("# DeQueue API Report\n")
output_lines.append("This report lists all API endpoints discovered in the application, along with their HTTP methods and required roles.\n")

for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith("Controller.java"):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
                
            class_request_mapping = ""
            match = re.search(r'@RequestMapping\("([^"]+)"\)', content)
            if match:
                class_request_mapping = match.group(1)
                
            class_pre_auth = None
            # Extract class level pre authorize (before public class)
            class_part = content.split('public class')[0]
            auth_match = re.search(r'@PreAuthorize\("([^"]+)"\)', class_part)
            if auth_match:
                class_pre_auth = auth_match.group(1)
            
            output_lines.append(f"## {file}\n")
            
            # Split by methods roughly
            method_blocks = re.split(r'public\s+(?:<[^>]+>\s+)?[\w<>, \?\[\]]+\s+\w+\s*\(', content)
            # The first block is the class header, the rest are method bodies (but we lost the method signature!)
            
            # Better approach: find all methods with their preceding annotations
            # Let's find all `@XMapping` using regex
            for match in re.finditer(r'((?:@[A-Za-z]+(?:\([^)]+\))?\s*)*)public\s+(?:<[^>]+>\s+)?[\w<>, \?\[\]]+\s+(\w+)\s*\(', content):
                annotations = match.group(1)
                method_name = match.group(2)
                
                mapping_match = re.search(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\("([^"]*)"\))?', annotations)
                if mapping_match:
                    method_type = mapping_match.group(1).upper()
                    sub_path = mapping_match.group(2) if mapping_match.group(2) else ""
                    full_path = class_request_mapping + sub_path
                    
                    pre_auth_match = re.search(r'@PreAuthorize\("([^"]+)"\)', annotations)
                    if pre_auth_match:
                        roles = pre_auth_match.group(1)
                    elif class_pre_auth:
                        roles = class_pre_auth + " (Inherited from class)"
                    else:
                        roles = "None (Public or Authenticated Default)"
                        
                    output_lines.append(f"### `{method_type} {full_path}`")
                    output_lines.append(f"- **Method Name:** `{method_name}`")
                    output_lines.append(f"- **Roles Required:** `{roles}`\n")

with open(r"D:\DeQueue\api_report_raw.md", "w", encoding='utf-8') as f:
    f.write("\n".join(output_lines))
print("Done")

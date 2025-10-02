import re
from pathlib import Path
from typing import Dict, Set, List, Optional

# Import shared data classes from migration_port
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from migration_port import Content, MigrationConfig


class AngularAuthenticationMigration:
    """
    Migrates ONLY authenticationService inject() call from class property to constructor parameter.
    
    This migration focuses on moving ONLY the authenticationService inject() call
    from class properties to constructor parameters with default values in classes with super() calls.
    """
    
    @staticmethod
    def process(cfg: MigrationConfig, content: str) -> Content:
        """
        Process TypeScript component file to migrate inject() calls to constructor.
        
        Args:
            cfg: Migration configuration containing file path and related files
            content: The TypeScript file content to process
            
        Returns:
            Content object with potentially modified content and changed flag
        """
        new_content = AngularAuthenticationMigration.migrate_inject_to_constructor(content)
        return Content(new_content, new_content != content)
    
    @staticmethod
    def migrate_inject_to_constructor(content: str) -> str:
        """
        Migrate ONLY authenticationService inject() call from class property to constructor parameter.
        
        Only migrates in classes that have a super() call in their constructor.
        
        Args:
            content: The TypeScript file content
            
        Returns:
            Modified content with inject calls moved to constructor
        """
        if not content.strip():
            return content
        
        # Find all classes
        classes = AngularAuthenticationMigration._find_classes(content)
        
        result = content
        
        # Process each class separately
        for class_info in classes:
            # Find inject properties in this class
            inject_properties = AngularAuthenticationMigration._find_inject_properties_in_class(
                result, class_info
            )
            
            if not inject_properties:
                continue
            
            # Find constructor in this class
            constructor_info = AngularAuthenticationMigration._find_constructor_in_class(
                result, class_info
            )
            
            if not constructor_info:
                continue
            
            # Check if constructor has super() call
            if not AngularAuthenticationMigration._has_super_call(result, constructor_info):
                continue
            
            # Remove properties first
            for prop in inject_properties:
                result = AngularAuthenticationMigration._remove_property_declaration(result, prop)
            
            # Re-find class and constructor after removal
            classes = AngularAuthenticationMigration._find_classes(result)
            # Find the same class (by position or name)
            class_info = next((c for c in classes if c['name'] == class_info['name']), None)
            if not class_info:
                continue
                
            constructor_info = AngularAuthenticationMigration._find_constructor_in_class(
                result, class_info
            )
            
            if not constructor_info:
                continue
            
            # Add parameters to constructor
            result = AngularAuthenticationMigration._add_constructor_parameters(
                result, constructor_info, inject_properties
            )
            
            # Update super() call
            result = AngularAuthenticationMigration._update_super_call(result, inject_properties)
        
        return result
    
    @staticmethod
    def _find_classes(content: str) -> List[dict]:
        """
        Find all class declarations in the content.
        
        Returns:
            List of dicts with class info: {
                'name': 'ClassName',
                'start': start position,
                'end': end position (approximate)
            }
        """
        classes = []
        pattern = re.compile(r'export\s+class\s+(\w+)', re.MULTILINE)
        
        matches = list(pattern.finditer(content))
        
        for i, match in enumerate(matches):
            class_name = match.group(1)
            class_start = match.start()
            
            # Find the end of this class (start of next class or end of file)
            if i < len(matches) - 1:
                class_end = matches[i + 1].start()
            else:
                class_end = len(content)
            
            classes.append({
                'name': class_name,
                'start': class_start,
                'end': class_end
            })
        
        return classes
    
    @staticmethod
    def _find_inject_properties_in_class(content: str, class_info: dict) -> list:
        """
        Find authentication service inject properties within a specific class.
        
        Args:
            content: The file content
            class_info: Class information dict
            
        Returns:
            List of property dicts
        """
        class_content = content[class_info['start']:class_info['end']]
        properties = []
        
        # Pattern: (private|public)? (readonly)? propertyName = inject(TypeName);
        pattern = re.compile(
            r'^\s*(private|public|protected)?\s*(readonly)?\s*(\w+)\s*=\s*inject\((\w+)\);',
            re.MULTILINE
        )
        
        for match in pattern.finditer(class_content):
            prop_name = match.group(3)
            # Only include properties with authentication service in the name
            if 'authenticationService' in prop_name or 'authService' in prop_name:
                prop_info = {
                    'visibility': match.group(1),
                    'readonly': match.group(2) == 'readonly',
                    'name': prop_name,
                    'type': match.group(4),
                    'full_match': match.group(0)
                }
                properties.append(prop_info)
        
        return properties
    
    @staticmethod
    def _find_constructor_in_class(content: str, class_info: dict) -> Optional[dict]:
        """
        Find the constructor within a specific class.
        
        Args:
            content: The file content
            class_info: Class information dict
            
        Returns:
            Constructor info dict or None
        """
        class_start = class_info['start']
        class_end = class_info['end']
        class_content = content[class_start:class_end]
        
        # Find constructor in class content
        constructor_pattern = re.compile(r'\bconstructor\s*\(', re.MULTILINE)
        match = constructor_pattern.search(class_content)
        
        if not match:
            return None
        
        # Adjust positions to be relative to full content
        constructor_start_in_class = match.start()
        params_start_in_class = match.end()
        
        # Find closing paren
        paren_count = 1
        i = params_start_in_class
        params_end_in_class = -1
        
        while i < len(class_content) and paren_count > 0:
            if class_content[i] == '(':
                paren_count += 1
            elif class_content[i] == ')':
                paren_count -= 1
                if paren_count == 0:
                    params_end_in_class = i
                    break
            i += 1
        
        if params_end_in_class == -1:
            return None
        
        # Find opening brace
        body_start_in_class = class_content.find('{', params_end_in_class)
        if body_start_in_class == -1:
            return None
        
        # Extract existing parameters
        params_text = class_content[params_start_in_class:params_end_in_class].strip()
        existing_params = []
        
        if params_text:
            existing_params = AngularAuthenticationMigration._split_parameters(params_text)
        
        return {
            'start': class_start + constructor_start_in_class,
            'params_start': class_start + params_start_in_class,
            'params_end': class_start + params_end_in_class,
            'body_start': class_start + body_start_in_class,
            'existing_params': existing_params
        }
    
    @staticmethod
    def _has_super_call(content: str, constructor_info: dict) -> bool:
        """
        Check if a constructor has a super() call.
        
        Args:
            content: The file content
            constructor_info: Constructor information dict
            
        Returns:
            True if constructor has a super() call, False otherwise
        """
        body_start = constructor_info['body_start']
        # Look for super( in the next ~300 characters
        body_preview = content[body_start:body_start + 300]
        return 'super(' in body_preview
    
    @staticmethod
    def _split_parameters(params_text: str) -> list:
        """
        Split parameter list by commas, handling nested generics.
        
        Args:
            params_text: The text between constructor parentheses
            
        Returns:
            List of parameter strings
        """
        params = []
        current_param = ''
        angle_bracket_depth = 0
        paren_depth = 0
        
        for char in params_text:
            if char == '<':
                angle_bracket_depth += 1
                current_param += char
            elif char == '>':
                angle_bracket_depth -= 1
                current_param += char
            elif char == '(':
                paren_depth += 1
                current_param += char
            elif char == ')':
                paren_depth -= 1
                current_param += char
            elif char == ',' and angle_bracket_depth == 0 and paren_depth == 0:
                params.append(current_param.strip())
                current_param = ''
            else:
                current_param += char
        
        if current_param.strip():
            params.append(current_param.strip())
        
        return params
    
    @staticmethod
    def _remove_property_declaration(content: str, prop_info: dict) -> str:
        """
        Remove a property declaration from the content.
        
        Args:
            content: The file content
            prop_info: Property information dict
            
        Returns:
            Content with the property declaration removed
        """
        # Remove the entire line containing the property declaration, including newline
        lines = content.split('\n')
        result_lines = []
        
        for line in lines:
            if prop_info['full_match'].strip() not in line:
                result_lines.append(line)
        
        return '\n'.join(result_lines)
    
    @staticmethod
    def _add_constructor_parameters(content: str, constructor_info: dict, inject_properties: list) -> str:
        """
        Add inject properties as constructor parameters with default values.
        
        Args:
            content: The file content
            constructor_info: Constructor information dict
            inject_properties: List of inject property information
            
        Returns:
            Content with constructor parameters added
        """
        params_start = constructor_info['params_start']
        params_end = constructor_info['params_end']
        existing_params = constructor_info['existing_params']
        
        # Build new parameters from inject properties
        new_params = []
        for prop in inject_properties:
            visibility = prop['visibility'] or 'private'
            readonly = 'readonly ' if prop['readonly'] else ''
            param = f"{visibility} {readonly}{prop['name']}: {prop['type']} = inject({prop['type']})"
            new_params.append(param)
        
        # Combine with existing parameters
        all_params = new_params + existing_params
        
        # Format parameters
        if not all_params:
            params_text = ''
        elif len(all_params) == 1:
            # Single parameter - inline format
            params_text = all_params[0]
        else:
            # Multiple parameters - multi-line format
            params_text = '\n    ' + ',\n    '.join(all_params) + '\n'
        
        # Build the new constructor signature
        # We need to replace from start of constructor to the opening brace
        constructor_start = constructor_info['start']
        body_start = constructor_info['body_start']
        
        # Build new constructor signature
        new_constructor = f"constructor({params_text})"
        
        # Replace from constructor keyword to opening brace (exclusive)
        result = content[:constructor_start] + new_constructor + ' ' + content[body_start:]
        
        return result
    
    @staticmethod
    def _update_super_call(content: str, inject_properties: list) -> str:
        """
        Update super() calls to use parameter names instead of this.property.
        
        Args:
            content: The file content
            inject_properties: List of inject property information
            
        Returns:
            Content with super() calls updated
        """
        result = content
        
        # Find properties that are passed to super()
        for prop in inject_properties:
            # Pattern: super(this.propertyName) -> super(propertyName)
            pattern = rf'\bsuper\s*\(\s*this\.{prop["name"]}\s*\)'
            replacement = f'super({prop["name"]})'
            result = re.sub(pattern, replacement, result)
        
        return result
    
    # Backward compatibility aliases for tests
    @staticmethod
    def _find_inject_properties(content: str) -> list:
        """Backward compatibility method for tests."""
        # For test compatibility, find all inject properties in the content
        properties = []
        pattern = re.compile(
            r'^\s*(private|public|protected)?\s*(readonly)?\s*(\w+)\s*=\s*inject\((\w+)\);',
            re.MULTILINE
        )
        
        for match in pattern.finditer(content):
            prop_name = match.group(3)
            prop_info = {
                'visibility': match.group(1),
                'readonly': match.group(2) == 'readonly',
                'name': prop_name,
                'type': match.group(4),
                'full_match': match.group(0)
            }
            properties.append(prop_info)
        
        return properties

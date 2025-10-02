import re
from pathlib import Path
from typing import Dict, Set

# Import shared data classes from migration_port
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from migration_port import Content, MigrationConfig


class AngularAuthenticationMigration:
    """
    Migrates inject() calls from class properties to constructor parameters.
    
    This migration focuses on moving inject() calls (especially authenticationService)
    from class properties to constructor parameters with default values.
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
        Migrate inject() calls from class properties to constructor parameters.
        
        Pattern to migrate:
        - Property: private/public router = inject(Router);
        - To: Constructor parameter with default value
        
        Args:
            content: The TypeScript file content
            
        Returns:
            Modified content with inject calls moved to constructor
        """
        if not content.strip():
            return content
        
        result = content
        
        # Find all inject() property declarations
        inject_properties = AngularAuthenticationMigration._find_inject_properties(result)
        
        if not inject_properties:
            return result
        
        # Find the constructor
        constructor_info = AngularAuthenticationMigration._find_constructor(result)
        
        if not constructor_info:
            # No constructor found, cannot migrate
            return result
        
        # Remove inject property declarations
        for prop in inject_properties:
            result = AngularAuthenticationMigration._remove_property_declaration(result, prop)
        
        # Add parameters to constructor
        result = AngularAuthenticationMigration._add_constructor_parameters(
            result, constructor_info, inject_properties
        )
        
        # Update super() call if authenticationService is involved
        result = AngularAuthenticationMigration._update_super_call(result, inject_properties)
        
        return result
    
    @staticmethod
    def _find_inject_properties(content: str) -> list:
        """
        Find all property declarations using inject().
        
        Returns:
            List of dicts with property info: {
                'visibility': 'private'/'public'/None,
                'readonly': True/False,
                'name': 'propertyName',
                'type': 'TypeName',
                'full_match': 'entire declaration'
            }
        """
        properties = []
        
        # Pattern: (private|public)? (readonly)? propertyName = inject(TypeName);
        pattern = re.compile(
            r'^\s*(private|public|protected)?\s*(readonly)?\s*(\w+)\s*=\s*inject\((\w+)\);',
            re.MULTILINE
        )
        
        for match in pattern.finditer(content):
            prop_info = {
                'visibility': match.group(1),  # private/public/protected or None
                'readonly': match.group(2) == 'readonly',
                'name': match.group(3),
                'type': match.group(4),
                'full_match': match.group(0)
            }
            properties.append(prop_info)
        
        return properties
    
    @staticmethod
    def _find_constructor(content: str) -> dict:
        """
        Find the constructor and extract its information.
        
        Returns:
            Dict with constructor info: {
                'start': start position,
                'params_start': position after opening paren,
                'params_end': position before closing paren,
                'body_start': position after opening brace,
                'existing_params': list of existing parameters
            }
            or None if no constructor found
        """
        # Pattern: constructor(...)
        constructor_pattern = re.compile(r'\bconstructor\s*\(', re.MULTILINE)
        match = constructor_pattern.search(content)
        
        if not match:
            return None
        
        params_start = match.end()
        
        # Find the closing paren for parameters
        paren_count = 1
        i = params_start
        params_end = -1
        
        while i < len(content) and paren_count > 0:
            if content[i] == '(':
                paren_count += 1
            elif content[i] == ')':
                paren_count -= 1
                if paren_count == 0:
                    params_end = i
                    break
            i += 1
        
        if params_end == -1:
            return None
        
        # Extract existing parameters
        params_text = content[params_start:params_end].strip()
        existing_params = []
        
        if params_text:
            # Split by comma, but be careful of nested types like Map<string, number>
            existing_params = AngularAuthenticationMigration._split_parameters(params_text)
        
        # Find the opening brace of constructor body
        body_start = content.find('{', params_end)
        
        return {
            'start': match.start(),
            'params_start': params_start,
            'params_end': params_end,
            'body_start': body_start,
            'existing_params': existing_params
        }
    
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
        
        # Replace the parameters section
        result = content[:params_start] + params_text + content[params_end:]
        
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



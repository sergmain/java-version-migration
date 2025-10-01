import re
from pathlib import Path
from dataclasses import dataclass
from typing import Dict


@dataclass
class Content:
    content: str
    changed: bool


@dataclass 
class MigrationConfig:
    path: Path
    files: Dict[str, str]


class AngularEmptyImportFix:
    """
    Adds missing 'imports: [],' line to Angular @Component decorators.
    
    This migration ensures that all @Component decorators have an imports array,
    which is required for standalone components in modern Angular.
    """
    
    @staticmethod
    def process(cfg: MigrationConfig, globals_config, content: str) -> Content:
        """
        Process TypeScript component file to add missing imports array to @Component decorators.
        
        Args:
            cfg: Migration configuration containing file path and related files
            globals_config: Global configuration (not used in this migration)
            content: The TypeScript file content to process
            
        Returns:
            Content object with potentially modified content and changed flag
        """
        new_content = AngularEmptyImportFix.add_empty_imports(content)
        return Content(new_content, new_content != content)
    
    @staticmethod
    def add_empty_imports(content: str) -> str:
        """
        Add 'imports: [],' to @Component decorators that don't already have it.
        
        The imports line is added as the first property after the opening brace
        of the @Component decorator configuration object.
        
        Args:
            content: The TypeScript file content
            
        Returns:
            Modified content with imports added where needed
        """
        if not content.strip():
            return content
        
        # Pattern to match @Component decorator with its configuration object
        # This pattern matches:
        # @Component({
        #     selector: '...',
        #     ...
        # })
        # The pattern uses a non-greedy match to find the opening brace and captures
        # everything until the closing brace, handling nested braces properly
        
        result = content
        
        # Find all @Component decorators
        component_pattern = re.compile(
            r'@Component\s*\(\s*\{',
            re.MULTILINE
        )
        
        matches = list(component_pattern.finditer(content))
        
        # Process matches in reverse order to maintain string positions
        for match in reversed(matches):
            start_pos = match.end()  # Position right after the opening {
            
            # Find the matching closing brace for this @Component
            closing_brace_pos = AngularEmptyImportFix._find_matching_closing_brace(content, start_pos)
            
            if closing_brace_pos == -1:
                # Couldn't find matching brace, skip this component
                continue
            
            # Extract the component configuration content
            config_content = content[start_pos:closing_brace_pos]
            
            # Check if 'imports' already exists in this configuration
            if AngularEmptyImportFix._has_imports_property(config_content):
                # Already has imports, skip
                continue
            
            # Find the first property or the position to insert imports
            insert_pos = AngularEmptyImportFix._find_imports_insert_position(content, start_pos)
            
            # Determine indentation based on the next line after the opening brace
            indentation = AngularEmptyImportFix._get_indentation(content, start_pos)
            
            # Insert 'imports: [],' with proper indentation
            imports_line = f"\n{indentation}imports: [],"
            result = result[:insert_pos] + imports_line + result[insert_pos:]
        
        return result
    
    @staticmethod
    def _find_matching_closing_brace(content: str, start_pos: int) -> int:
        """
        Find the matching closing brace for an opening brace at start_pos - 1.
        
        Args:
            content: The full content string
            start_pos: Position right after the opening brace
            
        Returns:
            Position of the matching closing brace, or -1 if not found
        """
        brace_count = 1
        i = start_pos
        in_string = False
        string_char = None
        in_comment_line = False
        in_comment_block = False
        
        while i < len(content) and brace_count > 0:
            char = content[i]
            prev_char = content[i-1] if i > 0 else ''
            
            # Handle line comments
            if not in_string and not in_comment_block:
                if char == '/' and i + 1 < len(content) and content[i+1] == '/':
                    in_comment_line = True
                    i += 2
                    continue
                elif char == '/' and i + 1 < len(content) and content[i+1] == '*':
                    in_comment_block = True
                    i += 2
                    continue
            
            # End of line comment
            if in_comment_line and char in ['\n', '\r']:
                in_comment_line = False
            
            # End of block comment
            if in_comment_block and char == '/' and prev_char == '*':
                in_comment_block = False
                i += 1
                continue
            
            # Skip if in comment
            if in_comment_line or in_comment_block:
                i += 1
                continue
            
            # Handle strings
            if char in ['"', "'", '`'] and prev_char != '\\':
                if not in_string:
                    in_string = True
                    string_char = char
                elif char == string_char:
                    in_string = False
                    string_char = None
            
            # Count braces only outside strings and comments
            if not in_string:
                if char == '{':
                    brace_count += 1
                elif char == '}':
                    brace_count -= 1
                    if brace_count == 0:
                        return i
            
            i += 1
        
        return -1
    
    @staticmethod
    def _has_imports_property(config_content: str) -> bool:
        """
        Check if the configuration object already has an 'imports' property.
        
        Args:
            config_content: The content between @Component({ and })
            
        Returns:
            True if imports property exists, False otherwise
        """
        # Pattern to match 'imports:' or 'imports :'
        # Look for word boundary before 'imports' to avoid matching 'someImports'
        imports_pattern = re.compile(r'\bimports\s*:', re.MULTILINE)
        
        for match in imports_pattern.finditer(config_content):
            # Check if this match is not in a comment
            pos = match.start()
            # Get the line containing this match
            line_start = config_content.rfind('\n', 0, pos) + 1
            line_segment = config_content[line_start:pos]
            
            # Check if it's in a line comment
            if '//' in line_segment:
                continue
            
            # Check if it's in a block comment
            last_block_start = config_content.rfind('/*', 0, pos)
            last_block_end = config_content.rfind('*/', 0, pos)
            if last_block_start != -1 and (last_block_end == -1 or last_block_end < last_block_start):
                continue
            
            # Found a valid imports property
            return True
        
        return False
    
    @staticmethod
    def _find_imports_insert_position(content: str, start_pos: int) -> int:
        """
        Find the position where 'imports: [],' should be inserted.
        This should be right after the opening brace, before any other properties.
        
        Args:
            content: The full content string
            start_pos: Position right after the @Component({ opening brace
            
        Returns:
            Position where imports should be inserted
        """
        # Insert right after the opening brace (after any whitespace)
        return start_pos
    
    @staticmethod
    def _get_indentation(content: str, start_pos: int) -> str:
        """
        Determine the indentation to use for the imports line.
        Look at the next line after start_pos to determine indentation.
        
        Args:
            content: The full content string
            start_pos: Position right after the @Component({ opening brace
            
        Returns:
            Indentation string (spaces or tabs)
        """
        # Find the next newline
        newline_pos = content.find('\n', start_pos)
        if newline_pos == -1:
            # No newline found, use default indentation
            return '    '
        
        # Find the next non-whitespace character after the newline
        line_start = newline_pos + 1
        indentation = ''
        
        for i in range(line_start, len(content)):
            char = content[i]
            if char in [' ', '\t']:
                indentation += char
            else:
                break
        
        # If no indentation found, use default
        if not indentation:
            return '    '
        
        return indentation

import re
from pathlib import Path
from dataclasses import dataclass
from typing import Dict, Set


@dataclass
class Content:
    content: str
    changed: bool


@dataclass 
class MigrationConfig:
    path: Path
    files: Dict[str, str]


class AngularHtmlSignalMigration:
    
    @staticmethod
    def process(cfg: MigrationConfig, content: str) -> Content:
        """Process HTML template to update signal property references."""
        new_content = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, content)
        return Content(new_content, new_content != content)
    
    @staticmethod
    def migrate_html_to_signals(cfg: MigrationConfig, html_content: str) -> str:
        """
        Migrate HTML template to add () after signal property references.
        Assumes the corresponding TypeScript file has already been processed.
        """
        if not html_content.strip():
            return html_content
        
        # Get the corresponding TypeScript file to analyze what properties are signals
        ts_content = AngularHtmlSignalMigration._get_ts_content(cfg)
        if not ts_content:
            return html_content
        
        # Detect signal properties from the TypeScript file
        signal_properties = AngularHtmlSignalMigration._detect_signal_properties(ts_content)
        computed_properties = AngularHtmlSignalMigration._detect_computed_properties(ts_content)
        
        # Combine all signal-based properties
        all_signal_properties = signal_properties | computed_properties
        
        if not all_signal_properties:
            return html_content
        
        result = html_content
        
        # Update HTML to add () after signal properties
        for signal_prop in all_signal_properties:
            result = AngularHtmlSignalMigration._add_signal_calls_to_property(result, signal_prop)
        
        return result
    
    @staticmethod
    def _get_ts_content(cfg: MigrationConfig) -> str:
        """Get the corresponding TypeScript file content."""
        html_file_name = cfg.path.name
        if not html_file_name.endswith(".html"):
            return ""
        
        # Try to find corresponding .ts file
        base_name = html_file_name[:-5]  # Remove .html
        ts_name = f"{base_name}.ts"
        
        return cfg.files.get(ts_name, "")
    
    @staticmethod
    def _detect_signal_properties(ts_content: str) -> Set[str]:
        """Detect properties that are signals in the TypeScript file."""
        signal_properties = set()
        
        # Pattern: propertyName = signal<...>(...)
        signal_pattern = re.compile(r"(\w+)\s*=\s*signal<")
        for match in signal_pattern.finditer(ts_content):
            signal_properties.add(match.group(1))
        
        # Pattern: propertyName = input<...>(...)
        # Note: input() signals are read-only but still need () in templates
        input_pattern = re.compile(r"(\w+)\s*=\s*input(?:\.required)?<")
        for match in input_pattern.finditer(ts_content):
            signal_properties.add(match.group(1))
        
        return signal_properties
    
    @staticmethod
    def _detect_computed_properties(ts_content: str) -> Set[str]:
        """Detect properties that are computed signals in the TypeScript file."""
        computed_properties = set()
        
        # Pattern: propertyName = computed(() => ...)
        computed_pattern = re.compile(r"(\w+)\s*=\s*computed\(")
        for match in computed_pattern.finditer(ts_content):
            computed_properties.add(match.group(1))
        
        return computed_properties
    
    @staticmethod
    def _add_signal_calls_to_property(html_content: str, property_name: str) -> str:
        """
        Add () after a signal property reference in HTML template.
        This is complex because we need to avoid:
        - Properties already with ()
        - Properties inside strings
        - Properties that are part of method calls
        """
        result = html_content
        
        # Context-aware replacement patterns
        # We'll process the HTML line by line to maintain context
        
        lines = result.split('\n')
        new_lines = []
        
        for line in lines:
            modified_line = line
            
            # Find all potential occurrences of the property
            # Use word boundary to avoid partial matches
            pattern = rf'\b{property_name}\b'
            
            # Find all matches with their positions
            matches = list(re.finditer(pattern, modified_line))
            
            # Process matches in reverse order to maintain positions
            for match in reversed(matches):
                start = match.start()
                end = match.end()
                
                # Check if already followed by (
                if end < len(modified_line) and modified_line[end:end+1] == '(':
                    continue
                
                # Check context to determine if we should add ()
                if AngularHtmlSignalMigration._should_add_parentheses(modified_line, start, end, property_name):
                    # Insert () after the property
                    modified_line = modified_line[:end] + '()' + modified_line[end:]
            
            new_lines.append(modified_line)
        
        return '\n'.join(new_lines)
    
    @staticmethod
    def _should_add_parentheses(line: str, start: int, end: int, property_name: str) -> bool:
        """
        Determine if we should add () after this property reference.
        Returns True if we should add parentheses, False otherwise.
        """
        # Check if inside a string (single or double quotes)
        if AngularHtmlSignalMigration._is_inside_string(line, start):
            return False
        
        # Check if inside a comment
        if AngularHtmlSignalMigration._is_inside_comment(line, start):
            return False
        
        # Check what comes after the property
        after = line[end:end+10] if end < len(line) else ""
        
        # Skip if followed by ( - already a method call
        if after.startswith('('):
            return False
        
        # Skip if followed by = - it's an assignment (shouldn't happen in templates but just in case)
        if after.lstrip().startswith('=') and not after.lstrip().startswith('=='):
            return False
        
        # Check what comes before the property
        before = line[max(0, start-10):start]
        
        # Skip if preceded by @ - it's a decorator or special syntax
        if before.rstrip().endswith('@'):
            return False
        
        # Skip if preceded by # - it's a template reference variable
        if before.rstrip().endswith('#'):
            return False
        
        # Skip if it's part of a property path like @Input() or similar
        if '@' in before and 'Input' in line[max(0, start-20):end]:
            return False
        
        # Add () in all other cases
        return True
    
    @staticmethod
    def _is_inside_string(line: str, pos: int) -> bool:
        """Check if position is inside a string literal."""
        # Count quotes before position
        single_quotes = 0
        double_quotes = 0
        
        for i in range(pos):
            if line[i] == "'" and (i == 0 or line[i-1] != '\\'):
                single_quotes += 1
            elif line[i] == '"' and (i == 0 or line[i-1] != '\\'):
                double_quotes += 1
        
        # If odd number of quotes, we're inside a string
        return (single_quotes % 2 == 1) or (double_quotes % 2 == 1)
    
    @staticmethod
    def _is_inside_comment(line: str, pos: int) -> bool:
        """Check if position is inside an HTML comment."""
        # Check for <!-- before position and --> after (or no --> meaning comment continues)
        before = line[:pos]
        after = line[pos:]
        
        last_comment_start = before.rfind('<!--')
        last_comment_end = before.rfind('-->')
        
        # If we found <!-- and no --> after it, we're in a comment
        if last_comment_start != -1:
            if last_comment_end == -1 or last_comment_end < last_comment_start:
                return True
        
        return False

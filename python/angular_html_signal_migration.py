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
            print(f"DEBUG: No TypeScript content found for {cfg.path.name}")
            return html_content
        
        print(f"DEBUG: Found TypeScript content for {cfg.path.name}, length: {len(ts_content)}")
        
        # Detect signal properties from the TypeScript file
        signal_properties = AngularHtmlSignalMigration._detect_signal_properties(ts_content)
        computed_properties = AngularHtmlSignalMigration._detect_computed_properties(ts_content)
        
        print(f"DEBUG: Detected signals: {signal_properties}")
        print(f"DEBUG: Detected computed: {computed_properties}")
        
        # Combine all signal-based properties
        all_signal_properties = signal_properties | computed_properties
        
        if not all_signal_properties:
            print(f"DEBUG: No signal properties detected for {cfg.path.name}")
            return html_content
        
        result = html_content
        
        # Update HTML to add () after signal properties
        for signal_prop in all_signal_properties:
            before = result
            result = AngularHtmlSignalMigration._add_signal_calls_to_property(result, signal_prop)
            if result != before:
                print(f"DEBUG: Updated property '{signal_prop}' in {cfg.path.name}")
        
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
        changes_made = 0
        
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
                should_add = AngularHtmlSignalMigration._should_add_parentheses(modified_line, start, end, property_name)
                
                if should_add:
                    # Insert () after the property
                    modified_line = modified_line[:end] + '()' + modified_line[end:]
                    changes_made += 1
            
            new_lines.append(modified_line)
        
        if changes_made > 0:
            print(f"DEBUG: Made {changes_made} changes for property '{property_name}'")
        else:
            print(f"DEBUG: No changes made for property '{property_name}' (found {len(list(re.finditer(rf'\b{property_name}\b', html_content)))} occurrences)")
        
        return '\n'.join(new_lines)
    
    @staticmethod
    def _should_add_parentheses(line: str, start: int, end: int, property_name: str) -> bool:
        """
        Determine if we should add () after this property reference.
        Returns True if we should add parentheses, False otherwise.
        """
        # Check if inside a string (single or double quotes)
        if AngularHtmlSignalMigration._is_inside_string(line, start):
            print(f"DEBUG: Skipping '{property_name}' - inside string")
            return False
        
        # Check if inside a comment
        if AngularHtmlSignalMigration._is_inside_comment(line, start):
            print(f"DEBUG: Skipping '{property_name}' - inside comment")
            return False
        
        # Check what comes after the property
        after = line[end:end+10] if end < len(line) else ""
        
        # Skip if followed by ( - already a method call
        if after.startswith('('):
            print(f"DEBUG: Skipping '{property_name}' - already has ()")
            return False
        
        # Check what comes before the property
        before = line[max(0, start-10):start]
        
        # CRITICAL: Skip if we're inside a property binding bracket [propertyName]
        # This handles cases like [designTimeTemplate]="value" where designTimeTemplate
        # is the INPUT PROPERTY NAME, not a signal being accessed
        # Look for [ before the property and ] after it (without intervening ] before the property)
        before_full = line[:start]
        after_full = line[end:]
        
        # Find the last [ before our position
        last_open_bracket = before_full.rfind('[')
        # Find the last ] before our position (to see if we closed the bracket)
        last_close_bracket = before_full.rfind(']')
        
        if last_open_bracket != -1:
            # Check if we're inside [...] by seeing if there's no ] between [ and our position
            if last_close_bracket == -1 or last_close_bracket < last_open_bracket:
                # We're inside a [, now check if there's a ] after us
                next_close_bracket = after_full.find(']')
                if next_close_bracket != -1:
                    # We're inside [...], which means this is a property binding NAME, not a value
                    print(f"DEBUG: Skipping '{property_name}' - inside property binding bracket [...]")
                    return False
        
        # Skip if followed by = - it's an assignment (shouldn't happen in templates but just in case)
        # BUT allow == and === for comparisons
        after_stripped = after.lstrip()
        if after_stripped.startswith('=') and not after_stripped.startswith('==') and not after_stripped.startswith('==='):
            print(f"DEBUG: Skipping '{property_name}' - followed by =")
            return False
        
        # Skip if preceded by @ - it's a decorator or special syntax
        if before.rstrip().endswith('@'):
            print(f"DEBUG: Skipping '{property_name}' - preceded by @")
            return False
        
        # Skip if preceded by # - it's a template reference variable
        if before.rstrip().endswith('#'):
            print(f"DEBUG: Skipping '{property_name}' - preceded by #")
            return False
        
        # Skip if preceded by . - it's a property access (like obj.propertyName)
        if before.rstrip().endswith('.'):
            print(f"DEBUG: Skipping '{property_name}' - preceded by .")
            return False
        
        # Skip if it's part of a property path like @Input() or similar
        if '@' in before and 'Input' in line[max(0, start-20):end]:
            print(f"DEBUG: Skipping '{property_name}' - looks like @Input")
            return False
        
        # Add () in all other cases
        print(f"DEBUG: Adding () to '{property_name}'")
        return True
    
    @staticmethod
    def _is_inside_string(line: str, pos: int) -> bool:
        """
        Check if position is inside a string literal that's NOT an Angular binding.
        In Angular templates, things like [attr]="expression" should NOT be considered
        as being inside a string - the expression part needs signal () added.
        """
        # First, check if we're inside an Angular binding expression
        # These should NEVER be considered "inside a string"
        before = line[:pos]
        after = line[pos:]
        
        # Find the nearest = before our position
        last_equals = before.rfind('=')
        if last_equals != -1:
            # Get the quote after the =
            after_equals = before[last_equals+1:].lstrip()
            if after_equals and after_equals[0] in ('"', "'"):
                quote_char = after_equals[0]
                # Find where this quote started
                quote_start = before.find(quote_char, last_equals)
                
                # Check if there's a closing quote after our position
                closing_quote = (before[quote_start+1:] + after).find(quote_char)
                
                if closing_quote != -1:
                    # We're inside a quoted expression after =
                    # Now check if this is an Angular binding
                    section_before_equals = before[:last_equals].rstrip()
                    
                    # Check for Angular binding patterns: [...], (...), [(...)], @if, @for, etc.
                    if section_before_equals.endswith(']') or \
                       section_before_equals.endswith(')') or \
                       ' @if ' in section_before_equals or \
                       ' @for ' in section_before_equals or \
                       ' @switch ' in section_before_equals:
                        # This is an Angular expression, not a regular string
                        return False
        
        # For non-Angular contexts, check if we're in a regular string
        # We need to be more careful here - only mark as "inside string" if we're truly
        # in a string literal that's NOT part of an Angular expression
        
        # Simple heuristic: if we see class=' or class=" before our position,
        # and we're between those quotes, we're in a CSS class name, not a binding
        class_attr_match = re.search(r'''class\s*=\s*(['"])([^'"]*$)''', before)
        if class_attr_match:
            quote_char = class_attr_match.group(1)
            # Check if there's a closing quote after our position
            if quote_char in after:
                # We're inside a class attribute value
                return True
        
        # Check for other regular HTML attributes (not Angular bindings)
        # Pattern: word="..." or word='...' where word doesn't start with [ or (
        attr_match = re.search(r'''\s(\w+)\s*=\s*(['"])([^'"]*$)''', before)
        if attr_match:
            attr_name = attr_match.group(1)
            quote_char = attr_match.group(2)
            # If attribute doesn't look like an Angular binding and has closing quote
            if not attr_name.startswith('[') and not attr_name.startswith('(') and quote_char in after:
                return True
        
        return False
    
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

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
        input_signals = AngularHtmlSignalMigration._detect_input_signals(ts_content)
        
        print(f"DEBUG: Detected signals: {signal_properties}")
        print(f"DEBUG: Detected computed: {computed_properties}")
        print(f"DEBUG: Detected input signals (read-only): {input_signals}")
        
        # Combine all signal-based properties (including input signals - they still need () in templates)
        all_signal_properties = signal_properties | computed_properties | input_signals
        
        if not all_signal_properties:
            print(f"DEBUG: No signal properties detected for {cfg.path.name}")
            return html_content
        
        result = html_content
        
        # FIRST: Convert two-way bindings [(ngModel)]="signal" to one-way binding + event
        # BUT skip input signals since they're read-only
        result = AngularHtmlSignalMigration._convert_two_way_bindings(result, all_signal_properties, input_signals)
        
        # THEN: Update HTML to add () after signal properties
        for signal_prop in all_signal_properties:
            before = result
            result = AngularHtmlSignalMigration._add_signal_calls_to_property(result, signal_prop)
            if result != before:
                print(f"DEBUG: Updated property '{signal_prop}' in {cfg.path.name}")
        
        return result
    
    @staticmethod
    def _convert_two_way_bindings(html_content: str, signal_properties: Set[str], input_signals: Set[str]) -> str:
        """
        Convert two-way bindings [(ngModel)]="signalName" to [ngModel]="signalName()" (ngModelChange)="signalName.set($event)"
        Because signals don't support two-way binding syntax.
        
        Note: In the binding [ngModel]="signalName()", we call the signal to get its value.
        But in (ngModelChange)="signalName.set($event)", we use the signal object itself (no parentheses).
        """
        result = html_content
        
        for signal_prop in signal_properties:
            # Skip input signals - they're read-only
            if signal_prop in input_signals:
                print(f"DEBUG: Skipping two-way binding conversion for input signal '{signal_prop}' (read-only)")
                continue
            
            # Pattern: [(ngModel)]="signalProp" or [(ngModel)]="signalProp()"
            # Convert to: [ngModel]="signalProp" (ngModelChange)="signalProp.set($event)"
            # The () will be added later in the _add_signal_calls_to_property step
            
            # Match with or without () already present
            pattern1 = rf'\[\(ngModel\)\]="({signal_prop})(?:\(\))?"'
            pattern2 = rf"\[\(ngModel\)\]='({signal_prop})(?:\(\))?'"
            
            def replace_two_way(match):
                prop_name = match.group(1)
                # In [ngModel], the signal will get () added later
                # In (ngModelChange), use .set() on the signal object (no ())
                return f'[ngModel]="{prop_name}" (ngModelChange)="{prop_name}.set($event)"'
            
            before = result
            result = re.sub(pattern1, replace_two_way, result)
            result = re.sub(pattern2, replace_two_way, result)
            
            if result != before:
                print(f"DEBUG: Converted two-way binding [(ngModel)] for signal '{signal_prop}'")
        
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
        """Detect properties that are writable signals in the TypeScript file."""
        signal_properties = set()
        
        # Pattern: propertyName = signal<...>(...)
        signal_pattern = re.compile(r"(\w+)\s*=\s*signal<")
        for match in signal_pattern.finditer(ts_content):
            signal_properties.add(match.group(1))
        
        return signal_properties
    
    @staticmethod
    def _detect_input_signals(ts_content: str) -> Set[str]:
        """Detect properties that are input signals (read-only) in the TypeScript file."""
        input_signals = set()
        
        # Pattern: propertyName = input<...>(...) or input.required<...>(...)
        # Note: input() signals are read-only and don't have .set()
        input_pattern = re.compile(r"(\w+)\s*=\s*input(?:\.required)?<")
        for match in input_pattern.finditer(ts_content):
            input_signals.add(match.group(1))
        
        return input_signals
    
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
        
        # Process the entire content at once to handle all occurrences
        # Use word boundary to avoid partial matches
        pattern = rf'\b{property_name}\b'
        
        # Find all matches with their positions
        matches = list(re.finditer(pattern, result))
        
        if not matches:
            print(f"DEBUG: No occurrences found for property '{property_name}'")
            return result
        
        print(f"DEBUG: Found {len(matches)} occurrences of '{property_name}'")
        
        # Process matches in REVERSE order to maintain positions
        # This is critical - when we modify the string, earlier positions stay valid
        changes_made = 0
        for match in reversed(matches):
            start = match.start()
            end = match.end()
            
            # Get the line containing this match for context checking
            line_start = result.rfind('\n', 0, start) + 1
            line_end = result.find('\n', end)
            if line_end == -1:
                line_end = len(result)
            line = result[line_start:line_end]
            
            # Adjust positions to be relative to the line
            pos_in_line = start - line_start
            end_in_line = end - line_start
            
            print(f"DEBUG: Checking occurrence of '{property_name}' at position {start} (line pos {pos_in_line})")
            print(f"DEBUG:   Line: {line}")
            
            # Check if already followed by (
            if end < len(result) and result[end:end+1] == '(':
                print(f"DEBUG: Skipping - already has ()")
                continue
            
            # Check context to determine if we should add ()
            should_add = AngularHtmlSignalMigration._should_add_parentheses(line, pos_in_line, end_in_line, property_name)
            
            if should_add:
                # Insert () after the property in the full content
                result = result[:end] + '()' + result[end:]
                changes_made += 1
                print(f"DEBUG: ADDED () to '{property_name}' at position {start}")
            else:
                print(f"DEBUG: SKIPPED '{property_name}' at position {start}")
        
        if changes_made > 0:
            print(f"DEBUG: Made {changes_made} changes for property '{property_name}'")
        else:
            print(f"DEBUG: No changes made for property '{property_name}'")
        
        return result
    
    @staticmethod
    def _should_add_parentheses(line: str, start: int, end: int, property_name: str) -> bool:
        """
        Determine if we should add () after this property reference.
        Returns True if we should add parentheses, False otherwise.
        """
        # Check if inside a string (single or double quotes)
        if AngularHtmlSignalMigration._is_inside_string(line, start):
            print(f"DEBUG: Skipping '{property_name}' at position {start} - inside string")
            return False
        
        # Check if inside a comment
        if AngularHtmlSignalMigration._is_inside_comment(line, start):
            print(f"DEBUG: Skipping '{property_name}' at position {start} - inside comment")
            return False
        
        # Check what comes before and after the property
        before = line[max(0, start-10):start]
        before_full = line[:start]
        after = line[end:end+10] if end < len(line) else ""
        after_full = line[end:]
        
        # CRITICAL: Skip if we're inside an HTML tag name
        # Pattern: <tagname or </tagname
        # Check if there's a < before us with no > between < and our position
        last_open_tag = before_full.rfind('<')
        last_close_tag = before_full.rfind('>')
        
        if last_open_tag != -1 and (last_close_tag == -1 or last_close_tag < last_open_tag):
            # We're after a < with no closing >
            # Check if we're in the tag name (before any space or attribute)
            section_after_tag = before_full[last_open_tag+1:]
            # Remove / if it's a closing tag
            if section_after_tag.startswith('/'):
                section_after_tag = section_after_tag[1:]
            
            # If there's no space between < and our position, we're in the tag name
            if ' ' not in section_after_tag and '\t' not in section_after_tag and '\n' not in section_after_tag:
                print(f"DEBUG: Skipping '{property_name}' at position {start} - inside HTML tag name")
                return False
        
        # Skip if followed by ( - already a method call
        if after.startswith('('):
            print(f"DEBUG: Skipping '{property_name}' at position {start} - already has ()")
            return False
        
        # Skip if followed by .set( - it's a signal.set() call, not a signal read
        if after.lstrip().startswith('.set('):
            print(f"DEBUG: Skipping '{property_name}' at position {start} - followed by .set()")
            return False
        
        # CRITICAL: Skip if we're inside an object literal like { cols: cols() }
        # Check if we're between { and } and there's a : before our position
        # BUT exclude {{ and }} which are Angular interpolation braces, not object literals
        last_open_brace = before_full.rfind('{')
        last_close_brace = before_full.rfind('}')
        
        # Check if the { is part of {{ (Angular interpolation)
        # The { at position N is part of {{ if the character at position N+1 is also {
        if last_open_brace != -1:
            # Check both: is it preceded by { (for the second { in {{) or followed by { (for the first { in {{)
            is_double_open = False
            if last_open_brace + 1 < len(before_full) and before_full[last_open_brace + 1] == '{':
                is_double_open = True  # This is the first { in {{
            if last_open_brace > 0 and before_full[last_open_brace - 1] == '{':
                is_double_open = True  # This is the second { in {{
            
            if is_double_open:
                # This is {{, not an object literal brace - skip this check
                last_open_brace = -1
        
        if last_open_brace != -1 and (last_close_brace == -1 or last_close_brace < last_open_brace):
            # We're inside a real {}, check if we're a KEY (before colon) or VALUE (after colon)
            
            # Look ahead: is there a : or , or } IMMEDIATELY after us (with only whitespace)?
            after_stripped_short = after.lstrip()
            if after_stripped_short.startswith(':'):
                # We're a KEY - pattern: { propertyName: ... }
                print(f"DEBUG: Skipping '{property_name}' at position {start} - object literal key")
                return False
            elif after_stripped_short.startswith(',') or after_stripped_short.startswith('}'):
                # Pattern: { key: propertyName, ... } or { key: propertyName }
                # We're after a colon, so we're a VALUE - check if there's a : before us
                section_after_brace = before_full[last_open_brace:]
                if ':' in section_after_brace:
                    # There's a : between { and us, so we're a value - ADD ()
                    pass  # Continue to add ()
                else:
                    # No : before us, so we might be shorthand: { propertyName, ... }
                    # This is treated as a key
                    print(f"DEBUG: Skipping '{property_name}' at position {start} - object literal shorthand key")
                    return False
        
        # CRITICAL: Skip if we're inside a property binding bracket [propertyName]
        # This handles cases like [designTimeTemplate]="value" where designTimeTemplate
        # is the INPUT PROPERTY NAME, not a signal being accessed
        # Look for [ before the property and ] after it (without intervening ] before the property)
        
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
                    print(f"DEBUG: Skipping '{property_name}' at position {start} - inside property binding bracket [...]")
                    return False
        
        # Skip if followed by = - it's an assignment (shouldn't happen in templates but just in case)
        # BUT allow == and === for comparisons
        after_stripped = after.lstrip()
        if after_stripped.startswith('=') and not after_stripped.startswith('==') and not after_stripped.startswith('==='):
            print(f"DEBUG: Skipping '{property_name}' at position {start} - followed by =")
            return False
        
        # Skip if preceded by @ - it's a decorator or special syntax
        if before.rstrip().endswith('@'):
            print(f"DEBUG: Skipping '{property_name}' at position {start} - preceded by @")
            return False
        
        # Skip if preceded by # - it's a template reference variable
        if before.rstrip().endswith('#'):
            print(f"DEBUG: Skipping '{property_name}' at position {start} - preceded by #")
            return False
        
        # Skip if preceded by . - it's a property access (like obj.propertyName)
        if before.rstrip().endswith('.'):
            print(f"DEBUG: Skipping '{property_name}' at position {start} - preceded by .")
            return False
        
        # Skip if it's part of a property path like @Input() or similar
        if '@' in before and 'Input' in line[max(0, start-20):end]:
            print(f"DEBUG: Skipping '{property_name}' at position {start} - looks like @Input")
            return False
        
        # Add () in all other cases
        print(f"DEBUG: Adding () to '{property_name}' at position {start}")
        print(f"DEBUG:   Line context: ...{line[max(0,start-20):min(len(line),end+20)]}...")
        return True
    
    @staticmethod
    def _is_inside_string(line: str, pos: int) -> bool:
        """
        Check if position is inside a string literal that's NOT an Angular binding.
        In Angular templates, things like [attr]="expression" should NOT be considered
        as being inside a string - the expression part needs signal () added.
        Also, {{ expression }} interpolations are NOT strings.
        """
        # First, check if we're inside an Angular interpolation {{ }}
        before = line[:pos]
        after = line[pos:]
        
        # Check for {{ before and }} after
        last_double_open = before.rfind('{{')
        last_double_close = before.rfind('}}')
        
        if last_double_open != -1 and (last_double_close == -1 or last_double_close < last_double_open):
            # We're inside {{, check if there's }} after
            if '}}' in after:
                # We're inside {{ }}, check if we're in a STRING inside the interpolation
                # Pattern: {{ 'string' }}
                text_after_open_braces = before[last_double_open+2:]
                
                # Count quotes to see if we're inside a string within the interpolation
                single_quotes = text_after_open_braces.count("'") - text_after_open_braces.count("\\'")
                double_quotes = text_after_open_braces.count('"') - text_after_open_braces.count('\\"')
                
                # If odd number of quotes, we're inside a string within {{ }}
                if single_quotes % 2 == 1 or double_quotes % 2 == 1:
                    return True
                
                # We're in interpolation but not in a string - NOT a string context
                return False
        
        # Check if we're inside an Angular binding expression
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

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


class AngularToSignalMigration:
    
    @staticmethod
    def process(cfg: MigrationConfig, content: str) -> Content:
        new_content = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, content)
        return Content(new_content, new_content != content)
    
    @staticmethod
    def migrate_angular_to_signal_migration(cfg: MigrationConfig, content: str) -> str:
        if not content.strip():
            return content
            
        result = content
        needs_signal_import = False
        needs_computed_import = False
        needs_input_import = False
        signal_properties = set()
        
        # Check if there are @Input() decorators
        if "@Input()" in result:
            needs_input_import = True
        
        # Get corresponding HTML file content for analysis
        html_content = AngularToSignalMigration._get_html_content(cfg)
        
        # Pattern 1: Convert simple properties used in templates to signals
        before_signals = result
        result = AngularToSignalMigration._convert_properties_to_signals(cfg, result, html_content, signal_properties)
        if result != before_signals:
            needs_signal_import = True
        
        # Pattern 2: Convert getter methods to computed signals
        before_computed = result
        result = AngularToSignalMigration._convert_getters_to_computed(result, html_content)
        if result != before_computed:
            needs_computed_import = True
            
        # Pattern 3: Update method calls that modify properties to use signal updates and handle property usage
        result = AngularToSignalMigration._convert_property_assignments(result, signal_properties)
        
        # Add necessary imports at the top
        result = AngularToSignalMigration._add_signal_imports(result, needs_signal_import, needs_computed_import, needs_input_import)
        
        return result
    
    @staticmethod
    def _get_html_content(cfg: MigrationConfig) -> str:
        # Look for corresponding .html file
        ts_file_name = cfg.path.name
        if ts_file_name.endswith(".ts"):
            base_name = ts_file_name[:-3]
            html_name = f"{base_name}.html"
            return cfg.files.get(html_name, "")
        return ""
    
    @staticmethod
    def _convert_properties_to_signals(cfg: MigrationConfig, content: str, html_content: str, signal_properties: Set[str]) -> str:
        result = content
        
        # Find all @Input() decorated properties - these MUST be converted to input()
        input_properties = set()
        input_pattern = re.compile(r"@Input\(\)\s+(\w+)")
        for match in input_pattern.finditer(content):
            input_properties.add(match.group(1))
        
        # Find all input() signal declarations (from ngxtension conversion)
        input_signal_properties = set()
        input_signal_pattern = re.compile(r"(\w+)\s*=\s*input(?:\.required)?<")
        for match in input_signal_pattern.finditer(content):
            input_signal_properties.add(match.group(1))
        
        # Find all existing signal() declarations  
        existing_signal_properties = set()
        existing_signal_pattern = re.compile(r"(\w+)\s*=\s*signal<")
        for match in existing_signal_pattern.finditer(content):
            existing_signal_properties.add(match.group(1))
        
        # Find properties that are assigned to internally
        properties_assigned_internally = set()
        assignment_pattern = re.compile(r"\bthis\.(\w+)\.set\(")
        for match in assignment_pattern.finditer(content):
            properties_assigned_internally.add(match.group(1))
        
        # Check if this component is used in other templates and which properties are passed as inputs
        # Look for patterns like [propertyName]="value" in parent HTML files
        properties_used_as_inputs_in_parents = set()
        
        # Get the component selector from @Component decorator
        selector_match = re.search(r"selector:\s*['\"]([^'\"]+)['\"]", content)
        if selector_match:
            selector = selector_match.group(1)
            print(f"DEBUG: Found component selector: {selector}")
            
            # Check all HTML files for usage of this component
            for file_name, file_content in cfg.files.items():
                if file_name.endswith('.html') and file_name != AngularToSignalMigration._get_html_filename_from_ts(cfg.path.name):
                    # Look for this component being used with input bindings
                    # Pattern: <selector [propertyName]="..."
                    component_usage_pattern = rf'<{re.escape(selector)}[^>]*?\[(\w+)\]='
                    matches = list(re.finditer(component_usage_pattern, file_content))
                    if matches:
                        print(f"DEBUG: Found component usage in {file_name}")
                        for match in matches:
                            prop_name = match.group(1)
                            properties_used_as_inputs_in_parents.add(prop_name)
                            print(f"DEBUG: Found input binding: [{prop_name}]")
        
        if properties_used_as_inputs_in_parents:
            print(f"DEBUG: Properties used as inputs from parents: {properties_used_as_inputs_in_parents}")
        
        # Signals that are used as inputs from parent components should be converted to input()
        # BUT only if they're not assigned internally
        signals_to_convert_to_inputs = existing_signal_properties & properties_used_as_inputs_in_parents - properties_assigned_internally
        
        if existing_signal_properties:
            print(f"DEBUG: Existing signal properties: {existing_signal_properties}")
        if properties_assigned_internally:
            print(f"DEBUG: Properties assigned internally: {properties_assigned_internally}")
        if signals_to_convert_to_inputs:
            print(f"DEBUG: Signals to convert to inputs: {signals_to_convert_to_inputs}")
        
        # Convert signal() to input() for properties used as inputs from parents
        if signals_to_convert_to_inputs:
            for prop_name in signals_to_convert_to_inputs:
                # Match patterns like: propName = signal<Type>(...);
                signal_decl_pattern = re.compile(
                    rf"(\s*)({prop_name})\s*=\s*signal<([^>]+)>\(([^)]*)\);"
                )
                
                def replace_signal_with_input(match):
                    indent = match.group(1)
                    prop_name = match.group(2)
                    type_annotation = match.group(3)
                    default_value = match.group(4).strip()
                    
                    signal_properties.add(prop_name)
                    
                    # input() doesn't take default values in the same way
                    # If there was a default value, make it optional, otherwise required
                    if default_value and default_value != 'undefined':
                        return f"{indent}{prop_name} = input<{type_annotation}>();"
                    else:
                        return f"{indent}{prop_name} = input<{type_annotation}>();"
                
                result = signal_decl_pattern.sub(replace_signal_with_input, result)
                print(f"DEBUG: Converted signal '{prop_name}' to input() because it's used as input from parent")
        
        # Find all getters - these should NOT be converted to signals (they'll be converted to computed separately)
        getter_names = set()
        getter_pattern = re.compile(r"(?s)get\s+(\w+)\(\)")
        for match in getter_pattern.finditer(content):
            getter_names.add(match.group(1))
        
        # First, find all getters used in the template
        getters_used_in_template = set()
        properties_used_in_getters = set()
        
        for match in getter_pattern.finditer(content):
            getter_name = match.group(0)
            # Extract the getter body
            getter_full = re.search(rf"get\s+{re.escape(getter_name)}\(\)[^{{]*\{{([^}}]+)\}}", content, re.DOTALL)
            if getter_full:
                getter_body = getter_full.group(1)
                
                if AngularToSignalMigration._is_property_used_in_template(getter_name, html_content):
                    getters_used_in_template.add(getter_name)
                    # Extract properties used in this getter
                    prop_pattern = re.compile(r"this\.(\w+)(?!\()")
                    for prop_match in prop_pattern.finditer(getter_body):
                        properties_used_in_getters.add(prop_match.group(1))
        
        # Find event handler methods and properties modified in them
        properties_modified_in_event_handlers = AngularToSignalMigration._find_properties_modified_in_event_handlers(content, html_content)
        
        # Helper function to check if a line is inside an interface/type definition OR object literal type
        def is_inside_interface(line_number: int) -> bool:
            lines = result.split('\n')
            if line_number >= len(lines):
                return False
            
            # Look backwards from current line to find if we're inside an interface or object type
            brace_count = 0
            in_object_type = False
            
            for i in range(line_number, -1, -1):
                line = lines[i].strip()
                
                # Check if this line starts an interface or type
                if re.match(r'^(export\s+)?(interface|type)\s+\w+', line):
                    # We found an interface/type definition
                    # Now count braces forward to see if our line is inside it
                    for j in range(i, line_number + 1):
                        brace_count += lines[j].count('{') - lines[j].count('}')
                    return brace_count > 0
                
                # Check if we're inside an object literal type: propertyName: { ... } = {...}
                # This pattern: "name: {" followed by properties
                if re.search(r':\s*\{', line) and not re.search(r'=\s*\{', line):
                    # Count braces to see if we're still inside
                    for j in range(i, line_number + 1):
                        brace_count += lines[j].count('{') - lines[j].count('}')
                    if brace_count > 0:
                        return True
                
                # If we hit a class definition, we're not in an interface
                if re.match(r'^(export\s+)?(class|@Component)', line):
                    return False
                
                # If we see "} = {" or similar, we might be past the object type definition
                if re.search(r'\}\s*=', line):
                    return False
            
            return False
        
        # Process property declarations in correct order to avoid conflicts
        
        # First handle @Input() decorated properties - convert to input() ONLY if not assigned internally
        input_with_type_pattern = re.compile(r"(?m)^(\s*)@Input\(\)\s+((?:private|public|protected)\s+)?(\w+)(\??)\s*:\s*([^=;\n]+)(?:\s*=\s*([^;\n]+))?;")
        
        def replace_input(match):
            indent = match.group(1)
            visibility = match.group(2) if match.group(2) else ""
            property_name = match.group(3)
            optional = match.group(4)
            type_annotation = match.group(5).strip()
            default_value = match.group(6)
            
            # If this @Input() is assigned internally, convert to regular signal instead
            if property_name in properties_assigned_internally:
                signal_properties.add(property_name)
                if default_value:
                    return f"{indent}{visibility}{property_name} = signal<{type_annotation}>({default_value.strip()});"
                else:
                    return f"{indent}{visibility}{property_name} = signal<{type_annotation} | undefined>(undefined);"
            
            # Otherwise convert to input()
            signal_properties.add(property_name)
            
            if default_value:
                return f"{indent}{visibility}{property_name} = input<{type_annotation}>({default_value.strip()});"
            elif optional == "?":
                return f"{indent}{visibility}{property_name} = input<{type_annotation}>();"
            else:
                return f"{indent}{visibility}{property_name} = input.required<{type_annotation}>();"
        
        result = input_with_type_pattern.sub(replace_input, result)
        
        # Then handle typed properties with initializers: property: Type = value;
        lines = result.split('\n')
        new_lines = []
        
        for line_num, line in enumerate(lines):
            # Check if this line matches the pattern (with OR without visibility modifier)
            typed_with_init_match = re.match(r'^(\s*)((?:private|public|protected)\s+)?(\w+)\s*:\s*([^=\n;]+?)\s*=\s*([^;\n]+);', line)
            
            if typed_with_init_match and not is_inside_interface(line_num):
                indent = typed_with_init_match.group(1)
                visibility = typed_with_init_match.group(2) if typed_with_init_match.group(2) else ""
                property_name = typed_with_init_match.group(3)
                type_annotation = typed_with_init_match.group(4).strip()
                value = typed_with_init_match.group(5).strip()
                
                # Skip if already an input
                if property_name in input_properties:
                    new_lines.append(line)
                    continue
                
                # Skip if already converted input signal
                if property_name in input_signal_properties:
                    new_lines.append(line)
                    continue
                
                # Skip if this is a getter name (getters will be converted to computed separately)
                if property_name in getter_names:
                    new_lines.append(line)
                    continue
                
                should_convert = (AngularToSignalMigration._is_property_used_in_template(property_name, html_content) or 
                                property_name in properties_used_in_getters or
                                property_name in properties_modified_in_event_handlers)
                
                if should_convert:
                    signal_properties.add(property_name)
                    new_lines.append(f"{indent}{visibility}{property_name} = signal<{type_annotation}>({value});")
                else:
                    new_lines.append(line)
            else:
                new_lines.append(line)
        
        result = '\n'.join(new_lines)
        
        # Then handle property declarations without initializers: property: Type; or property?: Type;
        lines = result.split('\n')
        new_lines = []
        
        for line_num, line in enumerate(lines):
            declaration_match = re.match(r'^(\s*)((?:private|public|protected)\s+)?(\w+)(\??)\s*:\s*([^=;\n]+);', line)
            
            if declaration_match and not is_inside_interface(line_num):
                indent = declaration_match.group(1)
                visibility = declaration_match.group(2) if declaration_match.group(2) else ""
                property_name = declaration_match.group(3)
                optional = declaration_match.group(4)
                type_annotation = declaration_match.group(5).strip()
                
                # Skip if already an input
                if property_name in input_properties:
                    new_lines.append(line)
                    continue
                
                # Skip if already converted input signal
                if property_name in input_signal_properties:
                    new_lines.append(line)
                    continue
                
                # Skip if this is a getter name (getters will be converted to computed separately)
                if property_name in getter_names:
                    new_lines.append(line)
                    continue
                
                should_convert = (AngularToSignalMigration._is_property_used_in_template(property_name, html_content) or 
                                property_name in properties_used_in_getters or
                                property_name in properties_modified_in_event_handlers)
                
                if should_convert:
                    signal_properties.add(property_name)
                    signal_type = f"{type_annotation} | undefined" if optional == "?" else f"{type_annotation} | undefined"
                    new_lines.append(f"{indent}{visibility}{property_name} = signal<{signal_type}>(undefined);")
                else:
                    new_lines.append(line)
            else:
                new_lines.append(line)
        
        result = '\n'.join(new_lines)
        
        return result
    
    @staticmethod
    def _get_html_filename_from_ts(ts_filename: str) -> str:
        """Convert TypeScript filename to HTML filename."""
        if ts_filename.endswith(".ts"):
            base_name = ts_filename[:-3]
            return f"{base_name}.html"
        return ""
    
    @staticmethod
    def _convert_getters_to_computed(content: str, html_content: str) -> str:
        result = content
        
        # Pattern: get propertyName(): ReturnType { body } -> propertyName = computed(() => { body });
        # Use a more robust pattern to capture the complete getter body including nested braces
        pattern = re.compile(r"(?s)(\s*)get\s+(\w+)\(\)(?:\s*:\s*([^\{]*?))?\s*\{((?:[^{}]*|\{[^}]*\})*?)\}")
        
        def replace_getter(match):
            indent = match.group(1)
            property_name = match.group(2)
            return_type = match.group(3)  # May be None
            body = match.group(4)
            
            if AngularToSignalMigration._is_property_used_in_template(property_name, html_content):
                # Transform property references in the getter body
                transformed_body = AngularToSignalMigration._transform_property_references_in_getter_body(body)
                return f"{indent}{property_name} = computed(() => {{{transformed_body}}});"
            else:
                return match.group(0)  # No change
        
        result = pattern.sub(replace_getter, result)
        return result
    
    @staticmethod
    def _transform_property_references_in_getter_body(body: str) -> str:
        result = body
        
        # Universal transformation: this.propertyName -> this.propertyName() 
        # Avoid transforming method calls that already have parentheses
        property_pattern = re.compile(r"\bthis\.(\w+)\b(?!\()")
        
        def replace_property(match):
            property_name = match.group(1)
            return f"this.{property_name}()"
        
        result = property_pattern.sub(replace_property, result)
        
        # Fix chaining by adding optional chaining: this.property().something -> this.property()?.something
        result = re.sub(r"\(\)\.", "()?.", result)
        
        # Add nullish coalescing for common patterns
        if "findIndex" in result and "?? -1" not in result:
            result = re.sub(r"(findIndex\([^}]+\))", r"\1 ?? -1", result)
            
        if "indexOf" in result and "?? -1" not in result:
            result = re.sub(r"(indexOf\([^)]+\))", r"\1 ?? -1", result)
        
        return result
    
    @staticmethod
    def _convert_property_assignments(content: str, signal_properties: Set[str]) -> str:
        result = content
        
        # First, detect additional signal properties by looking for existing signal() calls in the code
        detected_signal_properties = set(signal_properties)
        
        # Look for patterns like "property = signal(" which indicate the property is already a signal
        signal_declaration_pattern = re.compile(r"(\w+)\s*=\s*signal")
        for match in signal_declaration_pattern.finditer(content):
            detected_signal_properties.add(match.group(1))
        
        # Look for patterns like "property = computed(" which indicate the property is a computed signal
        computed_declaration_pattern = re.compile(r"(\w+)\s*=\s*computed")
        for match in computed_declaration_pattern.finditer(content):
            detected_signal_properties.add(match.group(1))
        
        # Look for patterns like this.propertyName() which indicate the property is a signal
        signal_call_pattern = re.compile(r"\bthis\.(\w+)\(\)")
        for match in signal_call_pattern.finditer(content):
            detected_signal_properties.add(match.group(1))
        
        # Pattern: this.propertyName = value; -> this.propertyName.set(value);
        # Also handle: this.propertyName() = value; (incorrect after conversion) -> this.propertyName.set(value);
        # Only convert if the property was converted to a signal or detected as signal usage
        # IMPORTANT: Use single = for assignment, not ==, ===, !=, !==
        assignment_pattern = re.compile(r"(?m)this\.(\w+)(\(\))?\s*=\s*([^=][^;]*);")
        
        def replace_assignment(match):
            property_name = match.group(1)
            has_parens = match.group(2)  # Will be "()" if present, None otherwise
            value = match.group(3)
            
            if property_name in detected_signal_properties:
                return f"this.{property_name}.set({value});"
            else:
                return match.group(0)  # No change
        
        result = assignment_pattern.sub(replace_assignment, result)
        
        # Now handle property usage (reading, not assignment)
        # Convert property usage to function calls for all detected signal properties
        for signal_prop in detected_signal_properties:
            # Instead of negative lookbehind, use a more careful pattern
            # Match this.propertyName but not when followed by ( or = or .set(
            prop_usage_pattern = re.compile(rf"\bthis\.{signal_prop}\b(?!\(|s*=|\s*\.set\()")
            
            # We need to avoid replacing inside getter definitions, so let's do a more manual approach
            # Split by lines and check each line
            lines = result.split('\n')
            new_lines = []
            in_getter = False
            
            for line in lines:
                # Check if we're entering a getter definition for this property
                if re.search(rf"get\s+{signal_prop}\s*\(", line):
                    in_getter = True
                    new_lines.append(line)
                    continue
                
                # Check if we're entering a computed definition for this property (already converted)
                if re.search(rf"{signal_prop}\s*=\s*computed", line):
                    in_getter = True
                    new_lines.append(line)
                    continue
                
                # Check if we're exiting the getter (simplified check)
                if in_getter and (line.strip().startswith('}') or line.strip() == ''):
                    in_getter = False
                    new_lines.append(line)
                    continue
                
                # Only apply the transformation if we're not in a getter
                if not in_getter:
                    line = prop_usage_pattern.sub(f"this.{signal_prop}()", line)
                
                new_lines.append(line)
            
            result = '\n'.join(new_lines)
        
        return result
    
    @staticmethod
    def _find_properties_modified_in_event_handlers(content: str, html_content: str) -> Set[str]:
        properties_modified_in_event_handlers = set()
        
        if not html_content or not html_content.strip():
            return properties_modified_in_event_handlers
        
        # Find event handler method names from HTML
        event_handler_methods = set()
        event_pattern = re.compile(r"\([a-zA-Z]+\)=\"([a-zA-Z]\w*)\([^)]*\)\"")
        for match in event_pattern.finditer(html_content):
            event_handler_methods.add(match.group(1))
        
        # For each event handler method, find properties that are assigned in it
        for method_name in event_handler_methods:
            method_pattern = re.compile(rf"(?s){method_name}\([^)]*\)\s*\{{([^}}]+)\}}")
            method_match = method_pattern.search(content)
            if method_match:
                method_body = method_match.group(1)
                # Find property assignments in this method
                assignment_pattern = re.compile(r"\bthis\.(\w+)\s*=")
                for assignment_match in assignment_pattern.finditer(method_body):
                    properties_modified_in_event_handlers.add(assignment_match.group(1))
        
        return properties_modified_in_event_handlers
    
    @staticmethod
    def _is_property_used_in_template(property_name: str, html_content: str) -> bool:
        if not html_content or not html_content.strip():
            return False  # Conservative approach: if no HTML, don't convert
        
        # Check common Angular template patterns
        return (f"{{ {property_name}" in html_content or
                f"{{{property_name}" in html_content or
                f"[{property_name}]" in html_content or
                f'[(ngModel)]="{property_name}"' in html_content or
                ("*ngFor" in html_content and property_name in html_content) or
                ("*ngIf" in html_content and property_name in html_content) or
                ("(click)" in html_content and property_name in html_content) or
                ("(change)" in html_content and property_name in html_content) or
                f'[disabled]="{property_name}' in html_content or
                ("[class." in html_content and property_name in html_content) or
                ("[style." in html_content and property_name in html_content) or
                ("[attr." in html_content and property_name in html_content) or
                f'"{property_name}"' in html_content or
                re.search(rf"\b{property_name}\b", html_content))
    
    @staticmethod
    def _add_signal_imports(content: str, needs_signal: bool, needs_computed: bool, needs_input: bool) -> str:
        if not needs_signal and not needs_computed and not needs_input:
            return content
        
        # Check if @angular/core import already exists
        import_pattern = re.compile(r"import\s*\{([^}]+)\}\s*from\s*['\"]@angular/core['\"];")
        match = import_pattern.search(content)
        
        if match:
            # Angular core import exists, add signal/computed/input to it
            full_match = match.group(0)
            existing_imports = match.group(1)
            
            # Parse existing imports to preserve formatting
            imports = [imp.strip() for imp in existing_imports.split(",")]
            has_spaces = " " in existing_imports
            separator = ", " if has_spaces else ", "
            
            new_imports = imports[:]
            
            if needs_signal and "signal" not in existing_imports:
                new_imports.append("signal")
            
            if needs_computed and "computed" not in existing_imports:
                new_imports.append("computed")
            
            if needs_input and "input" not in existing_imports:
                new_imports.append("input")
            
            # Preserve the original spacing style
            if has_spaces:
                replacement = full_match.replace(existing_imports, f" {separator.join(new_imports)} ")
            else:
                replacement = full_match.replace(existing_imports, separator.join(new_imports))
            
            return content.replace(full_match, replacement)
        else:
            # No @angular/core import, add it at the top
            imports = []
            if needs_signal:
                imports.append("signal")
            if needs_computed:
                imports.append("computed")
            if needs_input:
                imports.append("input")
            
            import_statement = f"import {{ {', '.join(imports)} }} from '@angular/core';\n"
            return import_statement + content

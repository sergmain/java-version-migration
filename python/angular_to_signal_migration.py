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
        signal_properties = set()
        
        # Get corresponding HTML file content for analysis
        html_content = AngularToSignalMigration._get_html_content(cfg)
        
        # Pattern 1: Convert simple properties used in templates to signals
        before_signals = result
        result = AngularToSignalMigration._convert_properties_to_signals(result, html_content, signal_properties)
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
        result = AngularToSignalMigration._add_signal_imports(result, needs_signal_import, needs_computed_import)
        
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
    def _convert_properties_to_signals(content: str, html_content: str, signal_properties: Set[str]) -> str:
        result = content
        
        # First, find all getters used in the template
        getter_pattern = re.compile(r"(?s)get\s+(\w+)\(\)\s*\{([^}]+)\}")
        getters_used_in_template = set()
        properties_used_in_getters = set()
        
        for match in getter_pattern.finditer(content):
            getter_name = match.group(1)
            getter_body = match.group(2)
            
            if AngularToSignalMigration._is_property_used_in_template(getter_name, html_content):
                getters_used_in_template.add(getter_name)
                # Extract properties used in this getter
                prop_pattern = re.compile(r"this\.(\w+)(?!\()")
                for prop_match in prop_pattern.finditer(getter_body):
                    properties_used_in_getters.add(prop_match.group(1))
        
        # Find event handler methods and properties modified in them
        properties_modified_in_event_handlers = AngularToSignalMigration._find_properties_modified_in_event_handlers(content, html_content)
        
        # Process property declarations in correct order to avoid conflicts
        
        # First handle typed properties with initializers: property: Type = value;
        typed_with_init_pattern = re.compile(r"(?m)^(\s*)((?:private|public|protected)\s+)?(\w+)\s*:\s*([^=\n;]+?)\s*=\s*([^;\n]+);")
        
        def replace_typed_with_init(match):
            indent = match.group(1)
            visibility = match.group(2) if match.group(2) else ""
            property_name = match.group(3)
            type_annotation = match.group(4).strip()
            value = match.group(5).strip()
            
            should_convert = (AngularToSignalMigration._is_property_used_in_template(property_name, html_content) or 
                            property_name in properties_used_in_getters or
                            property_name in properties_modified_in_event_handlers)
            
            if should_convert:
                signal_properties.add(property_name)
                return f"{indent}{visibility}{property_name} = signal<{type_annotation}>({value});"
            else:
                return match.group(0)  # No change
        
        result = typed_with_init_pattern.sub(replace_typed_with_init, result)
        
        # Then handle property declarations without initializers: property: Type; or property?: Type;
        declaration_pattern = re.compile(r"(?m)^(\s*)((?:private|public|protected)\s+)?(\w+)(\??)\s*:\s*([^=;\n]+);")
        
        def replace_declaration(match):
            indent = match.group(1)
            visibility = match.group(2) if match.group(2) else ""
            property_name = match.group(3)
            optional = match.group(4)
            type_annotation = match.group(5).strip()
            
            should_convert = (AngularToSignalMigration._is_property_used_in_template(property_name, html_content) or 
                            property_name in properties_used_in_getters or
                            property_name in properties_modified_in_event_handlers)
            
            if should_convert:
                signal_properties.add(property_name)
                signal_type = f"{type_annotation} | undefined" if optional == "?" else f"{type_annotation} | undefined"
                return f"{indent}{visibility}{property_name} = signal<{signal_type}>(undefined);"
            else:
                return match.group(0)  # No change
        
        result = declaration_pattern.sub(replace_declaration, result)
        
        return result
    
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
        
        # Look for patterns like this.propertyName() which indicate the property is a signal
        signal_call_pattern = re.compile(r"\bthis\.(\w+)\(\)")
        for match in signal_call_pattern.finditer(content):
            detected_signal_properties.add(match.group(1))
        
        # Pattern: this.propertyName = value; -> this.propertyName.set(value);
        # Only convert if the property was converted to a signal or detected as signal usage
        assignment_pattern = re.compile(r"(?m)this\.(\w+)\s*=\s*([^;]+);")
        
        def replace_assignment(match):
            property_name = match.group(1)
            value = match.group(2)
            
            if property_name in detected_signal_properties:
                return f"this.{property_name}.set({value});"
            else:
                return match.group(0)  # No change
        
        result = assignment_pattern.sub(replace_assignment, result)
        
        # Now handle property usage (reading, not assignment)
        # Convert property usage to function calls for all detected signal properties
        for signal_prop in detected_signal_properties:
            # Use negative lookbehind to avoid converting inside getter definitions
            prop_usage_pattern = re.compile(rf"(?<!get\s+{signal_prop}\(\)\s*[:\w\s]*\{{[^}}]*?)\bthis\.{signal_prop}\b(?!\(\)|s*=|\s*\.set\()")
            result = prop_usage_pattern.sub(f"this.{signal_prop}()", result)
        
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
    def _add_signal_imports(content: str, needs_signal: bool, needs_computed: bool) -> str:
        if not needs_signal and not needs_computed:
            return content
        
        # Check if @angular/core import already exists
        import_pattern = re.compile(r"import\s*\{([^}]+)\}\s*from\s*['\"]@angular/core['\"];")
        match = import_pattern.search(content)
        
        if match:
            # Angular core import exists, add signal/computed to it
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
            
            import_statement = f"import {{ {', '.join(imports)} }} from '@angular/core';\n"
            return import_statement + content

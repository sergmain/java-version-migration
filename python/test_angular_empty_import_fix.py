import unittest
from pathlib import Path
from angular_empty_import_fix import AngularEmptyImportFix, MigrationConfig, Content


class TestAngularEmptyImportFix(unittest.TestCase):
    """Unit tests for AngularEmptyImportFix class."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.config = MigrationConfig(
            path=Path("test.component.ts"),
            files={}
        )
    
    def test_simple_component_without_imports(self):
        """Test adding imports to a simple component without imports property."""
        content = """@Component({
    selector: 'billing',
    templateUrl: './billing.component.html',
    styleUrls: ['./billing.component.scss']
})"""
        
        expected = """@Component({
    imports: [],
    selector: 'billing',
    templateUrl: './billing.component.html',
    styleUrls: ['./billing.component.scss']
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertEqual(result.content, expected)
        self.assertTrue(result.changed)
    
    def test_component_with_existing_imports(self):
        """Test that component with imports is not modified."""
        content = """@Component({
    imports: [CommonModule, FormsModule],
    selector: 'billing',
    templateUrl: './billing.component.html',
    styleUrls: ['./billing.component.scss']
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertEqual(result.content, content)
        self.assertFalse(result.changed)
    
    def test_component_with_empty_imports(self):
        """Test that component with empty imports array is not modified."""
        content = """@Component({
    imports: [],
    selector: 'billing',
    templateUrl: './billing.component.html',
    styleUrls: ['./billing.component.scss']
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertEqual(result.content, content)
        self.assertFalse(result.changed)
    
    def test_component_with_tabs_indentation(self):
        """Test adding imports with tab indentation."""
        content = """@Component({
\tselector: 'billing',
\ttemplateUrl: './billing.component.html',
\tstyleUrls: ['./billing.component.scss']
})"""
        
        expected = """@Component({
\timports: [],
\tselector: 'billing',
\ttemplateUrl: './billing.component.html',
\tstyleUrls: ['./billing.component.scss']
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertEqual(result.content, expected)
        self.assertTrue(result.changed)
    
    def test_component_inline_on_single_line(self):
        """Test component with configuration on a single line."""
        content = """@Component({ selector: 'billing', templateUrl: './billing.component.html' })"""
        
        # Should add imports after the opening brace
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertIn('imports: []', result.content)
        self.assertTrue(result.changed)
    
    def test_multiple_components_in_file(self):
        """Test file with multiple @Component decorators."""
        content = """@Component({
    selector: 'first',
    templateUrl: './first.component.html'
})
export class FirstComponent {}

@Component({
    selector: 'second',
    templateUrl: './second.component.html'
})
export class SecondComponent {}"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        # Both components should get imports
        self.assertEqual(result.content.count('imports: []'), 2)
        self.assertTrue(result.changed)
    
    def test_component_with_nested_objects(self):
        """Test component with nested objects in configuration."""
        content = """@Component({
    selector: 'app-test',
    templateUrl: './test.component.html',
    styleUrls: ['./test.component.scss'],
    animations: [
        trigger('fade', [
            transition(':enter', [
                style({ opacity: 0 }),
                animate('300ms', style({ opacity: 1 }))
            ])
        ])
    ]
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertIn('imports: []', result.content)
        self.assertTrue(result.changed)
        # Verify the structure is maintained
        self.assertIn('animations:', result.content)
        self.assertIn('trigger', result.content)
    
    def test_component_with_template_literal(self):
        """Test component with template literals containing braces."""
        content = """@Component({
    selector: 'app-test',
    template: `
        <div>{{ value }}</div>
        <button (click)="onClick()">Click</button>
    `,
    styleUrls: ['./test.component.scss']
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertIn('imports: []', result.content)
        self.assertTrue(result.changed)
        # Verify template is preserved
        self.assertIn('{{ value }}', result.content)
        self.assertIn('(click)=', result.content)
    
    def test_component_with_comments(self):
        """Test component with comments in configuration."""
        content = """@Component({
    // This is the selector
    selector: 'app-test',
    /* Multi-line comment
       about template */
    templateUrl: './test.component.html'
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertIn('imports: []', result.content)
        self.assertTrue(result.changed)
        # Verify comments are preserved
        self.assertIn('// This is the selector', result.content)
        self.assertIn('/* Multi-line comment', result.content)
    
    def test_component_with_string_containing_imports_word(self):
        """Test that 'imports' in strings doesn't count as having imports property."""
        content = """@Component({
    selector: 'app-test',
    template: 'This template imports data',
    styleUrls: ['./test.component.scss']
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        # Should still add imports property
        self.assertIn('imports: []', result.content)
        self.assertTrue(result.changed)
    
    def test_empty_content(self):
        """Test with empty content."""
        content = ""
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertEqual(result.content, "")
        self.assertFalse(result.changed)
    
    def test_whitespace_only_content(self):
        """Test with whitespace-only content."""
        content = "   \n\n   \t\t  "
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertEqual(result.content, content)
        self.assertFalse(result.changed)
    
    def test_component_without_closing_brace(self):
        """Test malformed component without closing brace."""
        content = """@Component({
    selector: 'app-test',
    templateUrl: './test.component.html'
"""
        
        # Should handle gracefully without crashing
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        # Content should remain unchanged since we can't find matching brace
        self.assertEqual(result.content, content)
        self.assertFalse(result.changed)
    
    def test_component_with_extra_spaces_around_decorator(self):
        """Test component with extra spaces around @Component."""
        content = """@Component  (  {
    selector: 'app-test',
    templateUrl: './test.component.html'
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertIn('imports: []', result.content)
        self.assertTrue(result.changed)
    
    def test_component_with_standalone_true(self):
        """Test component with standalone: true property."""
        content = """@Component({
    standalone: true,
    selector: 'app-test',
    templateUrl: './test.component.html',
    styleUrls: ['./test.component.scss']
})"""
        
        expected = """@Component({
    imports: [],
    standalone: true,
    selector: 'app-test',
    templateUrl: './test.component.html',
    styleUrls: ['./test.component.scss']
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertEqual(result.content, expected)
        self.assertTrue(result.changed)
    
    def test_has_imports_property(self):
        """Test _has_imports_property method."""
        # Should find imports property
        self.assertTrue(AngularEmptyImportFix._has_imports_property("imports: []"))
        self.assertTrue(AngularEmptyImportFix._has_imports_property("imports : [CommonModule]"))
        self.assertTrue(AngularEmptyImportFix._has_imports_property("  imports:[]\n"))
        
        # Should not find imports in other contexts
        self.assertFalse(AngularEmptyImportFix._has_imports_property("selector: 'test'"))
        self.assertFalse(AngularEmptyImportFix._has_imports_property("// imports: []"))
        self.assertFalse(AngularEmptyImportFix._has_imports_property("myImports: []"))
    
    def test_find_matching_closing_brace_simple(self):
        """Test _find_matching_closing_brace with simple content."""
        content = "{ selector: 'test' }"
        # Position 1 is right after the opening {
        result = AngularEmptyImportFix._find_matching_closing_brace(content, 1)
        self.assertEqual(result, 19)  # Position of closing }
    
    def test_find_matching_closing_brace_nested(self):
        """Test _find_matching_closing_brace with nested braces."""
        content = "{ obj: { nested: true } }"
        result = AngularEmptyImportFix._find_matching_closing_brace(content, 1)
        self.assertEqual(result, 24)  # Position of outer closing }
    
    def test_find_matching_closing_brace_with_strings(self):
        """Test _find_matching_closing_brace with braces in strings."""
        content = "{ template: 'has } brace' }"
        result = AngularEmptyImportFix._find_matching_closing_brace(content, 1)
        self.assertEqual(result, 26)  # Should ignore } in string
    
    def test_component_with_multiline_array(self):
        """Test component with multiline array properties."""
        content = """@Component({
    selector: 'app-test',
    styleUrls: [
        './test.component.scss',
        './test-theme.scss'
    ],
    templateUrl: './test.component.html'
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertIn('imports: []', result.content)
        self.assertTrue(result.changed)
        # Verify multiline array is preserved
        self.assertIn("'./test.component.scss'", result.content)
        self.assertIn("'./test-theme.scss'", result.content)
    
    def test_component_with_complex_providers(self):
        """Test component with complex providers configuration."""
        content = """@Component({
    selector: 'app-test',
    templateUrl: './test.component.html',
    providers: [
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AuthInterceptor,
            multi: true
        }
    ]
})"""
        
        result = AngularEmptyImportFix.process(self.config, self.globals_config, content)
        self.assertIn('imports: []', result.content)
        self.assertTrue(result.changed)
        # Verify providers structure is maintained
        self.assertIn('HTTP_INTERCEPTORS', result.content)
        self.assertIn('multi: true', result.content)


class TestIntegration(unittest.TestCase):
    """Integration tests for real-world scenarios."""
    
    def test_full_component_file(self):
        """Test processing a complete component file."""
        content = """import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-billing',
    templateUrl: './billing.component.html',
    styleUrls: ['./billing.component.scss']
})
export class BillingComponent implements OnInit {
    constructor() {}
    
    ngOnInit(): void {
        console.log('Component initialized');
    }
}
"""
        
        config = MigrationConfig(path=Path("billing.component.ts"), files={})
        result = AngularEmptyImportFix.process(config, None, content)
        
        # Should add imports to the component
        self.assertIn('imports: []', result.content)
        self.assertTrue(result.changed)
        
        # Should preserve the rest of the file
        self.assertIn('export class BillingComponent', result.content)
        self.assertIn('ngOnInit(): void', result.content)
        self.assertIn("console.log('Component initialized')", result.content)
    
    def test_standalone_component_migration(self):
        """Test migrating a standalone component."""
        content = """import { Component } from '@angular/core';

@Component({
    standalone: true,
    selector: 'app-user-profile',
    templateUrl: './user-profile.component.html',
    styleUrls: ['./user-profile.component.scss']
})
export class UserProfileComponent {
    userName = 'John Doe';
}
"""
        
        config = MigrationConfig(path=Path("user-profile.component.ts"), files={})
        result = AngularEmptyImportFix.process(config, None, content)
        
        self.assertTrue(result.changed)
        # imports should be added as first property
        lines = result.content.split('\n')
        component_line_idx = next(i for i, line in enumerate(lines) if '@Component' in line)
        # Find the line with 'imports:'
        imports_line_idx = next(i for i, line in enumerate(lines) if 'imports:' in line)
        # Find the line with 'standalone:'
        standalone_line_idx = next(i for i, line in enumerate(lines) if 'standalone:' in line)
        
        # imports should come before standalone
        self.assertLess(imports_line_idx, standalone_line_idx)


def run_tests():
    """Run all tests."""
    # Create test suite
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # Add all test cases
    suite.addTests(loader.loadTestsFromTestCase(TestAngularEmptyImportFix))
    suite.addTests(loader.loadTestsFromTestCase(TestIntegration))
    
    # Run tests with verbose output
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    # Return exit code (0 for success, 1 for failure)
    return 0 if result.wasSuccessful() else 1


if __name__ == '__main__':
    exit_code = run_tests()
    exit(exit_code)

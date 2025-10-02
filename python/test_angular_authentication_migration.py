import unittest
from pathlib import Path
from angular_authentication_migration import AngularAuthenticationMigration, MigrationConfig, Content


class TestAngularAuthenticationMigration(unittest.TestCase):
    """Unit tests for AngularAuthenticationMigration class."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.config = MigrationConfig(
            path=Path("test.component.ts"),
            files={}
        )
    
    def test_simple_inject_migration(self):
        """Test migrating a simple inject property to constructor parameter."""
        content = """export class TestComponent {
    private authService = inject(AuthenticationService);

    constructor() {
        super(this.authService);
    }
}"""
        
        expected = """export class TestComponent {

    constructor(private authService: AuthenticationService = inject(AuthenticationService)) {
        super(authService);
    }
}"""
        
        result = AngularAuthenticationMigration.process(self.config, content)
        self.assertEqual(result.content, expected)
        self.assertTrue(result.changed)
    
    def test_multiple_inject_properties(self):
        """Test migrating multiple inject properties."""
        content = """export class AiRootComponent {
    private router = inject(Router);
    private settingsService = inject(SettingsService);
    public readonly authenticationService = inject(AuthenticationService);

    constructor() {
        super(this.authenticationService);
    }
}"""
        
        result = AngularAuthenticationMigration.process(self.config, content)
        
        # Verify properties are removed
        self.assertNotIn('private router = inject(Router);', result.content)
        self.assertNotIn('private settingsService = inject(SettingsService);', result.content)
        self.assertNotIn('public readonly authenticationService = inject(AuthenticationService);', result.content)
        
        # Verify constructor has parameters
        self.assertIn('private router: Router = inject(Router)', result.content)
        self.assertIn('private settingsService: SettingsService = inject(SettingsService)', result.content)
        self.assertIn('public readonly authenticationService: AuthenticationService = inject(AuthenticationService)', result.content)
        
        # Verify super call is updated
        self.assertIn('super(authenticationService)', result.content)
        self.assertNotIn('super(this.authenticationService)', result.content)
        
        self.assertTrue(result.changed)
    
    def test_constructor_with_existing_parameters(self):
        """Test adding inject parameters when constructor already has parameters."""
        content = """export class TestComponent {
    private authService = inject(AuthenticationService);

    constructor(private existingParam: string) {
        super(this.authService);
    }
}"""
        
        result = AngularAuthenticationMigration.process(self.config, content)
        
        # Verify new parameter is added
        self.assertIn('private authService: AuthenticationService = inject(AuthenticationService)', result.content)
        
        # Verify existing parameter is preserved
        self.assertIn('private existingParam: string', result.content)
        
        self.assertTrue(result.changed)
    
    def test_no_constructor(self):
        """Test when there's no constructor - should not migrate."""
        content = """export class TestComponent {
    private authService = inject(AuthenticationService);
}"""
        
        result = AngularAuthenticationMigration.process(self.config, content)
        
        # Should not change if no constructor
        self.assertEqual(result.content, content)
        self.assertFalse(result.changed)
    
    def test_no_inject_properties(self):
        """Test when there are no inject properties."""
        content = """export class TestComponent {
    private normalProperty: string;

    constructor() {
        // Empty constructor
    }
}"""
        
        result = AngularAuthenticationMigration.process(self.config, content)
        self.assertEqual(result.content, content)
        self.assertFalse(result.changed)
    
    def test_empty_content(self):
        """Test with empty content."""
        content = ""
        result = AngularAuthenticationMigration.process(self.config, content)
        self.assertEqual(result.content, "")
        self.assertFalse(result.changed)
    
    def test_readonly_inject_property(self):
        """Test migrating readonly inject property."""
        content = """export class TestComponent {
    public readonly authService = inject(AuthenticationService);

    constructor() {
        super(this.authService);
    }
}"""
        
        result = AngularAuthenticationMigration.process(self.config, content)
        
        # Verify readonly is preserved
        self.assertIn('public readonly authService: AuthenticationService = inject(AuthenticationService)', result.content)
        self.assertTrue(result.changed)
    
    def test_protected_inject_property(self):
        """Test migrating protected inject property."""
        content = """export class TestComponent {
    protected authService = inject(AuthenticationService);

    constructor() {
    }
}"""
        
        result = AngularAuthenticationMigration.process(self.config, content)
        
        # Verify protected visibility is preserved
        self.assertIn('protected authService: AuthenticationService = inject(AuthenticationService)', result.content)
        self.assertTrue(result.changed)
    
    def test_no_visibility_modifier(self):
        """Test migrating inject property without visibility modifier."""
        content = """export class TestComponent {
    authService = inject(AuthenticationService);

    constructor() {
    }
}"""
        
        result = AngularAuthenticationMigration.process(self.config, content)
        
        # Should default to private
        self.assertIn('private authService: AuthenticationService = inject(AuthenticationService)', result.content)
        self.assertTrue(result.changed)
    
    def test_super_call_without_this(self):
        """Test when super() call doesn't use this.property."""
        content = """export class TestComponent {
    private authService = inject(AuthenticationService);

    constructor() {
        super();
    }
}"""
        
        result = AngularAuthenticationMigration.process(self.config, content)
        
        # super() should remain unchanged
        self.assertIn('super()', result.content)
        self.assertTrue(result.changed)
    
    def test_find_inject_properties(self):
        """Test _find_inject_properties method."""
        content = """
    private router = inject(Router);
    public readonly authService = inject(AuthenticationService);
    protected settingsService = inject(SettingsService);
    serviceWithoutVisibility = inject(SomeService);
"""
        
        properties = AngularAuthenticationMigration._find_inject_properties(content)
        
        self.assertEqual(len(properties), 4)
        
        # Check first property
        self.assertEqual(properties[0]['visibility'], 'private')
        self.assertEqual(properties[0]['name'], 'router')
        self.assertEqual(properties[0]['type'], 'Router')
        self.assertFalse(properties[0]['readonly'])
        
        # Check second property
        self.assertEqual(properties[1]['visibility'], 'public')
        self.assertEqual(properties[1]['name'], 'authService')
        self.assertEqual(properties[1]['type'], 'AuthenticationService')
        self.assertTrue(properties[1]['readonly'])
        
        # Check third property
        self.assertEqual(properties[2]['visibility'], 'protected')
        self.assertEqual(properties[2]['name'], 'settingsService')
        self.assertEqual(properties[2]['type'], 'SettingsService')
        
        # Check fourth property (no visibility)
        self.assertIsNone(properties[3]['visibility'])
        self.assertEqual(properties[3]['name'], 'serviceWithoutVisibility')
        self.assertEqual(properties[3]['type'], 'SomeService')
    
    def test_split_parameters(self):
        """Test _split_parameters method with various parameter formats."""
        # Simple parameters
        params1 = "param1: string, param2: number"
        result1 = AngularAuthenticationMigration._split_parameters(params1)
        self.assertEqual(len(result1), 2)
        self.assertEqual(result1[0], "param1: string")
        self.assertEqual(result1[1], "param2: number")
        
        # Parameters with generics
        params2 = "map: Map<string, number>, list: Array<Item>"
        result2 = AngularAuthenticationMigration._split_parameters(params2)
        self.assertEqual(len(result2), 2)
        self.assertEqual(result2[0], "map: Map<string, number>")
        self.assertEqual(result2[1], "list: Array<Item>")
        
        # Empty parameters
        params3 = ""
        result3 = AngularAuthenticationMigration._split_parameters(params3)
        self.assertEqual(len(result3), 0)
    
    def test_complex_real_world_example(self):
        """Test the exact example from requirements."""
        content = """export class AiRootComponent extends UIStateComponent implements OnInit, OnDestroy {
    private router = inject(Router);
    private settingsService = inject(SettingsService);
    public readonly authenticationService = inject(AuthenticationService);
    settings: Settings;
    sidenavOpened: boolean;

    constructor() {
        super(this.authenticationService);
        this.router.routeReuseStrategy.shouldReuseRoute = () => false;
    }
}"""
        
        result = AngularAuthenticationMigration.process(self.config, content)
        
        # Verify inject properties are removed
        self.assertNotIn('private router = inject(Router);', result.content)
        self.assertNotIn('private settingsService = inject(SettingsService);', result.content)
        self.assertNotIn('public readonly authenticationService = inject(AuthenticationService);', result.content)
        
        # Verify constructor has all parameters
        self.assertIn('constructor(', result.content)
        self.assertIn('private router: Router = inject(Router)', result.content)
        self.assertIn('private settingsService: SettingsService = inject(SettingsService)', result.content)
        self.assertIn('public readonly authenticationService: AuthenticationService = inject(AuthenticationService)', result.content)
        
        # Verify super call uses parameter name
        self.assertIn('super(authenticationService)', result.content)
        self.assertNotIn('super(this.authenticationService)', result.content)
        
        # Verify other properties are preserved
        self.assertIn('settings: Settings;', result.content)
        self.assertIn('sidenavOpened: boolean;', result.content)
        
        # Verify constructor body is preserved
        self.assertIn('this.router.routeReuseStrategy.shouldReuseRoute = () => false;', result.content)
        
        self.assertTrue(result.changed)


class TestIntegration(unittest.TestCase):
    """Integration tests for real-world scenarios."""
    
    def test_full_component_migration(self):
        """Test migrating a complete component file."""
        content = """import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationService } from './auth.service';
import { inject } from '@angular/core';

export class AiRootComponent extends UIStateComponent implements OnInit {
    private router = inject(Router);
    public readonly authenticationService = inject(AuthenticationService);

    constructor() {
        super(this.authenticationService);
    }

    ngOnInit(): void {
        this.router.navigate(['/home']);
    }
}
"""
        
        config = MigrationConfig(path=Path("ai-root.component.ts"), files={})
        result = AngularAuthenticationMigration.process(config, content)
        
        # Should migrate inject properties to constructor
        self.assertIn('constructor(', result.content)
        self.assertIn('private router: Router = inject(Router)', result.content)
        self.assertIn('public readonly authenticationService: AuthenticationService = inject(AuthenticationService)', result.content)
        
        # Should update super call
        self.assertIn('super(authenticationService)', result.content)
        
        # Should preserve other content
        self.assertIn('ngOnInit(): void', result.content)
        self.assertIn("this.router.navigate(['/home']);", result.content)
        
        self.assertTrue(result.changed)


def run_tests():
    """Run all tests."""
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    suite.addTests(loader.loadTestsFromTestCase(TestAngularAuthenticationMigration))
    suite.addTests(loader.loadTestsFromTestCase(TestIntegration))
    
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    return 0 if result.wasSuccessful() else 1


if __name__ == '__main__':
    exit_code = run_tests()
    exit(exit_code)

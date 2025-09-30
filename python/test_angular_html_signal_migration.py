import unittest
from pathlib import Path
from angular_html_signal_migration import AngularHtmlSignalMigration, MigrationConfig


class TestAngularHtmlSignalMigration(unittest.TestCase):
    
    def setUp(self):
        """Set up test fixtures."""
        self.test_ts_content = """
import { Component, signal, computed, input } from '@angular/core';

@Component({
    selector: 'test-component',
    templateUrl: './test.component.html'
})
export class TestComponent {
    rows = signal<number>(5);
    cols = signal<number>(5);
    kakuro = signal<string>('test');
    activeField = input<any>();
    isGenerating = signal<boolean>(false);
    currentRow = computed(() => this.rows() + 1);
}
"""
    
    def create_config(self, html_content: str) -> MigrationConfig:
        """Helper to create a MigrationConfig for testing."""
        return MigrationConfig(
            path=Path("test.component.html"),
            files={
                "test.component.ts": self.test_ts_content,
                "test.component.html": html_content
            }
        )
    
    def test_interpolation_simple(self):
        """Test that signals in {{ }} interpolations get () added."""
        html = "<h2>{{rows}}</h2>"
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual("<h2>{{rows()}}</h2>", result)
    
    def test_interpolation_with_translation(self):
        """Test that signals after translations in {{ }} get () added."""
        html = "<h2>{{ 'label' | translate }} {{cols}}</h2>"
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual("<h2>{{ 'label' | translate }} {{cols()}}</h2>", result)
    
    def test_string_inside_interpolation_not_modified(self):
        """Test that strings inside {{ }} are not modified."""
        html = "<h2>{{ 'kakuro.Rows' | translate }}</h2>"
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual("<h2>{{ 'kakuro.Rows' | translate }}</h2>", result)
    
    def test_property_binding(self):
        """Test that signals in property bindings get () added."""
        html = '<button [disabled]="isGenerating">Click</button>'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual('<button [disabled]="isGenerating()">Click</button>', result)
    
    def test_property_binding_name_not_modified(self):
        """Test that property names in [prop]="..." are not modified."""
        html = '<component [activeField]="activeField"></component>'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        # The binding name should not get (), only the value
        self.assertEqual('<component [activeField]="activeField()"></component>', result)
    
    def test_two_way_binding_conversion(self):
        """Test that [(ngModel)] is converted to one-way binding + event."""
        html = '<input [(ngModel)]="rows">'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual('<input [ngModel]="rows()" (ngModelChange)="rows.set($event)">', result)
    
    def test_two_way_binding_input_signal_not_converted(self):
        """Test that [(ngModel)] for input signals is NOT converted to .set() but gets () added."""
        html = '<input [(ngModel)]="activeField">'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        # Input signals don't support two-way binding, so we just add () and let Angular error
        # This alerts the developer they need to change to one-way binding
        self.assertEqual('<input [(ngModel)]="activeField()">', result)
    
    def test_signal_set_not_modified(self):
        """Test that signal.set() calls are not given extra ()."""
        html = '<input (ngModelChange)="rows.set($event)">'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual('<input (ngModelChange)="rows.set($event)">', result)
    
    def test_object_literal_key_not_modified(self):
        """Test that object literal keys are not modified."""
        html = '<p>{{ data | translate: { rows: rows } }}</p>'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        # Key should not get (), but value should
        self.assertEqual('<p>{{ data | translate: { rows: rows() } }}</p>', result)
    
    def test_object_literal_in_interpolation(self):
        """Test object literals inside interpolations."""
        html = '<p>{{ "label" | translate: { cols: cols, rows: rows } }}</p>'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual('<p>{{ "label" | translate: { cols: cols(), rows: rows() } }}</p>', result)
    
    def test_html_tag_name_not_modified(self):
        """Test that HTML tag names are not modified."""
        # This would be unusual but let's make sure
        html = '<rows-component></rows-component>'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual('<rows-component></rows-component>', result)
    
    def test_already_has_parentheses(self):
        """Test that signals already with () are not modified."""
        html = '<h2>{{rows()}}</h2>'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual('<h2>{{rows()}}</h2>', result)
    
    def test_computed_signal(self):
        """Test that computed signals get () added."""
        html = '<span>{{currentRow}}</span>'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual('<span>{{currentRow()}}</span>', result)
    
    def test_input_signal(self):
        """Test that input signals get () added."""
        html = '<div>{{activeField}}</div>'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual('<div>{{activeField()}}</div>', result)
    
    def test_multiple_signals_on_same_line(self):
        """Test that multiple signals on the same line are all converted."""
        html = '<h2>{{ "label" | translate }} {{rows}} x {{cols}}</h2>'
        cfg = self.create_config(html)
        result = AngularHtmlSignalMigration.migrate_html_to_signals(cfg, html)
        self.assertEqual('<h2>{{ "label" | translate }} {{rows()}} x {{cols()}}</h2>', result)


if __name__ == '__main__':
    unittest.main()

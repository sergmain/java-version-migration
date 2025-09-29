import unittest
from pathlib import Path
from angular_to_signal_migration import AngularToSignalMigration, MigrationConfig


class TestAngularToSignalMigration(unittest.TestCase):

    def create_config(self, file_name: str, ts_content: str, html_content: str) -> MigrationConfig:
        ts_path = Path(file_name)
        base_name = ts_path.stem
        
        files = {
            f"{base_name}.ts": ts_content,
            f"{base_name}.html": html_content
        }
        
        return MigrationConfig(ts_path, files)

    def create_config_without_html(self, file_name: str, ts_content: str) -> MigrationConfig:
        ts_path = Path(file_name)
        base_name = ts_path.stem
        
        files = {
            f"{base_name}.ts": ts_content
        }
        
        return MigrationConfig(ts_path, files)

    def test_regex_replacement_with_curly_braces(self):
        html_content = """
            <button (click)="updateData()">Update</button>
            """
        
        ts_content = """
            export class TestComponent {
                private data: any = null;
                
                updateData() {
                    this.data = { clicked: true, timestamp: new Date() };
                    const obj = { key: 'value', nested: { inner: this.data } };
                }
            }
            """
        
        cfg = self.create_config("test.component.ts", ts_content, html_content)
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, ts_content)
        
        expected = """
            import { signal } from '@angular/core';
            export class TestComponent {
                private data = signal<any>(null);
                
                updateData() {
                    this.data.set({ clicked: true, timestamp: new Date() });
                    const obj = { key: 'value', nested: { inner: this.data() } };
                }
            }
            """
        
        self.assertEqual(expected, result)

    def test_simple_property(self):
        html_content = """
            <div>{{ items }}</div>
            """
        
        ts_content = """
            export class TestComponent {
                private items: any[] = [];
            }
            """
        
        cfg = self.create_config("test.component.ts", ts_content, html_content)
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, ts_content)
        
        expected = """
            import { signal } from '@angular/core';
            export class TestComponent {
                private items = signal<any[]>([]);
            }
            """
        
        self.assertEqual(expected, result)

    def test_property_not_used_in_template(self):
        html_content = """
            <div>Static content</div>
            """
        
        ts_content = """
            export class TestComponent {
                private items: any[] = [];
            }
            """
        
        cfg = self.create_config("test.component.ts", ts_content, html_content)
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, ts_content)
        
        # Should not change since property is not used in template
        self.assertEqual(ts_content, result)

    def test_getter_to_computed(self):
        html_content = """
            <div>{{ filteredItems }}</div>
            """
        
        ts_content = """
            export class TestComponent {
                get filteredItems() { return this.items.filter(item => item.active); }
            }
            """
        
        cfg = self.create_config("test.component.ts", ts_content, html_content)
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, ts_content)
        
        expected = """
            import { computed } from '@angular/core';
            export class TestComponent {
                filteredItems = computed(() => {
                    return this.items().filter(item => item.active);
                });
            }
            """
        
        self.assertEqual(expected, result)

    def test_property_assignment(self):
        ts_content = """
            export class TestComponent {
                updateItems() {
                    this.items = newItems;
                    this.items().forEach(item => console.log(item));
                }
            }
            """
        
        cfg = self.create_config("test.component.ts", ts_content, "")
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, ts_content)
        
        expected = """
            export class TestComponent {
                updateItems() {
                    this.items.set(newItems);
                    this.items().forEach(item => console.log(item));
                }
            }
            """
        
        self.assertEqual(expected, result)

    def test_existing_angular_import(self):
        html_content = """
            <div>{{ data }}</div>
            """
        
        ts_content = """
            import { Component, OnInit } from '@angular/core';
            
            export class TestComponent {
                private data: string = 'test';
            }
            """
        
        cfg = self.create_config("test.component.ts", ts_content, html_content)
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, ts_content)
        
        expected = """
            import { Component, OnInit, signal } from '@angular/core';
            
            export class TestComponent {
                private data = signal<string>('test');
            }
            """
        
        self.assertEqual(expected, result)

    def test_kakuro_design_component(self):
        html_content = """
            <div>
                <mat-slider [(ngModel)]="cols"></mat-slider>
                <mat-slider [(ngModel)]="rows"></mat-slider>
                <button (click)="generate()" [disabled]="isGenerating">Generate</button>
                <mat-slide-toggle [(ngModel)]="showDigits">Show Digits</mat-slide-toggle>
                <puzzle-grid [kakuro]="kakuro" [activeField]="activeField" (activeFieldChange)="onActiveField($event)"></puzzle-grid>
                <div>Current position: row {{ currentRow }}, col {{ currentCol }}</div>
            </div>
            """

        ts_content = """
            const MIN_SIZE: number = 3;

            export class KakuroDesignComponent {
                activeField?: Cell;

                kakuro: CellMatrix;

                cols: number = MIN_SIZE;
                rows: number = MIN_SIZE;
                isGenerating: boolean = false;

                showDigits: boolean = true;

                get currentRow(): number {
                    return this.kakuro.cells.findIndex(row => row.indexOf(this.activeField) !== -1);
                }

                get currentCol(): number {
                    if (!this.activeField || this.currentRow === -1) {
                        return -1;
                    }

                    return this.kakuro.cells[this.currentRow].indexOf(this.activeField);
                }

                generate() {
                    this.isGenerating = true;
                    // generation logic
                    this.isGenerating = false;
                }

                onActiveField(cell: CellWithRowCol) {
                    this.activeField = cell.cell;
                }

                updateColumns() {
                    this.cols = this.cols + 1;
                }
            }
            """

        cfg = self.create_config("kakuro-design.component.ts", ts_content, html_content)
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, ts_content)

        expected = """
            import { signal, computed } from '@angular/core';
            const MIN_SIZE: number = 3;

            export class KakuroDesignComponent {
                activeField = signal<Cell | undefined>(undefined);

                kakuro = signal<CellMatrix | undefined>(undefined);

                cols = signal<number>(MIN_SIZE);
                rows = signal<number>(MIN_SIZE);
                isGenerating = signal<boolean>(false);

                showDigits = signal<boolean>(true);

                currentRow = computed(() => {
                    return this.kakuro()?.cells.findIndex(row => row.indexOf(this.activeField()) !== -1) ?? -1;
                });

                currentCol = computed(() => {
                    if (!this.activeField() || this.currentRow() === -1) {
                        return -1;
                    }

                    return this.kakuro()?.cells[this.currentRow()]?.indexOf(this.activeField()) ?? -1;
                });

                generate() {
                    this.isGenerating.set(true);
                    // generation logic
                    this.isGenerating.set(false);
                }

                onActiveField(cell: CellWithRowCol) {
                    this.activeField.set(cell.cell);
                }

                updateColumns() {
                    this.cols.set(this.cols() + 1);
                }
            }
            """

        self.assertEqual(expected, result)

    def test_empty_content(self):
        cfg = self.create_config("test.component.ts", "", "")
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, "")
        self.assertEqual("", result)

    def test_no_html_file(self):
        ts_content = """
            export class TestComponent {
                private items: any[] = [];
            }
            """
        
        cfg = self.create_config_without_html("test.component.ts", ts_content)
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, ts_content)
        
        # Should not change since no HTML file is available
        self.assertEqual(ts_content, result)

    def test_real_world_escape_sequences(self):
        html_content = """
            <div>{{ expert10x10number }}</div>
            <button (click)="createExpertTemplate()">Create Expert</button>
            """
        
        ts_content = """
            const EXPERT_10_X_10 : string =
            `-1 -1 -1 -1 -1 -1 -1 -1 -1 -1
            -1  0  0  9  0  5  0  0  1  0`;
            
            export class KakuroDesignComponent {
                expert10x10number : number[][];
                
                constructor() {
                    this.expert10x10number = EXPERT_10_X_10
                        .split('\\n')
                        .map(row => row.trim().split(/\\s+/).map(Number));
                }
                
                createExpertTemplate(event?: Event) {
                    const matrix: number[][] = this.expert10x10number;
                    this.expert10x10number = matrix.map(row => [...row]);
                }
            }
            """
        
        cfg = self.create_config("kakuro-design.component.ts", ts_content, html_content)
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, ts_content)
        
        expected = """
            const EXPERT_10_X_10 : string =
            `-1 -1 -1 -1 -1 -1 -1 -1 -1 -1
            -1  0  0  9  0  5  0  0  1  0`;
            
            export class KakuroDesignComponent {
                expert10x10number : number[][];
                
                constructor() {
                    this.expert10x10number = EXPERT_10_X_10
                        .split('\\n')
                        .map(row => row.trim().split(/\\s+/).map(Number));
                }
                
                createExpertTemplate(event?: Event) {
                    const matrix: number[][] = this.expert10x10number;
                    this.expert10x10number = matrix.map(row => [...row]);
                }
            }
            """
        
        self.assertEqual(expected, result)

    def test_complex_types(self):
        html_content = """
            <div>{{ userProfile.name }}</div>
            <div *ngFor="let item of itemList">{{ item }}</div>
            """
        
        ts_content = """
            export interface UserProfile {
                name: string;
                email: string;
            }
            
            export class TestComponent {
                public userProfile: UserProfile = { name: 'John', email: 'john@example.com' };
                private itemList: string[] = ['item1', 'item2'];
                protected complexData: Map<string, any> = new Map();
            }
            """
        
        cfg = self.create_config("test.component.ts", ts_content, html_content)
        result = AngularToSignalMigration.migrate_angular_to_signal_migration(cfg, ts_content)
        
        expected = """
            import { signal } from '@angular/core';
            export interface UserProfile {
                name: string;
                email: string;
            }
            
            export class TestComponent {
                public userProfile = signal<UserProfile>({ name: 'John', email: 'john@example.com' });
                private itemList = signal<string[]>(['item1', 'item2']);
                protected complexData: Map<string, any> = new Map();
            }
            """
        
        self.assertEqual(expected, result)


if __name__ == '__main__':
    unittest.main()

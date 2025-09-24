/*
 * Copyright (c) 2025. Sergio Lissner
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package metaheuristic.java_version_migration.migrations;

import metaheuristic.java_version_migration.Migration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Sergio Lissner
 * Date: 9/24/2025
 * Time: 11:46 AM
 */
@Execution(CONCURRENT)
class AngularToSignalMigrationTest {

    @Test
    public void migrateAngularToSignalMigrationTest_simpleProperty() {
        String htmlContent = """
            <div>{{ items }}</div>
            """;
        
        String tsContent = """
            export class TestComponent {
                private items: any[] = [];
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
            import { signal } from '@angular/core';
            export class TestComponent {
                private items = signal<any[]>([]);
            }
            """;
        
        assertEquals(expected, result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_propertyNotUsedInTemplate() {
        String htmlContent = """
            <div>Static content</div>
            """;
        
        String tsContent = """
            export class TestComponent {
                private items: any[] = [];
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        // Should not change since property is not used in template
        assertEquals(tsContent, result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_getterToComputed() {
        String htmlContent = """
            <div>{{ filteredItems }}</div>
            """;
        
        String tsContent = """
            export class TestComponent {
                get filteredItems() { return this.items.filter(item => item.active); }
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
            import { computed } from '@angular/core';
            export class TestComponent {
                filteredItems = computed(() => { return this.items.filter(item => item.active); });
            }
            """;
        
        assertEquals(expected, result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_propertyAssignment() {
        String tsContent = """
            export class TestComponent {
                updateItems() {
                    this.items = newItems;
                    this.items().forEach(item => console.log(item));
                }
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, "");
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
            export class TestComponent {
                updateItems() {
                    this.items.set(newItems);
                    this.items().forEach(item => console.log(item));
                }
            }
            """;
        
        assertEquals(expected, result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_existingAngularImport() {
        String htmlContent = """
            <div>{{ data }}</div>
            """;
        
        String tsContent = """
            import { Component, OnInit } from '@angular/core';
            
            export class TestComponent {
                private data: string = 'test';
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
            import { Component, OnInit, signal } from '@angular/core';
            
            export class TestComponent {
                private data = signal<string>('test');
            }
            """;
        
        assertEquals(expected, result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_multipleProperties() {
        String htmlContent = """
            <div>{{ title }}</div>
            <div>{{ count }}</div>
            """;
        
        String tsContent = """
            export class TestComponent {
                public title: string = 'Hello';
                protected count: number = 0;
                private internal: boolean = false;
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
            import { signal } from '@angular/core';
            export class TestComponent {
                public title = signal<string>('Hello');
                protected count = signal<number>(0);
                private internal: boolean = false;
            }
            """;
        
        assertEquals(expected, result);
    }
    
    @Test 
    public void migrateAngularToSignalMigrationTest_complexPropertyUsage() {
        String htmlContent = """
            <div *ngFor="let item of items">{{ item.name }}</div>
            <button (click)="updateData()">Update</button>
            """;
        
        String tsContent = """
            export class TestComponent {
                private items: Item[] = [];
                private data: any = null;
                
                updateData() {
                    this.data = { updated: true };
                }
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
            import { signal } from '@angular/core';
            export class TestComponent {
                private items = signal<Item[]>([]);
                private data: any = null;
                
                updateData() {
                    this.data = { updated: true };
                }
            }
            """;
        
        assertEquals(expected, result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_emptyContent() {
        Migration.MigrationConfig cfg = createConfig("test.component.ts", "", "");
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, "");
        
        assertEquals("", result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_noHtmlFile() {
        String tsContent = """
            export class TestComponent {
                private items: any[] = [];
            }
            """;
        
        Migration.MigrationConfig cfg = createConfigWithoutHtml("test.component.ts", tsContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        // Should not change since no HTML file is available
        assertEquals(tsContent, result);
    }
    
    private Migration.MigrationConfig createConfig(String fileName, String tsContent, String htmlContent) {
        Path tsPath = Paths.get(fileName);
        Path htmlPath = tsPath.getParent() != null ? 
            tsPath.getParent().resolve(fileName.replace(".ts", ".html")) :
            Paths.get(fileName.replace(".ts", ".html"));
            
        Map<String, String> files = new HashMap<>();
        files.put(fileName+".ts", tsContent);
        files.put(fileName+".html", htmlContent);
        
        return new Migration.MigrationConfig(tsPath, files);
    }
    
    private Migration.MigrationConfig createConfigWithoutHtml(String fileName, String tsContent) {
        Path tsPath = Paths.get(fileName);
        Map<String, String> files = new HashMap<>();
        files.put(fileName, tsContent);
        
        return new Migration.MigrationConfig(tsPath, files);
    }
}

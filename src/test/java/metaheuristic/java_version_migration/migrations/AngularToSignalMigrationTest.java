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
                filteredItems = computed(() => {
                    return this.items.filter(item => item.active);
                });
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
    
    @Test
    public void migrateAngularToSignalMigrationTest_realWorldAngularComponent() {
        String htmlContent = """
            <div class="container">
                <h1>{{ title }}</h1>
                <div *ngIf="isLoading">Loading...</div>
                <ul *ngFor="let item of filteredItems">
                    <li>{{ item.name }} - {{ item.price | currency }}</li>
                </ul>
                <button (click)="addItem()" [disabled]="isLoading">Add Item</button>
                <input [(ngModel)]="searchTerm" placeholder="Search...">
            </div>
            """;
        
        String tsContent = """
            import { Component, OnInit } from '@angular/core';
            
            export class ProductListComponent implements OnInit {
                public title: string = 'Product List';
                private items: Product[] = [];
                protected isLoading: boolean = false;
                public searchTerm: string = '';
                
                get filteredItems() { 
                    return this.items.filter(item => 
                        item.name.toLowerCase().includes(this.searchTerm.toLowerCase())
                    );
                }
                
                ngOnInit() {
                    this.loadItems();
                }
                
                addItem() {
                    this.items = [...this.items, new Product()];
                    this.isLoading = true;
                }
                
                private loadItems() {
                    this.isLoading = true;
                }
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("product-list.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
            import { Component, OnInit, signal, computed } from '@angular/core';
            
            export class ProductListComponent implements OnInit {
                public title = signal<string>('Product List');
                private items = signal<Product[]>([]);
                protected isLoading = signal<boolean>(false);
                public searchTerm = signal<string>('');
                
                filteredItems = computed(() => { 
                    return this.items().filter(item => 
                        item.name.toLowerCase().includes(this.searchTerm().toLowerCase())
                    );
                });
                
                ngOnInit() {
                    this.loadItems();
                }
                
                addItem() {
                    this.items.set([...this.items(), new Product()]);
                    this.isLoading.set(true);
                }
                
                private loadItems() {
                    this.isLoading.set(true);
                }
            }
            """;
        
        assertEquals(expected, result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_propertyBinding() {
        String htmlContent = """
            <input [value]="username" [disabled]="isDisabled">
            <div [class.active]="isActive">Content</div>
            """;
        
        String tsContent = """
            export class TestComponent {
                public username: string = '';
                private isDisabled: boolean = false; 
                protected isActive: boolean = true;
                private unused: string = 'not used';
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
            import { signal } from '@angular/core';
            export class TestComponent {
                public username = signal<string>('');
                private isDisabled = signal<boolean>(false); 
                protected isActive = signal<boolean>(true);
                private unused: string = 'not used';
            }
            """;
        
        assertEquals(expected, result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_eventHandlers() {
        String htmlContent = """
            <button (click)="onClick()" (mouseover)="onHover()">Click me</button>
            <input (change)="onDataChange($event)">
            """;
        
        String tsContent = """
            export class TestComponent {
                private data: any = null;
                private hoverState: boolean = false;
                
                onClick() {
                    this.data = { clicked: true };
                }
                
                onHover() {
                    this.hoverState = true;
                }
                
                onDataChange(event: any) {
                    this.data = event.target.value;
                }
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
            import { signal } from '@angular/core';
            export class TestComponent {
                private data = signal<any>(null);
                private hoverState = signal<boolean>(false);
                
                onClick() {
                    this.data.set({ clicked: true });
                }
                
                onHover() {
                    this.hoverState.set(true);
                }
                
                onDataChange(event: any) {
                    this.data.set(event.target.value);
                }
            }
            """;
        
        assertEquals(expected, result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_mixedSignalAndNonSignal() {
        String htmlContent = """
            <div>{{ publicData }}</div>
            """;
        
        String tsContent = """
            import { Injectable } from '@angular/core';
            
            export class TestComponent {
                public publicData: string = 'visible';
                private privateData: string = 'hidden';
                
                @Injectable() 
                service: any;
                
                constructor(private http: HttpClient) {}
                
                updateData() {
                    this.privateData = 'updated';
                    this.publicData = 'new value';
                }
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
            import { Injectable, signal } from '@angular/core';
            
            export class TestComponent {
                public publicData = signal<string>('visible');
                private privateData: string = 'hidden';
                
                @Injectable() 
                service: any;
                
                constructor(private http: HttpClient) {}
                
                updateData() {
                    this.privateData = 'updated';
                    this.publicData.set('new value');
                }
            }
            """;
        
        assertEquals(expected, result);
    }
    
    @Test
    public void migrateAngularToSignalMigrationTest_complexTypes() {
        String htmlContent = """
            <div>{{ userProfile.name }}</div>
            <div *ngFor="let item of itemList">{{ item }}</div>
            """;
        
        String tsContent = """
            export interface UserProfile {
                name: string;
                email: string;
            }
            
            export class TestComponent {
                public userProfile: UserProfile = { name: 'John', email: 'john@example.com' };
                private itemList: string[] = ['item1', 'item2'];
                protected complexData: Map<string, any> = new Map();
            }
            """;
        
        Migration.MigrationConfig cfg = createConfig("test.component.ts", tsContent, htmlContent);
        String result = AngularToSignalMigration.migrateAngularToSignalMigration(cfg, tsContent);
        
        String expected = """
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
            """;
        
        assertEquals(expected, result);
    }
    
    private Migration.MigrationConfig createConfig(String fileName, String tsContent, String htmlContent) {
        Path tsPath = Paths.get(fileName);
        String baseName = tsPath.getFileName().toString().substring(0, fileName.length() - 3);

        Map<String, String> files = new HashMap<>();
        files.put(baseName+".ts", tsContent);
        files.put(baseName+".html", htmlContent);
        
        return new Migration.MigrationConfig(tsPath, files);
    }
    
    private Migration.MigrationConfig createConfigWithoutHtml(String fileName, String tsContent) {
        Path tsPath = Paths.get(fileName);
        String baseName = tsPath.getFileName().toString().substring(0, fileName.length() - 3);

        Map<String, String> files = new HashMap<>();
        files.put(baseName+".ts", tsContent);
        
        return new Migration.MigrationConfig(tsPath, files);
    }
}

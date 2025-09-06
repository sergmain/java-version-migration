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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * Unit tests for AngularTemplateSeparationMigration
 *
 * @author Sergio Lissner
 * Date: 9/4/2025
 * Time: 4:57 PM
 */
@Execution(CONCURRENT)
class AngularTemplateSeparationMigrationTest {

    @Test
    public void testBasicSeparation() {
        String input = "<ng-container *ngTemplateOutlet=\"addButtonTemplate\" *ngIf=\"condition\"></ng-container>";
        String expected = "<ng-container *ngIf=\"condition\">\n" +
                         "  <ng-container *ngTemplateOutlet=\"addButtonTemplate\"></ng-container>\n" +
                         "</ng-container>";
        
        String result = AngularTemplateSeparationMigration.migrateAngularTemplateSeparationMigration(input);
        assertEquals(expected, result);
    }

    @Test
    public void testReversedOrderSeparation() {
        String input = "<ng-container *ngIf=\"condition\" *ngTemplateOutlet=\"addButtonTemplate\"></ng-container>";
        String expected = "<ng-container *ngIf=\"condition\">\n" +
                         "  <ng-container *ngTemplateOutlet=\"addButtonTemplate\"></ng-container>\n" +
                         "</ng-container>";
        
        String result = AngularTemplateSeparationMigration.migrateAngularTemplateSeparationMigration(input);
        assertEquals(expected, result);
    }

    @Test
    public void testWithAdditionalAttributes() {
        String input = "<ng-container class=\"test\" *ngTemplateOutlet=\"template\" *ngIf=\"show\" id=\"container\"></ng-container>";
        String expected = "<ng-container *ngIf=\"show\" class=\"test\" id=\"container\">\n" +
                         "  <ng-container *ngTemplateOutlet=\"template\"></ng-container>\n" +
                         "</ng-container>";
        
        String result = AngularTemplateSeparationMigration.migrateAngularTemplateSeparationMigration(input);
        assertEquals(expected, result);
    }

    @Test
    public void testWithComplexCondition() {
        String input = "<ng-container *ngIf=\"user && user.isAdmin\" *ngTemplateOutlet=\"adminTemplate\"></ng-container>";
        String expected = "<ng-container *ngIf=\"user && user.isAdmin\">\n" +
                         "  <ng-container *ngTemplateOutlet=\"adminTemplate\"></ng-container>\n" +
                         "</ng-container>";
        
        String result = AngularTemplateSeparationMigration.migrateAngularTemplateSeparationMigration(input);
        assertEquals(expected, result);
    }

    @Test
    public void testWithInnerContent() {
        String input = "<ng-container *ngTemplateOutlet=\"template\" *ngIf=\"condition\">Some content</ng-container>";
        String expected = "<ng-container *ngIf=\"condition\">\n" +
                         "  <ng-container *ngTemplateOutlet=\"template\">Some content</ng-container>\n" +
                         "</ng-container>";
        
        String result = AngularTemplateSeparationMigration.migrateAngularTemplateSeparationMigration(input);
        assertEquals(expected, result);
    }

    @Test
    public void testNoChangesWhenOnlyOneDirective() {
        String input = "<ng-container *ngIf=\"condition\">Content</ng-container>";
        String result = AngularTemplateSeparationMigration.migrateAngularTemplateSeparationMigration(input);
        assertEquals(input, result);
    }

    @Test
    public void testNoChangesWhenOnlyTemplateOutlet() {
        String input = "<ng-container *ngTemplateOutlet=\"template\"></ng-container>";
        String result = AngularTemplateSeparationMigration.migrateAngularTemplateSeparationMigration(input);
        assertEquals(input, result);
    }

    @Test
    public void testNoChangesWhenNoStructuralDirectives() {
        String input = "<ng-container class=\"test\" id=\"container\">Content</ng-container>";
        String result = AngularTemplateSeparationMigration.migrateAngularTemplateSeparationMigration(input);
        assertEquals(input, result);
    }

    @Test
    public void testMultipleContainers() {
        String input = "<div>" +
                      "<ng-container *ngIf=\"show1\" *ngTemplateOutlet=\"template1\"></ng-container>" +
                      "<ng-container *ngIf=\"show2\" *ngTemplateOutlet=\"template2\"></ng-container>" +
                      "</div>";
        
        String expected = "<div>" +
                         "<ng-container *ngIf=\"show1\">\n" +
                         "  <ng-container *ngTemplateOutlet=\"template1\"></ng-container>\n" +
                         "</ng-container>" +
                         "<ng-container *ngIf=\"show2\">\n" +
                         "  <ng-container *ngTemplateOutlet=\"template2\"></ng-container>\n" +
                         "</ng-container>" +
                         "</div>";
        
        String result = AngularTemplateSeparationMigration.migrateAngularTemplateSeparationMigration(input);
        assertEquals(expected, result);
    }

    @Test
    public void testIgnoreOtherElements() {
        String input = "<input type=\"text\" *ngIf=\"showInput\" *ngTemplateOutlet=\"inputTemplate\" class=\"form-control\"/>";
        String result = AngularTemplateSeparationMigration.migrateAngularTemplateSeparationMigration(input);
        assertEquals(input, result);
    }
}

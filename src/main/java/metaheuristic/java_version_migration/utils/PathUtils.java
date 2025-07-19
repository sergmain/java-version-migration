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

package metaheuristic.java_version_migration.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Sergio Lissner
 * Date: 7/18/2025
 * Time: 3:09 PM
 */
public class PathUtils {

    private static String claude_isEnd = """
        java
        write static method
        public static boolean isEnd(Path path, List<String> excludePackages)
        
        that method must decide is given path ending with any package from excludePackages list
        
        Path path is actually a file, not directory
        
        new requirement:
        
        add new parameter to method which define base path:
        
        Path srcPath
        
        for determine exclude or not srcPath should be added by exclude path and then compare with 'path' variable.
        
        as a result such unit  test  should be working:
        
        ```
        @Test
        void test_exclude_as_root_package() {
            // Given
            Path path = Paths.get("src/main/java/com/example/excluded/a/b/c/file.txt");
            List<String> excludePackages = Arrays.asList("excluded.a");
        
            // When
            boolean result = PathUtils.isEnd(path, excludePackages);
        
            // Then
            assertTrue(result, "Should return true when path ends with 'excluded' package");
        
        }
        ```
        
        """;
    /**
     * Checks if the given file path's directory ends with any package from the excludePackages list.
     *
     * @param path the file path to check
     * @param excludePackages list of package names to check against
     * @return true if the file's directory path ends with any of the excluded packages, false otherwise
     */
    /**
     * Checks if the given file path's directory starts with any exclude path constructed from srcPath + excludePackages.
     *
     * @param path the file path to check
     * @param excludePackages list of package names (dot-separated) to check against
     * @param srcPath the base source path to combine with exclude packages
     * @return true if the file's directory path starts with any of the constructed exclude paths, false otherwise
     */
    public static boolean isEnd(Path path, List<String> excludePackages, Path srcPath) {
        if (path == null || excludePackages == null || excludePackages.isEmpty() || srcPath == null) {
            return false;
        }

        // Get the parent directory path (excluding the filename)
        Path parentPath = path.getParent();
        if (parentPath == null) {
            return false; // No parent directory
        }

        String pathString = parentPath.toString().replace('\\', '/'); // Normalize path separators
        String srcPathString = srcPath.toString().replace('\\', '/'); // Normalize source path

        for (String excludePackage : excludePackages) {
            if (excludePackage != null && !excludePackage.isEmpty()) {
                // Convert dot-separated package to path (e.g., "excluded.a" -> "excluded/a")
                String packagePath = excludePackage.replace('.', '/');

                // Combine srcPath with exclude package path
                String fullExcludePath = srcPathString + "/" + packagePath;

                // Check if file's directory path starts with the constructed exclude path
                if (pathString.startsWith(fullExcludePath)) {
                    return true;
                }
            }
        }

        return false;
    }
}

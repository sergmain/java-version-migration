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
        
        Path path is actually a file, not directory""";
    /**
     * Checks if the given file path's directory ends with any package from the excludePackages list.
     *
     * @param path the file path to check
     * @param excludePackages list of package names to check against
     * @return true if the file's directory path ends with any of the excluded packages, false otherwise
     */
    public static boolean isEnd(Path path, List<String> excludePackages) {
        if (path == null || excludePackages == null || excludePackages.isEmpty()) {
            return false;
        }

        // Get the parent directory path (excluding the filename)
        Path parentPath = Files.isDirectory(path) ? path : path.getParent();
        if (parentPath == null) {
            return false; // No parent directory
        }

        String pathString = parentPath.toString().replace('\\', '/'); // Normalize path separators

        for (String excludePackage : excludePackages) {
            if (excludePackage != null && !excludePackage.isEmpty()) {
                // Normalize the package path
                String normalizedPackage = excludePackage.replace('\\', '/');

                // Check if directory path ends with the package
                if (pathString.endsWith(normalizedPackage)) {
                    return true;
                }

                // Also check with leading slash to ensure proper package boundary
                if (pathString.endsWith("/" + normalizedPackage)) {
                    return true;
                }
            }
        }

        return false;
    }
}

/*
 * Copyright (c) 2023. Sergio Lissner
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

package metaheuristic.java_version_migration.meta;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Serge
 * Date: 9/9/2019
 * Time: 4:49 PM
 */
@Slf4j
public class MetaUtils {
    public static boolean isTrue(@Nullable Meta m) {
        return m!=null && "true".equals(m.getValue());
    }

    @Deprecated(forRemoval = true)
    public static boolean isFalse(@Nullable Meta m) {
        return !isTrue(m);
    }

    public static boolean isTrue(@Nullable List<Map<String, String>> metas, String... keys) {
        return isTrue(metas, false, keys);
    }

    public static boolean isTrue(@Nullable List<Map<String, String>> metas, boolean defaultValue, String... keys) {
        final Meta meta = getMeta(metas, keys);
        if (meta==null) {
            return defaultValue;
        }
        return isTrue(meta);
    }

    @Deprecated(forRemoval = true)
    public static boolean isFalse(@Nullable List<Map<String, String>> metas, String... keys) {
        return isFalse(getMeta(metas, keys));
    }

    @Nullable
    public static String getValue(@Nullable List<Map<String, String>> metas, String... keys) {
        Meta m = getMeta(metas, keys);
        return m!=null ? m.getValue() : null;
    }

    @Nullable
    public static Long getLong(@Nullable List<Map<String, String>> metas, String... keys) {
        Meta m = getMeta(metas, keys);
        return m!=null ? Long.valueOf(m.getValue()) : null;
    }

    @Nullable
    public static List<Map<String, String>> remove(@Nullable final List<Map<String, String>> metas, String... keys) {
        if (metas==null || keys.length==0) {
            return metas;
        }
        final List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> meta : metas) {
            boolean found = false;
            for (String key : keys) {
                if (meta.containsKey(key)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.add(meta);
            }
        }
        return result;
    }

    @Nullable
    public static Meta getMeta(@Nullable List<Map<String, String>> metas, @Nonnull String... keys) {
        if (metas==null) {
            return null;
        }
        if (keys.length==0) {
            return null;
        }
        for (Map<String, String> meta : metas) {
            for (String key : keys) {
                // because the map is created from yaml actual Class of value could be Boolean
                Object o = meta.get(key);
                if (o!=null) {
                    return new Meta(key, o.toString());
                }
            }
        }
        return null;
    }
}

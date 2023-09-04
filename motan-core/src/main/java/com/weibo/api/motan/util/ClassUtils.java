package com.weibo.api.motan.util;

/**
 * @author dinglang
 * @since 2023/9/4
 */
public class ClassUtils {
    public static ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader cl = null;
        if (!clazz.getName().startsWith("org.apache.dubbo")) {
            cl = clazz.getClassLoader();
        }
        if (cl == null) {
            try {
                cl = Thread.currentThread().getContextClassLoader();
            } catch (Exception ignored) {
                // Cannot access thread context ClassLoader - falling back to system class loader...
            }
            if (cl == null) {
                // No thread context class loader -> use class loader of this class.
                cl = clazz.getClassLoader();
                if (cl == null) {
                    // getClassLoader() returning null indicates the bootstrap ClassLoader
                    try {
                        cl = ClassLoader.getSystemClassLoader();
                    } catch (Exception ignored) {
                        // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                    }
                }
            }
        }

        return cl;
    }

    /**
     * Return the default ClassLoader to use: typically the thread context
     * ClassLoader, if available; the ClassLoader that loaded the ClassUtils
     * class will be used as fallback.
     * <p>
     * Call this method if you intend to use the thread context ClassLoader in a
     * scenario where you absolutely need a non-null ClassLoader reference: for
     * example, for class path resource loading (but not necessarily for
     * <code>Class.forName</code>, which accepts a <code>null</code> ClassLoader
     * reference as well).
     *
     * @return the default ClassLoader (never <code>null</code>)
     * @see java.lang.Thread#getContextClassLoader()
     */
    public static ClassLoader getClassLoader() {
        return getClassLoader(ClassUtils.class);
    }
}

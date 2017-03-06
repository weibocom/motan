package com.weibo.api.motan.config.apisupport;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by yunzhu on 17/3/3.
 */
public class MotanClassScanner {

    private static Logger logger = LoggerFactory.getLogger(MotanClassScanner.class);
    protected static final String PACKAGE_SEPARATOR = ".";
    protected static final String PATH_SEPARATOR = "/";
    public Set<MotanResources> doScan(String pkg) throws IOException {
        String pkgPath = convertClassNameToResourcePath(pkg);

        Set<MotanResources> rootResources =  doFindAllClassPathResources(pkgPath);
        Set<MotanResources> allFetchedResources = new LinkedHashSet<MotanResources>(16);
        for (MotanResources rootDirResource : rootResources) {
            allFetchedResources.addAll(doFindPathMatchingFileResources(rootDirResource));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Resolved location pattern [{}] to resources {}" ,pkg, allFetchedResources);
        }

        return allFetchedResources;

    }


    protected Set<MotanResources> doFindPathMatchingFileResources(MotanResources rootDirResource )
            throws IOException {

        File rootDir;
        try {
            rootDir = rootDirResource.getFile().getAbsoluteFile();
        }
        catch (Exception ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Cannot search for matching files underneath " + rootDirResource +
                        " because it does not correspond to a directory in the file system", ex);
            }
            return Collections.emptySet();
        }
        return doFindMatchingFileSystemResources(rootDir);
    }


    protected Set<MotanResources> doFindMatchingFileSystemResources(File rootDir) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Looking for matching resources in directory tree [" + rootDir.getPath() + "]");
        }
        Set<File> matchingFiles = retrieveMatchingFiles(rootDir);
        Set<MotanResources> result = new LinkedHashSet<MotanResources>(matchingFiles.size());
        for (File file : matchingFiles) {
            result.add(new MotanResources(file));
        }
        return result;
    }



    protected Set<File> retrieveMatchingFiles(File rootDir) throws IOException {

        if (!rootDir.exists()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping [" + rootDir.getAbsolutePath() + "] because it does not exist");
            }
            return Collections.emptySet();
        }
        if (!rootDir.isDirectory()) {
            if (logger.isWarnEnabled()) {
                logger.warn("Skipping [" + rootDir.getAbsolutePath() + "] because it does not denote a directory");
            }
            return Collections.emptySet();
        }
        if (!rootDir.canRead()) {
            if (logger.isWarnEnabled()) {
                logger.warn("Cannot search for matching files underneath directory [" + rootDir.getAbsolutePath() +
                        "] because the application is not allowed to read the directory");
            }
            return Collections.emptySet();
        }
        Set<File> result = new LinkedHashSet<File>(8);
        doRetrieveMatchingFiles( rootDir, result);
        return result;
    }


    protected void doRetrieveMatchingFiles(File dir, Set<File> result) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Searching directory [" + dir.getAbsolutePath() +
                    "] for files matching pattern [" + dir + "]");
        }
        File[] dirContents = dir.listFiles();
        if (dirContents == null) {
            if (logger.isWarnEnabled()) {
                logger.warn("Could not retrieve contents of directory [" + dir.getAbsolutePath() + "]");
            }
            return;
        }
        for (File content : dirContents) {
            String currPath =  replace(content.getAbsolutePath(), File.separator, "/");
            if ( content.isDirectory() ) {
                if (!content.canRead()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipping subdirectory [" + dir.getAbsolutePath() +
                                "] because the application is not allowed to read the directory");
                    }
                }
                else {
                    doRetrieveMatchingFiles(content, result);
                }
            }
            if (currPath.endsWith(".class")) {
                result.add(content);
            }
        }
    }


    protected MotanResources convertClassLoaderURL(URL url) {

        MotanResources motanResources = new MotanResources(url.getPath());

        return motanResources;
    }





    protected Set<MotanResources> doFindAllClassPathResources(String path) throws IOException {
        Set<MotanResources> result = new LinkedHashSet<MotanResources>(16);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
        while (resourceUrls.hasMoreElements()) {

            URL url = resourceUrls.nextElement();
            result.add(convertClassLoaderURL(url));

        }

        return result;
    }

    public static String convertClassNameToResourcePath(String className) {
        if( StringUtils.isEmpty(className) ) {
            throw  new IllegalArgumentException("className cannot be empty");
        }
        return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    }



    public static String replace(String inString, String oldPattern, String newPattern) {
        if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
            return inString;
        }
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        int index = inString.indexOf(oldPattern);
        int patLen = oldPattern.length();
        while (index >= 0) {
            sb.append(inString.substring(pos, index));
            sb.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }
        sb.append(inString.substring(pos));
        return sb.toString();
    }

    public static boolean hasLength(CharSequence str) {
        return (str != null && str.length() > 0);
    }

}

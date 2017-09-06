package com.weibo.api.motan.config.apisupport;

import java.io.File;

/**
 * Created by yunzhu on 17/3/3.
 */
public class MotanResources {


    private File file;

    private String path;


    public MotanResources(String path) {

        this.path = path;
        this.file = new File(path);
    }


    public MotanResources(File file) {

        this.file = file;
        this.path = file.getPath();
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "MotanResources{" +
                "file=" + file +
                ", path='" + path + '\'' +
                '}';
    }
}

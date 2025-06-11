package com.githubshare.dto;


import java.util.List;

public class FileNodeDto {
    private String name;
    private String path;
    private boolean directory;
    private List<FileNodeDto> children;

    public FileNodeDto() {}

    public FileNodeDto(String name, String path, boolean directory, List<FileNodeDto> children) {
        this.name = name;
        this.path = path;
        this.directory = directory;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return directory;
    }

    public List<FileNodeDto> getChildren() {
        return children;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public void setChildren(List<FileNodeDto> children) {
        this.children = children;
    }
}

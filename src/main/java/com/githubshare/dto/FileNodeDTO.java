package com.githubshare.dto;


import java.util.List;

public class FileNodeDTO {
    private String name;
    private String path;
    private boolean directory;
    private List<FileNodeDTO> children;

    public FileNodeDTO() {}

    public FileNodeDTO(String name, String path, boolean directory, List<FileNodeDTO> children) {
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

    public List<FileNodeDTO> getChildren() {
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

    public void setChildren(List<FileNodeDTO> children) {
        this.children = children;
    }
}

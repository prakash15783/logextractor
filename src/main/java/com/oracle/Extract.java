package com.oracle;

import java.util.LinkedList;
import java.util.List;

/*
This represents an extract
 */
public class Extract {
    private String managedServer;
    private List<String> entries = new LinkedList<String>();
    private String logFile;
    private String outputFile;

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public String getManagedServer() {
        return managedServer;
    }

    public void setManagedServer(String managedServer) {
        this.managedServer = managedServer;
    }

    public List<String> getEntries() {
        return entries;
    }

    public void setEntries(List<String> entries) {
        this.entries = entries;
    }
}

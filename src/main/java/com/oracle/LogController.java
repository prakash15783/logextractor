package com.oracle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/*
This controller exposes APIs for the application
 */
@RestController
public class LogController {

    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    StringBuilder diagnosticsBuilder = new StringBuilder();
    StringBuilder builder = new StringBuilder();
    boolean instanceFound = false;
    int lineNum = 0;


    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/uploadFile")
    public UploadedFile uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);
        logger.info("File " + fileName + " has been uploaded.");
        return new UploadedFile(fileName);
    }

    @GetMapping("/listFiles")
    public List<UploadedFile> listFiles() {
        return fileStorageService.listFiles();
    }

    @GetMapping("/extractFromExistingFile")
    public Extract extractFromFile(@RequestParam("file") String file, @RequestParam Map<String,String> allRequestParams){
        resetValues();
        final File extractedFolder = new File(fileStorageService.getRootPath() + File.separator + file.substring(0, file.lastIndexOf(".")));
            boolean found = false;
            Extract extract = new Extract();
            if (extractedFolder.exists()) {
                for (final File fileEntry : extractedFolder.listFiles()) {
                    if (fileEntry.isDirectory()) {
                        if (processFolder(fileEntry.getAbsoluteFile(), allRequestParams, extract)) {
                            logger.info("Logs for condition " + allRequestParams.toString() + " found in managed server " + fileEntry.getName());
                            found = true;
                            extract.setManagedServer(fileEntry.getName());
                            return extract;
                        }
                    }
                }
            if(!found){
                logger.info("Logs for conditions " + allRequestParams.toString() + " not found.");
                throw new NoEntryFoundException("Logs for conditions " + allRequestParams.toString() + " not found.");
            }
        }
        return extract;
    }

    @PostMapping("/extract")
    public Extract extract(@RequestParam("file") MultipartFile file, @RequestParam Map<String,String> allRequestParams) {
        String fileName = fileStorageService.storeFile(file);
        logger.info("File " + fileName + " has been uploaded.");
        resetValues();
        final File extractedFolder = new File(fileStorageService.getRootPath() + File.separator + fileName.substring(0, fileName.lastIndexOf(".")));
        boolean found = false;
        Extract extract = new Extract();
        if (extractedFolder.exists()) {
            for (final File fileEntry : extractedFolder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    if (processFolder(fileEntry.getAbsoluteFile(), allRequestParams, extract)) {
                        logger.info("Logs for condition " + allRequestParams.toString() + " found in managed server " + fileEntry.getName());
                        found = true;
                        extract.setManagedServer(fileEntry.getName());
                        return extract;
                    }
                }
            }
            if(!found){
                logger.info("Logs for conditions " + allRequestParams.toString() + " not found.");
                throw new NoEntryFoundException("Logs for conditions " + allRequestParams.toString() + " not found.");
            }
        }
        return extract;
    }

    @PostMapping("/extractFile")
    public ResponseEntity<Resource> extractFile(@RequestParam("file") MultipartFile file, @RequestParam Map<String,String> allRequestParams, HttpServletRequest request) {
        String fileName = fileStorageService.storeFile(file);
        logger.info("File " + fileName + " has been uploaded.");
        resetValues();
        final File extractedFolder = new File(fileStorageService.getRootPath() + File.separator + fileName.substring(0, fileName.lastIndexOf(".")));
        boolean found = false;
        Extract extract = new Extract();
        if (extractedFolder.exists()) {
            for (final File fileEntry : extractedFolder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    if (processFolder(fileEntry.getAbsoluteFile(), allRequestParams, extract)) {
                        logger.info("Logs for condition " + allRequestParams.toString() + " found in managed server " + fileEntry.getName());
                        found = true;
                        extract.setManagedServer(fileEntry.getName());

                        return extractFile(extract, request);
                    }
                }
            }
            if(!found){
                logger.info("Logs for conditions " + allRequestParams.toString() + " not found.");
                throw new NoEntryFoundException("Logs for conditions " + allRequestParams.toString() + " not found.");
            }
        }
        return extractFile(extract, request);
    }

    public ResponseEntity<Resource> extractFile(Extract extract, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(extract.getOutputFile());

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }


    public boolean processFolder(File folder, Map<String, String> allRequestParams, Extract extract) {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                boolean flag = false;
                if (name.endsWith("-diagnostic.log")) {
                    flag = true;
                }
                return flag;
            }
        };

        File diagnosticsFile = folder.listFiles(filter)[0];
        SearchCriteria criteria = SearchCriteriaGenerator.generate(allRequestParams);

        String outputFile = diagnosticsFile.getParent() + File.separator + UUID.randomUUID() + ".log";

        // read file into stream, try-with-resources
        try (Stream<String> stream = Files.lines(Paths.get(diagnosticsFile.getAbsolutePath()))) {
             stream.forEach(line -> {
                 lineNum++;
                 for(String searchTerm: criteria.getSearchTerms()){
                    if (!instanceFound && line.contains(searchTerm)) {
                        if(includeLineNumber(allRequestParams)) {
                            extract.getEntries().add("[" + lineNum + "] " + line);
                            diagnosticsBuilder.append("[" + lineNum + "] " + line + "\n");
                        } else{
                            extract.getEntries().add(line);
                            diagnosticsBuilder.append(line + "\n");
                        }
                        instanceFound = true;
                    } else if (instanceFound && !line.contains(searchTerm) && !line.contains("[oracle.soa.tracking.FlowId: ")){
                        if(includeLineNumber(allRequestParams)) {
                            extract.getEntries().add("[" + lineNum + "] " + line);
                            diagnosticsBuilder.append("[" + lineNum + "] " + line + "\n");
                        } else{
                            extract.getEntries().add(line);
                            diagnosticsBuilder.append(line + "\n");
                        }
                    } else if (instanceFound && line.contains(searchTerm)){
                        if(includeLineNumber(allRequestParams)) {
                            extract.getEntries().add("[" + lineNum + "] " + line);
                            diagnosticsBuilder.append("\n\n" + " [" + lineNum + "] " + line);
                        } else
                        {
                            extract.getEntries().add(line);
                            diagnosticsBuilder.append("\n\n" + line);
                        }
                    }
                    else if(instanceFound && !line.contains(searchTerm) && line.contains("[oracle.soa.tracking.FlowId: ")){
                        // switch for the next log entry
                        instanceFound = false;
                        diagnosticsBuilder.append("\n");
                    }
                }
             }
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (diagnosticsBuilder.length() > 0) {
            builder.append("########################################## METADATA ########################################## \n\n" );
            builder.append("EXTRACTED LOGS FROM " + diagnosticsFile.getName() );
            builder.append("\n\n############################################################################################## \n\n" );
            builder.append(diagnosticsBuilder);

            try {
                Files.write(Paths.get(outputFile), builder.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            //extract.setEntries(builder);
            extract.setLogFile(diagnosticsFile.getName());
            extract.setOutputFile(outputFile);
            return true;
        }

        return false;
    }

    private void resetValues(){
        diagnosticsBuilder.setLength(0);
        builder.setLength(0);
        instanceFound = false;
        lineNum = 0;
    }

    private boolean includeLineNumber(Map<String, String> allRequestParams){
        if(allRequestParams.containsKey("lineNo") && allRequestParams.get("lineNo").equals("on")){
            return true;
        }

        return false;
    }
}

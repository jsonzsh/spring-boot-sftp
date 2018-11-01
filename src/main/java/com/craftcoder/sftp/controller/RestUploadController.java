package com.craftcoder.sftp.controller;

import com.craftcoder.sftp.service.SftpService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
public class RestUploadController {

    private final Logger logger = LoggerFactory.getLogger(RestUploadController.class);

    @Autowired
    private SftpService sftpService;

    @Value("${sftp.directory}")
    private String dataDir;

    /**
     * 单个文件上传
     * @param uploadFile
     * @return
     */
    @PostMapping("/api/upload/file")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile uploadFile) {
        logger.info("Single file upload!");
        if (uploadFile.isEmpty()) {
            return new ResponseEntity<>("please select a file!", HttpStatus.OK);
        }
        try {
            sftpService.uploadFile(uploadFile);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(200, new HttpHeaders(), HttpStatus.OK);
    }

    /**
     * 多文件上传
     * @param uploadFiles
     * @return
     */
    @PostMapping("/api/upload/files")
    public ResponseEntity<?> uploadFileMulti(@RequestParam("files") MultipartFile[] uploadFiles) {
        logger.info("multi files upload!");
        String uploadedFileName = Arrays.stream(uploadFiles).map(MultipartFile::getOriginalFilename)
                .filter(x -> !StringUtils.isEmpty(x)).collect(Collectors.joining(" , "));
        if (StringUtils.isEmpty(uploadedFileName)) {
            return new ResponseEntity<>("please select a file!", HttpStatus.OK);
        }
        try {
            sftpService.uploadFiles(Arrays.asList(uploadFiles));
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("200", HttpStatus.OK);
    }

    /**
     * 附件删除
     * @param fileName 文件名称
     * @param response
     * @return http://localhost:9001/api/upload/delete?fileName=icon180.png
     */
    @PostMapping("/api/upload/delete")
    public ResponseEntity<?> delete(String fileName, HttpServletResponse response) throws IOException {
        String path = dataDir + fileName;
        String shellCmd = "shred -zvu -n 5 ";
        //int code=0;
        //try {
        int code = sftpService.deleteFileByShred(shellCmd, path);
        //sftpService.deleteFile(path);
        if (0 != code) {
            return new ResponseEntity<>(500, HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(200, HttpStatus.OK);
        }
       /* } catch (Throwable e) {
            logger.info("delete file failed:"+e.getStackTrace());
            return new ResponseEntity<>(500, HttpStatus.BAD_REQUEST);
        }*/
    }

    //http://localhost:9001/api/download?fileName=timg.jpg
    @RequestMapping(value = "/api/download")
    //public ResponseEntity<byte[]> testDownload(@RequestBody FileDownloadVO vo) throws IOException {
    public ResponseEntity<byte[]> testDownload(String fileName) throws IOException {
        //File file = sftpService.downloadFile("/root/chai/sftp-file/" + vo.getFileName(), "D:/temp/" + vo.getFileName() + ".dl");
        File file = sftpService.downloadFile("/data/sftp/mysftp/upload/" + fileName, "D:/temp/" + fileName + ".dl");
        HttpHeaders headers = new HttpHeaders();
        //headers.setContentDispositionFormData("attachment", vo.getFileName());
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(FileUtils.readFileToByteArray(file), headers, HttpStatus.CREATED);
    }

    /**
     * 文件流下载
     * @param fileName 文件名称
     * @param response http://localhost:9001/api/downloadByte?fileName=timg.jpg
     * @return
     */
    @RequestMapping(value = "/api/downloadByte")
    public HttpServletResponse download(String fileName, HttpServletResponse response) {
        try {
            String path=dataDir + fileName;
            File file = sftpService.downloadFile(path, System.getProperty("user.dir")+"\\" + fileName + ".dl");
            String filename = fileName;
            String ext = filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
            InputStream fis = new FileInputStream(file);
            file.delete();
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            response.reset();
            response.addHeader("Content-Disposition", "attachment;filename=" + new String(filename.getBytes()));
            response.addHeader("Content-Length", "" + file.length());
            OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
            response.setContentType("application/octet-stream");
            toClient.write(buffer);
            toClient.flush();
            toClient.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
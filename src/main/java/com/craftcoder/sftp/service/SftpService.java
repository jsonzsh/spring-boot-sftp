package com.craftcoder.sftp.service;

import com.craftcoder.sftp.config.SftpAdapter;
import com.craftcoder.sftp.util.FileUtils;
import com.craftcoder.sftp.util.SSHCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SftpService {

    private static final Logger logger = LoggerFactory.getLogger(SftpService.class);

    @Autowired
    private SftpRemoteFileTemplate remoteFileTemplate;

    @Autowired
    private SftpAdapter.UploadGateway gateway;

    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.port:22}")
    private int sftpPort;

    @Value("${sftp.user}")
    private String sftpUser;

    @Value("${sftp.password}")
    private String sftpPassword;

    @Value("${ssh.host}")
    private String sshHost;

    @Value("${ssh.port:22}")
    private int sshPort;

    @Value("${ssh.user}")
    private String sshUser;

    @Value("${ssh.password}")
    private String sshPassword;

    /**
     * 单文件上传
     *
     * @param file File
     */
    public void uploadFile(File file) {
        gateway.upload(file);
    }

    /**
     * 查询某个路径下所有文件
     *
     * @param path
     * @return
     */
    public List<String> listAllFile(String path) {
        return remoteFileTemplate.execute(session -> {
            Stream<String> names = Arrays.stream(session.listNames(path));
            names.forEach(name -> logger.info("Name = " + name));
            return names.collect(Collectors.toList());
        });
    }

    /**
     * 下载文件
     *
     * @param fileName 文件名
     * @param savePath 本地文件存储位置
     * @return
     */
    public File downloadFile(String fileName, String savePath) {
        return remoteFileTemplate.execute(session -> {
            boolean existFile = session.exists(fileName);
            if (existFile) {
                InputStream is = session.readRaw(fileName);
                return FileUtils.convertInputStreamToFile(is, savePath);
            } else {
                logger.info("file : {} not exist", fileName);
                return null;
            }
        });
    }

    /**
     * 文件是否存在
     *
     * @param filePath 文件名
     * @return
     */
    public boolean existFile(String filePath) {
        return remoteFileTemplate.execute(session ->
                session.exists(filePath));
    }

    /**
     * 删除文件
     *
     * @param fileName 待删除文件名
     * @return
     */
    public boolean deleteFile(String fileName) {
        return remoteFileTemplate.execute(session -> {
            boolean existFile = session.exists(fileName);
            if (existFile) {
                return session.remove(fileName);
            } else {
                logger.info("file : {} not exist", fileName);
                return false;
            }
        });
    }

    /**
     * shred混淆删除
     * @param cmds
     * @return
     */
    public int deleteFileByShred(String cmds,String filePath) throws IOException {
        //if (existFile(filePath)) {
            SSHCommandExecutor tool = new SSHCommandExecutor(sshHost, sshUser,
                    sshPassword, "utf-8",sshPort);
            String result = tool.exec(cmds+filePath);
            String out = System.getProperty("user.dir");
            //cmds = "rm -rf /data/sftp/mysftp/upload/demo08.png ";
            return 0;
        /*} else {
            //文件不存在
            return 400;
        }*/
    }

    /**
     * 批量上传 (MultipartFile)
     *
     * @param files List<MultipartFile>
     * @throws IOException
     */
    public void uploadFiles(List<MultipartFile> files, boolean deleteSource) throws IOException {
        for (MultipartFile multipartFile : files) {
            if (multipartFile.isEmpty()) {
                continue;
            }
            File file = FileUtils.convert(multipartFile);
            gateway.upload(file);
            if (deleteSource) {
                file.delete();
            }
        }
    }

    /**
     * 批量上传 (MultipartFile)
     *
     * @param files List<MultipartFile>
     * @throws IOException
     */
    public void uploadFiles(List<MultipartFile> files) throws IOException {
        uploadFiles(files, true);
    }

    /**
     * 单文件上传 (MultipartFile)
     *
     * @param multipartFile MultipartFile
     * @throws IOException
     */
    public void uploadFile(MultipartFile multipartFile) throws IOException {
        gateway.upload(FileUtils.convert(multipartFile));
    }
}
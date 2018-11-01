package com.craftcoder.sftp.util;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSHCommandExecutor {
    private static final Logger log = LoggerFactory.getLogger(SSHCommandExecutor.class);
    private Connection conn;
    private String ipAddr;
    private String charset = Charset.defaultCharset().toString();
    private String userName;
    private String password;
    private int port;

    public SSHCommandExecutor(String ipAddr, String userName, String password,
                           String charset,int port) {
        this.ipAddr = ipAddr;
        this.userName = userName;
        this.password = password;
        if (charset != null) {
            this.charset = charset;
        }
        this.port=port;
    }

    public boolean login() throws IOException {
        conn = new Connection(ipAddr,port);
        conn.connect(); // 连接
        return conn.authenticateWithPassword(userName, password); // 认证
    }

    public int exec(String cmds) {
        InputStream in = null;
        String result = "";
        int res=-1;
        try {
            if (this.login()) {
                Session session = conn.openSession();
                session.execCommand(cmds);
                InputStream is = new StreamGobbler(session.getStdout());
                InputStream stdout = new StreamGobbler(session.getStderr());
                result = this.processStdout(stdout, this.charset);
                if(result!=null && !result.equals("")){
                    res=0;
                }
                session.close();
                conn.close();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            log.error("执行删除异常："+e1.getMessage()+e1.getStackTrace());
        }
        return res;
    }

    public String processStdout(InputStream in, String charset) {
        byte[] buf = new byte[1024];
        StringBuffer sb = new StringBuffer();
        try {
            while (in.read(buf) != -1) {
                sb.append(new String(buf, charset));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        SSHCommandExecutor tool = new SSHCommandExecutor("192.168.27.41", "hadoop",
                "hadoop", "utf-8",22);
        int result = tool.exec("./test.sh xiaojun");
        System.out.print(result);
    }
}
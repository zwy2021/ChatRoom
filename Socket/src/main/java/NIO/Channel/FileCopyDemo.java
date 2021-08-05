package NIO.Channel;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

interface FileCopyRunner {
    void copyFile(File source, File target);
}

public class FileCopyDemo {
    private static final int ROUNDS=20;

    private static void benchmark(FileCopyRunner test,File source,File target){
        long elapsed=0L;
        for (int i = 0; i < ROUNDS; i++) {
            long startTime=System.currentTimeMillis();
            test.copyFile(source,target);
            elapsed+=System.currentTimeMillis()-startTime;
            target.delete();
        }
        System.out.println(test+":"+elapsed/ROUNDS);
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        FileCopyRunner noBufferStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                InputStream fin = null;
                OutputStream fout = null;
                try {
                    fin = new FileInputStream(source);
                    fout = new FileOutputStream(target);
                    //返回值是：下一个byte或者-1（结尾）
                    int result;
                    while ((result = fin.read()) != -1) {
                        fout.write(result);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "noBufferStreamCopy";
            }
        };
        FileCopyRunner bufferedStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                InputStream fin = null;
                OutputStream fout = null;
                try {
                    fin = new BufferedInputStream(new FileInputStream(source));
                    fout = new BufferedOutputStream(new FileOutputStream(target));
                    //设置缓冲区
                    byte[] buffer = new byte[1024];
                    int result;
                    //返回读取字节数

                    while ((result = fin.read(buffer)) != -1) {
                        fout.write(buffer, 0, result);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }
            @Override
            public String toString() {
                return "bufferedStreamCopy";
            }
        };
        /**
         * 通道
         * 一个文件对应一个通道
         */
        FileCopyRunner nioBufferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileChannel fin = null;
                FileChannel fout = null;
                try {
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(target).getChannel();
                    //缓冲区（把不能确定格式的文件当做二进制文件读取）
                    //能确定的话可以用xxBuffer
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    while ((fin.read(buffer)) != -1) {
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            fout.write(buffer);
                        }
                        buffer.clear();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }
            @Override
            public String toString() {
                return "nioBufferCopy";
            }
        };
        /**
         * 用通道间传输
         */
        FileCopyRunner nioTransferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileChannel fin=null;
                FileChannel fout=null;
                try {
                    fin=new FileInputStream(source).getChannel();
                    fout=new FileOutputStream(target).getChannel();
                    long transferred=0L;
                    long size=fin.size();
                    while (transferred!=size) {
                         transferred+= fin.transferTo(0, size, fout);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    close(fin);
                    close(fout);
                }
            }
            @Override
            public String toString() {
                return "nioTransferCopy";
            }
        };
        File smallFile=new File("C:\\Users\\dsw\\Desktop\\45.jpg");
        File smallFileCopy=new File("C:\\Users\\dsw\\Desktop\\45copy.jpg");
        System.out.println("------Copying small file------");
//        benchmark(noBufferStreamCopy,smallFile,smallFileCopy);
        benchmark(bufferedStreamCopy,smallFile,smallFileCopy);
        benchmark(nioBufferCopy,smallFile,smallFileCopy);
        benchmark(nioTransferCopy,smallFile,smallFileCopy);


    }
}
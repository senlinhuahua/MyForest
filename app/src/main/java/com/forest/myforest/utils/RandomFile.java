package com.forest.myforest.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

////使用RandomAccessFile实现数据加密
public class RandomFile {

    //存储文件对象信息
    private File file;
    //缓冲区，创建文件中的所有数据
    byte[] buf;
    RandomAccessFile fp;
    //用参数filename所指定的文件构建一个对象存储，同时为缓冲区buf分配与文件长度相等的内存空间
    public RandomFile(String filename){
        file=new File(filename);
        //buf=new byte[(int)file.length()];
        buf=new byte[1000];
    }
    public RandomFile(File desFilename){
        file=desFilename;
        buf=new byte[(int)desFilename.length()];
    }
    //按照读写方式打开文件
    public void openFile()throws FileNotFoundException {
        fp=new RandomAccessFile(file,"rw");
    }
    //关闭文件
    public void closeFile()throws IOException{
        fp.close();
    }
    //对文件进行加密或解密
    public void coding()throws IOException {
        //将文件内容读入到缓冲区
        fp.read(buf);
        //将缓冲区内的内容按位取反
        for(int i=0;i<buf.length;i++){
            buf[i]=(byte)(~buf[i]);
        }
        //将文件指针定位到文件头
        fp.seek(0);
        //将缓冲区中的内容写入到文件中
        fp.write(buf);

        //将文件指针定位到文件尾
        fp.seek((int)fp.length() - buf.length);
        fp.read(buf, 0,(int) buf.length);
        //将缓冲区内的内容按位取反
        for(int i=0;i<buf.length;i++){
            buf[i]=(byte)(~buf[i]);
        }
        fp.seek((int)fp.length() - buf.length);
        //将缓冲区中的内容写入到文件中
        fp.write(buf);
    }
}

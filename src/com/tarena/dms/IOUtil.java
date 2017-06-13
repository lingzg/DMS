package com.tarena.dms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.tarena.dms.bo.LogData;

/**
 * 该类是一个工具类,负责DMS客服的相关读写操作
 * @author tarena
 *
 */
public class IOUtil {
	/**
	 * 从给定的文件中读取第一行字符串,然后将其转换为long类型返回
	 * @param file 要读取的文件
	 * @return 文件中表示的long值
	 * @throws Exception 若获取过程中出错,抛出异常
	 */
	public static long readLong(File file) throws Exception{
		BufferedReader in=null;
		try{			
			in=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			long l=Long.parseLong(in.readLine().trim());
			return l;
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}finally{
			if(in!=null){
				in.close();
			}
		}				
	}
	/**
	 * 从给定的RandomAccessFile当前位置开始读取给定长度的字节量,将其转换为字符串后返回
	 * @param raf
	 * @param length
	 * @return
	 * @throws Exception 
	 */
	public static String readString(RandomAccessFile raf,int length) throws Exception{
		try{
			byte[] data=new byte[length];
			raf.read(data);
			return new String(data,"ISO8859-1");
		}catch(Exception e){
			throw e;
		}				
	}
	/**
	 * 将给定的集合中的每一个元素的toString返回的字符串以行为单位写入到给定的文件中
	 * @param c
	 * @param file
	 * @throws Exception 
	 */
	public static void saveCollection(Collection c,File file) throws Exception{
		PrintWriter out=null;
		try {
			out=new PrintWriter(file);
			for(Object obj:c){
				out.println(obj);
			}
		} catch (Exception e) {
			throw e;
		}finally{
			if(out!=null){
				out.close();
			}
		}
	}
	/**
	 * 将给定的数据以行为单位写入到给定的文件的第一行
	 * @param l
	 * @param file
	 * @throws Exception 
	 */
	public static void saveLong(long l,File file) throws Exception{
		PrintWriter pw=null;
		try{
			pw=new PrintWriter(file);
			pw.println(l);
		}catch(Exception e){
			throw e;
		}finally{
			if(pw!=null){
				pw.close();
			}
		}
	}
	/**
	 * 将给定的文件中的每一行转换为一个LogData实例存入一个集合中,返回该集合
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static List<LogData> loadLogData(File file) throws Exception{
		BufferedReader br=null;
		try{
			br=new BufferedReader(new InputStreamReader(new FileInputStream(file)));	
			List<LogData> list=new ArrayList<LogData>();
			String log;
			while((log=br.readLine())!=null){
				list.add(new LogData(log));
			}
			return list;
		}catch(Exception e){
			throw e;
		}finally{
			if(br!=null){				
				br.close();				
			}
		}				
	}
	public static List<String> loadLogRec(File file) throws Exception{
		BufferedReader br=null;
		try{
			br=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			List<String> list=new ArrayList<String>();
			String line=null;
			while((line=br.readLine())!=null){
				list.add(line);
			}
			return list;
		}catch(Exception e){
			throw e;
		}finally{
			if(br!=null){
				br.close();
			}
		}				
	}
}

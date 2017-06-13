package com.tarena.dms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * DMS服务端
 * 用来接收每一个客户端发送过来的配对日志并将日志保存起来,完成数据收集工作
 * @author tarena
 *
 */
public class DMSServer {
	//服务端运行的Socket
	private ServerSocket server;
	//管理与客户端交互的线程池
	private ExecutorService threadPool;
	//保存所有客户端发送过来的配对日志的文件
	private File serverLogFile;
	//消息队列,存放所有客户端发送过来的配对日志
	private BlockingQueue<String> messageQueue;
	
	/**
	 * 构造方法,用来初始化服务端
	 * @throws Exception 
	 */
	public DMSServer() throws Exception{
		try{			
			server=new ServerSocket(Integer.parseInt(loadServer().get("port")));
			threadPool=Executors.newFixedThreadPool(Integer.parseInt(loadServer().get("numofthread")));
			serverLogFile=new File(loadServer().get("serverlogfile"));
			messageQueue=new LinkedBlockingQueue<String>();
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}
	}
	private Map<String,String> loadServer() throws Exception{		
		try {
			Map<String,String> server=new HashMap<String,String>();
			SAXReader reader=new SAXReader();
			Document doc=reader.read("server.xml");
			Element root=doc.getRootElement();
			List<Element> elements=root.elements();
			for(Element ele:elements){
				String key=ele.getName();
				String value=ele.getTextTrim();
				server.put(key, value);
			}
			return server;
		} catch (Exception e) {			
			e.printStackTrace();
			throw e;
		}			
	}
	public void start(){
		try{
			/*
			 * 首先启动向文件中写入日志的线程
			 */
			Thread t=new Thread(new SaveLogHandler());
			t.start();
			/*
			 * 循环监听8088端口,一旦一个客户端连接了,就启动一个线程与其交互
			 */
			while(true){
				Socket socket=server.accept();
				ClientHandler handler=new ClientHandler(socket);
				threadPool.execute(handler);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void main(String[] args)  {
		try{
		 DMSServer server=new DMSServer();
		 server.start();
		}catch(Exception e){
			System.out.println("服务端启动失败!");
		}
	}
	/**
	 * 该线程负责从消息队列中取出每一条日志并写入文件中
	 * @author tarena
	 *
	 */
	private class SaveLogHandler implements Runnable{
		@Override
		public void run() {
			PrintWriter pw=null;
			try{
				pw=new PrintWriter(serverLogFile);
				while(true){
					//若队列中还有日志,就取出写入文件
					if(messageQueue.size()>0){
						pw.println(messageQueue.poll());
					}else{
						//队列中没有日志了,休息一下
						pw.flush();
						Thread.sleep(500);
					}
				}
			}catch(Exception e){
				
			}finally{
				if(pw!=null){
					pw.close();
				}
			}
		}
		
	}
	private class ClientHandler implements Runnable{
		private Socket socket;
		
		public ClientHandler(Socket socket) {			
			this.socket = socket;
		}

		public void run(){
			/*
			 * 步骤:
			 * 1:通过Socket获取输入流,并转换为缓冲输入流
			 * 2:循环读取客户端发送过来的每一条日志.若读取到"OVER",表示客户端已将所有日志发送完毕
			 * 3:将每一条文件写入日志保存
			 * 4:若全部保存完毕,回复客户端"OK"
			 */
			PrintWriter pw=null;
			try {
				InputStream in=socket.getInputStream();
				BufferedReader br=new BufferedReader(new InputStreamReader(in,"utf-8"));
				pw=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"utf-8"));
				String log=null;
				while((log=br.readLine())!=null){
					if("OVER".equals(log)){
						break;
					}
					//将日志写入文件
					messageQueue.offer(log);
				}
				//回复客户端
				pw.println("OK");
				pw.flush();
			} catch (Exception e) {				
				e.printStackTrace();
				pw.println("ERROR");
				pw.flush();
			}finally{
				try {
					socket.close();
				} catch (IOException e) {					
					e.printStackTrace();
				}
			}
			
		}
	}
}

package com.tarena.dms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.tarena.dms.bo.LogData;
import com.tarena.dms.bo.LogRec;

/**
 * DMS的客户端:
 * 主要工作:
 * 周期性的解析unix系统日志文件wtmpx,并将解析后的日志进行配对,并将配对的日志发送至服务端,完成数据收集工作
 * @author tarena
 *
 */
public class DMSClient {
	/*
	 * 第一步需要的属性定义
	 */
	//wtmpx系统日志文件
	private File logFile;
	//保存解析后的日志文件
	private File textLogFile;
	//保存上次解析后,wtmpx文件的位置,以便下次从这里继续
	private File lastPositionFile;
	//一次解析wtmpx文件中的日志条数
	private int batch;
	/*
	 * 第二步需要的属性定义
	 */
	//保存所有配对日志的文件
	private File logRecFile;
	//保存所有没配对日志的文件
	private File loginFile;
	/*
	 * 第三步需要的属性定义
	 */	
	//服务端地址
	private String serverHost;
	//服务端端口
	private int serverPort;
	/**
	 * 构造器,初始化客户端
	 * @throws Exception 
	 */
	public  DMSClient() throws Exception{
		try {
			//1 加载配置文件
			Map<String,String> config=loadConfig();
			//System.out.println(config);
			//2 根据配置文件的内容初始化相关属性
			init(config);
			System.out.println("初始化客户端完毕!");						
		} catch (Exception e) {			
			e.printStackTrace();
			throw e;
		}
	}
	/**
	 * 读取配置文件,并将配置文件中的内容以Map形式返回
	 * 其中: key: 标签的名字
	 *      value: 标签中的文本
	 * @return
	 * @throws Exception 
	 */
	private Map<String,String> loadConfig() throws Exception{
		try{
			Map<String ,String> config=new HashMap<String,String>();
			SAXReader reader=new SAXReader();
			Document doc=reader.read(new File("config.xml"));
			Element root=doc.getRootElement();
			List<Element> elements=root.elements();
			for(Element ele:elements){
				String key=ele.getName();
				String value=ele.getTextTrim();
				config.put(key,value);								
			}
			return config;
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}		
	}
	/**
	 * 根据给定的Map中的内容来初始化客户端的相关属性
	 * @param config
	 * @throws Exception 
	 */
	private void init(Map<String,String> config) throws Exception{
		try{
			this.logFile=new File(config.get("logfile"));
			this.textLogFile=new File(config.get("textlogfile"));
			this.lastPositionFile=new File(config.get("lastpositionfile"));
			this.batch=Integer.parseInt(config.get("batch"));
			this.logRecFile=new File(config.get("logrecfile"));
			this.loginFile=new File(config.get("loginfile"));
			this.serverHost=config.get("serverhost");
			this.serverPort=Integer.parseInt(config.get("serverport"));
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}
	}
	/**
	 * 判断wtmpx文件中是否还有可解析的日志
	 * 判断规则:
	 * 1: wtmpx文件要存在
	 * 2: 判断lastPositionFile文件是否存在,
	 *     若不存在,说明没有执行过,就从0位置开始
	 *     若存在,执行3
	 * 3:读取lastPositionFile文件,将上次读取的位置得到,用wtmpx文件的总长度减去该值,看是否大于或等于一条日志的长度,
	 *  若是,则说明至少还有一条日志可以读取,返回上次读取的位置.否则,返回-1,表示没有日志可以解析了
	 * @return
	 * @throws Exception 
	 */
	private long hasLogs() throws Exception{
		if(!logFile.exists()){
			System.out.println(logFile+"不存在!");
			return -1;
		}
		if(lastPositionFile.exists()){
			long position =IOUtil.readLong(lastPositionFile);					
			return logFile.length()-position>=LogData.LOG_LENGTH ? position : -1;			
		}else{
			return 0;
		}						
	}
	/**
	 * 第一步工作:
	 * 将wtmpx文件的batch条日志解析成可以看懂的字符串,并以行为单位写入到textLogFile文件中保存,等待第二步配对
	 * @return
	 */
	private boolean readNextLogs(){
		/*
		 * 执行步骤:
		 * 1: 必要的判断工作
		 *   1.1 判断wtmpx文件中是否还有可解析的日志
		 *   1.2 判断textLogFile文件是否存在,若存在,说明第一步已经成功执行过,但是第二步可能没有成功
		 *       所以,第一步无需重复执行.因为若第二步成功执行,会将该文件删除
		 * 2: 创建RandomAccessFile来读取wtmpx文件
		 * 3: 移动指针到上次读取的位置处,开始这次的解析工作
		 * 4: 解析日志
		 *   4.1 首先创建一个集合,用来保存所有解析出来的日志
		 *   4.2 循环batch次,进行解析每一条日志
		 *   4.3 读取一个372字节,将其中5个内容user,pid,time,type,host存入一个LogData实例中,并将该实例存入集合中
		 * 5:将集合中所有解析的日志以行为单位写入到textLogFile对应的文件中
		 *     将RandomAccessFile当前指针的位置保存到lastPositionFile文件中,以便下次继续解析
		 * 
		 */ 
		RandomAccessFile raf=null;
		try {
			long lastPosition=hasLogs();
			if(lastPosition==-1){
				System.out.println("没有日志可以解析了!");
				return false;
			}	
			if(textLogFile.exists()){
				return true;
			}
			//System.out.println(lastPosition);
			raf=new RandomAccessFile(logFile,"r");	
			raf.seek(lastPosition);
			List<LogData> list=new ArrayList<LogData>();
			for(int i=0;i<batch;i++){								
				if(logFile.length()-lastPosition>=LogData.LOG_LENGTH){
					raf.seek(lastPosition+LogData.USER_OFFSET);
					String user=IOUtil.readString(raf, LogData.USER_LENGTH).trim();
					raf.seek(lastPosition+LogData.PID_OFFSET);
					int pid=raf.readInt();
					raf.seek(lastPosition+LogData.TYPE_OFFSET);
					short type=raf.readShort();
					raf.seek(lastPosition+LogData.TIME_OFFSET);
					int time=raf.readInt();
					raf.seek(lastPosition+LogData.HOST_OFFSET);
					String host=IOUtil.readString(raf, LogData.HOST_LENGTH).trim();
					list.add(new LogData(user,pid,time,type,host));
					lastPosition=raf.getFilePointer();
				}else{
					break;
				}
			}
			/*for(LogData log:list){
				System.out.println(log);
			}*/
			/*
			 * 将集合中的每一个元素的toString返回的字符串以行为单位写入到给定的文件中
			 */
			IOUtil.saveCollection(list, textLogFile);
			/*
			 * 将这次读取的wtmpx文件的位置保存到lastPositionFile文件中,以便下次继续解析
			 */
			//System.out.println(lastPosition);
			IOUtil.saveLong(lastPosition, lastPositionFile);
			return true;
		} catch (Exception e) {
			System.out.println("第一步执行出错!");
			e.printStackTrace();
		}finally{
			if(raf!=null){
				try {
					raf.close();
				} catch (IOException e) {					
					e.printStackTrace();
				}
			}
		}
		return false;		
	}
	/**
	 * 第二步工作.配对日志
	 * 将第一步中生成的textLogFile中解析出来的日志与上次没有配对成功的所有登入日志一起进行配对,
	 * 然后将配对成功的日志存入logRecFile,将剩下的没有配对成功的登入日志存入loginFile中
	 * @return
	 */
	public boolean matchLogs(){
		/*
		 * 实现步骤:
		 * 1:必要的判断:
		 *   1.1: textLogFile文件要存在
		 *     1.2: 若logRecFile文件存在,说明第二步已经成功执行过,没必要重复执行.
		 *         因为第三步若成功会将该文件删除,所有文件还在说明第三步可能没成功执行,直接执行第三步即可
		 * 2:先将textLogFile中解析出来的日志读取处理,并转换为若干LogData实例存入集合,等待配对
		 * 3:判断上次没有配对成功的日志的文件是否存在,若存在,说明有上次没有配对成功的日志,
		 *    也将其读取出来,存入集合等待这次配对
		 * 4: 进行配对
		 *     4.1:创建一个集合，用来保存所有配对的日志
		 *     4.2:创建一个Map，用来保存所有待配对的登入日志
		 *     4.3:创建一个Map,用来保存所有待配对的登出日志
		 *     4.4:遍历所有待配对日志，将登入的日志存入登入的Map中，登出日志存入登出Map中
		 *       key:日志的user,pid,host拼的字符串  value:这条日志
		 *     4.5:遍历保存登出日志的Map中的每一条登出日志，使用对应的key去登入的Map中找到配对的登入日志，然后将这一组
		 *           配对日志转换为一个LogRec实例，并存入保存配对日志的集合中。然后将该登入日志从登入的Map中删除。
		 * 5:将所有配对日志保存到logRecFile中
		 * 6:将loginMap中剩下的所有没有配对的日志保存到loginFile中
		 * 7:将textLogFile文件删除,表示第二步成功
		 *    
		 */
		if(!textLogFile.exists()){
			System.out.println(textLogFile+"不存在");
			return false;
		}
		if(logRecFile.exists()){
			return true;
		}
		try {
			List<LogData> logs=IOUtil.loadLogData(textLogFile);
			/*for(LogData log:logs){
				System.out.println(log);
			}*/
			if(loginFile.exists()){
				logs.addAll(IOUtil.loadLogData(loginFile));
			}
			List<LogRec> matches=new ArrayList<LogRec>();
			Map<String,LogData> loginMap=new HashMap<String,LogData>();
			Map<String,LogData> logoutMap=new HashMap<String,LogData>();
			for(LogData log:logs){
				String key=log.getUser()+","+log.getPid()+","+log.getHost();
				if(log.getType()==LogData.TYPE_LOGIN){
					loginMap.put(key, log);
				}
				if(log.getType()==LogData.TYPE_LOGOUT){					
					logoutMap.put(key, log);
				}
			}
			//System.out.println(loginMap.size());
			//System.out.println(logoutMap.size());
			Set<Entry<String,LogData>> logouts=logoutMap.entrySet();
			for(Entry<String,LogData> entry:logouts){
				String key=entry.getKey();
				LogData logout=entry.getValue();
				LogData login=loginMap.get(key);
				matches.add(new LogRec(login,logout));
				loginMap.remove(key);								
			}
			/*for(LogRec log:matches){
				System.out.println(log);
			}*/
			IOUtil.saveCollection(matches, logRecFile);
			IOUtil.saveCollection(loginMap.values(), loginFile);
			textLogFile.delete();
			return true;
		} catch (Exception e) {			
			e.printStackTrace();
		}		
		return false;		
	}
	/**
	 * 第三步工作,发送配对日志到服务端
	 * 读取第二步中生成的配对日志文件logRecFile并将每一条配对日志发送到服务端
	 * 当成功发送后,将该文件删除,表示第三步执行成功
	 */
	private boolean sendLogs(){		
			/*
			 * 流程:
			 * 1:必要的判断
			 *   1.1: logRecFile文件要存在
			 * 2:通过Socket连接服务端
			 * 3:将输出流转换为缓冲字符输出流,准备发送配对日志
			 * 4:将配对日志从logRecFile文件中读取出来,并存入一个List中准备发送
			 * 5:遍历List集合,将每一条配对日志发送到服务端
			 * 6:通过Socket创建输入流,并转换为缓冲字符输入流,等待读取服务器响应
			 * 7:当读取服务端的响应为"OK",表示服务端接收成功.将logRecFile文件删除,表示发送日志成功
			 */
		if(!logRecFile.exists()){
			System.out.println(logRecFile+"文件不存在!");
			return false;
		}
		Socket socket=null;
		try {	
			socket=new Socket(serverHost,serverPort);
			OutputStream out=socket.getOutputStream();
			PrintWriter pw=new PrintWriter(new OutputStreamWriter(out,"utf-8"));
			List<String> matches=IOUtil.loadLogRec(logRecFile);
			for(String log:matches){
				pw.println(log);
			}
			//当所有日志都发完了,发送一个字符串"OVER",表示所有日志均已发送
			pw.println("OVER");
			pw.flush();
			InputStream in=socket.getInputStream();
			BufferedReader br=new BufferedReader(new InputStreamReader(in,"utf-8"));
			//读取服务端发回来的响应
			String response=br.readLine();
			if("OK".equals(response)){
				logRecFile.delete();
				return true;
			}
		} catch (Exception e) {			
			e.printStackTrace();
		}finally{
			if(socket!=null){
				try {
					socket.close();
				} catch (IOException e) {					
					e.printStackTrace();
				}
			}
		}
		return false;		
	}
	/**
	 * 客户端开始工作的方法
	 * 循环执行1,2,3步
	 * 解析,配对,发送
	 * 最终将wtmpx文件中的日志全部配对发送给服务端完成数据收集工作
	 * @throws Exception 
	 */
	public void start() throws Exception{	
		while(true){			
			//第一步,解析
			if(!readNextLogs()){				
				continue;
			}			
			//第二步,配对
			if(!matchLogs()){
				continue;
			}			
			//第三步,发送配对日志
			if(!sendLogs()){
				continue;
			}									
		}
	}
	public static void main(String[] args) {		
		try {
			DMSClient client = new DMSClient();
			client.start();
		} catch (Exception e) {						
			System.out.println("客户端启动失败!");
			e.printStackTrace();
		}		
	}
}

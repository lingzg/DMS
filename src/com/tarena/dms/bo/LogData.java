package com.tarena.dms.bo;

/**
 * 该类是用来表示wtmpx日志文件中的一条日志的
 * 在wtmpx文件中,一条日志占用372个字节
 * 每天日志中的内容比较多,但我们这个项目中,只关注其中五个部分,分别是:
 * user,pid,time,type,host
 * @author tarena
 *
 */
public class LogData {
	/**
	 * 定义常量
	 * 一条日志文件占用的字节数
	 */
	public static final int LOG_LENGTH=372;
	/**
	 * 用户名在一条日志中的起始位置
	 */
	public static final int USER_OFFSET=0;
	/**
	 * 用户名在一条日志中占用的字节数
	 */
	public static final int USER_LENGTH=32;
	/**
	 * 进程ID在一条日志中的起始位置
	 */
	public static final int PID_OFFSET=68;
	/**
	 * 日志类型在一条日志中的起始位置
	 */
	public static final int TYPE_OFFSET=72;
	/**
	 * 日志类型对应的值,登入日志
	 */
	public static final int TYPE_LOGIN=7;
	/**
	 * 日志类型对应的值,登出日志
	 */
	public static final int TYPE_LOGOUT=8;
	/**
	 * 日志生成时间在一条日志中的起始位置
	 */
	public static final int TIME_OFFSET=80;
	/**
	 * 用户地址在一条日志中的起始位置
	 */
	public static final int HOST_OFFSET=114;
	/**
	 * 用户地址在一条日志中占用的字节数
	 */
	public static final int HOST_LENGTH=258;
	//用户名
	private String user;
	//进程号
	private int pid;
	//日志生成时间
	private int time;
	//日志类型
	private short type;
	//用户的地址
	private String host;
	
	public LogData(){
		
	}
	
	public LogData(String user, int pid, int time, short type, String host) {
		super();
		this.user = user;
		this.pid = pid;
		this.time = time;
		this.type = type;
		this.host = host;
	}
	
	/**
	 * 根据给定的字符串创建一个LogData实例
	 * 该字符串应当是当前类的toString方法返回的格式
	 * @param log
	 */
	public LogData(String log){
		String[] data=log.split(",");		   
		this.user=data[0];
		this.pid=Integer.parseInt(data[1]);
		this.time=Integer.parseInt(data[2]);
		this.type=Short.parseShort(data[3]);
		this.host=data[4];
	}
	
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public int getPid() {
		return pid;
	}
	public void setPid(int pid) {
		this.pid = pid;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public short getType() {
		return type;
	}
	public void setType(short type) {
		this.type = type;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String toString() {
		return user + "," + pid + "," + time+ "," + type + "," + host;
	}
	
}

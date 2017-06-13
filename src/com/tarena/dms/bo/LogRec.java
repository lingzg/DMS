package com.tarena.dms.bo;

/**
 * 该类用于表示一对配对日志
 * @author tarena
 *
 */
public class LogRec {
	private LogData login;
	private LogData logout;
	public LogRec(){
		
	}
	public LogRec(LogData login, LogData logout) {
		super();
		this.login = login;
		this.logout = logout;
	}
	public LogData getLogin() {
		return login;
	}
	public void setLogin(LogData login) {
		this.login = login;
	}
	public LogData getLogout() {
		return logout;
	}
	public void setLogout(LogData logout) {
		this.logout = logout;
	}
	@Override
	public String toString() {
		return login + "|" + logout;
	}
	
}

package com.companyname.project.logGrepper;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface LogGrepperServerIntf extends Remote {
	public List<String> logGrep (String start_time, String end_time,
			String a_pattern, String a_path) throws RemoteException;
	public List<String> logGrep (String a_pattern, String a_path) throws RemoteException;
}
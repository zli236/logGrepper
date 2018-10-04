package com.companyname.project.logGrepper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class LogGrepper extends UnicastRemoteObject implements LogGrepperServerIntf{

	public class Cache {
		public LinkedHashMap<String, String> cache;
		private int cache_capacity;
		private String cache_dir;

		public Cache (int capacity, String cacheDir) {
			cache_capacity = capacity;
			cache = new LinkedHashMap<String, String> ();
			cache_dir = cacheDir;
			File theDir = new File(cacheDir);

			// if the directory does not exist, create it
			if (!theDir.exists()) {
				System.out.println("creating directory: " + theDir.getName());
				boolean result = false;

				try{
					theDir.mkdir();
					result = true;
				} 
				catch(SecurityException se){
					//handle it
				}        
				if(result) {    
					System.out.println("DIR created");  
				}
			}
		}

		public boolean contains(String key) {
			return cache.containsKey(key);
		}

		public List<String> getFromCache(String key) {
			String fileName = cache.get(key);
			File file = new File(fileName);
			ArrayList<String> lines = new ArrayList<String>();
			FileInputStream inputStream = null;
			Scanner sc = null;
			try {
				inputStream = new FileInputStream(file);
				sc = new Scanner(inputStream, "UTF-8");
				while (sc.hasNextLine()) {
					lines.add(sc.nextLine());
				}
				// note that Scanner suppresses exceptions
				if (sc.ioException() != null) {
					throw sc.ioException();
				}
			}
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (sc != null) {
					sc.close();
				}
			}
			//refresh the position of fileName in the linkedHashSet
			cache.remove(key);
			cache.put(key, fileName);
			return lines;
		}

		public void putInCache(String key, List<String> lines) {
			if(lines == null)
				return;
			String fileName = cache_dir+System.currentTimeMillis();
			try {
				Files.write(Paths.get(fileName), lines, Charset.forName("UTF-8"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(cache.size() == cache_capacity)
			{
				String lru = cache.keySet().iterator().next();
				cache.remove(lru);
			}
			cache.put(key, fileName);
		}

		public void clearCache() {
			for(String fileName: cache.values())
			{
				File file = new File(fileName);
				file.delete();
				cache.clear();
			}
		}
	}

	private Cache cache;
	private static final String cache_dir = "cache/";
	private static final int cache_capacity = 1000;
	private static final long serialVersionUID = 1L;
	private static final String dateTimeFormatPattern = "dd/MMM/yyyy:HH:mm:ss Z";

	protected LogGrepper() throws RemoteException {
		// required to avoid the 'rmic' step
		super();
		cache = new Cache(cache_capacity, cache_dir);
	}

	public static void main(String args[]) throws Exception {
		System.out.println("logGrepper server started");

		try { //special exception handler for registry creation
			LocateRegistry.createRegistry(1099); 
			System.out.println("java RMI registry created.");
		} catch (RemoteException e) {
			//do nothing, error means registry already exists
			System.out.println("java RMI registry already exists.");
		}

		//Instantiate logGrepper Server

		LogGrepper obj = new LogGrepper();

		// Bind this object instance to the name "RmiServer"
		Naming.rebind("//localhost/logGrepper", obj);
		System.out.println("PeerServer bound in registry");
	}

	public Cache getCache() {
		return this.cache;
	}

	public String buildKey(String a_pattern, String a_path, String start_time, String end_time) {
		if(start_time == null && end_time == null) {
			return a_pattern+"_"+a_path;
		}
		return a_pattern+"_"+a_path+"_"+start_time+"_"+end_time;
	}
	/* Grep pattern within time period (start_time, end_time) from a directory of files */
	public List<String> logGrep (String start_time, String end_time,
			String a_pattern, String a_path) {
		//If already in cache, get search result from cache
		String key = buildKey(a_pattern, a_path, start_time, end_time);
		List<String> lines = new ArrayList<String> ();
		if(cache.contains(key)) {
			lines = cache.getFromCache(key);
			return lines;
		}

		File dir = new File(a_path);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File file : directoryListing) {
				//recursive call if file is a directory
				if(file.isDirectory())
					lines.addAll(this.logGrep(start_time, end_time, a_pattern, file.getName()));
				// Grep from file
				logGrep(start_time, end_time, a_pattern, file, lines);
			}
		} else {
			// Handle the case where dir is not really a directory.
			System.out.println("Empty directory or invalid directory path.");
		}
		cache.putInCache(key, lines);
		return lines;
	}

	/* Grep pattern within time period (start_time, end_time) from file */
	public static void logGrep (String start_time, String end_time,
			String a_pattern, File a_file, List<String> a_lines) {
		if(a_file == null)
			return;
		FileInputStream inputStream = null;
		Scanner sc = null;
		try {
			inputStream = new FileInputStream(a_file);
			sc = new Scanner(inputStream, "UTF-8");
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				// split line by date-time string, assume all date-time string is surrounded by []
				int left = line.indexOf("[");
				int right = line.indexOf("]");
				// has date-time info, process it.
				if(left >= 0 && right >= 0)
				{
					String dateTime = line.substring(left+1, right);
					if(!isBetweenDateTime(dateTime, start_time, end_time))
						continue;
					String rest = line.substring(0, left) + line.substring(right+1, line.length());
					if(hasMatch(a_pattern, rest))
						a_lines.add(line);
				}
				// Otherwise treat it as time-date match so that we don't lose possible match of pattern.
				else
				{
					if(hasMatch(a_pattern, line))
						a_lines.add(line);
				}
			}
			// note that Scanner suppresses exceptions
			if (sc.ioException() != null) {
				throw sc.ioException();
			}
		} 
		catch (FileNotFoundException e) {}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (sc != null) {
				sc.close();
			}
		}
	}

	/* Grep a_pattern from directory path a_path, result will be stored in a_lines */
	public List<String> logGrep(String a_pattern, String a_path) {
		//If already in cache, get search result from cache
		String key = buildKey(a_pattern, a_path, null, null);
		List<String> lines = new ArrayList<String> ();
		if(cache.contains(key)) {
			lines = cache.getFromCache(key);
			return lines;
		}

		File dir = new File(a_path);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File file : directoryListing) {
				//recursive call if file is a directory
				if(file.isDirectory())
					lines.addAll(logGrep(a_pattern, file.getName()));
				// Grep from file
				logGrep(a_pattern, file, lines);
			}
		} else {
			// Handle the case where dir is not really a directory.
			System.out.println("Empty directory or invalid directory path.");
		}
		cache.putInCache(key, lines);
		return lines;
	}

	/* Grep a_pattern from a_fine, result will be stored in a_lines */
	public static void logGrep(String a_pattern, File a_file, List<String> a_lines) {
		if(a_file == null)
			return;
		FileInputStream inputStream = null;
		Scanner sc = null;
		try {
			inputStream = new FileInputStream(a_file);
			sc = new Scanner(inputStream, "UTF-8");
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if(hasMatch(a_pattern, line))
					a_lines.add(line);
				// System.out.println(line);
			}
			// note that Scanner suppresses exceptions
			if (sc.ioException() != null) {
				throw sc.ioException();
			}
		} 
		catch (FileNotFoundException e) {}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (sc != null) {
				sc.close();
			}
		}
	}

	public static String[] splitByDateTime (String a_line) {
		// split line by date-time string, assume all date-time string is surrounded by []
		int left = a_line.indexOf("[");
		int right = a_line.indexOf("]");
		String dateTime = a_line.substring(left+1, right);
		String rest = a_line.substring(0, left) + a_line.substring(right+1, a_line.length());
		return new String[] {dateTime, rest};
	}

	public static boolean isBetweenDateTime (String a_dateTime, String a_start, String a_end) {
		final DateTimeFormatter formatter =
				DateTimeFormatter.ofPattern(dateTimeFormatPattern);
		LocalDateTime dateTime = ZonedDateTime.parse(a_dateTime, formatter).toLocalDateTime();
		LocalDateTime start = ZonedDateTime.parse(a_start, formatter).toLocalDateTime();
		LocalDateTime end = ZonedDateTime.parse(a_end, formatter).toLocalDateTime();
		return dateTime.isBefore(start) || dateTime.isAfter(end) ? false : true;
	}

	/* return true if regular expression pattern a_pattern occurs in a_line */
	public static boolean hasMatch(String a_pattern, String a_line) {
		if(a_pattern == null || a_line == null)
			return false;
		Pattern p = Pattern.compile(a_pattern);
		return p.matcher(a_line).find();
	}
}

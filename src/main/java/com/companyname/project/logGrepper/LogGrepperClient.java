package com.companyname.project.logGrepper;

import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;

public class LogGrepperClient { 
	public LogGrepperClient() {
		super();
		// TODO Auto-generated constructor stub
	}

	public static void main(String args[]) throws Exception {
		LogGrepperServerIntf obj = (LogGrepperServerIntf)Naming.lookup("//localhost/logGrepper");
		List<String> lines = new ArrayList<String>();
		TextIO textIO = TextIoFactory.getTextIO();
		String pattern = null;
		String path = null;
		String start = null;
		String end = null;

		while (true) {
			int choice = textIO.newIntInputReader()
					.withMinVal(1)
					.withMaxVal(2)
					.read("1-Search without time range\n"
							+ "2-Search with time range:\n");

			switch (choice) {
			case 1:
				pattern = textIO.newStringInputReader()
				.read("Pattern:\n");

				path = textIO.newStringInputReader()
						.read("Directory path:\n");

				lines = obj.logGrep(pattern, path);

				break;
			case 2:
				pattern = textIO.newStringInputReader()
				.withDefaultValue("")
				.read("Pattern:\n");

				path = textIO.newStringInputReader()
						.read("Directory path:\n");

				start = textIO.newStringInputReader()
						.read("Starting time: dd/MMM/yyyy:HH:mm:ss Z:\n");

				end = textIO.newStringInputReader()
						.read("Ending time: dd/MMM/yyyy:HH:mm:ss Z:\n");

				lines = obj.logGrep(start, end, pattern, path);
				break;
			}

			int pageSize = 25;
			int j;
			List<List<String>> pages = getPages(lines, pageSize);
			for(int i = 0; i < pages.size();)
			{
				for(String line: pages.get(i))
					textIO.getTextTerminal().printf(line+"\n");
				textIO.getTextTerminal().printf("page "+i+"\n");
				textIO.getTextTerminal().printf("Total # of matches: "+lines.size()+"\n");
				j = textIO.newIntInputReader().read("1.First page 2.Previous page 3.Next Page 4.Last page 0.Exit \n");
				switch (j) {
				case 0:
						i = pages.size();
						break;
				case 1:
						i = 0;
						break;
				case 2:
						i = i==0? 0 : i-1;
						break;
				case 3:
						i++;
						break;
				case 4:
						i = pages.size() - 1;
						break;
				}
			}
		}
	}

	// Pagination
	public static <T> List<List<T>> getPages(Collection<T> c, Integer pageSize) {
		if (c == null)
			return Collections.emptyList();
		List<T> list = new ArrayList<T>(c);
		if (pageSize == null || pageSize <= 0 || pageSize > list.size())
			pageSize = list.size();
		int numPages = (int) Math.ceil((double)list.size() / (double)pageSize);
		List<List<T>> pages = new ArrayList<List<T>>(numPages);
		for (int pageNum = 0; pageNum < numPages;)
			pages.add(list.subList(pageNum * pageSize, Math.min(++pageNum * pageSize, list.size())));
		return pages;
	}

	/*
	public static void main(String args[]) throws Exception {
        RmiServerIntf obj = (RmiServerIntf)Naming.lookup("//localhost/logGrepper");
        String path = "data/2files";
	    String pattern = ".*NASA.*[.]gif";
	    String start = "01/Jul/1994:02:05:50 -0400";
	    String end = "01/Jul/1996:03:05:55 -0400";
	    List<String> lines = new ArrayList<String>();
	    lines = obj.logGrep(start, end, pattern, path, lines);

        System.out.println(lines.size()); 
    }
	 */
}
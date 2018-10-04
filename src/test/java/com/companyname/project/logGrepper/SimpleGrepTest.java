package com.companyname.project.logGrepper;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;


public class SimpleGrepTest {

	@Test
	public void testGrepFromFile() {
		int expectedLineCount = 573;
		File file = new File("data/nasa-log-jul95aa");
		String pattern = ".*NASA.*[.]gif";
		List<String> lines = new ArrayList<String>();
		LogGrepper.logGrep(pattern, file, lines);

		//System.out.println(lines);
		assertEquals(expectedLineCount, lines.size());
	}

	@Test
	public void testGrepFromPath() throws RemoteException {
		LogGrepper grep = new LogGrepper();
		int expectedLineCount = 1207;
		String path = "data/2files/";
		String pattern = ".*NASA.*[.]gif";
		List<String> lines = grep.logGrep(pattern, path);
		assertEquals(expectedLineCount, lines.size());
	}

	@Test
	public void testGrepFromEmptyDirectory() throws RemoteException {
		LogGrepper grep = new LogGrepper();
		int expectedLineCount = 0;
		String path = "data/0file/";
		String pattern = ".*NASA.*[.]gif";
		List<String> lines = grep.logGrep(pattern, path);
		assertEquals(expectedLineCount, lines.size());
	}

	@Test
	public void testNullPattern() {
		int expectedLineCount = 0;
		File file = new File("data/nasa-log-jul95aa");
		String pattern = null;
		List<String> lines = new ArrayList<String>();
		LogGrepper.logGrep(pattern, file, lines);

		//System.out.println(lines);
		assertEquals(expectedLineCount, lines.size());
	}

	@Test
	public void testNullFile() {
		int expectedLineCount = 0;
		File file = null;
		String pattern = null;
		List<String> lines = new ArrayList<String>();
		LogGrepper.logGrep(pattern, file, lines);

		//System.out.println(lines);
		assertEquals(expectedLineCount, lines.size());
	}

	@Test
	public void testInvalidPath() throws RemoteException {
		LogGrepper grep = new LogGrepper();
		int expectedLineCount = 0;
		String path = "dat/0file";
		String pattern = ".*NASA.*[.]gif";
		List<String> lines = grep.logGrep(pattern, path);
		assertEquals(expectedLineCount, lines.size());
	}

	@Test
	public void testSplitDateTime() {
		String line = "ppp-mia-47.shadow.net - - [01/Jul/1995:03:05:50 -0400] \"GET /shuttle/countdown/video/livevideo.jpeg HTTP/1.0\" 200 32300";
		String expectedDateTime = "01/Jul/1995:03:05:50 -0400";
		String[] str = LogGrepper.splitByDateTime(line);
		System.out.println(str[0]);
		System.out.println(str[1]);
		assertEquals(expectedDateTime, str[0]);	    
	}

	@Test
	public void testIsBetweenDateTime() {
		String dateTime = "01/Jul/1995:03:05:50 -0400";
		String start = "01/Jul/1995:02:05:50 -0400";
		String end = "01/Jul/1995:03:05:55 -0400";
		assertEquals(true, LogGrepper.isBetweenDateTime(dateTime, start, end));

		start = "01/Dec/1995:02:05:50 -0400";
		end = "01/Jan/1996:03:05:55 -0400";
		assertEquals(false, LogGrepper.isBetweenDateTime(dateTime, start, end));
	}

	@Test
	public void testGrepFromFileWithDateTime() {
		int expectedLineCount = 573;
		File file = new File("data/nasa-log-jul95aa");
		String pattern = ".*NASA.*[.]gif";
		String start = "01/Jul/1994:02:05:50 -0400";
		String end = "01/Jul/1996:03:05:55 -0400";
		List<String> lines = new ArrayList<String>();
		LogGrepper.logGrep(start, end, pattern, file, lines);
		assertEquals(expectedLineCount, lines.size());

		expectedLineCount = 0;
		lines.clear();
		start = "01/Jul/1995:00:00:11 -0400";
		end = "01/Jul/1995:00:00:01 -0400";
		lines = new ArrayList<String>();
		LogGrepper.logGrep(start, end, pattern, file, lines);
		assertEquals(expectedLineCount, lines.size());
	}

	@Test
	public void testGrepFromPathWithDateTime() throws RemoteException {
		LogGrepper grep = new LogGrepper();
		int expectedLineCount = 1207;
		String path = "data/2files";
		String pattern = ".*NASA.*[.]gif";
		String start = "01/Jul/1994:02:05:50 -0400";
		String end = "01/Jul/1996:03:05:55 -0400";
		List<String> lines = grep.logGrep(start, end, pattern, path);
		assertEquals(expectedLineCount, lines.size());

		expectedLineCount = 0;
		lines.clear();
		start = "01/Jul/1995:00:00:11 -0400";
		end = "01/Jul/1995:00:00:01 -0400";
		lines = grep.logGrep(start, end, pattern, path);
		assertEquals(expectedLineCount, lines.size());
	}

	@Test
	public void testCache() throws RemoteException {
		LogGrepper grep = new LogGrepper();
		int expectedLineCount = 1207;
		String path = "data/2files";
		String pattern = ".*NASA.*[.]gif";
		String start = "01/Jul/1994:02:05:50 -0400";
		String end = "01/Jul/1996:03:05:55 -0400";
		List<String> lines = grep.logGrep(start, end, pattern, path);
		assertEquals(expectedLineCount, lines.size());

		String key = grep.buildKey(pattern, path, start, end);
		assertEquals(true, grep.getCache().contains(key));
		assertEquals(expectedLineCount, grep.getCache().getFromCache(key).size());
	}

	@Test
	public void testGrepMultiLevelDir() throws RemoteException {
		LogGrepper grep = new LogGrepper();
		int expectedLineCount = 555;
		String path = "data/1file";
		String pattern = ".*NASA.*[.]gif";
		List<String> lines = grep.logGrep(pattern, path);
		assertEquals(expectedLineCount, lines.size());
		System.out.println(lines);
	}

}

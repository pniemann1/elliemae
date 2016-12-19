package elliemae;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Entries {
	private final StringBuilder buf = new StringBuilder();
	private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public Entries(){}
	
	public synchronized void addEntry(long timestamp, String filename, String dirTopLevel, long threadId){
		String formattedTimeStamp = format.format(new Date(timestamp)).toString();
		buf.append(formattedTimeStamp + '\t' + filename + '\t' + dirTopLevel + '\t' + threadId + '\n');
	}
	
	// print to file
	public synchronized void createReport(String path){
		
		System.out.println(buf.toString());
	}
}

package elliemae;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A thread safe entry logger.
 */
public class Entries {
	private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	File reportFile;
	BufferedWriter bw;
	FileWriter fw;
	
	public Entries(String path) throws IOException{
		reportFile = new File(path, "directoryTraverserEntryOutput.txt");
		bw = new BufferedWriter(fw = new FileWriter(reportFile));
	}
	
	/**
	 * Formats a time stamp and writes entries to a file.
	 * @param timestamp
	 * @param filename
	 * @param dirTopLevel
	 * @param threadId
	 */
	public synchronized void addEntry(long timestamp, String filename, String dirTopLevel, long threadId){
		StringBuilder buf = new StringBuilder();
		String formattedTimeStamp = format.format(new Date(timestamp)).toString();
		buf.append(formattedTimeStamp);
		buf.append('\t');
		buf.append(filename);
		buf.append('\t');
		buf.append(dirTopLevel);
		buf.append('\t');
		buf.append(threadId);
		
		try {
			bw.write(buf.toString());
			bw.newLine();
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Closes file handles.  This method should be called after there are no more entries to write
	 * to clean up open file handles.
	 */
	public synchronized void closeReader(){
		try {
			if (bw != null)
				bw.close();
			if (fw != null)
				fw.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}

package elliemae;

public class Entry{
	private long timestamp;
	private String filename;
	private String dirTopLevel;
	private long threadId;
	
	Entry(long timestamp, String filename, String dirTopLevel, long threadId){
		this.timestamp = timestamp;
		this.filename = filename;
		this.dirTopLevel = dirTopLevel;
		this.threadId = threadId;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public String getFilename() {
		return filename;
	}

	public String getDirTopLevel() {
		return dirTopLevel;
	}


	public long getThreadId() {
		return threadId;
	}
}

package elliemae;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The DirectoryWalker class recursively finds files in a directory.  An in memory cache tracks
 * the count of unique file names.  The entries found are logged in a file as they are found so
 * that data is kept as up to date as is possible.
 */
public class DirectoryWalker implements Runnable {
	
	private File dir;
	private ConcurrentHashMap<String, AtomicInteger> countMap;
	private EntryLogger entryLogger;
	private String topLevel;
	private static final String LOCKFILE = "lockfile";
	private ArrayList<Entry> entryList = new ArrayList<>();
	
	/**
	 * Constructor
	 * @param dir the dir to be traversed
	 * @param map a map to hold the in memory file counts
	 * @param entries a class to log file entries to a file
	 */
	public DirectoryWalker(File dir, ConcurrentHashMap<String, AtomicInteger> map, EntryLogger entries){
		this.dir = dir;
		this.countMap = map;
		this.entryLogger = entries;
		topLevel = dir.getName();
	}
	
	public String getTopLevel(){return topLevel;}
	
	/**
	 * Recursively traverses the dir.  Updates an in memory counter for file names and adds
	 * entries found to a file.
	 * @param dir
	 */
	private void traverse(File dir){	
	    if (dir.isDirectory()) {
	        String[] children = dir.list();
	        if (children != null){
		        for (int i = 0; i < children.length; i++) {
		            traverse(new File(dir, children[i]));
		        }
	        }
	    }
	    if (dir.isFile() && !dir.getName().equals(LOCKFILE)) {
	    	AtomicInteger atomic = null;
	    	atomic = countMap.putIfAbsent(dir.getName(), new AtomicInteger(1));
	    	if (atomic != null){
	    		atomic.addAndGet(1);
	    	}
	
	    	entryList.add(new Entry(System.currentTimeMillis(), topLevel, dir.getName(), Thread.currentThread().getId()));
	    }
	}

	@Override
	public void run() {
		FileOutputStream out = null;
		java.nio.channels.FileLock lock = null;
		
		try {
			// create a lockfile if it does not exist
			File lockfile = new File(dir, LOCKFILE);
			if (!lockfile.exists()) {
				lockfile.createNewFile();
			}
			
			// get a lock to the file
			out = new FileOutputStream(new File(dir, LOCKFILE));
			FileChannel channel = out.getChannel();
			
			while (lock == null){
				try {
					lock = channel.tryLock();
				}
				catch (OverlappingFileLockException ex){
					//ex.printStackTrace();
					//System.out.println("sleeping getting lockfile");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			// traverse the directory
			traverse(dir);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
		    try {
		    	if (lock != null)
		    		lock.release();
		    	if(out != null)
		    		out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		entryLogger.logEntry(entryList);
	}
}

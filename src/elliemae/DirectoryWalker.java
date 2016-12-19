package elliemae;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The DirectoryWalker class recursively finds files in a directory.  An in memory cache tracks
 * the count of unique file names.  The entries found are logged in a file as they are found so
 * that data is kept as up to date as is possible.
 */
public class DirectoryWalker implements Runnable {
	
	private File dir;
	private ConcurrentHashMap<String, AtomicInteger> map;
	private Entries entries;
	private String topLevel;
	
	/**
	 * Constructor
	 * @param dir the dir to be traversed
	 * @param map a map to hold the in memory file counts
	 * @param entries a class to log file entries to a file
	 */
	public DirectoryWalker(File dir, ConcurrentHashMap<String, AtomicInteger> map, Entries entries){
		this.dir = dir;
		this.map = map;
		this.entries = entries;
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
	    if (dir.isFile()) {
	    	if( map.putIfAbsent(dir.getName(), new AtomicInteger(1)) != null ) {    
	    		  map.get(dir.getName()).addAndGet(1);
	    	}
	
	    	entries.addEntry(System.currentTimeMillis(), topLevel, dir.getName(), Thread.currentThread().getId());
	    }
	}


	@Override
	public void run() {
		traverse(dir);
	}

}

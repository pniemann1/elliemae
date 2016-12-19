package elliemae;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectoryWalker implements Runnable {
	
	private File dir;
	private ConcurrentHashMap<String, AtomicInteger> map;
	private Entries entries;
	private String topLevel;
	
	public DirectoryWalker(File dir, ConcurrentHashMap<String, AtomicInteger> map, Entries entries){
		this.dir = dir;
		this.map = map;
		this.entries = entries;
		topLevel = dir.getName();
	}
	
	public String getTopLevel(){return topLevel;}
	
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

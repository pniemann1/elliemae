package elliemae;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectoryTraverser {
	private ThreadPoolExecutor executor;
	private ConcurrentHashMap<String, AtomicInteger> map = new ConcurrentHashMap<>();
	private Entries entries;
	private int threadCounter = 0;
	
	// properties
	private static final String NUMBER_OF_THERADS = "numberOfThreads";
	private static final String REPORT_OUTPUT_PATH = "reportOutPutPath";
	private static final String DIR_A_PATH = "dirAPath";
	private static final String DIR_B_PATH = "dirBPath";
	private static final String DIR_C_PATH = "dirCPath";

	private Integer numberOfThreads;
	private String reportOutPutPath;
	private String dirAPath;
	private String dirBPath;
	private String dirCPath;
	
	
	public DirectoryTraverser(){
		readProperties();
		createThreadPoolExecutor();
		try {
			entries = new Entries(reportOutPutPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readProperties(){
		try {
			Properties props = new Properties();
			props.load(this.getClass().getResourceAsStream("config.properties"));
			numberOfThreads = Integer.valueOf(props.getProperty(NUMBER_OF_THERADS, "10"));
			reportOutPutPath = props.getProperty(REPORT_OUTPUT_PATH, "C:\\elliemae");
			dirAPath = props.getProperty(DIR_A_PATH, "C:\\elliemae/a");
			dirBPath = props.getProperty(DIR_B_PATH, "C:\\elliemae/b");
			dirCPath = props.getProperty(DIR_C_PATH, "C:\\elliemae/c");
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	private void createThreadPoolExecutor(){
		BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(50);
		executor = new CustomThreadPoolExecutor(numberOfThreads, numberOfThreads, 5000, TimeUnit.MILLISECONDS, blockingQueue);
		executor.prestartAllCoreThreads();
	
		executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				//System.out.println("DirectoryWalker Rejected for dir: "+ ((DirectoryWalker) r).getTopLevel() + "\n" +
						//"Waiting for a second and adding again !!");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				executor.execute(r);
			}
		});
	}
	
	/**
	 * This is the way to add dirs to be traversed to the ThreadPoolExecutor.
	 * @param dir
	 */
	public void addTask(File dir){
		threadCounter++;
		//System.out.println("Adding Task : " + threadCounter);
		executor.execute(new DirectoryWalker(dir, map, entries));
	}
	
	public void shutdownThreadPoolExecutor(){
		executor.shutdown();
		try {
			executor.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(executor.isShutdown())
			entries.closeReader();
		
		printInMemoryReport();
	}
	
	/**
	 * print map to console and file
	 */
	public void printInMemoryReport(){
		map.forEach((key, value) -> System.out.println(key + " - " + value));
	}
	
	public String getDirAPath() {
		return dirAPath;
	}

	public String getDirBPath() {
		return dirBPath;
	}

	public String getDirCPath() {
		return dirCPath;
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args){
		DirectoryTraverser traverser = new DirectoryTraverser();
		
		int counter = 0;
		while (true) {
			counter++;
			// Adding threads one by one
			traverser.addTask(new File(traverser.getDirAPath()));
			traverser.addTask(new File(traverser.getDirBPath()));
			traverser.addTask(new File(traverser.getDirCPath()));
			if(counter == 50) break;
		}
		
		traverser.shutdownThreadPoolExecutor();
	}
}

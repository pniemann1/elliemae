package elliemae;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * DirectoryTraverser traverses directories using a Thread Pool Executor and an ArrayBlockingQueue.  
 * The queue is set to 50 and any rejected executions are added back to the queue with a 1 second
 * delay.  The thread pool size is configurable but the queue size is not.
 */
public class DirectoryTraverser {
	private ThreadPoolExecutor executor;
	private ConcurrentHashMap<String, AtomicInteger> map = new ConcurrentHashMap<>();
	private Entries entries;
	private TimerTask timerTask;
	private Timer timer;
	private int threadCounter = 0;
	
	// properties
	private static final String NUMBER_OF_THERADS = "numberOfThreads";
	private static final String REPORT_OUTPUT_PATH = "reportOutPutPath";
	private static final String RUNTIME_MILLIS = "runTimeMillis";
	private static final String DIR_A_PATH = "dirAPath";
	private static final String DIR_B_PATH = "dirBPath";
	private static final String DIR_C_PATH = "dirCPath";

	private Integer numberOfThreads;
	private long runTimeMillis;
	private String reportOutPutPath;
	private String dirAPath;
	private String dirBPath;
	private String dirCPath;
	
	/**
	 * DirectoryTraverser constructor.  Reads Properties, creates a threadpoolexecutor and a configurable
	 * timer task to shutdown the executor.
	 */
	public DirectoryTraverser(){
		readProperties();
		createThreadPoolExecutor();
		
		try {
			entries = new Entries(reportOutPutPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		createTimerTask();
		timer = new Timer();
		timer.schedule(timerTask, runTimeMillis);
	}

	private void readProperties(){
		try {
			Properties props = new Properties();
			props.load(this.getClass().getResourceAsStream("config.properties"));
			numberOfThreads = Integer.valueOf(props.getProperty(NUMBER_OF_THERADS, "10"));
			runTimeMillis = Long.valueOf(props.getProperty(RUNTIME_MILLIS, "10000"));
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
	 * Creates a CustomThreadPoolExector.  The core threads are prestarted.  The rejection handler
	 * will add the tasks back to the queue with a 1 second delay.
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
	 * Add dirs to be traversed to the ThreadPoolExecutor.
	 * @param dir
	 */
	public void addTask(File dir){
		System.out.println("Adding Task : " + ++threadCounter);
		executor.execute(new DirectoryWalker(dir, map, entries));
	}
	
	private void createTimerTask(){
		timerTask = new TimerTask(){
			@Override
			public void run() {
				shutdownThreadPoolExecutor();
				timer.cancel();
			}	
		};
	}
	
	/**
	 * Shuts down the thread pool executor.  The shutdown will wait up to 10 minutes for tasks
	 * to complete.  After shutdown the realtime data entries file handles are closed and the 
	 * in memory report is generated.  A system.exit is called to terminate the program.
	 */
	public void shutdownThreadPoolExecutor(){
		executor.shutdown();
		try {
			System.out.println("waiting termination");
			executor.awaitTermination(10, TimeUnit.MINUTES);
			System.out.println("waiting termination done");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(executor.isShutdown())
			entries.closeReader();
		
		printInMemoryReport();
		// exit successfully
		System.exit(0);
	}
	
	/**
	 * Prints in memory map to console and file
	 */
	public void printInMemoryReport(){
		// print to console
		map.forEach((key, value) -> System.out.println(key + " - " + value));
		
		// print to file
		File reportFile = new File(reportOutPutPath, "directoryTraverserInMemoryOutput.txt");
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile))){
			map.forEach((key, value) -> {
				try {
					bw.write(key + " - " + value);
					bw.newLine();
					bw.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	 * The DirectoryTraverser Driver adds tasks with directories to be read to the Executor.
	 * The main thread will exit when the Executor timer expires as System.exit is called in
	 * the shutdownThreadPoolExecutor method.
	 * @param args
	 */
	public static void main(String[] args){
		DirectoryTraverser traverser = new DirectoryTraverser();
		
		while (true) {
			// Adding threads one by one
			traverser.addTask(new File(traverser.getDirAPath()));
			traverser.addTask(new File(traverser.getDirBPath()));
			traverser.addTask(new File(traverser.getDirCPath()));
		}
	}
}

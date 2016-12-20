package elliemae;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The CustomThreadPoolExecutor uses beforetask and aftertask methods to ensure
 * that a dir is not being processed by two tasks at the same time.
 */
public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
	private ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
	
	public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}
	
	
	/**
	 * Makes sure a dir is not being processed by 2 tasks at the same time.
	 * A map is used to check if a dir is being processed.  If a dir is being processed
	 * the task is removed from the executor and is submitted back to the queue.
	 */
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		//System.out.println("Perform beforeExecute() logic");
		// if the dir is already being worked on then remove and requeue the task
		synchronized(this){
			if (map.containsKey(((DirectoryWalker) r).getTopLevel())){
				if(!this.isShutdown()){
					//System.out.println("Removing task and adding to queue");
					super.remove(r);
					super.execute(r);
				} else {
					while (map.containsKey(((DirectoryWalker) r).getTopLevel())){
						// if in shutdown then sleep until it can work on the dir
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					//System.out.println("adding to map from shutdown");
					map.put( ((DirectoryWalker) r).getTopLevel(), 1);
				}
			}
			else {
				//System.out.println("adding to map");
				map.put( ((DirectoryWalker) r).getTopLevel(), 1);
			}
			super.beforeExecute(t, r);
		}
	}
	
	/**
	 * After a task is executed the dir entry is removed from the map so that it can be 
	 * processed again by any new task.
	 */
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		//System.out.println("Perform afterExecute() logic");
		//System.out.println("removing from map");
		map.remove(((DirectoryWalker) r).getTopLevel());
	}
}

package elliemae;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
	private ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
	
	public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}
	
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		//System.out.println("Perform beforeExecute() logic");
		// if the dir is already being worked on then remove and requeue the task
		if (map.containsKey(((DirectoryWalker) r).getTopLevel())){
			super.remove(r);
			if(!this.isShutdown())
				super.execute(r);
		}
		else{
			map.put( ((DirectoryWalker) r).getTopLevel(), 1);
		}
		super.beforeExecute(t, r);
	}
	
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		//System.out.println("Perform afterExecute() logic");
		map.remove(((DirectoryWalker) r).getTopLevel());
	}

}

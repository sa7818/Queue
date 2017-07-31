import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.*;
import java.util.concurrent.*;

public class GroceryQueue implements Runnable {

	private int maxQueueLength;
	private int numCustomersInQueue = 0;
	private Customer[] customerQ;	
	private GroceryQueuePopper groceryqpopper;
	private Condition customerfinished;
	private Condition customerremoved;
	private ReentrantLock groceryqlock;
	Customer nextCustomer;
	
	public GroceryQueue(ReentrantLock clockLock, ReentrantLock queueLock, Condition clockTicked, Condition customerFinished, Condition customerArrived,
			Condition customerRemoved, Semaphore groceryQSemaphore, int maxQLength)
	{
		maxQueueLength = maxQLength;
		numCustomersInQueue = 0;
		customerfinished = customerFinished;
		customerremoved = customerRemoved;
		groceryqlock = queueLock;
		nextCustomer = null;
		customerQ = new Customer[maxQLength];
		groceryqpopper = new GroceryQueuePopper();
	}
	
	// This thread just pops off customers off the grocery queue when they have been served
	private class GroceryQueuePopper implements Runnable
	{
		boolean interrupted = false;
		public void run() {			
			while (!interrupted)
			{
				try {
					groceryqlock.lockInterruptibly();
					customerfinished.await(30, TimeUnit.SECONDS);
					nextCustomer = popFromQueue();
					customerremoved.signal();
					groceryqlock.unlock();
				}
				catch (InterruptedException e)
				{				
					interrupted = true;
					customerfinished.signalAll();
					customerremoved.signalAll();
				}							
			}			
		}
		
		public void setInterrupted()
		{
			interrupted = true;
		}
	}
	
	public boolean isQueueFull()
	{
		groceryqlock.lock();
		boolean retval = (numCustomersInQueue == maxQueueLength);
		groceryqlock.unlock();
		return retval;
	}
	
	public int queueLength()
	{
		return numCustomersInQueue;
	}
	
	public void setInterrupted()
	{
		groceryqpopper.setInterrupted();
	}
	
	public boolean isQueueEmpty()
	{
		try 
		{
			groceryqlock.lockInterruptibly();
			boolean retval = (numCustomersInQueue <= 0);
			groceryqlock.unlock();
			return retval;
		}
		catch (InterruptedException e)
		{
			return false;
		}
	}
	
	public int addToQueue(Customer currentCustomer)
	{
		try {
			groceryqlock.lockInterruptibly();
			if (numCustomersInQueue < maxQueueLength)
			{
				currentCustomer.myPositionInQ = numCustomersInQueue;
				customerQ[numCustomersInQueue] = currentCustomer;
				numCustomersInQueue++;
				groceryqlock.unlock();
				return numCustomersInQueue-1;
			}
			groceryqlock.unlock();
		}
		catch(InterruptedException e) {			
		}
		return -1;
	}
	
	public Customer popFromQueue()
	{
		try
		{
			groceryqlock.lockInterruptibly();
			Customer frontCustomer = customerQ[0];
			numCustomersInQueue--;
			for (int i = 0; i < numCustomersInQueue; i++)
			{
				customerQ[i] = customerQ[i+1];	
				customerQ[i].moveAhead();
			}
			frontCustomer.moveAhead();
			groceryqlock.unlock();
			return frontCustomer;
		}
		catch (InterruptedException e)
		{
			return null;
		}
	}
	
	public Customer GetNextCustomer()
	{
		return nextCustomer;
	}
	
	public void run() {
		new Thread(groceryqpopper).start();		
	}
}


import java.util.concurrent.locks.*;
import java.util.concurrent.*;

public class BankQueue implements Runnable
{
	private Customer[] customerQ;
	private int numQueuedCustomers;
	private int maxQueueLength;
	private Condition[] customerfinished;
	private Condition[] customerremoved;
	private BankQueuePopper bankqpopper;
	private ReentrantLock bankqlock;
	private int numtellers;

	// This queue just pops off bank customers once they have been served
	private class BankQueuePopper implements Runnable
	{
		boolean interrupted = false;
		public void run() {			
			while (!interrupted)
			{
				try {			
					bankqlock.lockInterruptibly();
					customerfinished[0].await(30, TimeUnit.SECONDS);
					popFromQueue();	
					customerremoved[0].signal();
					bankqlock.unlock();
				}
				catch (InterruptedException e)
				{
					interrupted = true;				
					customerfinished[0].signalAll();
					customerremoved[0].signalAll();
				}
				
			}
		}
		
		// This will stop the BankQueuePopper thread
		public void setInterrupted()
		{
			interrupted = true;
		}
	}
		
	public BankQueue(ReentrantLock clockLock, ReentrantLock queueLock, Condition clockTicked, Condition[] customerFinished, Condition customerArrived, 
			Condition[] customerRemoved, Semaphore bankQSemaphore, int numTellers, int maxQLength)
	{
		maxQueueLength = maxQLength;
		numQueuedCustomers = 0;
		customerfinished = customerFinished;
		customerremoved = customerRemoved;
		bankqlock = queueLock;
		numtellers = numTellers;
		customerQ = new Customer[maxQLength];
		bankqpopper = new BankQueuePopper();
	}
	
	public boolean isQueueFull()
	{
		try
		{
			bankqlock.lockInterruptibly();
			boolean retval = (numQueuedCustomers == maxQueueLength);
			bankqlock.unlock();	
			return retval;
		}
		catch(InterruptedException e)
		{
			return true;
		}
	}
	
	public void setInterrupted()
	{
		bankqpopper.setInterrupted();
	}
	
	public boolean isQueueEmpty()
	{
		try 
		{
			bankqlock.lockInterruptibly();
			boolean retval = (numQueuedCustomers <= 0);
			bankqlock.unlock();
			return retval;
		}
		catch (InterruptedException e)
		{
			return false;
		}		
	}
	
	public int addToQueue(Customer currentCustomer)
	{
		try
		{
			bankqlock.lockInterruptibly();
			if (!isQueueFull())
			{
				customerQ[numQueuedCustomers] = currentCustomer;
				numQueuedCustomers++;
			}
			bankqlock.unlock();
			return numQueuedCustomers;
		}
		catch(InterruptedException e)
		{
			return -1;
		}
	}
	
	public Customer[] popFromQueue()
	{
		try 
		{
			bankqlock.lockInterruptibly();
			boolean remove[];
			remove = new boolean[numtellers];
			int numCustomersToRemove = 0;
			for (int i = 0; i < numtellers; i++)
			{
				remove[i] = false;
				if (customerQ[i].timeLeftToServe <= 0)
				{
					remove[i] = true;
					numCustomersToRemove++;
				}
			}
			Customer finishedCustomers[];
			finishedCustomers = new Customer[numtellers];
			int k = 0;
			int numtellers2 = numtellers;
			for (int i = 0; i < numtellers2; i++)
			{
				if (remove[i])
				{
					numQueuedCustomers--;
					finishedCustomers[k] = customerQ[i];
					for (int j = i; j < numQueuedCustomers; j++)
					{
						customerQ[j] = customerQ[j+1];
						customerQ[j].moveAhead();					
					}
					numtellers2--;
					for (int j = i; j < numtellers2; j++)
					{
						remove[j] = remove[j+1];
					}
					i--;					
					finishedCustomers[k].moveAhead();
					k++;
				}
			}			
			bankqlock.unlock();
			return finishedCustomers;
		}
		catch (InterruptedException e)
		{
			return null;
		}
	}
				
	public int getNumTellers()
	{
		return numtellers;
	}
			
	public void run() 
	{
		new Thread(bankqpopper).start();		
	}	
}
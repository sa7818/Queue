import java.util.concurrent.locks.*;
import java.util.Random;
import java.util.concurrent.*;

public class GroceryQueues implements Runnable {

	private GroceryQueue groceryQueues[];
	private int numGroceryQueues = 0;
	private ReentrantLock qlock;
	private int maxqlength;
	private Random rand;
	
	public GroceryQueues(ReentrantLock clockLock, ReentrantLock queueLock, Condition clockTicked, Condition[] customerFinished, Condition customerArrived, 
			Condition[] customerRemoved, Semaphore groceryQSemaphore, int numQs, int maxQLength)	
	{
		groceryQueues = new GroceryQueue[numQs];
		numGroceryQueues = numQs;
		qlock = queueLock;
		maxqlength = maxQLength;
		for (int i = 0; i < numQs; i++)
		{
			groceryQueues[i] = new GroceryQueue(clockLock, queueLock, clockTicked, customerFinished[i], customerArrived, 
					customerRemoved[i], groceryQSemaphore, maxQLength);
		}	
		rand = new Random();
	}	
				
	public boolean AllQueuesFull()
	{
		try
		{
			qlock.lockInterruptibly();
			boolean allfull = true;
			for (int i = 0; i < numGroceryQueues; i++)
			{
				if (!groceryQueues[i].isQueueFull())
				{
					allfull = false;
					break;
				}
			}
			qlock.unlock();
			return allfull;
		}
		catch(InterruptedException e)
		{
			return true;
		}
	}
	
	public boolean SomeQueueEmpty()
	{
		try 
		{
			qlock.lockInterruptibly();
			boolean empty = false;
			for (int i = 0; i < numGroceryQueues; i++)
			{
				if (groceryQueues[i].isQueueEmpty())
				{
					empty = true;
					break;
				}
			}
			qlock.unlock();
			return empty;
		}
		catch (InterruptedException e)
		{
			return false;
		}
	}
	
	public int[] addToQueue(Customer customer)
	{
		try 
		{
			qlock.lockInterruptibly();
			int posInQ = -1;
			int i;
			int[] numInQ = new int[numGroceryQueues]; 
			int minNumInQ = 1000;
			int[] minLengthQ = new int[3];
			int numMinLengthQs = 0;
			
			for (i = 0; i < numGroceryQueues; i++)
			{
				if (!groceryQueues[i].isQueueFull())
				{
					numInQ[i] = groceryQueues[i].queueLength();								
				}
				else
				{
					numInQ[i] = maxqlength;
				}
				if (numInQ[i] < minNumInQ)
				{
					minLengthQ[0] = i;
					numMinLengthQs = 1;
					minNumInQ = numInQ[i];
				}
				else if (numInQ[i] == minNumInQ)
				{
					minLengthQ[numMinLengthQs] = i;
					numMinLengthQs++;
				}
			}
			int[] queueinfo = new int[2];		
			if (numMinLengthQs == 1)
			{
				posInQ = groceryQueues[minLengthQ[0]].addToQueue(customer);
				queueinfo[0] = minLengthQ[0];
			}
			else
			{
				int queueToChoose = rand.nextInt(numMinLengthQs);
				queueinfo[0] = minLengthQ[queueToChoose];
				posInQ = groceryQueues[minLengthQ[queueToChoose]].addToQueue(customer);
			}
			qlock.unlock();
			queueinfo[1] = posInQ;
			return queueinfo;
		}
		catch (InterruptedException e)
		{
			return null;
		}
	}
	
	public void setInterrupted()
	{
		// Set the interrupted flag for each of the grocery queues to true - trigger them to stop
		for (int i = 0; i < numGroceryQueues; i++)
		{
			groceryQueues[i].setInterrupted();
		}
	}
	
	public void run() {
		for (int i = 0; i < numGroceryQueues; i++) // Start up a separate thread for each grocery queue
		{
			new Thread(groceryQueues[i]).start();
		}
	}
}

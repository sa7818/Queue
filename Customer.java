import java.util.concurrent.locks.*;
import java.util.concurrent.*;
import java.util.*;

public class Customer implements Runnable 
{
	int arrivalTime;
	int timeNeededToServe;
	int timeWaiting;
	boolean wasServed;
	boolean beingServed;
	boolean waitingForQ;
	boolean waitingInQ;
	boolean bankcustomer;
	boolean grocerycustomer;

	int timeLeftToServe;
	int timeWaitingForQ;
	int myPositionInQ;
	int myQ;
	ReentrantLock clocklock;
	ReentrantLock queuelock;
	Condition clockticked;
	Semaphore qsemaphore;
	Random rand;	
	GroceryQueues groceryqs;
	BankQueue bankq;
	QueueStats queuestats;
	Condition[] customerfinished;
	Condition customerarrived;
	Condition[] customerremoved;
		
	private int minCustomerServeTime = 60;
	private int maxCustomerServeTime = 300;	
	private int maxAllFullWaitTime = 10;
	private boolean interrupted;
		
	// Grocery customer constructor
	public Customer(ReentrantLock clockLock, ReentrantLock queueLock, Condition clockTicked, Condition[] customerFinished, Condition customerArrived, Condition[] customerRemoved, Semaphore queueSemaphore, GroceryQueues GroceryQs, 
			QueueStats groceryqstats)
	{
		arrivalTime = 0;		
		timeWaiting = 0;
		wasServed = false;
		waitingForQ = true;
		waitingInQ = false;
		beingServed = false;
		timeWaitingForQ = 0;
		clocklock = clockLock;
		queuelock = queueLock;
		clockticked = clockTicked;
		rand = new Random();		
		timeLeftToServe = rand.nextInt(maxCustomerServeTime-minCustomerServeTime+1)+minCustomerServeTime;
		qsemaphore = queueSemaphore;
		bankcustomer = false;
		grocerycustomer = true;
		groceryqs = GroceryQs;
		bankq = null;
		myPositionInQ = -1;
		myQ = -1;
		queuestats = groceryqstats;
		customerfinished = customerFinished;
		customerremoved = customerRemoved;
		customerarrived = customerArrived;
	}
	
	// Bank customer constructor
	public Customer(ReentrantLock clockLock, ReentrantLock queueLock, Condition clockTicked, Condition[] customerFinished, Condition customerArrived, Condition[] bankCustomerRemoved, Semaphore queueSemaphore, BankQueue BankQ,
			QueueStats bankqstats)
	{
		arrivalTime = 0;		
		timeWaiting = 0;
		wasServed = false;
		waitingForQ = true;
		waitingInQ = false;
		beingServed = false;
		clocklock = clockLock;
		queuelock = queueLock;
		clockticked = clockTicked;
		rand = new Random();		
		timeLeftToServe = rand.nextInt(maxCustomerServeTime-minCustomerServeTime+1)+minCustomerServeTime;
		qsemaphore = queueSemaphore;
		bankcustomer = true;
		grocerycustomer = false;
		groceryqs = null;
		bankq = BankQ;
		myPositionInQ = -1;
		queuestats = bankqstats;
		customerfinished = customerFinished;
		customerarrived = customerArrived;
		customerremoved = bankCustomerRemoved;
	}
	
	public boolean isWaitingInQ()
	{
		return waitingInQ;
	}
	
	public boolean isWaitingForQ()
	{
		return waitingForQ;
	}
	
	public void setArrivalTime(int arrivaltime)
	{
		arrivalTime = arrivaltime;
	}
	
	public void setWasServed(boolean wasserved)
	{
		wasServed = wasserved;
	}
	
	public void setBeingServed(boolean beingserved)
	{
		beingServed = beingserved;
	}
	
	public void moveAhead()
	{
		myPositionInQ--;
	}
	
	public void setInterrupted()
	{
		interrupted = true;
	}
	
	public void run()
	{
		interrupted = false;
		try {
			queuelock.lockInterruptibly();
			queuestats.newCustomerArrived();
			customerarrived.signalAll();
			queuelock.unlock();
			int[] QInfo = new int[2];
			while (!interrupted)
			{		
				try
				{				
					clocklock.lockInterruptibly();
					timeNeededToServe++;
					if (beingServed)
					{
						timeLeftToServe--; // Count down until we are done being served
						if (timeLeftToServe <= 0)
						{
							beingServed = false;
							wasServed = true;
							interrupted = true;		// Once we have been served, we should stop this thread				
							queuestats.CustomerServed(timeNeededToServe);
							queuelock.lock();
							if (bankq == null)
							{
								customerfinished[myQ].signalAll();
								customerremoved[myQ].await(30, TimeUnit.SECONDS);
							}
							else
							{
								customerfinished[0].signalAll();
								customerremoved[0].await(30, TimeUnit.SECONDS);
							}                      
							
							queuelock.unlock();
							qsemaphore.release();					
						}
					}
					else if (waitingForQ)
					{
	
						if (bankq != null)  // Bank Queue case
						{
							queuelock.lockInterruptibly();
							if (!bankq.isQueueFull())
							{
								waitingForQ = false;
								waitingInQ = true;
								qsemaphore.acquire();
								myPositionInQ = bankq.addToQueue(this);
								if (myPositionInQ < bankq.getNumTellers())
								{
									waitingInQ = false;
									beingServed = true;
								}							
							}
							else  // Don't wait if the bank queue is full 
							{
								interrupted = true;
								wasServed = false;
								queuestats.CustomerTurnedAway();
							}
							queuelock.unlock();
						}
						else  // Grocery Queues case
						{
							queuelock.lockInterruptibly();
							if (groceryqs.AllQueuesFull()) // Wait maxAllFullWaitTime for queue to free up, then stop
							{	
								timeWaitingForQ++;
								if (timeWaitingForQ >= maxAllFullWaitTime)
								{
									queuestats.CustomerTurnedAway();
									interrupted = true;
									wasServed = false;
								}
							}
							else
							{
								waitingForQ = false;
								waitingInQ = true;
								qsemaphore.acquire();
								QInfo = groceryqs.addToQueue(this);
								myQ = QInfo[0];
								myPositionInQ = QInfo[1];
								if (myPositionInQ == 0)
								{
									waitingInQ = false;
									beingServed = true;
								}
							}				
							queuelock.unlock();
						}
					}
					else if (waitingInQ)
					{
						queuelock.lockInterruptibly();				
						timeWaiting++;	
						if (bankq == null)
						{
							if (myPositionInQ == 0)
							{
								waitingInQ = false;
								beingServed = true;						
							}
						}
						else
						{
							if (myPositionInQ < bankq.getNumTellers()) // Assume the first bankq.numTellers() positions in the queue are being served
							{
								waitingInQ = false;
								beingServed = true;						
							}
						}
						queuelock.unlock();
					}		
					clockticked.await(30, TimeUnit.SECONDS);
					clocklock.unlock();
				}								
				catch (InterruptedException e)
				{
					qsemaphore.release();
				}	
				finally
				{
					
					
				}
			}		
		}
		catch (InterruptedException e)
		{
			
		}
		finally
		{
			
		}
	}
}

import java.util.concurrent.locks.*;
import java.util.*;
import java.util.concurrent.*;


public class CustomerFactory implements Runnable {
	
	private Condition customerArrived;
	private Customer newestCustomer;
	private Customer[] customerArray;
	private Condition clockticked;
	private ReentrantLock clocklock;
	private ReentrantLock queuelock;
	private Condition[] customerFinished;
	private Condition[] groceryCustomerFinished;
	private Condition[] groceryCustomerRemoved;
	private Condition[] bankCustomerRemoved;
	private Semaphore qsemaphore;
	private GroceryQueues groceryqs;
	private BankQueue bankq;
	private int minCustomerArrivalInterval = 20;
	private int maxCustomerArrivalInterval = 60;
	int timeToNextArrival;
	Random rand;
	QueueStats qstats;
	private static int MaxCustomers = 1000;
	private boolean interrupted;
	
	// Grocery customer factory constructor
	public CustomerFactory(ReentrantLock clockLock, ReentrantLock queueLock, Condition clockTicked, Condition[] CustomerFinished, Condition CustomerArrived, Condition[] CustomerRemoved,
			Semaphore groceryQSemaphore, GroceryQueues groceryQs, QueueStats QStats) 
	{
		clocklock = clockLock;
		queuelock = queueLock;
		clockticked = clockTicked;		
		groceryCustomerRemoved = CustomerRemoved;		
		groceryCustomerFinished = CustomerFinished;
		customerArrived = CustomerArrived;
		qsemaphore = groceryQSemaphore;
		groceryqs = groceryQs;
		bankq = null;
		rand = new Random();
		qstats = QStats;
		customerArray = new Customer[MaxCustomers];		
	}
	
	// Bank Queue
	public CustomerFactory(ReentrantLock clockLock, ReentrantLock queueLock, Condition clockTicked, Condition[] CustomerFinished, Condition CustomerArrived, Condition[] CustomerRemoved, Semaphore QSemaphore, 
			BankQueue bankQ, QueueStats QStats) 
	{
		clocklock = clockLock;
		queuelock = queueLock;
		clockticked = clockTicked;
		customerFinished = CustomerFinished;
		customerArrived = CustomerArrived;
		bankCustomerRemoved = CustomerRemoved;
		qsemaphore = QSemaphore;
		bankq = bankQ;
		groceryqs = null;	
		rand = new Random();
		qstats = QStats;
		customerArray = new Customer[MaxCustomers];
	}
	
	public void setInterrupted()
	{
		interrupted = true;
	}
	
	public void run()
	{
		interrupted = false;
		int currentCustomer = 0;
		
		// Generate the first customer and spawn a thread for it
		if (groceryqs != null) // Grocery Queue case
		{
			customerArray[currentCustomer++] = new Customer(clocklock, queuelock, clockticked, groceryCustomerFinished, customerArrived, groceryCustomerRemoved, qsemaphore, groceryqs, qstats);
		}
		else // Bank Queue case
		{
			customerArray[currentCustomer++] = new Customer(clocklock, queuelock, clockticked, customerFinished, customerArrived, bankCustomerRemoved, qsemaphore, bankq, qstats);
		}
		Thread newestCustomerThread = new Thread(customerArray[currentCustomer-1]);
		newestCustomerThread.start();
		
		// Determine when the next customer will arrive
		timeToNextArrival = rand.nextInt(maxCustomerArrivalInterval-minCustomerArrivalInterval+1)+minCustomerArrivalInterval;
		while (!interrupted)
		{			
			try {
				clocklock.lockInterruptibly();
				clockticked.await(30, TimeUnit.SECONDS); // Use timed await so we don't spin forever
				timeToNextArrival--;
				if (timeToNextArrival == 0)
				{
					if (groceryqs != null)
					{
						customerArray[currentCustomer++] = new Customer(clocklock, queuelock, clockticked, groceryCustomerFinished, customerArrived, groceryCustomerRemoved, qsemaphore, groceryqs, qstats);
					}
					else
					{
						customerArray[currentCustomer++] = new Customer(clocklock, queuelock, clockticked, customerFinished, customerArrived, bankCustomerRemoved, qsemaphore, bankq, qstats);
					}
					newestCustomerThread = new Thread(customerArray[currentCustomer-1]);
					newestCustomerThread.start();
					timeToNextArrival = rand.nextInt(maxCustomerArrivalInterval-minCustomerArrivalInterval+1)+minCustomerArrivalInterval;
				}
				clocklock.unlock();
			}
			catch (InterruptedException e) {
				interrupted = true;
				clockticked.signalAll();
				clocklock.unlock();				
				newestCustomerThread.interrupt();
			}	
			finally {
			}
		}
		
		// We are now done so we should tell all the customers we generated to stop
		for (int i = 0; i < currentCustomer; i++)
		{
			customerArray[i].setInterrupted();
		}
	}
}

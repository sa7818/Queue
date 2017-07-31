
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class QueueSimulator implements Runnable {

	private static Clock SimulationClock;
	private static GroceryQueues groceryQs;
	private static BankQueue bankQ;
	
	private static int simulationTime = 7200;
	private static int numBankQTellers = 3;
	private static int maxBankQLength = 8;  // Add three for customers being served
	private static int numGroceryQs = 3;
	private static int maxGroceryQLength = 3; // Add one for customer being served
	
	private QueueStats bankQStats;
	private QueueStats groceryQStats;
		
	private CustomerFactory groceryCustomerFactory;
	private CustomerFactory bankCustomerFactory;
	
	// Allocate synchronization variables
	ReentrantLock groceryclockLock = new ReentrantLock();
	ReentrantLock bankclockLock = new ReentrantLock();
	ReentrantLock groceryqueueLock = new ReentrantLock();
	ReentrantLock bankqueueLock = new ReentrantLock();
	Semaphore groceryQSemaphore = new Semaphore(numGroceryQs*(maxGroceryQLength));
	Semaphore bankQSemaphore = new Semaphore(maxBankQLength);
	Condition groceryCustomerFinished[] = new Condition[numGroceryQs];
	Condition groceryCustomerRemoved[] = new Condition[numGroceryQs];
	Condition bankCustomerRemoved[] = new Condition[1];
	Condition bankCustomerFinished[] = new Condition[1];
	Condition grocerycustomerArrived = groceryqueueLock.newCondition();
	Condition bankcustomerArrived = bankqueueLock.newCondition();
	Condition groceryclockTicked = groceryclockLock.newCondition();
	Condition bankclockTicked = bankclockLock.newCondition();
	
	// Constructor
	public QueueSimulator()
	{
		for (int i = 0; i < numGroceryQs; i++)
		{
			groceryCustomerFinished[i] = groceryqueueLock.newCondition();
			groceryCustomerRemoved[i] = groceryqueueLock.newCondition();
		}
		bankCustomerFinished[0] = bankqueueLock.newCondition();
		bankCustomerRemoved[0] = bankqueueLock.newCondition();
		bankQStats = new QueueStats();
		groceryQStats = new QueueStats();
	}
		
	public void run() {		
		// Create new GroceryQueues and BankQueue objects for simulation purposes
		groceryQs = new GroceryQueues(groceryclockLock, groceryqueueLock, groceryclockTicked, groceryCustomerFinished, grocerycustomerArrived, groceryCustomerRemoved, groceryQSemaphore, numGroceryQs, maxGroceryQLength);
		bankQ = new BankQueue(bankclockLock, bankqueueLock, bankclockTicked, bankCustomerFinished, bankcustomerArrived, bankCustomerRemoved, bankQSemaphore, numBankQTellers, maxBankQLength);
		groceryCustomerFactory = new CustomerFactory(groceryclockLock, groceryqueueLock, groceryclockTicked, groceryCustomerFinished, grocerycustomerArrived, groceryCustomerRemoved, groceryQSemaphore, groceryQs, groceryQStats);
		
		Thread groceryCustomerFactoryThread = new Thread(groceryCustomerFactory);
		Thread groceryQsThread = new Thread(groceryQs);
		groceryCustomerFactoryThread.start();
		groceryQsThread.start();
		SimulationClock = new Clock();
		// Simulate Grocery Queues		
		while (SimulationClock.getTick() < simulationTime)
		{
			try
			{
				Thread.sleep(50);
				groceryclockLock.lockInterruptibly(); // Use lockInterruptibly so that thread doesn't get stuck waiting for lock
				SimulationClock.tick();		
				groceryclockTicked.signalAll();										
			}	
			catch (InterruptedException e)
			{				
			}
			finally
			{	
				groceryclockLock.unlock();			
			}	
		}		
		
		// Output grocery queue stats
		int totalGroceryCustomersArrived = groceryQStats.getTotalCustomersArrived();
		int numGroceryCustomersTurnedAway = groceryQStats.getNumCustomersTurnedAway();
		int totalGroceryCustomerServeTime = groceryQStats.getTotalCustomerServeTime();
		int totalGroceryCustomersServed = groceryQStats.getTotalCustomersServed();
		float averageGroceryServeTime = ((float) totalGroceryCustomerServeTime)/totalGroceryCustomersServed;
		
		System.out.println("Total Number of Grocery Customers Arrived: " + totalGroceryCustomersArrived);
		System.out.println("Number of Grocery Customers Turned Away: " + numGroceryCustomersTurnedAway);
		System.out.println("Average Time to Serve a Grocery Customer: " + averageGroceryServeTime + " seconds.");
				
		groceryQs.setInterrupted();
		groceryCustomerFactory.setInterrupted();
		
		SimulationClock.reset();
		
		bankCustomerFactory = new CustomerFactory(bankclockLock, bankqueueLock, bankclockTicked, bankCustomerFinished, bankcustomerArrived, bankCustomerRemoved, bankQSemaphore, bankQ, bankQStats);
		Thread bankCustomerFactoryThread = new Thread(bankCustomerFactory);
		bankCustomerFactoryThread.start();
		Thread bankQThread = new Thread(bankQ);
		bankQThread.start();
		// Simulate Bank Queue
		while (SimulationClock.getTick() < simulationTime)
		{
			try {
				bankclockLock.lockInterruptibly();
				SimulationClock.tick();
				bankclockTicked.signalAll();
				bankclockLock.unlock();
				try
				{
					Thread.sleep(50);
				}
				catch (InterruptedException e)
				{
					bankclockTicked.signalAll();
				}					
			}		
			catch (InterruptedException e)
			{
				bankclockTicked.signalAll();
			}
			finally
			{				
			}	
		}
		
		// Output Bank queue stats
		int totalBankCustomersArrived = bankQStats.getTotalCustomersArrived();
		int numBankCustomersTurnedAway = bankQStats.getNumCustomersTurnedAway();
		int totalBankCustomerServeTime = bankQStats.getTotalCustomerServeTime();
		int totalBankCustomersServed = bankQStats.getTotalCustomersServed();
		float averageBankServeTime = ((float) totalBankCustomerServeTime)/totalBankCustomersServed;
		
		System.out.println("");
		System.out.println("Total Number of Bank Customers Arrived: " + totalBankCustomersArrived);
		System.out.println("Number of Bank Customers Turned Away: " + numBankCustomersTurnedAway);
		System.out.println("Average Time to Serve a Bank Customer: " + averageBankServeTime + " seconds.");
		
		bankQ.setInterrupted();
		bankCustomerFactory.setInterrupted();
	}	
}
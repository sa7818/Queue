import java.util.concurrent.locks.*;


public class QueueStats {
	
	private int totalCustomersArrived;
	private int numCustomersTurnedAway;
	private int totalCustomerServeTime;
	private int totalCustomersServed;
	private Lock QStatLock;
	
	public QueueStats() {
		totalCustomersArrived = 0;
		numCustomersTurnedAway = 0;
		totalCustomerServeTime = 0;
		totalCustomersServed = 0;
		QStatLock = new ReentrantLock();
	}
	
	public void newCustomerArrived() 
	{
		QStatLock.lock();
		totalCustomersArrived++;
		QStatLock.unlock();
	}
	
	public void CustomerTurnedAway()
	{
		QStatLock.lock();
		numCustomersTurnedAway++;
		QStatLock.unlock();
	}
		
	public void CustomerServed(int timeNeededToServe)
	{
		QStatLock.lock();
		totalCustomersServed++;
		totalCustomerServeTime += timeNeededToServe;
		QStatLock.unlock();
	}
	
	public int getTotalCustomersArrived()
	{
		return totalCustomersArrived;
	}
	
	public int getNumCustomersTurnedAway()
	{
		return numCustomersTurnedAway;
	}
	
	public int getTotalCustomerServeTime()
	{
		return totalCustomerServeTime;
	}
	
	public int getTotalCustomersServed()
	{
		return totalCustomersServed;
	}
	
	public void resetStats()
	{
		totalCustomersArrived = 0;
		numCustomersTurnedAway = 0;
		totalCustomerServeTime = 0;
		totalCustomersServed = 0;
	}
}

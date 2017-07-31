
public class ConcurrentQueueTester {

	static QueueSimulator qsimulator;
	public static void main(String[] args)
	{
		qsimulator = new QueueSimulator();
		qsimulator.run();
	}
}

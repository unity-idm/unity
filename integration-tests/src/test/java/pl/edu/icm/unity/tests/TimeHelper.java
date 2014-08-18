package pl.edu.icm.unity.tests;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
/**
 * Helper to time measure. 
 * @author P.Piernik
 *
 */
public class TimeHelper
{
	private long startT;
	private final String outFile = "target/tests.csv";
	protected boolean consoleOut = true;
	protected List<Long> results;
	
	public TimeHelper()
	{
		results = new ArrayList<Long>();
	}
	
	public void startTimer()
	{
		startT = System.currentTimeMillis();
	}

	public long stopTimer(int ops, String label) throws IOException
	{
		long endT = System.currentTimeMillis();
		long periodMs = endT - startT;
		results.add(periodMs);
				
		double periodS = periodMs / 1000.0;
		double opsPerS = (ops * 1000 / periodMs);
		if (consoleOut)
			System.out.println(label + " performed " + ops + " in " + periodS + "s, "+ opsPerS + " ops/s");
		toFile(label + "," + ops + "," + periodS + "," + opsPerS);	
		return periodMs;
		
	}
	
	private void toFile(String line) throws IOException
	{
		FileWriter fw = new FileWriter(outFile, true);
		PrintWriter pw = new PrintWriter(fw);
		pw.println(line);
		pw.flush();
		pw.close();
		fw.close();
	}
	
	public void calculateResults(String label) throws IOException
	{
		Long avg = 0l;
		Long dev = 0l;	
		Long sum = 0l;
		for (Long l:results)
			sum+=l;
		avg = sum/results.size();
		long td = 0l;
		for (Long l:results)
		{
			td+= Math.pow(l-avg,2);
		}	
		dev = (long) Math.sqrt(td/results.size());
		
		double periodS = avg / 1000.0;
		if(consoleOut)
			System.out.println(label + " - average - " + periodS + "s");
		toFile(label +" average " + periodS + "s");
		periodS = dev / 1000.0;
		if(consoleOut)
			System.out.println(label + " - deviation - " + periodS + "s");	
		toFile(label + " deviation" + periodS + "s");
	}
		
	public void clear()
	{
		results.clear();
	}
}

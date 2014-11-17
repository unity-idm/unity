/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.performance;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Helper to time measure.
 * 
 * @author P.Piernik
 * 
 */
public class TimeHelper
{

	public final boolean PRINT_TO_CONSOLE = false;
	public String file;
	private long startT;

	protected List<SingleResult> results;

	public TimeHelper(String file) throws IOException
	{
		this.file = file;
		toFile("OPERATION , COUNT , TOTAL_TIME_S , OPS_PER_S ");
		results = new ArrayList<SingleResult>();
	}

	public void startTimer()
	{
		startT = System.currentTimeMillis();
	}

	public long stopTimer(int ops, String label) throws IOException
	{
		long endT = System.currentTimeMillis();
		long periodMs = endT - startT;
		double periodS = periodMs / 1000.0;
		double opsPerS = (ops * 1000 / periodMs);

		results.add(new SingleResult(periodMs, opsPerS, ops));

		if (PRINT_TO_CONSOLE)
			System.out.printf(Locale.US, label + "  %d ops, %.2f s, %.2f ops/s \n",
					ops, periodS, opsPerS);
		toFile(String.format(Locale.US, label + " , %d , %.2f , %.2f ", ops, periodS,
				opsPerS));
		return periodMs;
	}

	private void toFile(String line) throws IOException
	{
		FileWriter fw = new FileWriter(file, true);
		PrintWriter pw = new PrintWriter(fw);
		pw.println(line);
		pw.flush();
		pw.close();
		fw.close();
	}

	public void calculateResults(String label) throws IOException
	{
		double pavg = 0l;
		double pdev = 0l;
		double oavg = 0l;
		double odev = 0l;
		double psum = 0l;
		double osum = 0l;
		long count = 0;
		for (SingleResult l : results)
		{
			psum += l.periodMs;
			osum += l.opsPerS;
			count += l.count;
		}
		double size = (double) results.size();

		pavg = psum / size;
		oavg = osum / size;
		double ptd = 0l;
		double otd = 0l;
		for (SingleResult l : results)
		{
			ptd += Math.pow(l.periodMs - pavg, 2);
			otd += Math.pow(l.opsPerS - oavg, 2);
		}
		pdev = (double) Math.sqrt(ptd / size);
		odev = (double) Math.sqrt(otd / size);

		double periodS = pavg / 1000.0;
		if (PRINT_TO_CONSOLE)
			System.out.printf(Locale.US, label + " average , %d , %.2f s, %.2f ops/s \n",
					count, periodS, oavg);
		toFile(String.format(Locale.US, label + " average , %d , %.2f , %.2f", count, periodS, oavg));
		periodS = pdev / 1000.0;
		if (PRINT_TO_CONSOLE)
			System.out.printf(Locale.US,
					label + " deviation , %d , %.2f s, %.2f ops/s \n", count, periodS,
					odev);
		toFile(String.format(Locale.US, label + " deviation , %d , %.2f , %.2f", count, periodS, odev));
	}

	public void clear()
	{
		results.clear();
	}

	public class SingleResult
	{
		public double periodMs;
		public double opsPerS;
		public int count;

		public SingleResult(double periodMs, double opsPerS, int count)
		{
			this.periodMs = periodMs;
			this.opsPerS = opsPerS;
			this.count = count;
		}

	}

}

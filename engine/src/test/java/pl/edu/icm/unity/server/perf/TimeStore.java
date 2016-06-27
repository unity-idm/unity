/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.perf;

import java.util.HashMap;
import java.util.Map;

public class TimeStore
{
	private Map<String, Long> times = new HashMap<>();
	
	public void add(String key, long amount)
	{
		Long cur = times.get(key);
		if (cur == null)
			cur = 0l;
		cur = cur + amount;
		times.put(key, cur);
	}

	public Map<String, Long> getTimes()
	{
		return times;
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		for (Map.Entry<String, Long> e: times.entrySet())
		{
			s.append(e.getKey() + ": " + e.getValue() + "\n");
		}
		return s.toString();
	}
}

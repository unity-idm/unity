/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;

/**
 * Counts unsuccessful authentication attempts per client's IP address.
 * Configured with maximum number of attempts. Signals if the access should be blocked.
 *  
 * Thread safe.
 * @author K. Benedyczak
 */
public class UnsuccessfulAuthenticationCounter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, UnsuccessfulAuthenticationCounter.class);
	private int maxAttepts;
	private long blockTime;
	private Map<String, ClientInfo> accessMap;
	
	public UnsuccessfulAuthenticationCounter(int maxAttepts, long blockTime)
	{
		this.maxAttepts = maxAttepts;
		this.blockTime = blockTime;
		this.accessMap = new HashMap<>(64);
	}
	
	public synchronized long getRemainingBlockedTime(String ip)
	{
		ClientInfo clientInfo = accessMap.get(ip);
		if (clientInfo == null || clientInfo.blockedStartTime == -1)
			return 0;
		long blockedFor = System.currentTimeMillis() - clientInfo.blockedStartTime;
		if (blockedFor >= blockTime)
		{
			accessMap.remove(ip);
			return 0;
		}
		return blockTime - blockedFor;
	}
	
	public synchronized void unsuccessfulAttempt(String ip)
	{
		ClientInfo clientInfo = accessMap.get(ip);
		if (clientInfo == null)
		{
			clientInfo = new ClientInfo();
			accessMap.put(ip, clientInfo);
		}
		clientInfo.unsuccessfulAttempts++;
		if (clientInfo.unsuccessfulAttempts >= maxAttepts)
		{
			log.info("Blocking access for IP " + ip + " after " + clientInfo.unsuccessfulAttempts +
					" unsuccessful login attempts for " + blockTime + "ms");
			clientInfo.blockedStartTime = System.currentTimeMillis();
		}
	}
	
	public synchronized void successfulAttempt(String ip)
	{
		accessMap.remove(ip);
	}
	
	
	private static class ClientInfo
	{
		private int unsuccessfulAttempts = 0;
		private long blockedStartTime = -1;
	}
}

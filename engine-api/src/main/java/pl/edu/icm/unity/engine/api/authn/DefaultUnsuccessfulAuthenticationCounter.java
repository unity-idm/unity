/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;

import pl.edu.icm.unity.base.utils.Log;

/**
 * Counts unsuccessful authentication attempts per client's IP address.
 * Configured with maximum number of attempts. Signals if the access should be blocked.
 *  
 * Thread safe.
 * @author K. Benedyczak
 */
public class DefaultUnsuccessfulAuthenticationCounter implements UnsuccessfulAuthenticationCounter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, DefaultUnsuccessfulAuthenticationCounter.class);
	private int maxAttepts;
	private long blockTime;
	private Map<String, ClientInfo> accessMap;
	
	public DefaultUnsuccessfulAuthenticationCounter(int maxAttepts, long blockTime)
	{
		this.maxAttepts = maxAttepts;
		this.blockTime = blockTime;
		this.accessMap = new HashMap<>(64);
	}
	
	@Override
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
	
	@Override
	public synchronized void unsuccessfulAttempt(String ip)
	{
		Preconditions.checkNotNull(ip);
		ClientInfo clientInfo = accessMap.get(ip);
		if (clientInfo == null)
		{
			clientInfo = new ClientInfo();
			accessMap.put(ip, clientInfo);
		}
		clientInfo.unsuccessfulAttempts++;
		log.debug("Unsuccessful attempts count for {} is {}", ip, clientInfo.unsuccessfulAttempts);
		if (clientInfo.unsuccessfulAttempts >= maxAttepts)
		{
			log.info("Blocking access for IP {} after {} unsuccessful login attempts for {}ms", 
					ip, clientInfo.unsuccessfulAttempts, blockTime);
			clientInfo.blockedStartTime = System.currentTimeMillis();
		}
	}
	
	@Override
	public synchronized void successfulAttempt(String ip)
	{
		if (accessMap.containsKey(ip))
			log.info("Cleaning unsuccessful attempts for {}", ip);
		accessMap.remove(ip);
	}
	
	
	private static class ClientInfo
	{
		private int unsuccessfulAttempts = 0;
		private long blockedStartTime = -1;
	}
}

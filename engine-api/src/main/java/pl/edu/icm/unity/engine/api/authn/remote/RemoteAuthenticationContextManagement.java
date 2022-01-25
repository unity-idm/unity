/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;

/**
 * Maintains a map of remote authentication contexts matched by some string key.
 * The contexts are matched by the random relay state. The in-memory store of contexts is purged, so the stale 
 * contexts are automatically removed after a timeout.
 * <p>
 * This class is not anyhow persisted, so after restart all information is lost. This is intended.
 * <p> 
 * Naturally this class is thread safe. 

 * @author K. Benedyczak
 */
public class RemoteAuthenticationContextManagement<T extends RelayedAuthnState>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, RemoteAuthenticationContextManagement.class);
	private static final Duration DEF_CLEANUP_INTERVAL = Duration.ofMinutes(5);
	private static final Duration DEF_SHORT_CLEANUP_INTERVAL = Duration.ofSeconds(5);
	
	private final Map<String, T> contexts = new HashMap<>();
	private LocalDateTime lastCleanup = LocalDateTime.now();
	private final Duration maxTTL;
	private final Duration cleanupInterval;
	
	public RemoteAuthenticationContextManagement(Duration maxTTL)
	{
		this(maxTTL, DEF_CLEANUP_INTERVAL);
	}

	public RemoteAuthenticationContextManagement(Duration maxTTL, Duration cleanupInterval)
	{
		this.maxTTL = maxTTL;
		this.cleanupInterval = cleanupInterval.compareTo(maxTTL) > 0 ? DEF_SHORT_CLEANUP_INTERVAL : cleanupInterval;
		log.debug("Stale authn context will be cleaned after {}, interval is {}", maxTTL, this.cleanupInterval);
	}
	
	public synchronized void addAuthnContext(T context)
	{
		cleanup();
		String relayState = context.getRelayState();
		if (contexts.containsKey(relayState))
			throw new IllegalArgumentException("Ups, the relay state " + relayState + " is already assigned");
		
		contexts.put(relayState, context);
	}
	
	public synchronized T getAndRemoveAuthnContext(String relayState)
	{
		cleanup();
		T ret = contexts.remove(relayState);
		if (ret == null)
			throw new UnboundRelayStateException(relayState);
		return ret;
	}
	
	private void cleanup()
	{
		LocalDateTime now = LocalDateTime.now();
		if (now.minus(cleanupInterval).isBefore(lastCleanup))
			return;
		
		lastCleanup = LocalDateTime.now();
		LocalDateTime oldestAllowed = now.minus(maxTTL);
		Date oldestAllowedDate = Date.from(oldestAllowed.atZone(ZoneId.systemDefault()).toInstant());
		
		Iterator<T> it = contexts.values().iterator();
		while (it.hasNext())
		{
			T ctx = it.next();
			if (ctx.getCreationTime().before(oldestAllowedDate))
			{
				log.debug("Dropping stale since {} authN context: {}", ctx.getCreationTime(), ctx);
				it.remove();
			}
		}
	}
	
	public static class UnboundRelayStateException extends RuntimeException
	{
		public UnboundRelayStateException(String relayState)
		{
			super("The relay state " + relayState + " is not assigned");
		}
	}
}

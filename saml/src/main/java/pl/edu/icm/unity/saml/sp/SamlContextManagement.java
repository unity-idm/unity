/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import pl.edu.icm.unity.exceptions.WrongArgumentException;

/**
 * Singleton component managing SAML contexts used in all remote authentications currently handled by the server.
 * The contexts are matched by the random relay state. The in-memory store of contexts is purged, so the stale 
 * contexts are automatically removed after a timeout.
 * <p>
 * This class is not anyhow persisted, so after restart all information is lost. This is intended.
 * <p> 
 * Naturally this class is thread safe. 
 * @author K. Benedyczak
 */
public class SamlContextManagement
{
	public static final long MAX_TTL = 15*3600*1000;
	public static final long CLEANUP_INTERVAL = 3600*1000;
	public static final int MAX_CONTEXTS = 10000;
	
	private Map<String, RemoteAuthnContext> contexts = new HashMap<String, RemoteAuthnContext>();
	private Date lastCleanup = new Date();
	
	public synchronized void addAuthnContext(RemoteAuthnContext context)
	{
		cleanup();
		String relayState = context.getRelayState();
		if (contexts.containsKey(relayState))
			throw new IllegalArgumentException("Ups, the relay state " + relayState + " is already assigned");
		
		contexts.put(relayState, context);
	}
	
	public synchronized RemoteAuthnContext getAuthnContext(String relayState) throws WrongArgumentException
	{
		cleanup();
		RemoteAuthnContext ret = contexts.get(relayState);
		if (ret == null)
			throw new WrongArgumentException("The relay state " + relayState + " is not assigned");
		return ret;
	}
	
	public synchronized void removeAuthnContext(String relayState)
	{
		contexts.remove(relayState);
	}
	
	private void cleanup()
	{
		long now = System.currentTimeMillis();
		if (new Date(now-CLEANUP_INTERVAL).before(lastCleanup))
			return;
		
		lastCleanup = new Date(now);
		Date oldestAllowed = new Date(now - MAX_TTL);
		
		Iterator<RemoteAuthnContext> it = contexts.values().iterator();
		while (it.hasNext())
		{
			RemoteAuthnContext ctx = it.next();
			if (ctx.getCreationTime().before(oldestAllowed))
				it.remove();
		}
	}
	
}

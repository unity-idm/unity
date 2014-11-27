/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;

/**
 * In memory storage of logout contexts. Ensures that the contexts are expired after some time.
 * Thread safe.
 * @author K. Benedyczak
 */
public class LogoutContextsStore
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, LogoutContextsStore.class);
	public static final long MAX_AGE = 180000;
	public static final long CLEANUP_EVERY = 30000;
	
	private Map<String, SAMLInternalLogoutContext> intContexts = new HashMap<String, SAMLInternalLogoutContext>(64);
	private Map<String, SAMLExternalLogoutContext> extContexts = new HashMap<String, SAMLExternalLogoutContext>(64);
	private Map<String, PlainExternalLogoutContext> plainExtContexts = 
			new HashMap<String, PlainExternalLogoutContext>(64);
	private long lastCleanup;
	
	public synchronized SAMLInternalLogoutContext getInternalContext(String id)
	{
		cleanup();
		return intContexts.get(id);
	}

	public synchronized SAMLExternalLogoutContext getExternalContext(String id)
	{
		cleanup();
		return extContexts.get(id);
	}

	public synchronized PlainExternalLogoutContext getPlainExternalContext(String id)
	{
		cleanup();
		return plainExtContexts.get(id);
	}
	
	public synchronized void removeInternalContext(String key)
	{
		intContexts.remove(key);
	}

	public synchronized void removeExternalContext(String key)
	{
		extContexts.remove(key);
	}

	public synchronized void removePlainExternalContext(String key)
	{
		plainExtContexts.remove(key);
	}

	/**
	 * The identifier of the context must be provided. Useful for logouts started with SAML.
	 * @param context
	 */
	public synchronized void addInternalContext(String key, SAMLInternalLogoutContext context)
	{
		cleanup();
		intContexts.put(key, context);
	}
	
	/**
	 * Adds a new external logout context, the key is returned.
	 * @param key
	 * @param context
	 */
	public synchronized String addExternalContext(SAMLExternalLogoutContext context)
	{
		cleanup();
		String key = UUID.randomUUID().toString();
		extContexts.put(key, context);
		return key;
	}

	public synchronized void addPlainExternalContext(String key, PlainExternalLogoutContext context)
	{
		cleanup();
		plainExtContexts.put(key, context);
	}
	
	private void cleanup()
	{
		if (lastCleanup + CLEANUP_EVERY > System.currentTimeMillis())
			return;
		log.debug("Running SAML logout contexts expiration task");
		lastCleanup = System.currentTimeMillis();
		cleanup(extContexts);
		cleanup(intContexts);
	}
	
	private void cleanup(Map<String, ? extends AbstractSAMLLogoutContext> contexts)
	{
		Iterator<? extends AbstractSAMLLogoutContext> mapIt = contexts.values().iterator();
		while (mapIt.hasNext())
		{
			AbstractSAMLLogoutContext ctx = mapIt.next();
			if (ctx.getCreationTs().getTime() + MAX_AGE < lastCleanup)
			{
				if (log.isDebugEnabled())
					log.debug("Expiring stale SAML logout context " + ctx + ", is " + 
						((lastCleanup - ctx.getCreationTs().getTime())/1000) + "s old");
				mapIt.remove();
			}
		}
	}
}

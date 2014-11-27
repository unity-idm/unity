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
	
	private Map<String, SAMLLogoutContext> contexts = new HashMap<String, SAMLLogoutContext>(64);
	private long lastCleanup;
	
	public synchronized SAMLLogoutContext getContext(String id)
	{
		cleanup();
		return contexts.get(id);
	}
	
	/**
	 * The identifier of the context is set in the context's relay state.
	 * @param context
	 */
	public synchronized void addContext(SAMLLogoutContext context)
	{
		cleanup();
		String key = UUID.randomUUID().toString();
		context.setRelayState(key);
		contexts.put(key, context);
	}
	
	private void cleanup()
	{
		if (lastCleanup + CLEANUP_EVERY > System.currentTimeMillis())
			return;
		log.debug("Running SAML logout contexts expiration task");
		lastCleanup = System.currentTimeMillis();
		Iterator<SAMLLogoutContext> mapIt = contexts.values().iterator();
		while (mapIt.hasNext())
		{
			SAMLLogoutContext ctx = mapIt.next();
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

/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.userimport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.config.ConfigurationLoader;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPI;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPIFactory;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;


/**
 * Implementation of user import service. Loads configured importers, configures them and run when requested.
 * Maintains timers to skip too often imports.
 * 
 * @author K. Benedyczak
 */
@Component
public class UserImportServiceImpl implements UserImportSerivce
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, UserImportServiceImpl.class);
	private List<SingleUserImportHandler> handlers;
	
	@Autowired
	public UserImportServiceImpl(UnityServerConfiguration mainCfg, Optional<List<UserImportSPIFactory>> importersF,
			CacheProvider cacheProvider, RemoteAuthnResultProcessor remoteAuthnResultProcessor)
	{
		this(mainCfg, importersF.orElse(new ArrayList<>()), cacheProvider, 
				remoteAuthnResultProcessor, new ConfigurationLoader());
	}
	
	public UserImportServiceImpl(UnityServerConfiguration mainCfg, List<UserImportSPIFactory> importersF,
			CacheProvider cacheProvider, RemoteAuthnResultProcessor verificatorUtil, 
			ConfigurationLoader configLoader)
	{
		Map<String, UserImportSPIFactory> importersFM = new HashMap<>();
		importersF.forEach(spiF -> importersFM.put(spiF.getName(), spiF));
		List<String> definedImporters = mainCfg.getListOfValues(UnityServerConfiguration.IMPORT_PFX);
		handlers = new ArrayList<>();
		int i=0;
		for (String importerCfg: definedImporters)
			handlers.add(loadHandler(importerCfg, importersFM, cacheProvider, i++, 
					verificatorUtil, configLoader));
	}
	
	private SingleUserImportHandler loadHandler(String importerCfg, Map<String, UserImportSPIFactory> importersFM,
			CacheProvider cacheProvider, int index, RemoteAuthnResultProcessor verificatorUtil, 
			ConfigurationLoader cfgLoader)
	{
		Properties properties = cfgLoader.getProperties(importerCfg);
		UserImportProperties cfg = new UserImportProperties(properties);
		String type = cfg.getValue(UserImportProperties.TYPE);
		UserImportSPIFactory userImportSPIFactory = importersFM.get(type);
		if (userImportSPIFactory == null)
			throw new ConfigurationException("The type '" + type + 
					"' of user import is not known in " + importerCfg + 
					". Known types are: " + importersFM.keySet());
		String remoteIdp = cfg.getValue(UserImportProperties.REMOTE_IDP_NAME);
		UserImportSPI instance = userImportSPIFactory.getInstance(properties, remoteIdp);
		return new SingleUserImportHandler(verificatorUtil, instance, cfg, cacheProvider, index);
	}

	@Override
	public AuthenticationResult importUser(String identity, String type)
	{
		log.debug("Trying to import user " + identity);
		
		for (SingleUserImportHandler handler: handlers)
		{
			AuthenticationResult result;
			try
			{
				result = handler.importUser(identity, type);
			} catch (AuthenticationException e)
			{
				log.debug("User import has thrown an authentication exception, skipping it", e);
				continue;
			} catch (Exception e)
			{
				log.error("User import has thrown an exception, skipping it", e);
				continue;
			}
			if (result != null && result.getStatus() != Status.notApplicable)
			{
				log.debug("Import handler " + handler.getIndex() + " has imported the user " 
						+ identity);
				return result;
			} else
			{
				log.debug("Import handler " + handler.getIndex() + " returned nothing.");
			}
		}
		return new AuthenticationResult(Status.notApplicable, null);
	}
}

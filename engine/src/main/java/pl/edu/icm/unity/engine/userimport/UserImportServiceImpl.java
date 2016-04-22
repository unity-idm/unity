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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.userimport.UserImportSPI;
import pl.edu.icm.unity.server.api.userimport.UserImportSPIFactory;
import pl.edu.icm.unity.server.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemoteVerificatorUtil;
import pl.edu.icm.unity.server.utils.CacheProvider;
import pl.edu.icm.unity.server.utils.ConfigurationLoader;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;


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
			CacheProvider cacheProvider, IdentityResolver identityResolver,
			@Qualifier("insecure") TranslationProfileManagement profileManagement,
			InputTranslationEngine trEngine)
	{
		this(mainCfg, importersF.orElse(new ArrayList<>()), cacheProvider, 
				new RemoteVerificatorUtil(identityResolver, profileManagement, trEngine), 
				new ConfigurationLoader());
	}
	
	public UserImportServiceImpl(UnityServerConfiguration mainCfg, List<UserImportSPIFactory> importersF,
			CacheProvider cacheProvider, RemoteVerificatorUtil verificatorUtil, 
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
			CacheProvider cacheProvider, int index, RemoteVerificatorUtil verificatorUtil, 
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

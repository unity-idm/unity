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
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationException;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.config.ConfigurationLoader;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPI;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPIFactory;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;
import pl.edu.icm.unity.types.basic.IdentityTaV;


/**
 * Implementation of user import service. Loads configured importers, configures them and run when requested.
 * Maintains timers to skip too frequent imports.
 */
@Component
public class UserImportServiceImpl implements UserImportSerivce
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_USER_IMPORT, UserImportServiceImpl.class);
	private Map<String, SingleUserImportHandler> handlersByKey;
	
	@Autowired
	public UserImportServiceImpl(UnityServerConfiguration mainCfg, Optional<List<UserImportSPIFactory>> importersF,
			RemoteAuthnResultTranslator remoteAuthnResultProcessor)
	{
		this(mainCfg, importersF.orElse(new ArrayList<>()),  
				remoteAuthnResultProcessor, new ConfigurationLoader());
	}
	
	public UserImportServiceImpl(UnityServerConfiguration mainCfg, List<UserImportSPIFactory> importersF,
			RemoteAuthnResultTranslator verificatorUtil, 
			ConfigurationLoader configLoader)
	{
		Map<String, UserImportSPIFactory> importersFM = new HashMap<>();
		importersF.forEach(spiF -> importersFM.put(spiF.getName(), spiF));
		List<String> definedImporters = mainCfg.getSortedListKeys(
				UnityServerConfiguration.IMPORT_PFX);
		handlersByKey = new HashMap<>();
		for (String key: definedImporters)
		{
			String importerCfg = mainCfg.getValue(UnityServerConfiguration.IMPORT_PFX + key);
			handlersByKey.put(key, loadHandler(importerCfg, importersFM,   
					verificatorUtil, configLoader));
		}
	}
	
	private SingleUserImportHandler loadHandler(String importerCfg, Map<String, UserImportSPIFactory> importersFM,
			RemoteAuthnResultTranslator verificatorUtil, 
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
		return new SingleUserImportHandler(verificatorUtil, instance, cfg);
	}

	@Override
	public List<ImportResult> importToExistingUser(List<UserImportSpec> imports,
			IdentityTaV existingUser)
	{
		return importUser(imports, Optional.of(existingUser));
	}
	
	@Override
	public List<ImportResult> importUser(List<UserImportSpec> imports)
	{
		return importUser(imports, Optional.empty());
	}
	
	private List<ImportResult> importUser(List<UserImportSpec> imports, 
			Optional<IdentityTaV> existingIdentity)
	{
		if (imports.size() == 1 && imports.get(0).isUseAllImporters())
			imports = getAllImportersFor(imports.get(0).identityValue, 
					imports.get(0).identityType);

		List<ImportResult> ret = new ArrayList<>();
		for (UserImportSpec userImport: imports)
		{
			log.debug("Trying to import user {} from {}", userImport.identityValue,
					userImport.importerKey);
			SingleUserImportHandler handler = handlersByKey.get(userImport.importerKey);
			if (handler == null)
			{
				log.warn("There is no importer configured with key {}, skipping it",
						userImport.importerKey);
				continue;
			}
			
			AuthenticationResult result;
			try
			{
				result = handler.importUser(userImport.identityValue, 
						userImport.identityType, existingIdentity);
			} catch (RemoteAuthenticationException e)
			{
				log.debug("User import has thrown an authentication exception, skipping it", e);
				ret.add(new ImportResult(userImport.importerKey, RemoteAuthenticationResult.notApplicable()));
				continue;
			} catch (Exception e)
			{
				log.debug("User import has thrown an exception, skipping it", e);
				ret.add(new ImportResult(userImport.importerKey, RemoteAuthenticationResult.notApplicable()));
				continue;
			}
			
			ret.add(new ImportResult(userImport.importerKey, result != null ? 
					result : RemoteAuthenticationResult.notApplicable()));
			
			if (result != null && result.getStatus() != Status.notApplicable)
			{
				log.info("Import handler {} has imported the user {}", 
						userImport.importerKey, userImport.identityValue);
			} else
			{
				log.info("Import handler {} returned nothing or notApplicable status",
						userImport.importerKey);
			}
		}
		return ret;
	}

	
	private List<UserImportSpec> getAllImportersFor(String identityValue, String identityType)
	{
		return handlersByKey.keySet().stream()
			.map(key -> new UserImportSpec(key, identityValue, identityType))
			.collect(Collectors.toList());
	}
}

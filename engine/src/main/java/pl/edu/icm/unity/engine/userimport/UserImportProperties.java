/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.userimport;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Configuration of user import.
 * 
 * @author K. Benedyczak
 */
public class UserImportProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, UserImportProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.userImport.";
	
	public static final String TYPE = "importerType";
	public static final String REMOTE_IDP_NAME = "remoteIdpName";
	public static final String TRANSLATION_PROFILE = "inputTranslationProfile";
	public static final String POSITIVE_CACHE = "cacheAfterSuccessfulImport";
	public static final String NEGATIVE_CACHE = "cacheAfterFailedImport";
	
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static
	{
		META.put(TYPE, new PropertyMD().setMandatory().
				setDescription("Name of the importer facility to be used."));
		META.put(REMOTE_IDP_NAME, new PropertyMD().setMandatory().
				setDescription("Name of the remote idp, it will be used for identyfing imported users"
						+ "in the internal database."));
		META.put(TRANSLATION_PROFILE, new PropertyMD().setMandatory().
				setDescription("Translation profile which is used to postprocess imported user data"
						+ " and add it to database. Note that the profile used with import "
						+ "facility should create/update user identity. A profile "
						+ "intended for remote user registration "
						+ "(only matching aginst existing identity) "
						+ "is useless for import."));
		META.put(POSITIVE_CACHE, new PropertyMD("600").
				setDescription("The user won't be re-imported for this time (in s), "
						+ "after a successful import. Don't set this to a too small value"
						+ " as the import is a heavyweight operation."));
		META.put(NEGATIVE_CACHE, new PropertyMD("60").
				setDescription("The user import won't be retried for this time (in s), "
						+ "after a failed import. Don't set this to a too small value"
						+ " as the import is a heavyweight operation."));
	}
	
	public UserImportProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}

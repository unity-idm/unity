/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.idp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;

/**
 * Contains settings which are common for all IdP endpoints
 * @author Krzysztof Benedyczak
 */
public class CommonIdPProperties
{
	public static final String SKIP_CONSENT = "skipConsent";
	public static final String TRANSLATION_PROFILE = "translationProfile";
	public static final String ASSUME_FORCE = "assumeForceOnSessionClash";
	public static final String SKIP_USERIMPORT = "skipUserImport";
	public static final String USERIMPORT_PFX = "userImport.";
	public static final String USERIMPORT_IMPORTER = "importer";
	public static final String USERIMPORT_IDENTITY_TYPE = "identityType";


	public static Map<String, PropertyMD> getDefaultsWithCategory(DocumentationCategory category,
			String defaultProfileMessage, String defaultProfile)
	{
		Map<String, PropertyMD> defaults = getDefaults(defaultProfileMessage, defaultProfile);
		for (PropertyMD md: defaults.values())
			md.setCategory(category);
		return defaults;
	}

	public static Map<String, PropertyMD> getDefaults(String defaultProfileMessage, String defaultProfile)
	{
		Map<String, PropertyMD> defaults = new HashMap<String, PropertyMD>();
		defaults.put(SKIP_CONSENT, new PropertyMD("false").
				setDescription("Controls whether the user being authenticated should see the consent screen"
						+ " with the information what service requested authorization and what data "
						+ "is going to be released. Note that user may always choose to disable "
						+ "the consent screen for each service, even if this setting is set to false."));

		defaults.put(TRANSLATION_PROFILE, defaultProfile != null
				? new PropertyMD(defaultProfile)
						.setDescription(defaultProfileMessage)
				: new PropertyMD().setDescription(defaultProfileMessage));

		defaults.put(ASSUME_FORCE, new PropertyMD("true").
				setDescription("Controls what to do in case of initialization of a new authentication, "
						+ "while another one was not finished within the same browser session."
						+ " By default a warning page is rendered and the user has "
						+ "a choice to cancel or forcefully continue. However, "
						+ "if this setting is set to true, then the new interaction forcefully"
						+ "takes over the old interaction, without asking the user."));

		defaults.put(USERIMPORT_PFX, new PropertyMD().setStructuredList(false)
				.setDescription("Under this prefix it is possible to configure enabled "
						+ "user importers on this endpoint. If no such options are defined then the user import"
						+ " feature is disabled, except of SAML SOAP assertion query endpoint "
						+ "on which by default all system defined user importers are enabled "
						+ "(this is for backwards compatibility reasons and can be disabled with skip option)."));
		defaults.put(USERIMPORT_IMPORTER, new PropertyMD().setStructuredListEntry(USERIMPORT_PFX)
				.setDescription("Defines which user import configuration should be triggered."));
		defaults.put(USERIMPORT_IDENTITY_TYPE, new PropertyMD().setStructuredListEntry(USERIMPORT_PFX)
				.setDescription("Authenticated user's identity of this type will be used as user importer parameter. "
						+ "If user has more then one identity of this type then a random one is used."));
		
		defaults.put(SKIP_USERIMPORT, new PropertyMD("false")
				.setDescription("Allows to completely disable user import functionality per endpoint. "
						+ "Useful mostly on SAML SOAP endpoint, where default is to use"
						+ "all (and not only defined with " + USERIMPORT_PFX + ") importers. "));
		return defaults;
	}

	public static List<UserImportSpec> getUserImportsLegacy(PropertiesHelper cfg, 
			String identity, String type)
	{
		Set<String> structuredListKeys = cfg.getStructuredListKeys(USERIMPORT_PFX);
		Boolean skip = cfg.getBooleanValue(SKIP_USERIMPORT);
		if (structuredListKeys.isEmpty())
		{
			return skip ? Collections.emptyList() 
				: Lists.newArrayList(UserImportSpec.withAllImporters(identity, type));
		} else
		{
			Map<String, String> map = new HashMap<>();
			map.put(type, identity);
			return getUserImports(cfg, map);
		}
	}

	public static List<UserImportSpec> getUserImports(PropertiesHelper cfg, 
			Map<String, String> identitiesByType)
	{
		Set<String> structuredListKeys = cfg.getStructuredListKeys(USERIMPORT_PFX);
		if (structuredListKeys.isEmpty() || cfg.getBooleanValue(SKIP_USERIMPORT))
			return Collections.emptyList();
		List<UserImportSpec> ret = new ArrayList<>();
		for (String key: structuredListKeys)
		{
			String importer = cfg.getValue(key + USERIMPORT_IMPORTER);
			String type = cfg.getValue(key + USERIMPORT_IDENTITY_TYPE);
			String identityValue = identitiesByType.get(type);
			if (identityValue != null)
				ret.add(new UserImportSpec(importer, identityValue, type));
		}
		return ret;
	}
}

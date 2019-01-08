/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.idp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;
import pl.edu.icm.unity.types.basic.DynamicAttribute;

/**
 * Contains settings which are common for all IdP endpoints
 * @author Krzysztof Benedyczak
 */
public class CommonIdPProperties
{
	public static final String SKIP_CONSENT = "skipConsent";
	public static final String TRANSLATION_PROFILE = "translationProfile";
	public static final String SKIP_USERIMPORT = "skipUserImport";
	public static final String USERIMPORT_PFX = "userImport.";
	public static final String USERIMPORT_IMPORTER = "importer";
	public static final String USERIMPORT_IDENTITY_TYPE = "identityType";

	public static final String ACTIVE_VALUE_SELECTION_PFX = "activeValue.";
	public static final String ACTIVE_VALUE_CLIENT = "client";
	public static final String ACTIVE_VALUE_SINGLE_SELECTABLE = "singleValueAttributes.";
	public static final String ACTIVE_VALUE_MULTI_SELECTABLE = "multiValueAttributes.";
	
	private static final String ASSUME_FORCE = "assumeForceOnSessionClash";

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
		Map<String, PropertyMD> defaults = new HashMap<>();
		defaults.put(SKIP_CONSENT, new PropertyMD("false").
				setDescription("Controls whether the user being authenticated should see the consent screen"
						+ " with the information what service requested authorization and what data "
						+ "is going to be released. Note that user may always choose to disable "
						+ "the consent screen for each service, even if this setting is set to false."));

		defaults.put(TRANSLATION_PROFILE, defaultProfile != null
				? new PropertyMD(defaultProfile)
						.setDescription(defaultProfileMessage)
				: new PropertyMD().setDescription(defaultProfileMessage));

		defaults.put(ASSUME_FORCE, new PropertyMD("true").setDeprecated().
				setDescription("Ignored since 2.5.0, please remove the option from configuration"));

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
		

		defaults.put(ACTIVE_VALUE_SELECTION_PFX, new PropertyMD().setStructuredList(false)
				.setDescription("Under this prefix it is possible to configure a separate screen on which "
						+ "user can select attribute values which are used for the session. "
						+ "Multiple entries can be added with settings which are clients specific."));
		defaults.put(ACTIVE_VALUE_CLIENT, new PropertyMD().setStructuredListEntry(ACTIVE_VALUE_SELECTION_PFX)
				.setDescription("Identifier of a client for which the settings should be applied. "
						+ "If unset then those settings will be default. "
						+ "If more then one entry without client set is present, "
						+ "or there is more then one with the same client, "
						+ "then it is undefined which one will be used."));
		defaults.put(ACTIVE_VALUE_SINGLE_SELECTABLE, new PropertyMD().setStructuredListEntry(ACTIVE_VALUE_SELECTION_PFX).setList(false)
				.setDescription("List of attribute names for which a single active value must be selected by a user."));
		defaults.put(ACTIVE_VALUE_MULTI_SELECTABLE, new PropertyMD().setStructuredListEntry(ACTIVE_VALUE_SELECTION_PFX).setList(false)
				.setDescription("List of attribute names for which multiple active values must be selected by a user."));

		
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

	public static Optional<ActiveValueSelectionConfig> getActiveValueSelectionConfig(PropertiesHelper cfg, 
			String client, Collection<DynamicAttribute> allAttributes)
	{
		Optional<String> key = getActiveValueSelectionConfigKey(cfg, client);
		return key.isPresent() ? getActiveValueSelectionConfigFromKey(cfg, key.get(), allAttributes) : Optional.empty();
	}

	public static boolean isActiveValueSelectionConfiguredForClient(PropertiesHelper cfg, String client)
	{
		return getActiveValueSelectionConfigKey(cfg, client).isPresent();
	}

	private static Optional<String> getActiveValueSelectionConfigKey(PropertiesHelper cfg, String client)
	{
		Set<String> listKeys = cfg.getStructuredListKeys(ACTIVE_VALUE_SELECTION_PFX);
		String defaultClientKey = null;
		for (String key: listKeys)
		{
			String entryClient = cfg.getValue(key + ACTIVE_VALUE_CLIENT);
			if (entryClient == null)
			{
				defaultClientKey = key;
				continue;
			}
			if (entryClient.equals(client))
			{
				return Optional.of(key);
			}
		}
		return Optional.ofNullable(defaultClientKey);
	}
	
	private static Optional<ActiveValueSelectionConfig> getActiveValueSelectionConfigFromKey(PropertiesHelper cfg,
			String key, Collection<DynamicAttribute> attributes)
	{
		Map<String, DynamicAttribute> attrsMap = attributes.stream()
				.collect(Collectors.toMap(da -> da.getAttribute().getName(), da -> da));
		List<DynamicAttribute> singleSelectable = getAttributeForSelection(cfg, attrsMap, key + ACTIVE_VALUE_SINGLE_SELECTABLE);
		List<DynamicAttribute> multiSelectable = getAttributeForSelection(cfg, attrsMap, key + ACTIVE_VALUE_MULTI_SELECTABLE);
		if (singleSelectable.isEmpty() && multiSelectable.isEmpty())
			return Optional.empty();
		List<DynamicAttribute> remaining = new ArrayList<>(attributes);
		remaining.removeAll(singleSelectable);
		remaining.removeAll(multiSelectable);
		return Optional.of(new ActiveValueSelectionConfig(multiSelectable, singleSelectable, remaining));
	}
	
	private static List<DynamicAttribute> getAttributeForSelection(PropertiesHelper cfg, 
			Map<String, DynamicAttribute> attributes, String key)
	{
		List<String> names = cfg.getListOfValues(key);
		return names.stream()
				.map(attr -> attributes.get(attr))
				.filter(attr -> attr != null)
				.collect(Collectors.toList());
	}

	public static class ActiveValueSelectionConfig
	{
		public final List<DynamicAttribute> multiSelectableAttributes;
		public final List<DynamicAttribute> singleSelectableAttributes;
		public final List<DynamicAttribute> remainingAttributes;
		
		public ActiveValueSelectionConfig(List<DynamicAttribute> multiSelectableAttributes,
				List<DynamicAttribute> singleSelectableAttributes,
				List<DynamicAttribute> remainingAttributes)
		{
			this.multiSelectableAttributes = multiSelectableAttributes;
			this.singleSelectableAttributes = singleSelectableAttributes;
			this.remainingAttributes = remainingAttributes;
		}
	}
}

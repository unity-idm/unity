/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

/**
 * Configuration of the Home endpoint.
 * 
 * @author K. Benedyczak
 */
public class HomeEndpointProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, HomeEndpointProperties.class);

	public enum Components {credentialTab, trustedApplications, userDetailsTab, accountUpdateTab, 
		accountRemoval, attributesManagement, userInfo, identitiesManagement,
		accountLinking};

	public enum RemovalModes {remove, disable, blockAuthentication};
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.userhome.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();

	public static final String DISABLED_COMPONENTS = "disabledComponents.";
	public static final String ATTRIBUTES = "attributes.";
	public static final String ENQUIRIES = "accountUpdateEnquiries.";
	public static final String GWA_ATTRIBUTE = "attribute";
	public static final String GWA_GROUP = "group";
	public static final String GWA_SHOW_GROUP = "showGroup";
	public static final String GWA_EDITABLE = "editable";
	public static final String REMOVAL_MODE = "selfRemovalMode";
	public static final String DISABLE_REMOVAL_SCHEDULE = "disableSelfRemovalScheduling";
	public static final String DISABLE_2ND_FACTOR_OPT_IN = "disable2ndFactorOptIn";
	public static final String ENABLE_PROJECT_MANAGEMENT_LINK = "enableProjectManagementLink";
	public static final String PROJECT_MANAGEMENT_ENDPOINT = "projectManagementEndpoint";
	
	static
	{
		META.put(DISABLED_COMPONENTS, new PropertyMD().setList(false).setDescription("List of tags of UI components "
				+ "which should be disabled. Valid tags: '" + Arrays.asList(Components.values() + "'")));
		META.put(REMOVAL_MODE, new PropertyMD(RemovalModes.remove).
				setDescription("Relevant ONLY if the " + DISABLE_REMOVAL_SCHEDULE + " is true. "
				+ "Controls what action should be performed when user "
				+ "orders account removal. Account can be fully removed, "
				+ "disabled, or only login can be blocked."));
		META.put(DISABLE_REMOVAL_SCHEDULE, new PropertyMD("false").
				setDescription("If set to true then user won't be presented with an "
						+ "option to schedule account removal with grace period. "
						+ "At the same time this enables " + REMOVAL_MODE));
		META.put(DISABLE_2ND_FACTOR_OPT_IN, new PropertyMD("false").
				setDescription("If set to true then user won't be able to change setting "
						+ "of 2nd factor opt-in setting on the credentials panel."));
		META.put(ATTRIBUTES, new PropertyMD().setStructuredList(true).
				setDescription("Prefix under which it is possible to define attributes "
				+ "which should be either presented or made editable on the User Home UI."));
		META.put(GWA_ATTRIBUTE, new PropertyMD().setStructuredListEntry(ATTRIBUTES).setMandatory().
				setDescription("Attribute name."));
		META.put(GWA_GROUP, new PropertyMD().setStructuredListEntry(ATTRIBUTES).setMandatory().
				setDescription("Group of the attribute."));
		META.put(GWA_EDITABLE, new PropertyMD("true").setStructuredListEntry(ATTRIBUTES).
				setDescription("If enabled and the attribute is marked as self modificable,"
				+ " it will be possible to edit it. Otherwise it is shown in "
				+ "read only mode."));
		META.put(GWA_SHOW_GROUP, new PropertyMD("false").setStructuredListEntry(ATTRIBUTES).
				setDescription("If true then the group is shown next to the attribute."));
		META.put(ENQUIRIES, new PropertyMD().setList(false).
				setDescription("List of enquiries to be presented on User Home UI."));	
		META.put(ENABLE_PROJECT_MANAGEMENT_LINK, new PropertyMD("true").setDescription(
				"If true then the project managament link is shown in header of User Home UI."));
		META.put(PROJECT_MANAGEMENT_ENDPOINT, new PropertyMD().setDescription(
				"If project management link is active, then link redirect to this endpoint address. By default first active project management endpoint is used."));
	}
	
	public HomeEndpointProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
	
	public Set<String> getDisabledComponents()
	{
		return new HashSet<>(getListOfValues(DISABLED_COMPONENTS));
	}
	
	public List<String> getEnabledEnquiries()
	{
		return new ArrayList<>(getListOfValues(ENQUIRIES));
	}
	
	public boolean isProjectManLinkIsEnabled()
	{
		return getBooleanValue(ENABLE_PROJECT_MANAGEMENT_LINK);
	}
	
	public String getProjectManEndpoint()
	{
		return getValue(PROJECT_MANAGEMENT_ENDPOINT);
	}	
}

/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of the Home endpoint.
 * 
 * @author K. Benedyczak
 */
public class HomeEndpointProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, HomeEndpointProperties.class);

	public enum Components {credentialTab, preferencesTab, userDetailsTab, 
		accountRemoval, attributesManagement, userInfo};
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.userhome.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();

	public static final String DISABLED_COMPONENTS = "disabledComponents.";
	public static final String ATTRIBUTES = "attributes.";
	public static final String GWA_ATTRIBUTE = "attribute";
	public static final String GWA_GROUP = "group";
	public static final String GWA_SHOW_GROUP = "showGroup";
	public static final String GWA_EDITABLE = "editable";
	
	static
	{
		META.put(DISABLED_COMPONENTS, new PropertyMD().setList(false).
				setDescription("List of tags of UI components "
				+ "which should be disabled. Valid tags: '" + 
				Arrays.toString(Components.values()) + "'"));
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
	}
	
	public HomeEndpointProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
	
	public Set<String> getDisabledComponents()
	{
		return new HashSet<>(getListOfValues(DISABLED_COMPONENTS));
	}
}

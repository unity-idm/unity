/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.webui.common.idpselector.IdpSelectorComponent.ScaleMode;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Generic settings for all Vaadin web-endpoints.
 * @author K. Benedyczak
 */
public class VaadinEndpointProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, VaadinEndpointProperties.class);
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.endpoint.web.";
	
	public static final String PRODUCTION_MODE = "productionMode";
	public static final String ENABLE_REGISTRATION = "enableRegistration";
	public static final String ENABLED_REGISTRATION_FORMS = "enabledRegistrationForms.";
	public static final String AUTHN_TILES_PFX = "authenticationTiles.";
	public static final String AUTHN_TILE_CONTENTS = "tileContents";
	public static final String AUTHN_TILE_PER_LINE = "tileAuthnsPerLine";
	public static final String AUTHN_TILE_ICON_SIZE = "tileIconSize";
	public static final String DEFAULT_PER_LINE = "authnsPerLine";
	public static final String DEFAULT_AUTHN_ICON_SIZE = "authnIconSize";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static
	{
		META.put(PRODUCTION_MODE, new PropertyMD("true").setHidden().
				setDescription("Controls wether Vaadin should work in production mode or in debug mode (false)."));
		META.put(ENABLE_REGISTRATION, new PropertyMD("false").
				setDescription("Controls if registration option should be allowed for an endpoint."));
		META.put(ENABLED_REGISTRATION_FORMS, new PropertyMD().setList(false).
				setDescription("Defines which registration forms should be enabled for the endpoint. " +
						"Values are form names. If the form with given name doesn't exist it "
						+ "will be ignored." +
						"If there are no forms defined with this property, then all public "
						+ "forms are made available."));
		META.put(DEFAULT_PER_LINE, new PropertyMD("3").setBounds(1, 10).
				setDescription("Defines how many authenticators should be presented in a single "
						+ "line of an authentication tile."));
		META.put(DEFAULT_AUTHN_ICON_SIZE, new PropertyMD(ScaleMode.height50).
				setDescription("Defines how to scale authenticator icons in a tile."));
		META.put(AUTHN_TILES_PFX, new PropertyMD().setStructuredList(true).
				setDescription("Under this prefix authentication tiles can be defined. "
						+ "Authentication tile is a purely visual grouping of authentication options. "
						+ "If this list is undefined all authentication options will be "
						+ "put in a single default tile."));
		META.put(AUTHN_TILE_CONTENTS, new PropertyMD().setMandatory().setStructuredListEntry(AUTHN_TILES_PFX).
				setDescription("Specification of the authentication tile contents. "
						+ "Value is a list of name prefixes separated by spaces. "
						+ "Authentication option whose (primary) authenticator "
						+ "name starts with one of the prefixes will be assigned to this tile. "
						+ "The unassigned authentication options will be assigned to additional tile."));
		META.put(AUTHN_TILE_PER_LINE, new PropertyMD().setInt().setBounds(1, 10).setStructuredListEntry(AUTHN_TILES_PFX).
				setDescription("Defines how many authenticators should be presented in a single line of a tile. "
						+ "Overrides the default setting."));
		META.put(AUTHN_TILE_ICON_SIZE, new PropertyMD().setEnum(ScaleMode.height50).setStructuredListEntry(AUTHN_TILES_PFX).
				setDescription("Defines how to scale authenticator icons in a tile. "
						+ "Overrides the default setting."));
		
	}
	
	public VaadinEndpointProperties(Properties properties)
			throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}

}

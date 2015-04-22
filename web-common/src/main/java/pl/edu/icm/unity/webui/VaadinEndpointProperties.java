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
	private enum ScaleModeOld {
		none(ScaleMode.none), 
		height100(ScaleMode.heightSmall), 
		width100(ScaleMode.widthSmall), 
		height50(ScaleMode.heightTiny),
		width50(ScaleMode.widthTiny), 
		maxHeight50(ScaleMode.maxHeightTiny), 
		maxHeight100(ScaleMode.maxHeightSmall), 
		maxHeight200(ScaleMode.maxHeightMedium);
		
		private ScaleMode translated;
		ScaleModeOld(ScaleMode translated)
		{
			this.translated = translated;
		}
		
		public ScaleMode toScaleMode()
		{
			return translated;
		}
	}
	
	public enum ScaleMode {
		none, 
		heightSmall, 
		widthSmall, 
		heightTiny, 
		widthTiny, 
		maxHeightTiny, 
		maxHeightSmall, 
		maxHeightMedium}
	
	public enum TileMode {
		table,
		simple
	}
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.endpoint.web.";
	
	public static final String PRODUCTION_MODE = "productionMode";
	public static final String WEB_CONTENT_PATH = "webContentDirectory";
	public static final String DEF_THEME = "defaultTheme";
	public static final String THEME = "mainTheme";
	public static final String AUTHN_THEME = "authnTheme";
	
	public static final String ENABLE_REGISTRATION = "enableRegistration";
	public static final String ENABLED_REGISTRATION_FORMS = "enabledRegistrationForms.";
	public static final String AUTHN_TILES_PFX = "authenticationTiles.";
	public static final String AUTHN_TILE_CONTENTS = "tileContents";
	public static final String AUTHN_TILE_PER_LINE = "tileAuthnsPerLine";
	private static final String AUTHN_TILE_ICON_SIZE = "tileIconSize";
	public static final String AUTHN_TILE_ICON_SCALE = "tileIconScale";
	public static final String AUTHN_TILE_TYPE = "tileMode";
	public static final String AUTHN_TILE_DISPLAY_NAME = "tileName";
	public static final String DEFAULT_PER_LINE = "authnsPerLine";
	private static final String DEFAULT_AUTHN_ICON_SIZE = "authnIconSize";
	public static final String DEFAULT_AUTHN_ICON_SCALE = "authnIconScale";
	
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static
	{
		META.put(PRODUCTION_MODE, new PropertyMD("true").setHidden().
				setDescription("Controls wether Vaadin should work in production mode or in debug mode (false)."));
		META.put(WEB_CONTENT_PATH, new PropertyMD().setPath().setDescription(
				"Defines a folder from which the endpoint will serve static content, "
				+ "configured locally. If undefined the default value from the server's main configration is used."));
		META.put(DEF_THEME, new PropertyMD().setHidden().setDescription(
				"Placeholder, filled automatically with the copy of the main server's "
				+ "configuration setting, so the endpoints need not to access the main config for this single default value."));
		META.put(THEME, new PropertyMD().setDescription(
				"Overrides the default theme name as used for rendering the endpoint contents."));
		META.put(AUTHN_THEME, new PropertyMD().setDescription(
				"Overrides the default theme name as used for rendering the endpoint's "
				+ "authentication screen contents. If undefined the same setting as for the "
				+ "main endpoint UI is used."));
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
						+ "line of an authentication tile in simple mode."));
		META.put(DEFAULT_AUTHN_ICON_SIZE, new PropertyMD().setEnum(ScaleModeOld.height50).setDeprecated().
				setDescription("Deprecated, please use " + DEFAULT_AUTHN_ICON_SCALE));
		META.put(DEFAULT_AUTHN_ICON_SCALE, new PropertyMD(ScaleMode.maxHeightTiny).
				setDescription("Defines how to scale authenticator icons in the simple mode. "
						+ "Can be overriden per tile."));
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
		META.put(AUTHN_TILE_TYPE, new PropertyMD(TileMode.simple).setStructuredListEntry(AUTHN_TILES_PFX).
				setDescription("Defines how the tile should present authentication options assigned to it. "
						+ "In the +simple+ mode all logos (or names if logo is undefined) "
						+ "of authentication options are displayed,"
						+ "and it is configurable how many per row and how to scale them. "
						+ "The +table+ mode presents authentication options in rows one by one "
						+ "with logo in the first column. The table is much better choice when "
						+ "many authenticators are present as those are lazy loaded. In case "
						+ "when there is more then ca 30 authentication options the "
						+ "simple mode should not be used."));
		META.put(AUTHN_TILE_PER_LINE, new PropertyMD().setInt().setBounds(1, 10).setStructuredListEntry(AUTHN_TILES_PFX).
				setDescription("Defines how many authenticators should be presented in a single "
						+ "line of a tile in the simple mode. Overrides the default setting."));
		META.put(AUTHN_TILE_ICON_SIZE, new PropertyMD().setStructuredListEntry(AUTHN_TILES_PFX).
				setEnum(ScaleModeOld.height50).setDeprecated().
				setDescription("Deprecated, please use " + AUTHN_TILE_ICON_SCALE));
		META.put(AUTHN_TILE_ICON_SCALE, new PropertyMD().setEnum(ScaleMode.maxHeightTiny).setStructuredListEntry(AUTHN_TILES_PFX).
				setDescription("Defines how to scale authenticator icons in a tile in the simple mode. "
						+ "Overrides the default setting."));
		META.put(AUTHN_TILE_DISPLAY_NAME, new PropertyMD().setCanHaveSubkeys().setStructuredListEntry(AUTHN_TILES_PFX).
				setDescription("Defines the displayed name of the tile. "
						+ "Can have language specific versions. "
						+ "If undefined then tile has no name."));
		
	}
	
	private ScaleMode defaultScaleMode;
	
	public VaadinEndpointProperties(Properties properties)
			throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
		defaultScaleMode = getScaleModeInternal(DEFAULT_AUTHN_ICON_SCALE, DEFAULT_AUTHN_ICON_SIZE);
	}

	public ScaleMode getScaleMode(String tileKey)
	{
		ScaleMode ret = getScaleModeInternal(tileKey + VaadinEndpointProperties.AUTHN_TILE_ICON_SCALE, 
				tileKey + VaadinEndpointProperties.AUTHN_TILE_ICON_SIZE);
		return ret == null ? defaultScaleMode : ret;
	}

	public ScaleMode getDefaultScaleMode()
	{
		return defaultScaleMode;
	}

	private ScaleMode getScaleModeInternal(String key, String legacyKey)
	{
		ScaleMode ret = getEnumValue(key, ScaleMode.class);
		if (ret == null)
		{
			ScaleModeOld legacy = getEnumValue(legacyKey, ScaleModeOld.class);
			if (legacy != null)
				ret = legacy.toScaleMode();
		}
		return ret;
	}
	
}

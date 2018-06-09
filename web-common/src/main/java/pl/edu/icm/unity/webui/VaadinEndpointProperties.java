/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Generic settings for all Vaadin web-endpoints.
 * @author K. Benedyczak
 */
public class VaadinEndpointProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, VaadinEndpointProperties.class);
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
	public static final String TEMPLATE = "template";
	public static final String AUTO_LOGIN = "autoLogin";
	
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
			
			
	public static final String AUTHN_LOGO = "authnScreenLogo";
	public static final String AUTHN_TITLE = "authnScreenTitle";
	public static final String AUTHN_SHOW_SEARCH = "authnScreenShowSearch";
	public static final String AUTHN_SHOW_CANCEL = "authnScreenShowCancel";
	public static final String AUTHN_ADD_ALL = "authnScreenShowAllOptions";

	public static final String AUTHN_OPTION_LABEL_PFX = "authnScreenOptionsLabel.";
	public static final String AUTHN_OPTION_LABEL_TEXT = "text";
	
	public static final String AUTHN_COLUMNS_PFX = "authnScreenColumn.";
	public static final String AUTHN_COLUMN_TITLE = "columnTitle";
	public static final String AUTHN_COLUMN_SEPARATOR = "columnSeparator";
	public static final float DEFAULT_AUTHN_COLUMN_WIDTH = 14.5f;
	public static final String AUTHN_COLUMN_WIDTH = "columnWidth";
	public static final String AUTHN_COLUMN_CONTENTS = "columnContents";



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
		META.put(TEMPLATE, new PropertyMD("default.ftl").setDescription(
				"The name of a Freemarker template, relative to templates directory, with a "
				+ "template of the endpoint web interface. Custom template can be used to add "
				+ "static header/footer etc."));
		META.put(AUTO_LOGIN, new PropertyMD("false").setDescription(
				"If set to true and the endpoint has a single authentication option configured "
				+ " and this option supports automated login (as remote SAML or OAuth login), then this "
				+ "option will be activated automatically, without presenting (or even loading) "
				+ "the authentication screen."));
		
		META.put(AUTHN_LOGO, new PropertyMD("file:../common/img/other/logo.png").
				setDescription("Sets URL of image that should be shown above all authentication options."));
		META.put(AUTHN_TITLE, new PropertyMD().setCanHaveSubkeys()
				.setDescription("Message (can be localized) which will be displayed above "
						+ "all authentication options. If unset a default message from "
						+ "message bundle will be used. Set to empty string to completely remove the title."));
		META.put(AUTHN_SHOW_SEARCH, new PropertyMD("false").
				setDescription("Whether to show a filter control, allowing to search for desred authentication option. "
						+ "Useful if many authentication options are allowed."));
		META.put(AUTHN_SHOW_CANCEL, new PropertyMD("true").
				setDescription("Whether to show a cancel button. This setting is relevant only on "
						+ "authentication screens which are not accessed directly "
						+ "(e.g. on IdP authentication screen after redirection from SP)."));
		META.put(AUTHN_ADD_ALL, new PropertyMD("true").
				setDescription("If set to true then all authentication options configured for the "
						+ "edpoint will be added on the screen. If any of options is not explicitly "
						+ "enumerated, then it will be appended to the last column."));
		META.put(AUTHN_OPTION_LABEL_PFX, new PropertyMD().setStructuredList(false)
				.setDescription("Under this prefix it is possible to define text separators, "
						+ "which can be referenced in column definitions. The separators "
						+ "are defined separately so that it is possible to provide international "
						+ "variants of the text, what would be difficult in inline in the column "
						+ "contents specification"));
		META.put(AUTHN_OPTION_LABEL_TEXT, new PropertyMD().setStructuredListEntry(AUTHN_OPTION_LABEL_PFX).setCanHaveSubkeys()
				.setDescription("Separator's message (can be localized)."));
		META.put(AUTHN_COLUMNS_PFX, new PropertyMD().setStructuredList(true)
				.setDescription("Under this prefix are defined columns in which authenticators are organized."));
		META.put(AUTHN_COLUMN_TITLE, new PropertyMD().setStructuredListEntry(AUTHN_COLUMNS_PFX).setCanHaveSubkeys()
				.setDescription("Message (can be localized) which will be displayed at the top of the column."));
		META.put(AUTHN_COLUMN_SEPARATOR, new PropertyMD().setStructuredListEntry(AUTHN_COLUMNS_PFX).setCanHaveSubkeys()
				.setDescription("Message (can be localized) which will be displayed after the column, "
						+ "i.e. will become separator between this and the next column. Not used for the last column."));
		META.put(AUTHN_COLUMN_WIDTH, new PropertyMD(String.valueOf(DEFAULT_AUTHN_COLUMN_WIDTH))
				.setFloat().setMin(1.0).setStructuredListEntry(AUTHN_COLUMNS_PFX)
				.setDescription("Width of the column, specified in +em+ unit (see CSS spec for details)"));
		META.put(AUTHN_COLUMN_CONTENTS, new PropertyMD().setMandatory().setStructuredListEntry(AUTHN_COLUMNS_PFX)
				.setDescription("Contents of the column. Values are space separated prefixes of "
						+ "authentication options. All options whose (primary) authenticator "
						+ "name starts with the prefixes will be added to the column, "
						+ "in the order of provided. There are special values which can be also used: "
						+ "+_LAST_USED+ - dynamic option which is set to the one which was recently used on the clinet machine. "
						+ "+_REGISTER+ - button allowing to sign up (makes sense if registration forms are configured) "
						+ "+_SEPARATOR_KEY+ - text from the message with the given key is inserted as a separator. "
						+ "If +_KEY+ suffix is skipped then empty separator is inserted. "
						+ "Separator will be only added if there is non text element before it and after it."
						+ "+_HEADER_KEY+ - text from the message with the given key is inserted as a in-line header. "
						+ "If +_KEY+ suffix is skipped then empty header is inserted. "
						+ "Header will be only added if there is a non-text element after it (what is the only difference to separator)."));
		
		
		
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

	/**
	 * Returns either a theme configured with the key given as argument or the default theme if the
	 * specific one is not defined. Can return null if neither is available.
	 * @param themeConfigKey
	 * @return configuration theme
	 */
	public String getConfiguredTheme(String themeConfigKey)
	{
		if (isSet(themeConfigKey))
			return getValue(themeConfigKey);
		else if (isSet(VaadinEndpointProperties.DEF_THEME))
			return getValue(VaadinEndpointProperties.DEF_THEME);
		return null;
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
	
	public Properties getProperties()
	{
		return properties;
	}
}

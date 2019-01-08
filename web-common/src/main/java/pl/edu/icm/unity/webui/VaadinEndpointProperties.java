/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

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
	
	enum ScaleMode {
		none, 
		heightSmall, 
		widthSmall, 
		heightTiny, 
		widthTiny, 
		maxHeightTiny, 
		maxHeightSmall, 
		maxHeightMedium}
	
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
	public static final String EXTERNAL_REGISTRATION_URL = "externalRegistrationURL";
	public static final String ENABLED_REGISTRATION_FORMS = "enabledRegistrationForms.";
	public static final String SHOW_REGISTRATION_FORMS_IN_HEADER = "showRegistrationFormsInHeader";

	public static final String AUTHN_SCREEN_MODE = "screenType";	
	
	public static final String AUTHN_LOGO = "authnScreenLogo";
	public static final String AUTHN_TITLE = "authnScreenTitle";
	public static final String AUTHN_SHOW_SEARCH = "authnScreenShowSearch";
	public static final String AUTHN_SHOW_CANCEL = "authnScreenShowCancel";
	public static final String AUTHN_SHOW_LAST_OPTION_ONLY = "authnShowLastOptionOnly";
	public static final String AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT = "authnLastOptionOnlyLayout";
	public static final String AUTHN_ADD_ALL = "authnScreenShowAllOptions";

	public static final String AUTHN_OPTION_LABEL_PFX = "authnScreenOptionsLabel.";
	public static final String AUTHN_OPTION_LABEL_TEXT = "text";

	public static final String AUTHN_GRIDS_PFX = "authnGrid.";
	public static final String AUTHN_GRID_CONTENTS = "gridContents";
	public static final String AUTHN_GRID_ROWS = "gridRows";

	public static final String AUTHN_COLUMNS_PFX = "authnScreenColumn.";
	public static final String AUTHN_COLUMN_TITLE = "columnTitle";
	public static final String AUTHN_COLUMN_SEPARATOR = "columnSeparator";
	public static final float DEFAULT_AUTHN_COLUMN_WIDTH = 15f;
	public static final String AUTHN_COLUMN_WIDTH = "columnWidth";
	public static final String AUTHN_COLUMN_CONTENTS = "columnContents";
	
	public static final String CRED_RESET_COMPACT = "compactCredentialReset";
	


	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();
	
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
						+ "Useful if many authentication options are allowed. Note that "
						+ "search will be only visible if at least one grid widget with authenticators is on the screen."));
		META.put(AUTHN_SHOW_CANCEL, new PropertyMD("true").
				setDescription("Whether to show a cancel button. This setting is relevant only on "
						+ "authentication screens which are not accessed directly "
						+ "(e.g. on IdP authentication screen after redirection from SP)."));
		META.put(AUTHN_SHOW_LAST_OPTION_ONLY, new PropertyMD("true").
				setDescription("Used for returning users (who authenticated at least once before). "
						+ "If set to true only the previously used authentication option "
						+ "will be shown to the user (it will be still possible to reveal other ones by clicking special button)."
						+ " If set to false then this feature is turned off and users always see all available options."));
		META.put(AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT, new PropertyMD("_LAST_USED _SEPARATOR _EXPAND").
				setDescription("Advanced setting, typically should not be changed. Same syntax as column's contents. "
						+ "Defines layout which is used when a single last used authN is presented "
						+ "(i.e. relevant only when " + AUTHN_SHOW_LAST_OPTION_ONLY + " is enabled). "
						+ "May be used to change spacing or add label."));
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

		META.put(AUTHN_GRIDS_PFX, new PropertyMD().setStructuredList(false)
				.setDescription("Definitions of grid widgets for presenting mass lists of authentication "
						+ "options are configured under this prefix."
						+ " Can be referenced in column definitions by name."));
		META.put(AUTHN_GRID_CONTENTS, new PropertyMD().setStructuredListEntry(AUTHN_GRIDS_PFX).setMandatory()
				.setDescription("Contents of the grid. Similar syntax as column contents, "
						+ "but no special entry is allowed. Note that only some authenticators "
						+ "(basically those that offer one click login) are compatible with the grid - "
						+ "others (like password) are skipped."));
		META.put(AUTHN_GRID_ROWS, new PropertyMD("10").setStructuredListEntry(AUTHN_GRIDS_PFX).setMin(2)
				.setDescription("Number of rows to be shown in the grid."));

		
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
				.setDescription("Contents of the column. Values are space separated names of authenticators or "
						+ "authentication options (e.g. oauthWeb or oauthWeb.google). "
						+ "All options whose (primary) authenticator "
						+ "name matches will be added to the column, "
						+ "in the order of provided. Additionally there are special values which can be also used: "
						+ "+_LAST_USED+ - dynamic option which is set to the one which was recently used on the clinet machine. "
						+ "+_REGISTER+ - button allowing to sign up (makes sense if registration forms are configured) "
						+ "+_SEPARATOR_KEY+ - text from the message with the given key is inserted as a separator. "
						+ "If +_KEY+ suffix is skipped then empty separator is inserted. "
						+ "Separator will be only added if there is non text element before it and after it."
						+ "+_HEADER_KEY+ - text from the message with the given key is inserted as a in-line header. "
						+ "If +_KEY+ suffix is skipped then empty header is inserted. "
						+ "Header will be only added if there is a non-text element after it"
						+ " (what is the only difference to separator). "
						+ "+_GRID_ID+ - inserts grid widget into the column. Grid must be defined with the provided id."));

		META.put(CRED_RESET_COMPACT, new PropertyMD("true").
				setDescription("Controls if credential reset UI and outdated credential UI should use separate captions (false), "
						+ "or captions put as placeholders resembling authN UI (true)."));
		
		META.put(ENABLE_REGISTRATION, new PropertyMD("false").
				setDescription("Controls if registration option should be allowed for an endpoint."));
		META.put(EXTERNAL_REGISTRATION_URL, new PropertyMD().
				setDescription("If set then registration links on the authentication screen are not "
						+ "showing any Unity-provided registration forms, but redirect "
						+ "straight to the given address."));
		META.put(SHOW_REGISTRATION_FORMS_IN_HEADER, new PropertyMD("true").
				setDescription("Displays the links with registration forms in the top right corner of page, if registration "
						+ "is configured."));
		META.put(ENABLED_REGISTRATION_FORMS, new PropertyMD().setList(false).
				setDescription("Defines which registration forms should be enabled for the endpoint. " +
						"Values are form names. If the form with given name doesn't exist it "
						+ "will be ignored." +
						"If there are no forms defined with this property, then all public "
						+ "forms are made available."));
	}
	
	public VaadinEndpointProperties(Properties properties)
			throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
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

	public Properties getProperties()
	{
		return properties;
	}
	
	public EndpointRegistrationConfiguration getRegistrationConfiguration()
	{
		String extURL = getValue(EXTERNAL_REGISTRATION_URL);
		Optional<String> externalRegistrationURL = Strings.isEmpty(extURL) ? 
				Optional.empty() : Optional.of(extURL);
				
		return new EndpointRegistrationConfiguration(getListOfValues(
				VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS),
				getBooleanValue(VaadinEndpointProperties.ENABLE_REGISTRATION),
				getBooleanValue(VaadinEndpointProperties.SHOW_REGISTRATION_FORMS_IN_HEADER),
				externalRegistrationURL);
	}

}

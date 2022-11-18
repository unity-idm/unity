/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.idp;

import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains settings which are common for all IdP endpoints
 * @author Krzysztof Benedyczak
 */
public class CommonIdPProperties
{
	public static final String SKIP_CONSENT = "skipConsent";
	public static final String TRANSLATION_PROFILE = "translationProfile";
	public static final String EMBEDDED_TRANSLATION_PROFILE = "embeddedTranslationProfile";
	public static final String SKIP_USERIMPORT = "skipUserImport";
	public static final String USERIMPORT_PFX = "userImport.";
	public static final String USERIMPORT_IMPORTER = "importer";
	public static final String USERIMPORT_IDENTITY_TYPE = "identityType";

	public static final String ACTIVE_VALUE_SELECTION_PFX = "activeValue.";
	public static final String ACTIVE_VALUE_CLIENT = "client";
	public static final String ACTIVE_VALUE_SINGLE_SELECTABLE = "singleValueAttributes.";
	public static final String ACTIVE_VALUE_MULTI_SELECTABLE = "multiValueAttributes.";
	
	private static final String ASSUME_FORCE = "assumeForceOnSessionClash";
	
	public static final String POLICY_AGREEMENTS_TITLE = "policyAgreementsTitle";
	public static final String POLICY_AGREEMENTS_INFO = "policyAgreementsInfo";
	public static final String POLICY_AGREEMENTS_WIDTH = "policyAgreementsWidth";
	public static final String POLICY_AGREEMENTS_WIDTH_UNIT = "policyAgreementsWidthUnit";
	public static final String POLICY_AGREEMENTS_PFX = "policyAgreements.";
	public static final String POLICY_AGREEMENT_DOCUMENTS = "policyDocuments";
	public static final String POLICY_AGREEMENT_PRESENTATION_TYPE = "policyAgreementPresentationType";
	public static final String POLICY_AGREEMENT_TEXT= "text";
	
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
		defaults.put(EMBEDDED_TRANSLATION_PROFILE, new PropertyMD().setHidden().
				setDescription(defaultProfileMessage));
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

		
		defaults.put(POLICY_AGREEMENTS_TITLE,
				new PropertyMD().setCanHaveSubkeys().setDescription("Policy acceptanance view title"));
		defaults.put(POLICY_AGREEMENTS_INFO, new PropertyMD().setCanHaveSubkeys()
				.setDescription("Policy acceptanance view additional information"));
		defaults.put(POLICY_AGREEMENTS_WIDTH,
				new PropertyMD("40").setDescription("Policy acceptanance view widht"));
		defaults.put(POLICY_AGREEMENTS_WIDTH_UNIT,
				new PropertyMD("em").setDescription("Policy acceptanance view width unit"));
		defaults.put(POLICY_AGREEMENTS_INFO, new PropertyMD().setCanHaveSubkeys()
				.setDescription("Policy acceptanance view additional information"));
		defaults.put(POLICY_AGREEMENTS_PFX, new PropertyMD().setStructuredList(true).setDescription(
				"Individual policy agreement items are configured under this prefix"));
		defaults.put(POLICY_AGREEMENT_DOCUMENTS,
				new PropertyMD().setStructuredListEntry(POLICY_AGREEMENTS_PFX).setDescription(
						"List of policy documents ids included in the item"));
		defaults.put(POLICY_AGREEMENT_PRESENTATION_TYPE,
				new PropertyMD().setStructuredListEntry(POLICY_AGREEMENTS_PFX)
						.setDescription("Policy agreement presentation type"));
		defaults.put(POLICY_AGREEMENT_TEXT, new PropertyMD().setCanHaveSubkeys()
				.setStructuredListEntry(POLICY_AGREEMENTS_PFX)
				.setDescription("Policy agreement text with placeholders. Format of placeholder is {PolicyDocucmentId:DisplayedText}"));

		return defaults;
	}

	public static IdpPolicyAgreementsConfiguration getPolicyAgreementsConfig(MessageSource msg, UnityPropertiesHelper cfg)
	{
		return IdpPolicyAgreementsConfigurationParser.fromPropoerties(msg, cfg);
	}
}

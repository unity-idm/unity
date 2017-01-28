/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.trust.CheckingMode;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.trust.StrictSamlTrustChecker;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.ecp.SAMLECPProperties;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

/**
 * Configuration of a SAML requester (or SAML SP).
 * @author K. Benedyczak
 */
public class SAMLSPProperties extends SamlProperties
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, SAMLSPProperties.class);
	
	public enum MetadataSignatureValidation {require, ignore};
	
	@DocumentationReferencePrefix
	public static final String P = "unity.saml.requester.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	public static final String REQUESTER_ID = "requesterEntityId";
	public static final String CREDENTIAL = "requesterCredential";
	public static final String ACCEPTED_NAME_FORMATS = "acceptedNameFormats.";
	public static final String DISPLAY_NAME = "displayName";
	public static final String PROVIDERS_IN_ROW = "idpsInRow";
	private static final String ICON_SCALE = "iconScale";
	public static final String SELECTED_PROVDER_ICON_SCALE = "selectedProviderIconScale";
	public static final String METADATA_PATH = "metadataPath";
	public static final String SLO_PATH = "sloPath";
	public static final String SLO_REALM = "sloRealm";
	
	public static final String DEF_SIGN_REQUEST = "defaultSignRequest";
	public static final String DEF_REQUESTED_NAME_FORMAT = "defaultRequestedNameFormat";
	public static final String REQUIRE_SIGNED_ASSERTION = "requireSignedAssertion";

	public static final String IDPMETA_PREFIX = "metadataSource.";
	public static final String IDPMETA_TRANSLATION_PROFILE = "perMetadataTranslationProfile";
	public static final String IDPMETA_REGISTRATION_FORM = "perMetadataRegistrationForm";
	
	public static final String IDP_PREFIX = "remoteIdp.";
	public static final String IDP_NAME = "name";
	public static final String IDP_LOGO = "logoURI";
	public static final String IDP_ID = "samlId";
	public static final String IDP_ADDRESS = "address";
	public static final String IDP_BINDING = "binding";
	public static final String IDP_CERTIFICATE = "certificate";
	public static final String IDP_CERTIFICATES = "certificates.";
	public static final String IDP_SIGN_REQUEST = "signRequest";
	public static final String IDP_REQUESTED_NAME_FORMAT = "requestedNameFormat";
	public static final String IDP_GROUP_MEMBERSHIP_ATTRIBUTE = "groupMembershipAttribute";
	
	static
	{
		DocumentationCategory common = new DocumentationCategory(
				"Common settings", "01");
		DocumentationCategory idp = new DocumentationCategory(
				"Manual settings of trusted IdPs", "03");
		DocumentationCategory webRetrieval = new DocumentationCategory(
				"SAML web UI specific settings", "04");

		META.put(IDP_PREFIX, new PropertyMD().setStructuredList(false).setCategory(idp).setDescription(
				"With this prefix configuration of trusted and enabled remote SAML IdPs is stored. " +
				"There must be at least one IdP defined. If there are multiple ones defined, then the user can choose which one to use."));
		META.put(IDP_ADDRESS, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
				"Address of the IdP endpoint."));
		META.put(IDP_BINDING, new PropertyMD(Binding.HTTP_REDIRECT).setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
				"SAML binding to be used to send a request to the IdP. If you use 'SOAP' here then the IdP will be available only for ECP logins, not via the web browser login."));
		META.put(REDIRECT_LOGOUT_URL, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
				"Address of the IdP Single Logout Endpoint supporting HTTP Redirect binding."));
		META.put(REDIRECT_LOGOUT_RET_URL, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
				"Address of the IdP Single Logout response endpoint supporting HTTP Redirect binding. "
				+ "If undefined the base redirect endpoint address is used."));
		META.put(POST_LOGOUT_URL, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
				"Address of the IdP Single Logout Endpoint supporting HTTP POST binding."));
		META.put(POST_LOGOUT_RET_URL, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
				"Address of the IdP Single Logout response endpoint supporting HTTP POST binding. "
				+ "If undefined the base redirect endpoint address is used."));
		META.put(SOAP_LOGOUT_URL, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
				"Address of the IdP Single Logout Endpoint supporting SOAP binding."));
		META.put(IDP_NAME, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setCanHaveSubkeys().setDescription(
				"Displayed name of the IdP. If not defined then the name is created " +
				"from the IdP address (what is rather not user friendly). The property can have subkeys being "
				+ "locale names; then the localized value is used if it is matching the selected locale of the UI."));	
		META.put(IDP_LOGO, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setCanHaveSubkeys().setDescription(
				"Displayed logo of the IdP. If not defined then only the name is used. "
				+ "The value can be a file:, http(s): or data: URI. The last option allows for embedding the logo in the configuration. "
				+ "The property can have subkeys being "
				+ "locale names; then the localized value is used if it is matching the selected locale of the UI."));
		META.put(IDP_ID, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setMandatory().setCategory(idp).setDescription(
				"SAML entity identifier of the IdP."));
		META.put(IDP_CERTIFICATE, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
				"Certificate name (as used in centralized PKI store) of the IdP. This certificate is used to verify signature of SAML " +
				"response and included assertions. Therefore it is of highest importance for the whole system security."));
		META.put(IDP_CERTIFICATES, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setList(false).setDescription(
				"Using this property additional trusted certificates of an IdP can be added (when IdP uses more then one). See " 
						+ IDP_CERTIFICATE + " for details. Those properties can be used together or alternatively."));
		META.put(IDP_SIGN_REQUEST, new PropertyMD("false").setCategory(idp).setStructuredListEntry(IDP_PREFIX).setDescription(
				"Controls whether the requests for this IdP should be signed."));
		META.put(IDP_REQUESTED_NAME_FORMAT, new PropertyMD().setCategory(idp).setStructuredListEntry(IDP_PREFIX).setDescription(
				"If defined then specifies what SAML name format should be requested from the IdP." +
				" If undefined then IdP is free to choose, however see the " + ACCEPTED_NAME_FORMATS +
				" property. Value is arbitrary string, meaningful for the IdP. SAML specifies several standard formats:" +
				" +urn:oasis:names:tc:SAML:2.0:nameid-format:persistent+," +
				" +urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress+," +
				" +urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName+ and " +
				" +urn:oasis:names:tc:SAML:2.0:nameid-format:transient+ are the most popular."));
		META.put(IDP_GROUP_MEMBERSHIP_ATTRIBUTE, new PropertyMD().setCategory(idp).setStructuredListEntry(IDP_PREFIX).setDescription(
				"Defines a SAML attribute name which will be treated as an attribute carrying group" +
				" membership information."));
		META.put(CommonWebAuthnProperties.TRANSLATION_PROFILE, new PropertyMD().setCategory(idp).setStructuredListEntry(IDP_PREFIX).
				setDescription("Name of a translation" +
				" profile, which will be used to map remotely obtained attributes and identity" +
				" to the local counterparts. The profile should at least map the remote identity."));
		META.put(CommonWebAuthnProperties.REGISTRATION_FORM, new PropertyMD().setCategory(idp).setStructuredListEntry(IDP_PREFIX).setDescription(
				"Name of a registration form to be shown for a remotely authenticated principal who " +
				"has no local account. If unset such users will be denied."));	
		META.put(CommonWebAuthnProperties.ENABLE_ASSOCIATION, new PropertyMD().setCategory(idp).
				setStructuredListEntry(IDP_PREFIX).setDescription(
				"If true then unknown remote user gets an option to associate the remote identity "
				+ "with an another local (already existing) account. Overrides the global setting."));	
		META.put(REQUESTER_ID, new PropertyMD().setMandatory().setCategory(common).setDescription(
				"SAML entity ID (must be a URI) of the local SAML requester (or service provider)."));
		META.put(CREDENTIAL, new PropertyMD().setCategory(common).setDescription(
				"Local credential, used to sign requests and to decrypt encrypted assertions. "
				+ "If neither signing nor decryption is used it can be skipped."));
		META.put(SLO_PATH, new PropertyMD().setCategory(common).setDescription(
				"Last element of the URL, under which the SAML Single Logout functionality should "
				+ "be published for this SAML authenticator. Any suffix can be used, however it "
				+ "must be unique for all SAML authenticators in the system. If undefined the SLO functionality won't be enabled."));
		META.put(SLO_REALM, new PropertyMD().setCategory(common).setDescription(
				"Name of the authentication realm of the endpoints using this authenticator. "
				+ "This is needed to enable Single Logout functionality (if undefined the SLO "
				+ "functionality will be disabled). If this authenticator is used by endpoints placed in different realms and "
				+ "you still want to have SLO functionality you have to define one authenticator per realm."));
		META.put(METADATA_PATH, new PropertyMD().setCategory(SamlProperties.samlMetaCat).setDescription(
				"Last element of the URL, under which the SAML metadata should be published for this SAML authenticator." +
				"Used only if metadata publication is enabled. See the SAML Metadata section for more details."));
		META.put(ACCEPTED_NAME_FORMATS, new PropertyMD().setList(false).setCategory(common).setDescription(
				"If defined then specifies what SAML name formatd are accepted from IdP. " +
				"Useful when the property " + IDP_REQUESTED_NAME_FORMAT + " is undefined for at least one IdP. "));
		META.put(REQUIRE_SIGNED_ASSERTION, new PropertyMD("false").setCategory(common).setDescription(
				"SAML authN responses may be signed as a whole and/or may have signed individual assertions"
				+ " which are contained in the response. In general SAML SSO protocol requires "
				+ "assertions to be signed, but in the wild this is not always the case. If this option"
				+ "is set to false, then response will be accepted also when it is signed, "
				+ "but its assertions are not."));
		META.put(DEF_SIGN_REQUEST, new PropertyMD("false").setCategory(common).setDescription(
				"Default setting of request signing. Used for those IdPs, for which the setting is not set explicitly."));
		META.put(DEF_REQUESTED_NAME_FORMAT, new PropertyMD().setCategory(common).setDescription(
				"Default setting of requested identity format. Used for those IdPs, for which the setting is not set explicitly."));	
		META.put(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION, new PropertyMD("true").setCategory(common).setDescription(
				"Default setting allowing to globally control whether account association feature is enabled. "
				+ "Used for those IdPs, for which the setting is not set explicitly."));	
		META.put(ICON_SCALE, new PropertyMD().setCategory(webRetrieval).setDeprecated().
				setDescription("Deprecated, use either authentication UI icon scalling or "
						+ "the " + SELECTED_PROVDER_ICON_SCALE));
		META.put(SELECTED_PROVDER_ICON_SCALE, new PropertyMD(ScaleMode.none).setCategory(webRetrieval).
				setDescription("Controls whether and how "
				+ "the icon of a selected provider should be scalled. Note that this setting affects only the "
				+ "icon of a currently selected provider."));
		META.put(SAMLECPProperties.JWT_P, new PropertyMD().setCanHaveSubkeys().setHidden());	
			
		META.put(IDPMETA_TRANSLATION_PROFILE, new PropertyMD().setCategory(remoteMeta).
				setStructuredListEntry(IDPMETA_PREFIX).setDescription(
				"Deafult translation profile for all the IdPs from the metadata. "
				+ "Can be overwritten by individual IdP configuration entries."));
		META.put(IDPMETA_REGISTRATION_FORM, new PropertyMD().setCategory(remoteMeta).
				setStructuredListEntry(IDPMETA_PREFIX).setDescription(
				"Deafult registration form for all the IdPs from the metadata. Can be overwritten by "
				+ "individual IdP configuraiton entries."));
		
		META.put(IDENTITY_MAPPING_PFX, new PropertyMD().setStructuredList(false).setCategory(common).
				setDescription("Prefix used to store mappings of SAML identity types to Unity identity types. "
						+ "Those mappings are used to reverse the mapping process of remote identity "
						+ "mapping into Unity representation (as configured with an input translation profile). "
						+ "This is used solely to provide a single logout functionality, "
						+ "where remote peer may request to logout an identity previously "
						+ "authenticated. Unity needs to be able to find this person's session to terminate it."));
		META.put(IDENTITY_LOCAL, new PropertyMD().setStructuredListEntry(IDENTITY_MAPPING_PFX).setMandatory().setCategory(common).
				setDescription("Unity identity to which the SAML identity is mapped. If it is set to an empty value, then the mapping is disabled, "
						+ "what is useful for turning off the default mappings."));
		META.put(IDENTITY_SAML, new PropertyMD().setStructuredListEntry(IDENTITY_MAPPING_PFX).setMandatory().setCategory(common).
				setDescription("SAML identity to be mapped"));	

		
		META.putAll(SamlProperties.getDefaults(IDPMETA_PREFIX, "Under this prefix you can configure "
				+ "the remote trusted SAML IdPs however not providing all their details but only "
				+ "their metadata."));
		
		META.put(DISPLAY_NAME, new PropertyMD().setCanHaveSubkeys().setDeprecated());
		META.put(PROVIDERS_IN_ROW, new PropertyMD().setDeprecated());
	}
	
	private PKIManagement pkiManagement;
	private Properties sourceProperties;

	public SAMLSPProperties(Properties properties, PKIManagement pkiMan) throws ConfigurationException
	{
		this(properties, META, pkiMan);
	}

	/**
	 * For cloning only.
	 * @param pkiMan
	 * @throws ConfigurationException
	 */
	protected SAMLSPProperties(SAMLSPProperties cloned) throws ConfigurationException
	{
		super(cloned);
		this.pkiManagement = cloned.pkiManagement;
		this.sourceProperties = new Properties(cloned.sourceProperties);
	}
	
	protected SAMLSPProperties(Properties properties, Map<String, PropertyMD> meta, 
			PKIManagement pkiMan) throws ConfigurationException
	{
		super(P, properties, meta, log);
		
		addCachedPrefixes("unity\\.saml\\.requester\\.remoteIdp\\.[^.]+\\.certificates\\.",
				"unity\\.saml\\.requester\\.remoteIdp\\.[^.]+\\.name\\.");
		
		sourceProperties = new Properties();
		sourceProperties.putAll(properties);
		this.pkiManagement = pkiMan;
		Set<String> idpKeys = getStructuredListKeys(IDP_PREFIX);
		boolean sign = false;
		for (String idpKey: idpKeys)
		{
			boolean s = isSignRequest(idpKey);  
			sign |= s;
			Binding b = getEnumValue(idpKey+IDP_BINDING, Binding.class); 
			if (s && (b == Binding.HTTP_REDIRECT || b == Binding.SOAP))
			{
				String name = getName(idpKey);
				throw new ConfigurationException("IdP " + name + " is configured to use " +
						"HTTP Redirect binding or SOAP binding for ECP and at "
						+ "the same time Unity is configured to sign requests for this IdP. "
						+ "This is unsupported currently and against SAML interoperability specification.");
			}
			
		}
		if (sign)
		{
			String credential = getValue(CREDENTIAL);
			if (credential == null)
				throw new ConfigurationException("Credential must be defined when " +
						"request signing is enabled for at least one IdP.");
			try
			{
				if (!pkiMan.getCredentialNames().contains(credential))
					throw new ConfigurationException("Credential name is invalid - there is no such " +
							"credential available '" + credential + "'.");
			} catch (EngineException e)
			{
				throw new ConfigurationException("Can't esablish a list of known credentials", e);
			}
		}
		
		Set<String> metaKeys = getStructuredListKeys(IDPMETA_PREFIX);
		Set<String> certs;
		try
		{
			certs = pkiManagement.getCertificateNames();
		} catch (EngineException e)
		{
			throw new ConfigurationException("Can't retrieve available certificates", e);
		}
		for (String metaKey: metaKeys)
		{
			MetadataSignatureValidation validation = getEnumValue(metaKey + METADATA_SIGNATURE, 
					MetadataSignatureValidation.class);
			if (validation == MetadataSignatureValidation.require)
			{
				String certName = getValue(metaKey + METADATA_ISSUER_CERT);
				if (certName == null)
					throw new ConfigurationException("For the " + metaKey + 
						" entry the certificate for metadata signature verification is not set");
				if (!certs.contains(certName))
					throw new ConfigurationException("For the " + metaKey + 
						" entry the certificate for metadata signature "
						+ "verification is incorrect: " + certName);
			}
		}
		
		//test drive
		getTrustChecker();
		
		if (getBooleanValue(PUBLISH_METADATA) && !isSet(METADATA_PATH))
			throw new ConfigurationException("Metadata path " + getKeyDescription(METADATA_PATH) + 
					" must be set if metadata publication is enabled.");
	}
	
	@Override
	public synchronized void setProperties(Properties properties) throws ConfigurationException
	{
		long start = System.currentTimeMillis();
		super.setProperties(properties);
		log.info("Updated trusted IdPs configuration with " + getStructuredListKeys(IDP_PREFIX).size() 
				+ " explicit trusted providers, took " + (System.currentTimeMillis() - start) + "ms");
	}

	public X509Credential getRequesterCredential()
	{
		String credential = getValue(SAMLSPProperties.CREDENTIAL);
		if (credential == null)
			return null;
		try
		{
			return pkiManagement.getCredential(credential);
		} catch (EngineException e)
		{
			return null;
		}
	}
	
	public SamlTrustChecker getTrustChecker() throws ConfigurationException
	{
		Set<String> idpKeys = getStructuredListKeys(IDP_PREFIX);
		CheckingMode mode = getBooleanValue(REQUIRE_SIGNED_ASSERTION) ? 
					CheckingMode.REQUIRE_SIGNED_ASSERTION : 
					CheckingMode.REQUIRE_SIGNED_RESPONSE_OR_ASSERTION;
		StrictSamlTrustChecker trustChecker = new StrictSamlTrustChecker(mode);
		for (String idpKey: idpKeys)
		{
			String idpId = getValue(idpKey+IDP_ID);
			Set<String> idpCertNames = getCertificateNames(idpKey);
			
			for (String idpCertName: idpCertNames)
			{
				X509Certificate idpCert;
				try
				{
					idpCert = pkiManagement.getCertificate(idpCertName);
				} catch (EngineException e)
				{
					throw new ConfigurationException("Remote SAML IdP certificate can not be loaded " 
							+ idpCertName, e);
				}
				trustChecker.addTrustedIssuer(idpId, SAMLConstants.NFORMAT_ENTITY, 
						idpCert.getPublicKey());
			}
		}
		return trustChecker;
	}
	
	public Set<String> getCertificateNames(String idpKey)
	{
		return getCertificateNames(idpKey, IDP_CERTIFICATE, IDP_CERTIFICATES);
	}
	
	public boolean isSignRequest(String idpKey)
	{
		return isSet(idpKey + IDP_SIGN_REQUEST) ? 
				getBooleanValue(idpKey + IDP_SIGN_REQUEST) : 
				getBooleanValue(DEF_SIGN_REQUEST);
	}
	
	public String getRequestedNameFormat(String idpKey)
	{
		return isSet(idpKey + IDP_REQUESTED_NAME_FORMAT) ? 
				getValue(idpKey + IDP_REQUESTED_NAME_FORMAT) : 
				getValue(DEF_REQUESTED_NAME_FORMAT);
	}
	
	/**
	 * As trusted IdP entries can be partially created from default values and/or generated from remote metadata
	 * it may happen that some of the entries are in the end incomplete. This method verifies this.
	 * 
	 * @param key
	 * @return
	 */
	public boolean isIdPDefinitionComplete(String key)
	{
		String entityId;
		if (!isSet(key + IDP_ID))
		{
			log.warn("No entityId for " + key + " ignoring IdP");
			return false;
		} else
			entityId = getValue(key + IDP_ID);
		if (!isSet(key + IDP_ADDRESS))
		{
			log.warn("No address for " + entityId + " ignoring IdP");
			return false;
		}
		if (getCertificateNames(key).size() == 0)
		{
			log.warn("No certificate for " + entityId + " ignoring IdP");
			return false;
		}		
		if (!isSet(key + CommonWebAuthnProperties.TRANSLATION_PROFILE))
		{
			log.warn("No translation profile for " + entityId + " ignoring IdP");
			return false;
		}		
		return true;
	}
	
	public String getIdPConfigKey(NameIDType requester)
	{
		Set<String> allowedKeys = getStructuredListKeys(IDP_PREFIX);
		for (String allowedKey: allowedKeys)
		{
			String name = getValue(allowedKey + IDP_ID);
			if (name == null)
				continue;
			if (!name.equals(requester.getStringValue()))
				continue;
			return allowedKey;
		}
		return null;
	}
	
	/**
	 * @return original properties, i.e. those which were used to configure the authenticator.
	 * The {@link #getProperties()} returns runtime properties which can include additional entries
	 * added from remote metadata. Always a copy is returned.
	 */
	public Properties getSourceProperties()
	{
		Properties configProps = new Properties();
		configProps.putAll(sourceProperties);
		return configProps;
	}

	public String getLocalizedName(String idpKey, Locale locale)
	{
		String ret = getLocalizedValue(idpKey + IDP_NAME, locale);
		return ret != null ? ret : getName(idpKey);
	}
	
	/**
	 * @param idpKey
	 * @return idp name if set, otherwise its id which is mandatory.
	 */
	public String getName(String idpKey)
	{
		String key = idpKey + IDP_NAME;
		return isSet(key) ? getValue(key) : getValue(idpKey + IDP_ID);
	}
	
	@Override
	public SAMLSPProperties clone()
	{
		return new SAMLSPProperties(this);
	}
}

/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.ecp.SAMLECPProperties;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.webui.common.idpselector.IdpSelectorComponent.ScaleMode;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.trust.StrictSamlTrustChecker;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;

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
	public static final String ICON_SCALE = "iconScale";
	public static final String METADATA_PATH = "metadataPath";
	
	public static final String DEF_SIGN_REQUEST = "defaultSignRequest";
	public static final String DEF_REQUESTED_NAME_FORMAT = "defaultRequestedNameFormat";

	public static final String IDPMETA_PREFIX = "metadataSource.";
	public static final String IDPMETA_URL = "url";
	public static final String IDPMETA_HTTPS_TRUSTSTORE = "httpsTruststore";
	public static final String IDPMETA_REFRESH = "refreshInterval";
	public static final String IDPMETA_SIGNATURE = "signaturVerification";
	public static final String IDPMETA_ISSUER_CERT = "signatureVerificationCertificate";
	public static final String IDPMETA_TRANSLATION_PROFILE = "perMetadataTranslationProfile";
	public static final String IDPMETA_REGISTRATION_FORM = "perMetadataRegistrationForm";
	
	public static final String IDP_PREFIX = "remoteIdp.";
	public static final String IDP_NAME = "name";
	public static final String IDP_LOGO = "logoURI";
	public static final String IDP_ID = "samlId";
	public static final String IDP_ADDRESS = "address";
	public static final String IDP_BINDING = "binding";
	public static final String IDP_REDIRECT_LOGOUT_URL = "redirectLogoutEndpoint";
	public static final String IDP_POST_LOGOUT_URL = "postLogoutEndpoint";
	public static final String IDP_SOAP_LOGOUT_URL = "soapLogoutEndpoint";
	public static final String IDP_CERTIFICATE = "certificate";
	public static final String IDP_CERTIFICATES = "certificates.";
	public static final String IDP_SIGN_REQUEST = "signRequest";
	public static final String IDP_REQUESTED_NAME_FORMAT = "requestedNameFormat";
	public static final String IDP_GROUP_MEMBERSHIP_ATTRIBUTE = "groupMembershipAttribute";
	public static final String IDP_TRANSLATION_PROFILE = "translationProfile";
	public static final String IDP_REGISTRATION_FORM = "registrationFormForUnknown";
	
	static
	{
		DocumentationCategory common = new DocumentationCategory(
				"Common settings", "01");
		DocumentationCategory remoteMeta = new DocumentationCategory(
				"Configuration from trusted SAML metadata", "02");
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
		META.put(IDP_REDIRECT_LOGOUT_URL, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
				"Address of the IdP Single Logout Endpoint supporting HTTP Redirect binding."));
		META.put(IDP_POST_LOGOUT_URL, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
				"Address of the IdP Single Logout Endpoint supporting HTTP POST binding."));
		META.put(IDP_SOAP_LOGOUT_URL, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(idp).setDescription(
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
		META.put(IDP_TRANSLATION_PROFILE, new PropertyMD().setCategory(idp).setStructuredListEntry(IDP_PREFIX).
				setDescription("Name of a translation" +
				" profile, which will be used to map remotely obtained attributes and identity" +
				" to the local counterparts. The profile should at least map the remote identity."));
		META.put(IDP_REGISTRATION_FORM, new PropertyMD().setCategory(idp).setStructuredListEntry(IDP_PREFIX).setDescription(
				"Name of a registration form to be shown for a remotely authenticated principal who " +
				"has no local account. If unset such users will be denied."));
		
		META.put(REQUESTER_ID, new PropertyMD().setMandatory().setCategory(common).setDescription(
				"SAML entity ID (must be a URI) of the lcoal SAML requester (or service provider)."));
		META.put(CREDENTIAL, new PropertyMD().setCategory(common).setDescription(
				"Local credential, used to sign requests. If signing is disabled it is not used."));
		META.put(METADATA_PATH, new PropertyMD().setCategory(SamlProperties.samlMetaCat).setDescription(
				"Last element of the URL, under which the SAML metadata should be published for this SAML authenticator." +
				"Used only if metadata publication is enabled. See the SAML Metadata section for more details."));
		META.put(ACCEPTED_NAME_FORMATS, new PropertyMD().setList(false).setCategory(common).setDescription(
				"If defined then specifies what SAML name formatd are accepted from IdP. " +
				"Useful when the property " + IDP_REQUESTED_NAME_FORMAT + " is undefined for at least one IdP. "));

		META.put(DEF_SIGN_REQUEST, new PropertyMD("false").setCategory(common).setDescription(
				"Default setting of request signing. Used for those IdPs, for which the setting is not set explicitly."));
		META.put(DEF_REQUESTED_NAME_FORMAT, new PropertyMD().setCategory(common).setDescription(
				"Default setting of requested identity format. Used for those IdPs, for which the setting is not set explicitly."));

		
		META.put(DISPLAY_NAME, new PropertyMD("SAML authentication").setCategory(webRetrieval).setDescription(
				"Name of the SAML authentication GUI component"));
		META.put(PROVIDERS_IN_ROW, new PropertyMD("2").setPositive().setCategory(webRetrieval).setDescription(
				"How many IdPs should be displayed in a single row on the IdP selection screen. Relevant only if you define multiple providers."));
		META.put(ICON_SCALE, new PropertyMD(ScaleMode.height100).setDescription("Controls whether and how "
				+ "the icons of providers should be scalled."));
		
		META.put(IDPMETA_PREFIX, new PropertyMD().setCategory(remoteMeta).setStructuredList(false).setDescription(
				"Under this prefix you can configure the remote trusted SAML IdPs however not providing all their details but only their metadata."));
		META.put(IDPMETA_REFRESH, new PropertyMD("3600").setCategory(remoteMeta).setDescription(
				"How often the metadata should be reloaded."));
		META.put(IDPMETA_URL, new PropertyMD().setCategory(remoteMeta).setMandatory().setStructuredListEntry(IDPMETA_PREFIX).setDescription(
				"URL with the metadata location. Can be local or HTTP(s) URL. "
				+ "In case of HTTPS the server's certificate will be checked against the main Unity server's truststore"
				+ " only if ."));
		META.put(IDPMETA_HTTPS_TRUSTSTORE, new PropertyMD().setCategory(remoteMeta).setStructuredListEntry(IDPMETA_PREFIX).setDescription(
				"If set then the given truststore will be used for HTTPS connection validation during metadata fetching. Otherwise the default Java trustststore will beused."));
		META.put(IDPMETA_TRANSLATION_PROFILE, new PropertyMD().setCategory(remoteMeta).setStructuredListEntry(IDPMETA_PREFIX).setDescription(
				"Deafult translation profile for all the IdPs from the metadata. Can be overwritten by individual IdP configuration entries."));
		META.put(IDPMETA_REGISTRATION_FORM, new PropertyMD().setCategory(remoteMeta).setStructuredListEntry(IDPMETA_PREFIX).setDescription(
				"Deafult registration form for all the IdPs from the metadata. Can be overwritten by individual IdP configuraiton entries."));
		META.put(IDPMETA_SIGNATURE, new PropertyMD(MetadataSignatureValidation.ignore).setCategory(remoteMeta).setStructuredListEntry(IDPMETA_PREFIX).setDescription(
				"Controls whether metadata signatures should be checked. If checking is turned on then the validation certificate must be set."));
		META.put(IDPMETA_ISSUER_CERT, new PropertyMD().setCategory(remoteMeta).setStructuredListEntry(IDPMETA_PREFIX).setDescription(
				"Name of certificate to check metadata signature. Used only if signatures checking is turned on."));
		
		META.put(SAMLECPProperties.JWT_P, new PropertyMD().setCanHaveSubkeys().setHidden());
		
		META.putAll(SamlProperties.defaults);
	}
	
	private PKIManagement pkiManagement;
	private Properties sourceProperties;

	public SAMLSPProperties(Properties properties, PKIManagement pkiMan) throws ConfigurationException
	{
		this(properties, META, pkiMan);
	}
	
	protected SAMLSPProperties(Properties properties, Map<String, PropertyMD> meta, 
			PKIManagement pkiMan) throws ConfigurationException
	{
		super(P, properties, meta, log);
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
			MetadataSignatureValidation validation = getEnumValue(metaKey + IDPMETA_SIGNATURE, 
					MetadataSignatureValidation.class);
			if (validation == MetadataSignatureValidation.require)
			{
				String certName = getValue(metaKey + IDPMETA_ISSUER_CERT);
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
		StrictSamlTrustChecker trustChecker = new StrictSamlTrustChecker();
		for (String idpKey: idpKeys)
		{
			String idpId = getValue(idpKey+IDP_ID);
			Set<String> idpCertNames = new HashSet<String>();
			if (isSet(idpKey+IDP_CERTIFICATE))
				idpCertNames.add(getValue(idpKey+IDP_CERTIFICATE));
			idpCertNames.addAll(getListOfValues(idpKey+IDP_CERTIFICATES));
			
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
	
	public boolean isSignRequest(String idpKey)
	{
		return isSet(idpKey + IDP_SIGN_REQUEST) ? 
				getBooleanValue(idpKey + IDP_SIGN_REQUEST) : 
				getBooleanValue(idpKey + DEF_SIGN_REQUEST);
	}
	
	public String getRequestedNameFormat(String idpKey)
	{
		return isSet(idpKey + IDP_REQUESTED_NAME_FORMAT) ? 
				getValue(idpKey + IDP_REQUESTED_NAME_FORMAT) : 
				getValue(idpKey + DEF_REQUESTED_NAME_FORMAT);
	}
	
	/**
	 * As trusted IdP entries can be partially created from default values and/or generated from remote metadata
	 * it may happen that some of the entries are in the end incomplete. This method verifies this.
	 * 
	 * @param key
	 * @return
	 */
	public boolean isIdPDefinitioncomplete(String key)
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
		if (!isSet(key + IDP_CERTIFICATE) && getListOfValues(key+IDP_CERTIFICATES).size() == 0)
		{
			log.warn("No certificate for " + entityId + " ignoring IdP");
			return false;
		}		
		if (!isSet(key + IDP_TRANSLATION_PROFILE))
		{
			log.warn("No translation profile for " + entityId + " ignoring IdP");
			return false;
		}		
		return true;
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
	
	public String getName(String idpKey)
	{
		String key = idpKey + IDP_NAME;
		return isSet(key) ? getValue(key) : getValue(idpKey + IDP_ID);
	}
	
	public SAMLSPProperties clone()
	{
		return new SAMLSPProperties(getProperties(), pkiManagement);
	}
	
	
}

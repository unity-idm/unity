/*
 * Copyright (c) 2007-2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.saml.idp;


import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.saml.SamlProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Properties-based configuration of SAML IdP endpoint.
 *  
 * @author K. Benedyczak
 */
public class SamlIdpProperties extends SamlProperties
{
	private static final Logger log = Log.getLogger(SamlIdpProperties.LOG_PFX, SamlIdpProperties.class);
	
	public static final String LOG_PFX = Log.U_SERVER_CFG;
	
	@DocumentationReferencePrefix
	public static final String P = "unity.saml.";
	
	public static final String AUTHENTICATION_TIMEOUT = "authenticationTimeout";

	public static final String SIGN_RESPONSE = "signResponses";
	public static final String SIGN_ASSERTION = "signAssertion";
	public static final String CREDENTIAL = "credential";
	public static final String ADDITIONALLY_ADVERTISED_CREDENTIAL = "additionallyAdvertisedCredential";
	public static final String TRUSTSTORE = "truststore";
	public static final String DEF_ATTR_ASSERTION_VALIDITY = "validityPeriod";
	public static final String SAML_REQUEST_VALIDITY = "requestValidityPeriod";
	public static final String ISSUER_URI = "issuerURI";
	public static final String RETURN_SINGLE_ASSERTION = "returnSingleAssertion";
	public static final String SP_ACCEPT_POLICY = "spAcceptPolicy";
	
	public static final String SPMETA_PREFIX = "acceptedSPMetadataSource.";
	
	public static final String ALLOWED_SP_PREFIX = "acceptedSP.";
	public static final String ALLOWED_SP_DN = "dn";
	public static final String ALLOWED_SP_ENTITY = "entity";
	public static final String ALLOWED_SP_RETURN_URL = "returnURL";
	public static final String ALLOWED_SP_RETURN_URLS = "returnURLs.";
	public static final String ALLOWED_SP_ENCRYPT = "encryptAssertion";
	public static final String ALLOWED_SP_NAME = "name";
	public static final String ALLOWED_SP_LOGO = "logoURI";
	public static final String ALLOWED_SP_CERTIFICATE = "certificate";
	public static final String ALLOWED_SP_CERTIFICATES = "certificates.";
	
	public static final String GROUP_PFX = "groupMapping.";
	public static final String GROUP_TARGET = "serviceProvider";
	public static final String GROUP = "mappingGroup";
	public static final String DEFAULT_GROUP = "defaultGroup";
	public static final String USER_EDIT_CONSENT = "userCanEditConsent";
	
	public static final String DEFAULT_TRANSLATION_PROFILE = "sys:saml";
	public static final int DEFAULT_SAML_REQUEST_VALIDITY = 600;
	public static final int DEFAULT_AUTHENTICATION_TIMEOUT = 600;
	public static final int DEFAULT_ATTR_ASSERTION_VALIDITY = 14400;
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults=new HashMap<String, PropertyMD>();

	
	
	private Properties sourceProperties;
	
	static
	{
		DocumentationCategory sp = new DocumentationCategory("Manual settings of allowed Sps", "03");
		
		DocumentationCategory samlCat = new DocumentationCategory("SAML subsystem settings", "5");
		

		defaults.put(GROUP_PFX, new PropertyMD().setStructuredList(false).setCategory(samlCat).
				setDescription("Prefix used to mark requester to group mappings."));
		defaults.put(GROUP_TARGET, new PropertyMD().setStructuredListEntry(GROUP_PFX).setMandatory().setCategory(samlCat).
				setDescription("Requester for which this entry applies."));
		defaults.put(GROUP, new PropertyMD().setStructuredListEntry(GROUP_PFX).setMandatory().setCategory(samlCat).
				setDescription("Group for the requester."));
		defaults.put(DEFAULT_GROUP, new PropertyMD().setMandatory().setCategory(samlCat).
				setDescription("Default group to be used for all requesers without an explicite mapping."));
		defaults.put(IDENTITY_MAPPING_PFX, new PropertyMD().setStructuredList(false).setCategory(samlCat).
				setDescription("Prefix used to store mappings of SAML identity types to Unity identity types. Those mappings can override and/or complement the default mapping."));
		defaults.put(IDENTITY_LOCAL, new PropertyMD().setStructuredListEntry(IDENTITY_MAPPING_PFX).setMandatory().setCategory(samlCat).
				setDescription("Unity identity to which the SAML identity is mapped. If it is set to an empty value, then the mapping is disabled, "
						+ "what is useful for turning off the default mappings."));
		defaults.put(IDENTITY_SAML, new PropertyMD().setStructuredListEntry(IDENTITY_MAPPING_PFX).setMandatory().setCategory(samlCat).
				setDescription("SAML identity to be mapped"));	
		defaults.put(SAML_REQUEST_VALIDITY, new PropertyMD(String.valueOf(DEFAULT_SAML_REQUEST_VALIDITY)).setPositive().setCategory(samlCat).
				setDescription("Defines maximum validity period (in seconds) of a SAML request. Requests older than this value are denied. It also controls the validity of an authentication assertion."));
		defaults.put(AUTHENTICATION_TIMEOUT, new PropertyMD(String.valueOf(DEFAULT_AUTHENTICATION_TIMEOUT)).setPositive().setCategory(samlCat).
				setDescription("Defines maximum time (in seconds) after which the authentication in progress is invalidated. This feature is used to clean up authentications started by users but not finished."));
		defaults.put(SIGN_RESPONSE, new PropertyMD(SAMLIdPConfiguration.ResponseSigningPolicy.asRequest).setCategory(samlCat).
				setDescription("Defines when SAML responses should be signed. "
						+ "Note that it is not related to signing SAML assertions which "
						+ "are included in response. "
						+ "'asRequest' setting will result in signing only those responses "
						+ "for which the corresponding request was signed."));
		defaults.put(SIGN_ASSERTION, new PropertyMD(SAMLIdPConfiguration.AssertionSigningPolicy.always).setCategory(samlCat).
				setDescription("Defines when SAML assertions (contained in SAML response) "
						+ "should be signed: either always or if signing may be skipped "
						+ "if wrapping request will be anyway signed"));
		defaults.put(DEF_ATTR_ASSERTION_VALIDITY, new PropertyMD(String.valueOf(DEFAULT_ATTR_ASSERTION_VALIDITY)).setPositive().setCategory(samlCat).
				setDescription("Controls the maximum validity period of an attribute assertion returned to client (in seconds). It is inserted whenever query is compliant with 'SAML V2.0 Deployment Profiles for X.509 Subjects', what usually is the case."));
		defaults.put(ISSUER_URI, new PropertyMD().setCategory(samlCat).setMandatory().
				setDescription("This property controls the server's URI which is inserted into SAML responses (the Issuer field). It should be a unique URI which identifies the server. The best approach is to use the server's URL."));
		defaults.put(RETURN_SINGLE_ASSERTION, new PropertyMD("true").setCategory(samlCat).
				setDescription("If true then a single SAML assertion is returned what provides a better interoperability with 3rd party solutions. If false then attributes are returned in a separate assertion, what is required by certain consumers as UNICORE."));
		
		defaults.put(SP_ACCEPT_POLICY, new PropertyMD(SAMLIdPConfiguration.RequestAcceptancePolicy.validRequester).setCategory(samlCat).
				setDescription("Controls which requests are authorized. +all+ accepts all, +validSigner+ " +
				"accepts all requests which are signed with a trusted certificate, " +
				"+validRequester+ accepts all requests (even unsigned) which are issued by a known " +
				"entity with a fixed response address, " +
				"finally +strict+ allows only requests signed by one of the enumerated issuers. " +
				"Important: this setting fully works for web endpoints only, for SOAP endpoints only "
				+ "+validRequester+ and +all+ acceptance policies can be used. "
				+ "All other will be treated as +all+. This is because you can control the "
				+ "access with authentication and authorization of the client, additional SAML "
				+ "level configuraiton is not neccessary."));
		defaults.put(ALLOWED_SP_PREFIX, new PropertyMD().setStructuredList(false).setCategory(samlCat).
				setDescription("List of entries defining allowed Service Providers (clients). Used " +
				"only for +validRequester+ and +strict+ acceptance policies."));
				
		defaults.put(ALLOWED_SP_DN, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).setCanHaveSubkeys().setCategory(sp).
				setDescription("Rarely useful: for SPs which use DN SAML identifiers as UNICORE portal. " +
				"Typically " + ALLOWED_SP_ENTITY + " is used instead. " +
				"Value must be the X.500 DN of the trusted SP."));
		defaults.put(ALLOWED_SP_ENTITY, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).setCategory(sp).
				setDescription("Entity ID (typically an URI) of a trusted SAML requester (SP)."));	
		defaults.put(ALLOWED_SP_ENCRYPT, new PropertyMD("false").setStructuredListEntry(ALLOWED_SP_PREFIX).setCategory(sp).
				setDescription("Whether to encrypt assertions sent to this peer. "
						+ "Usually not needed as Unity uses TLS. If turned on, then certificate of the peer must be also set."));
		defaults.put(ALLOWED_SP_RETURN_URL, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).setCategory(sp).
				setDescription("Response consumer address of the SP. Mandatory when acceptance " +
				"policy is +validRequester+, optional otherwise as SAML requesters may send this address" +
				"with a request. In case when more then one response consumer address is allowed, then this one denotes the default."));
		defaults.put(ALLOWED_SP_RETURN_URLS, new PropertyMD().setList(false).setStructuredListEntry(ALLOWED_SP_PREFIX).setCategory(sp).
				setDescription("List of response consumer addresses of the SP. Used only when acceptance " +
				"policy is +validRequester+. The format for each entry is +[N]URL+ where N is the index of the endpoint " +
				"(as used in SAML metadata spec) and URL is the endpoints address. Note that it makes perfect sense to "
				+ "specify the default endpoint also in this list as this allows to assign it an index."));
		defaults.put(SOAP_LOGOUT_URL, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).
				setCategory(sp).setDescription("SOAP Single Logout Endpoint of the SP."));
		defaults.put(REDIRECT_LOGOUT_URL, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).
				setCategory(sp).setDescription("HTTP Redirect Single Logout Endpoint of the SP."));
		defaults.put(POST_LOGOUT_URL, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).
				setCategory(sp).setDescription("HTTP POST Single Logout Endpoint of the SP."));
		defaults.put(REDIRECT_LOGOUT_RET_URL, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).
				setCategory(sp).setDescription("HTTP Redirect Single Logout response endpoint of the SP. "
						+ "If undefined the base endpoint address is assumed."));
		defaults.put(POST_LOGOUT_RET_URL, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).
				setCategory(sp).setDescription("HTTP POST Single Logout response endpoint of the SP. "
						+ "If undefined the base endpoint address is assumed."));

		defaults.put(ALLOWED_SP_NAME, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).
				setCategory(sp).setCanHaveSubkeys().setDescription(
				"Displayed name of the Sp. If not defined then the name is created " +
				"from the Sp address (what is rather not user friendly). The property can have subkeys being "
				+ "locale names; then the localized value is used if it is matching the selected locale of the UI."));	
		defaults.put(ALLOWED_SP_LOGO, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).
				setCategory(sp).setCanHaveSubkeys().setDescription(
				"Displayed logo of the SP. If not defined then only the name is used. "
				+ "The value can be a file:, http(s): or data: URI. The last option allows for embedding the logo in the configuration. "
				+ "The property can have subkeys being "
				+ "locale names; then the localized value is used if it is matching the selected locale of the UI."));
		defaults.put(ALLOWED_SP_CERTIFICATE, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).setCategory(sp).
				setDescription("Certificate of the SP. Used only when acceptance policy is +strict+ "
						+ "and when assertion encryption is turned on for this SP. Also"
						+ "used for single logout, initiated by the peer."));
		defaults.put(ALLOWED_SP_CERTIFICATES, new PropertyMD().setStructuredListEntry(ALLOWED_SP_PREFIX).
				setCategory(sp).setList(false).setDescription(
				"Using this property additional trusted certificates of an SP can be added (when SP uses more then one). See " 
						+ ALLOWED_SP_CERTIFICATE + " for details. Those properties can be used together or alternatively."));

		defaults.put(TRUSTSTORE, new PropertyMD().setCategory(samlCat).
				setDescription("Truststore name to setup SAML trust settings. The truststore "
						+ "is used to verify request signature issuer, " +
						"if the Service Provider accept policy requires so."));
		defaults.put(CREDENTIAL, new PropertyMD().setMandatory().setCategory(samlCat).
				setDescription("SAML IdP credential name, which is used to sign responses."));	
		defaults.put(ADDITIONALLY_ADVERTISED_CREDENTIAL, new PropertyMD().setCategory(samlCat).
				setDescription("SAML IdP additionally advertised credential name."));	
		defaults.putAll(SamlProperties.getDefaults(SPMETA_PREFIX, 
				"Under this prefix you can configure the remote trusted SAML Sps however not "
				+ "providing all their details but only their metadata."));
		
		defaults.putAll(CommonIdPProperties.getDefaultsWithCategory(samlCat, 
				"Name of an output translation profile which can be used to dynamically modify the "
				+ "data being returned on this endpoint.", DEFAULT_TRANSLATION_PROFILE));
		
		defaults.put(USER_EDIT_CONSENT, new PropertyMD("true").setCategory(samlCat)
				.setDescription("Controls whether user is allowed to remove released attributes on the "
						+ "consent screen. Note that attributes marked as mandatory in output profile "
						+ "can not be removed regardless of this option."));
	}
	
	public SamlIdpProperties(Properties src) throws ConfigurationException
	{
		super(P, cleanupLegacyProperties(src), defaults, log);
		sourceProperties = new Properties();
		sourceProperties.putAll(properties);
	}
	
	private static Properties cleanupLegacyProperties(Properties src)
	{
		if (src.containsKey("unity.saml.groupSelection"))
		{
			src.remove("unity.saml.groupSelection");
			log.warn("The legacy property 'unity.saml.groupSelection' was removed from "
					+ "endpoint's configuration. If needed use output "
					+ "translation profile to define group membership encoding attribute.");
		}
		if (src.containsKey("unity.saml.groupAttribute"))
		{
			src.remove("unity.saml.groupAttribute");
			log.warn("The legacy property 'unity.saml.groupAttribute' was removed from "
					+ "endpoint's configuration. If needed use output "
					+ "translation profile to define group membership encoding attribute.");
		}
		return src;
	}

	@Override
	public SamlProperties clone()
	{
		try
		{
			return new SamlIdpProperties(getProperties());
		} catch (Exception e)
		{
			throw new ConfigurationException("Can not clone saml properties", e);
		} 
		
	}

	public Properties getSourceProperties()
	{
		Properties configProps = new Properties();
		configProps.putAll(sourceProperties);
		return configProps;
	}
}

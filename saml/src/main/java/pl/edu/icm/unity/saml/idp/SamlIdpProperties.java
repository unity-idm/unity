/*
 * Copyright (c) 2007-2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.saml.idp;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.validator.UnityAuthnRequestValidator;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.CommonIdPProperties;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.trust.AcceptingSamlTrustChecker;
import eu.unicore.samly2.trust.EnumeratedTrustChecker;
import eu.unicore.samly2.trust.PKISamlTrustChecker;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.trust.StrictSamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;

/**
 * Properties-based configuration of SAML IdP endpoint.
 *  
 * @author K. Benedyczak
 */
public class SamlIdpProperties extends SamlProperties
{
	private static final Logger log = Log.getLogger(SamlIdpProperties.LOG_PFX, SamlIdpProperties.class);
	public enum RequestAcceptancePolicy {all, validSigner, validRequester, strict};
	public enum ResponseSigningPolicy {always, never, asRequest};
	public enum AssertionSigningPolicy {always, ifResponseUnsigned};
	
	public static final String LOG_PFX = Log.U_SERVER_CFG;
	
	@DocumentationReferencePrefix
	public static final String P = "unity.saml.";
	
	public static final String AUTHENTICATION_TIMEOUT = "authenticationTimeout";

	public static final String SIGN_RESPONSE = "signResponses";
	public static final String SIGN_ASSERTION = "signAssertion";
	public static final String CREDENTIAL = "credential";
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
		defaults.put(SAML_REQUEST_VALIDITY, new PropertyMD("600").setPositive().setCategory(samlCat).
				setDescription("Defines maximum validity period (in seconds) of a SAML request. Requests older than this value are denied. It also controls the validity of an authentication assertion."));
		defaults.put(AUTHENTICATION_TIMEOUT, new PropertyMD("600").setPositive().setCategory(samlCat).
				setDescription("Defines maximum time (in seconds) after which the authentication in progress is invalidated. This feature is used to clean up authentications started by users but not finished."));
		defaults.put(SIGN_RESPONSE, new PropertyMD(ResponseSigningPolicy.asRequest).setCategory(samlCat).
				setDescription("Defines when SAML responses should be signed. "
						+ "Note that it is not related to signing SAML assertions which "
						+ "are included in response. "
						+ "'asRequest' setting will result in signing only those responses "
						+ "for which the corresponding request was signed."));
		defaults.put(SIGN_ASSERTION, new PropertyMD(AssertionSigningPolicy.always).setCategory(samlCat).
				setDescription("Defines when SAML assertions (contained in SAML response) "
						+ "should be signed: either always or if signing may be skipped "
						+ "if wrapping request will be anyway signed"));
		defaults.put(DEF_ATTR_ASSERTION_VALIDITY, new PropertyMD("14400").setPositive().setCategory(samlCat).
				setDescription("Controls the maximum validity period of an attribute assertion returned to client (in seconds). It is inserted whenever query is compliant with 'SAML V2.0 Deployment Profiles for X.509 Subjects', what usually is the case."));
		defaults.put(ISSUER_URI, new PropertyMD().setCategory(samlCat).setMandatory().
				setDescription("This property controls the server's URI which is inserted into SAML responses (the Issuer field). It should be a unique URI which identifies the server. The best approach is to use the server's URL."));
		defaults.put(RETURN_SINGLE_ASSERTION, new PropertyMD("true").setCategory(samlCat).
				setDescription("If true then a single SAML assertion is returned what provides a better interoperability with 3rd party solutions. If false then attributes are returned in a separate assertion, what is required by certain consumers as UNICORE."));
		
		defaults.put(SP_ACCEPT_POLICY, new PropertyMD(RequestAcceptancePolicy.validRequester).setCategory(samlCat).
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
				"with a request."));
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

		defaults.putAll(SamlProperties.getDefaults(SPMETA_PREFIX, 
				"Under this prefix you can configure the remote trusted SAML Sps however not "
				+ "providing all their details but only their metadata."));
		
		defaults.putAll(CommonIdPProperties.getDefaultsWithCategory(samlCat, 
				"Name of an output translation profile which can be used to dynamically modify the "
				+ "data being returned on this endpoint. When not defined the default profile is used: "
				+ "attributes are not filtered, memberOf attribute is added with group membership"));
		
		defaults.put(USER_EDIT_CONSENT, new PropertyMD("true").setCategory(samlCat)
				.setDescription("Controls whether user is allowed to remove released attributes on the "
						+ "consent screen. Note that attributes marked as mandatory in output profile "
						+ "can not be removed regardless of this option."));
	}

	private boolean signRespNever;
	private boolean signRespAlways;
	private ReplayAttackChecker replayChecker;
	private SamlTrustChecker authnTrustChecker;
	private SamlTrustChecker soapTrustChecker;
	private SamlTrustChecker sloTrustChecker;
	private long requestValidity;
	private X509CertChainValidator trustedValidator;
	private GroupChooser groupChooser;
	private SamlAttributeMapper attributesMapper;
	private PKIManagement pkiManagement;
	private IdentityTypeMapper idTypeMapper;
	
	public SamlIdpProperties(Properties src, PKIManagement pkiManagement) throws ConfigurationException, IOException
	{
		super(P, cleanupLegacyProperties(src), defaults, log);
		sourceProperties = new Properties();
		sourceProperties.putAll(properties);
		this.pkiManagement = pkiManagement;
		checkIssuer();
		try
		{
			initPki();
		} catch (EngineException e)
		{
			throw new ConfigurationException("Can't init SAML PKI settings", e);
		}
		init();
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
	
	private void init()
	{
		ResponseSigningPolicy repPolicy = getEnumValue(SamlIdpProperties.SIGN_RESPONSE, ResponseSigningPolicy.class);
		signRespAlways = signRespNever = false;
		if (repPolicy == ResponseSigningPolicy.always)
			signRespAlways = true;
		else if (repPolicy == ResponseSigningPolicy.never)
			signRespNever = true;

		RequestAcceptancePolicy spPolicy = getEnumValue(SP_ACCEPT_POLICY, RequestAcceptancePolicy.class);
		
		if (spPolicy == RequestAcceptancePolicy.all)
		{
			authnTrustChecker = new AcceptingSamlTrustChecker();
			sloTrustChecker = new AcceptingSamlTrustChecker();
			log.debug("All SPs will be authorized to submit authentication requests");
		} else if (spPolicy == RequestAcceptancePolicy.validSigner)
		{
			authnTrustChecker = new PKISamlTrustChecker(trustedValidator);
			sloTrustChecker = new PKISamlTrustChecker(trustedValidator);
			log.debug("All SPs using a valid certificate will be authorized to submit authentication requests");
		} else if (spPolicy == RequestAcceptancePolicy.strict)
		{
			authnTrustChecker = createStrictTrustChecker();
			sloTrustChecker = authnTrustChecker;
		} else
		{
			EnumeratedTrustChecker authnTrustChecker = new EnumeratedTrustChecker();
			this.authnTrustChecker = authnTrustChecker;
			
			Set<String> allowedKeys = getStructuredListKeys(ALLOWED_SP_PREFIX);
			for (String allowedKey: allowedKeys)
			{
				String returnAddress = getValue(allowedKey + ALLOWED_SP_RETURN_URL);
				if (returnAddress == null)
					throw new ConfigurationException("Invalid specification of allowed Service " +
						"Provider " + allowedKey + ", return address is not set.");
				
				if (isSet(allowedKey + ALLOWED_SP_ENTITY) && isSet(allowedKey + ALLOWED_SP_DN))
					throw new ConfigurationException("The allowed SP entry " + allowedKey + 
							" has both the DN and SAML entity id defined. "
							+ "Please use only one, which is actually used by "
							+ "the SP to identify itself." );
				
				String name = getValue(allowedKey + ALLOWED_SP_ENTITY);
				if (name != null)
					authnTrustChecker.addTrustedIssuer(name, returnAddress);	
				else
				{
					name = getValue(allowedKey + ALLOWED_SP_DN);
					if (name == null)
						throw new ConfigurationException("Invalid specification of allowed Service " +
							"Provider " + allowedKey + ", neither Entity ID nor DN is set.");
					authnTrustChecker.addTrustedDNIssuer(name, returnAddress);
				}

				
				log.debug("SP authorized to submit authentication requests: " + name);
			}
			this.sloTrustChecker = createStrictTrustChecker();
		}
		
		Set<String> allowedKeys = getStructuredListKeys(ALLOWED_SP_PREFIX);
		for (String allowedKey: allowedKeys)
		{
			Set<String> spCertNames = getAllowedSpCerts(allowedKey);
			if (getBooleanValue(allowedKey + ALLOWED_SP_ENCRYPT) && spCertNames.isEmpty())
				throw new ConfigurationException(
						"Invalid specification of allowed Service "
								+ "Provider "
								+ allowedKey
								+ " must have the certificate defined to be able to encrypt assertions.");
		}
		
		if (trustedValidator != null)
			soapTrustChecker = new PKISamlTrustChecker(trustedValidator, true);
		else
			soapTrustChecker = new AcceptingSamlTrustChecker();
		replayChecker = new ReplayAttackChecker();
		requestValidity = getLongValue(SamlIdpProperties.SAML_REQUEST_VALIDITY)*1000;
		
		groupChooser = new GroupChooser(this);
		idTypeMapper = new IdentityTypeMapper(this);
		attributesMapper = new DefaultSamlAttributesMapper();
	}
	
	private void initPki() throws EngineException
	{
		RequestAcceptancePolicy policy = getEnumValue(SP_ACCEPT_POLICY, RequestAcceptancePolicy.class); 
		if (policy == RequestAcceptancePolicy.validSigner)
		{
			String validator = getValue(TRUSTSTORE);
			if (validator == null)
				throw new ConfigurationException("The SAML truststore must be defined for " +
						"the selected SP acceptance policy " + policy);
			if (!pkiManagement.getValidatorNames().contains(validator))
				throw new ConfigurationException("The SAML truststore " + validator + " is unknown");
			trustedValidator = pkiManagement.getValidator(validator);
		}
		String credential = getValue(CREDENTIAL);
		if (!pkiManagement.getCredentialNames().contains(credential))
			throw new ConfigurationException("The SAML credential " + credential + " is unknown");
	}
	
	private StrictSamlTrustChecker createStrictTrustChecker()
	{
		StrictSamlTrustChecker authnTrustChecker = new StrictSamlTrustChecker();
		Set<String> allowedKeys = getStructuredListKeys(ALLOWED_SP_PREFIX);
		for (String allowedKey: allowedKeys)
		{
			
			String type = SAMLConstants.NFORMAT_ENTITY;
			String name = getValue(allowedKey + ALLOWED_SP_ENTITY);
			if (name == null)
			{
				name = getValue(allowedKey + ALLOWED_SP_DN);
				type = SAMLConstants.NFORMAT_DN;
			}
			if (name == null)
				throw new ConfigurationException("Invalid specification of allowed Service " +
						"Provider " + allowedKey + ", neither Entity ID nor DN is set.");
			
			Set<String> spCertNames = getAllowedSpCerts(allowedKey);
			for (String spCertName: spCertNames)
			{
				X509Certificate spCert;
				try
				{
					spCert = pkiManagement.getCertificate(spCertName);
					authnTrustChecker.addTrustedIssuer(
							name, type, spCert.getPublicKey());
				} catch (EngineException e)
				{
					throw new ConfigurationException("Can't set certificate of trusted " +
							"issuer named " + spCertName, e);
				}
			}
			
			log.debug("SP authorized to submit authentication requests: " + name);
		}
		return authnTrustChecker;
	}
	
	private void checkIssuer()
	{
		String uri = getValue(ISSUER_URI);
		try
		{
			new URI(uri);
		} catch (URISyntaxException e)
		{
			throw new ConfigurationException("SAML endpoint's issuer is not a valid URI: " + 
					e.getMessage(), e);
		}
	}
	
	public long getRequestValidity()
	{
		return requestValidity;
	}
	
	public SamlTrustChecker getAuthnTrustChecker()
	{
		return authnTrustChecker;
	}
	
	public void configureKnownRequesters(UnityAuthnRequestValidator validator)
	{
		Set<String> allowedKeys = getStructuredListKeys(ALLOWED_SP_PREFIX);
		for (String allowedKey: allowedKeys)
		{
			String name = getValue(allowedKey + ALLOWED_SP_ENTITY);
			if (name == null)
				continue;
			if (!isSet(allowedKey + ALLOWED_SP_RETURN_URL))
				continue;
			validator.addKnownRequester(name);
		}
	}

	public String getReturnAddressForRequester(NameIDType requester)
	{
		String spKey = getSPConfigKey(requester);
		if (spKey == null)
			return null;

		return getValue(spKey + ALLOWED_SP_RETURN_URL);
	}
	
	/**
	 * @param requester
	 * @return certificate which should be used for encryption or null if no encryption should be performed
	 * for the given requester
	 */
	public X509Certificate getEncryptionCertificateForRequester(NameIDType requester)
	{
		X509Certificate rc = null;
		String spKey = getSPConfigKey(requester);
		if (spKey == null)
			return null;
		
		if (!getBooleanValue(spKey + ALLOWED_SP_ENCRYPT))
			return null;
						
		Set<String> spCertNames = getAllowedSpCerts(spKey);
		Set<X509Certificate> certs = new HashSet<X509Certificate>();
		for (String spCertName: spCertNames)
		{
			try
			{	 
				certs.add(pkiManagement.getCertificate(spCertName));

			} catch (EngineException e)
			{
				throw new InternalException("Can't retrieve SAML encryption certificate " + spCertName +
						" for requester with config key " + spKey, e);
			}	
		}

		for (X509Certificate c : certs)
		{
			if (rc == null)
			{
				rc = c;
			} else if (c.getNotAfter().compareTo(rc.getNotAfter()) > 0)
			{
				rc = c;
			}
		}
		return rc;
	}
	
	public Set<String> getAllowedSpCerts(String idpKey)
	{
		return getCertificateNames(idpKey, ALLOWED_SP_CERTIFICATE, ALLOWED_SP_CERTIFICATES);
	}
	
	public String getSPConfigKey(NameIDType requester)
	{
		Set<String> allowedKeys = getStructuredListKeys(ALLOWED_SP_PREFIX);
		boolean dnName = requester.getFormat() != null && requester.getFormat().equals(
				SAMLConstants.NFORMAT_DN); 
		for (String allowedKey: allowedKeys)
		{
			if (dnName)
			{
				String name = getValue(allowedKey + ALLOWED_SP_DN);
				if (name == null)
					continue;
				if (!X500NameUtils.equal(name, requester.getStringValue()))
					continue;
			} else
			{
				String name = getValue(allowedKey + ALLOWED_SP_ENTITY);
				if (name == null)
					continue;
				if (!name.equals(requester.getStringValue()))
					continue;
			}
			return allowedKey;
		}
		return null;
	}

	public SamlTrustChecker getSoapTrustChecker()
	{
		return soapTrustChecker;
	}

	public SamlTrustChecker getSloTrustChecker()
	{
		return sloTrustChecker;
	}

	public X509Credential getSamlIssuerCredential()
	{
		try
		{
			return pkiManagement.getCredential(getValue(CREDENTIAL));
		} catch (EngineException e)
		{
			throw new InternalException("Can't retrieve SAML credential", e);
		}
	}
	
	public ReplayAttackChecker getReplayChecker()
	{
		return replayChecker;
	}

	public boolean isSignRespNever()
	{
		return signRespNever;
	}

	public boolean isSignRespAlways()
	{
		return signRespAlways;
	}

	public Properties getProperties()
	{
		return properties;
	}

	public GroupChooser getGroupChooser()
	{
		return groupChooser;
	}

	public IdentityTypeMapper getIdTypeMapper()
	{
		return idTypeMapper;
	}

	public SamlAttributeMapper getAttributesMapper()
	{
		return attributesMapper;
	}

	@Override
	public SamlProperties clone()
	{
		try
		{
			return new SamlIdpProperties(getProperties(), pkiManagement);
		} catch (IOException e)
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

/*
 * Copyright (c) 2007-2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.samlidp;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.trust.AcceptingSamlTrustChecker;
import eu.unicore.samly2.trust.PKISamlTrustChecker;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.trust.StrictSamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.canl.LoggingStoreUpdateListener;
import eu.unicore.security.canl.TrustedIssuersProperties;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;

/**
 * Properties-based configuration of SAML endpoints. 
 * Maybe this will be rewritten to use JSON. But this is not strongly required.
 * @author K. Benedyczak
 */
public class SamlProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(SamlProperties.LOG_PFX, SamlProperties.class);
	public enum RequestAcceptancePolicy {all, validSigner, strict};
	public enum ResponseSigningPolicy {always, never, asRequest};
	
	public static final String LOG_PFX = Log.U_SERVER_CFG;
	
	@DocumentationReferencePrefix
	public static final String P = "unity.saml.";
	
	public static final String AUTHENTICATION_TIMEOUT = "authenticationTimeout";

	public static final String SIGN_RESPONSE = "signResponses";
	public static final String DEF_ATTR_ASSERTION_VALIDITY = "validityPeriod";
	public static final String SAML_REQUEST_VALIDITY = "requestValidityPeriod";
	public static final String ISSUER_URI = "issuerURI";
	//public static final String AUTHN_ENDPOINT_URI = "authEndpointURI";
	//public static final String ASSERTION_ENDPOINT_URI = "assertionEndpointURI";
	//public static final String NAMEMAP_ENDPOINT_URI = "nameMapEndpointURI";
	public static final String SP_ACCEPT_POLICY = "spAcceptPolicy";
	public static final String ALLOWED_URI_SP = "acceptedUriSP.";
	public static final String ALLOWED_DN_SP = "acceptedDNSP.";
	
	//attribute filter properties
	/*
	public static final String ATTRIBUTE_FILTER_FILE = "saml.attributeFiltersConfig";
	public static final String ATTRIBUTE_FILTER_EXPOSED_ATTRIBUTES = "exposedAttribute";
	public static final String ATTRIBUTE_FILTER_EXCLUDED_ATTRIBUTES = "excludedAttribute";
	public static final String ATTRIBUTE_FILTER_ALLOWED_SCOPE_INFIX=".scope.";
	public static final String ATTRIBUTE_FILTER_IGNORED_SCOPE_INFIX=".scope.not.";
	*/
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults=new HashMap<String, PropertyMD>();
	
	static
	{
		DocumentationCategory samlCat = new DocumentationCategory("SAML subsystem settings", "5");
		
//		defaults.put(ATTRIBUTE_FILTER_FILE, new PropertyMD("conf/attributeFilters.properties").setPath().setCategory(samlCat).
//				setDescription("Specifies what file is used to provide filters defining which attributes are exposed by the SAML attribute query interface."));
		
		defaults.put(SAML_REQUEST_VALIDITY, new PropertyMD("600").setPositive().setCategory(samlCat).
				setDescription("Defines maximum validity period (in seconds) of a SAML request. Requests older than this value are denied. It also controls the validity of an authentication assertion."));
		defaults.put(AUTHENTICATION_TIMEOUT, new PropertyMD("600").setPositive().setCategory(samlCat).
				setDescription("Defines maximum time (in seconds) after which the authentication in progress is invalidated. This feature is used to clean up authentications started by users but not finished."));
		defaults.put(SIGN_RESPONSE, new PropertyMD(ResponseSigningPolicy.asRequest).setCategory(samlCat).
				setDescription("Defines when SAML responses should be signed. Note that it is not related to signing SAML assertions which are included in response. 'asRequest' setting will result in signing only those responses for which the corresponding request was signed."));
		defaults.put(DEF_ATTR_ASSERTION_VALIDITY, new PropertyMD("14400").setPositive().setCategory(samlCat).
				setDescription("Controls the maximum validity period of an attribute assertion returned to client (in seconds). It is inserted whenever query is compliant with 'SAML V2.0 Deployment Profiles for X.509 Subjects', what usually is the case."));
		defaults.put(ISSUER_URI, new PropertyMD().setCategory(samlCat).setMandatory().
				setDescription("This property controls the server's URI which is inserted into SAML responses (the Issuer field). It should be a unique URI which identifies the server. The best approach is to use the server's URL . If absent the server will try to autogenerate one."));
/*
		defaults.put(AUTHN_ENDPOINT_URI, new PropertyMD().setCategory(samlCat).
				setDescription("This property controls the server's URL of its authentication provider endpoint. It is used to check if requests are properly addressed. If not specified manually it will be set to issuerURI concatenated with '/UVOSAuthenticationService'"));
		defaults.put(ASSERTION_ENDPOINT_URI, new PropertyMD().setCategory(samlCat).
				setDescription("This property controls the server's URL of its assertion query endpoint. It is used to check if requests are properly addressed. If not specified manually it will be set to issuerURI concatenated with '/UVOSAssertionQueryService'"));
		defaults.put(NAMEMAP_ENDPOINT_URI, new PropertyMD().setCategory(samlCat).
				setDescription("This property controls the server's URL of its name id map endpoint. It is used to check if requests are properly addressed. If not specified manually it will be set to issuerURI concatenated with '/UVOSNameIdMappingService'"));
*/
		defaults.put(SP_ACCEPT_POLICY, new PropertyMD(RequestAcceptancePolicy.all).setCategory(samlCat).
				setDescription("Controls which requests are authorized. All accepts all, validSigner accepts all requests which are signed with a trusted certificate, finally strict allows only requests signed by one of the enumerated issuers."));
		defaults.put(ALLOWED_DN_SP, new PropertyMD().setList(true).setCategory(samlCat).
				setDescription("List of Service Providers which are allowed to redirect its clients for authentication and retrieval of ETDs and users attributes from this server. This property is used to configure SPs which use DN SAML identifiers as UNICORE portals. " +
						"Each value must be a path to a file with certificate (in PEM format) of the SP."));
		defaults.put(ALLOWED_URI_SP, new PropertyMD().setList(true).setCategory(samlCat).
				setDescription("List of Service Providers which are allowed to redirect its clients for authentication and retrieval of ETDs and users attributes from this server. This property is used to configure SPs which use URI SAML identifiers as Shibboleth SP. " +
						"Each entry must contain two space separated tokens. The first token must be a URI of SAML service provider. The second token must be a path to a file with certificate (in PEM format) of the SP."));

		defaults.put(TrustedIssuersProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties starting with this prefix are used to configure SAML trust settings. See separate documentation for details."));
		defaults.put(CredentialProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties starting with this prefix are used to configure SAML IdP credential, which is used to sign responses. See separate documentation for details."));
	}

	private boolean signRespNever;
	private boolean signRespAlways;
	private ReplayAttackChecker replayChecker;
	//private SamlTrustChecker trustChecker;
	private SamlTrustChecker authnTrustChecker;
	private long requestValidity;
	private TrustedIssuersProperties trustedProperties;
	private CredentialProperties issuerCredentialProperties;
	
	
	public SamlProperties(Properties src) throws ConfigurationException, IOException
	{
		super(P, src, defaults, log);
		checkIssuer();
		initPki();
		init();
	}
	
	private void init()
	{
		ResponseSigningPolicy repPolicy = getEnumValue(SamlProperties.SIGN_RESPONSE, ResponseSigningPolicy.class);
		signRespAlways = signRespNever = false;
		if (repPolicy == ResponseSigningPolicy.always)
			signRespAlways = true;
		else if (repPolicy == ResponseSigningPolicy.never)
			signRespNever = true;

		RequestAcceptancePolicy spPolicy = getEnumValue(SP_ACCEPT_POLICY, RequestAcceptancePolicy.class);
		
		if (spPolicy == RequestAcceptancePolicy.all)
		{
			authnTrustChecker = new AcceptingSamlTrustChecker();
			log.debug("All SPs will be authorized to submit authentication requests");
		} else if (spPolicy == RequestAcceptancePolicy.validSigner)
		{
			authnTrustChecker = new PKISamlTrustChecker(trustedProperties.getValidator());
			log.debug("All SPs using a valid certificate will be authorized to submit authentication requests");
		} else
		{
			authnTrustChecker = new StrictSamlTrustChecker();
			List<String> allowedSpecs = getListOfValues(SamlProperties.ALLOWED_URI_SP);
			for (String allowedSpec: allowedSpecs)
			{
				String[] parsed = allowedSpec.split("\\s+", 3);
				if (parsed.length != 2)
					throw new ConfigurationException("Invalid specification of allowed Service Provider, must have three elements: " + parsed);
				try
				{
					InputStream is = new BufferedInputStream(new FileInputStream(parsed[1]));
					X509Certificate cert = CertificateUtils.loadCertificate(is, Encoding.PEM);
					((StrictSamlTrustChecker)authnTrustChecker).addTrustedIssuer(
							parsed[0], SAMLConstants.NFORMAT_ENTITY, cert.getPublicKey());
				} catch (IOException e)
				{
					throw new ConfigurationException("Can't load certificate of trusted issuer from " + parsed[1], e);
				}
				log.debug("SP authorized to submit authentication requests: " + parsed[0]);
			}

			List<String> allowedByDnSpecs = getListOfValues(SamlProperties.ALLOWED_DN_SP);
			for (String allowedByDn: allowedByDnSpecs)
			{
				String allowed;
				try
				{
					InputStream is = new BufferedInputStream(new FileInputStream(allowedByDn));
					X509Certificate cert = CertificateUtils.loadCertificate(is, Encoding.PEM);
					allowed = cert.getSubjectX500Principal().getName();
					is.close();
					((StrictSamlTrustChecker)authnTrustChecker).addTrustedIssuer(
							allowed, SAMLConstants.NFORMAT_ENTITY, cert.getPublicKey());
				} catch (IOException e)
				{
					throw new ConfigurationException("Can't load certificate of trusted issuer from " + allowedByDn, e);
				}
				log.debug("SP authorized to submit authentication requests: " + X500NameUtils.getReadableForm(allowed));
			}
		}
		replayChecker = new ReplayAttackChecker();
		requestValidity = getLongValue(SamlProperties.SAML_REQUEST_VALIDITY)*1000;
	}

	private void initPki()
	{
		if (getEnumValue(SP_ACCEPT_POLICY, RequestAcceptancePolicy.class) != RequestAcceptancePolicy.all)
		{
			trustedProperties = new TrustedIssuersProperties(properties, 
						Collections.singleton(new LoggingStoreUpdateListener()), 
						P+TrustedIssuersProperties.DEFAULT_PREFIX);
		}
		issuerCredentialProperties = new CredentialProperties(properties, 
					P+CredentialProperties.DEFAULT_PREFIX); 
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
	
//	public SamlTrustChecker getGenericSamlTrustChecker()
//	{
//		return trustChecker;
//	}

	public SamlTrustChecker getAuthnTrustChecker()
	{
		return authnTrustChecker;
	}

	public X509Credential getSamlIssuerCredential()
	{
		return issuerCredentialProperties.getCredential();
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
}

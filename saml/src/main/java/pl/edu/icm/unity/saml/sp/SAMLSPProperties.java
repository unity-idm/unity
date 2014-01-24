/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.NameFormat;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.trust.StrictSamlTrustChecker;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;

/**
 * Configuration of a SAML requester (or SAML SP).
 * @author K. Benedyczak
 */
public class SAMLSPProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, SAMLSPProperties.class);
	
	/**
	 * Note: it is intended that {@link SAMLBindings} is not used here: we want to have only the 
	 * supported bindings here. However the names here must be exactly the same as in {@link SAMLBindings}.
	 */
	public enum Binding {HTTP_REDIRECT, HTTP_POST};
	
	@DocumentationReferencePrefix
	public static final String P = "unity.saml.requester.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	public static final String REQUESTER_ID = "requesterEntityId";
	public static final String CREDENTIAL = "requesterCredential";
	public static final String REQUESTED_NAME_FORMAT = "requestedNameFormat";
	public static final String ACCEPTED_NAME_FORMATS = "acceptedNameFormats.";
	public static final String GROUP_MEMBERSHIP_ATTRIBUTE = "groupMembershipAttribute";
	
	public static final String DISPLAY_NAME = "displayName";
	public static final String IDP_PREFIX = "remoteIdp.";
	public static final String IDP_NAME = "name";
	public static final String IDP_ID = "samlId";
	public static final String IDP_ADDRESS = "address";
	public static final String IDP_BINDING = "binding";
	public static final String IDP_CERTIFICATE = "certificate";
	public static final String IDP_SIGN_REQUEST = "signRequest";

	public static final String TRANSLATION_PROFILE = "translationProfile";
	public static final String REGISTRATION_FORM = "registrationFormForUnknown";
	
	static
	{
		DocumentationCategory common = new DocumentationCategory(
				"Common settings", "01");
		DocumentationCategory verificator = new DocumentationCategory(
				"SAML validator specific settings", "02");
		DocumentationCategory webRetrieval = new DocumentationCategory(
				"SAML web retrieval specific settings", "03");

		META.put(IDP_PREFIX, new PropertyMD().setStructuredList(true).setMandatory().setCategory(common).setDescription(
				"With this prefix configuration of trusted and enabled remote SAML IdPs is stored. " +
				"There must be at least one IdP defined. If there are multiple ones defined, then the user can choose which one to use."));
		META.put(IDP_ADDRESS, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setMandatory().setCategory(common).setDescription(
				"Address of the IdP endpoint."));
		META.put(IDP_BINDING, new PropertyMD(Binding.HTTP_REDIRECT).setStructuredListEntry(IDP_PREFIX).setCategory(common).setDescription(
				"SAML binding to be used to send a request to the IdP."));
		META.put(IDP_NAME, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setCategory(common).setDescription(
				"Displayed name of the IdP. If not defined then the name is created " +
				"from the IdP address (what is rather not user friendly)."));
		META.put(IDP_ID, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setMandatory().setCategory(common).setDescription(
				"SAML entity identifier of the IdP."));
		META.put(IDP_CERTIFICATE, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setMandatory().setCategory(common).setDescription(
				"Certificate name (as used in centralized PKI store) of the IdP. This certificate is used to verify signature of SAML " +
				"response and included assertions. Therefore it is of highest importance for the whole system security."));
		META.put(IDP_SIGN_REQUEST, new PropertyMD("false").setCategory(common).setStructuredListEntry(IDP_PREFIX).setDescription(
				"Controls whether the requests for this IdP should be signed."));
		
		META.put(REQUESTER_ID, new PropertyMD().setMandatory().setCategory(verificator).setDescription(
				"SAML entity ID (must be a URI) of the lcoal SAML requester (or service provider)."));
		META.put(CREDENTIAL, new PropertyMD().setCategory(verificator).setDescription(
				"Local credential, used to sign requests. If signing is disabled it is not used."));
		META.put(REQUESTED_NAME_FORMAT, new PropertyMD().setEnum(NameFormat.emailAddress).setCategory(verificator).setDescription(
				"If defined then specifies what SAML name format should be requested from the IdP." +
				" If undefined then IdP is free to choose, however see the " + ACCEPTED_NAME_FORMATS +
				" property."));
		META.put(ACCEPTED_NAME_FORMATS, new PropertyMD().setList(false).setCategory(verificator).setDescription(
				"If defined then specifies what SAML name formatd are accepted from IdP. " +
				"Useful when the property " + REQUESTED_NAME_FORMAT + " is undefined. " +
				"If this property is defined then this setting is ignored. Allowed values are the same" +
				"as for the " + REQUESTED_NAME_FORMAT + "."));
		META.put(GROUP_MEMBERSHIP_ATTRIBUTE, new PropertyMD().setCategory(verificator).setDescription(
				"Defines a SAML attribute name which will be treated as an attribute carrying group" +
				" membership information."));
		META.put(TRANSLATION_PROFILE, new PropertyMD().setMandatory().setCategory(verificator).setDescription("Name of a translation" +
				" profile, which will be used to map remotely obtained attributes and identity" +
				" to the local counterparts. The profile should at least map the remote identity."));
		
		META.put(REGISTRATION_FORM, new PropertyMD().setCategory(webRetrieval).setDescription(
				"Name of a registration form to be shown for a remotely authenticated principal who " +
				"has no local account. If unset such users will be denied."));
		META.put(DISPLAY_NAME, new PropertyMD("SAML authentication").setCategory(webRetrieval).setDescription(
				"Name of the SAML authentication GUI component"));
	}
	
	private PKIManagement pkiManagement;
	
	public SAMLSPProperties(Properties properties, PKIManagement pkiMan) throws ConfigurationException
	{
		super(P, properties, META, log);
		this.pkiManagement = pkiMan;
		Set<String> idpKeys = getStructuredListKeys(IDP_PREFIX);
		boolean sign = false;
		for (String idpKey: idpKeys)
		{
			boolean s = getBooleanValue(idpKey+IDP_SIGN_REQUEST);  
			sign |= s;
			if (s && getEnumValue(idpKey+IDP_BINDING, Binding.class) == Binding.HTTP_REDIRECT)
			{
				String name = getValue(idpKey+IDP_NAME);  
				throw new ConfigurationException("IdP " + name + " is configured to use " +
						"HTTP Redirect binding and sign requests. This is unsupported " +
						"currently and against SAML interoperability specification.");
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
		//test drive
		getTrustChecker();
	}
	
	public Properties getProperties()
	{
		return properties;
	}
	
	public SamlTrustChecker getTrustChecker() throws ConfigurationException
	{
		Set<String> idpKeys = getStructuredListKeys(IDP_PREFIX);
		StrictSamlTrustChecker trustChecker = new StrictSamlTrustChecker();
		for (String idpKey: idpKeys)
		{
			String idpId = getValue(idpKey+IDP_ID);
			String idpCertName = getValue(idpKey+IDP_CERTIFICATE);
			X509Certificate idpCert;
			try
			{
				idpCert = pkiManagement.getCertificate(idpCertName);
			} catch (EngineException e)
			{
				throw new ConfigurationException("Remote SAML IdP certificate can not be loaded " 
						+ idpCertName, e);
			}
			trustChecker.addTrustedIssuer(idpId, SAMLConstants.NFORMAT_ENTITY, idpCert.getPublicKey());
		}
		return trustChecker;
	}
}

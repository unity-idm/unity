/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of the OAuth AS. Used for OpenID Connect mode too. Shared by web and rest endpoints (authz and token
 * respectively).
 * @author K. Benedyczak
 */
public class OAuthASProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, OAuthASProperties.class);
	
	@DocumentationReferencePrefix
	public static final String P = "unity.oauth2.as.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults = new HashMap<String, PropertyMD>();

	public static final String ISSUER_URI = "issuerUri";
	public static final String ACCESS_TOKEN_VALIDITY = "accessTokenValidity";
	public static final String CODE_TOKEN_VALIDITY = "codeTokenValidity";
	public static final String ID_TOKEN_VALIDITY = "idTokenValidity";
	public static final String CREDENTIAL = "signingCredential";
	
	public static final String CLIENTS_GROUP = "clientsGroup";
	public static final String USERS_GROUP = "usersGroup";
	public static final String TRANSLATION_PROFILE = "translationProfile";
	
	public static final String SCOPES = "scopes.";
	public static final String SCOPE_ATTRIBUTES = "attributes.";
	public static final String SCOPE_DESCRIPTION = "description";
	public static final String SCOPE_NAME = "name";
	
	static
	{
		defaults.put(ISSUER_URI, new PropertyMD().setMandatory().
				setDescription("This property controls the server's URI which is used in tokens to identify the Authorization Server. "
						+ "It should be a unique URI which identifies the server. It should start with the 'https://' prefix. "
						+ "The best approach is to use the server's URL of the OAuth endpoint."));
		defaults.put(CODE_TOKEN_VALIDITY, new PropertyMD("600").setPositive().
				setDescription("Controls the maximum validity period of a code token returned to a client (in seconds)."));
		defaults.put(ID_TOKEN_VALIDITY, new PropertyMD("3600").setPositive().
				setDescription("Controls the maximum validity period of an OpenID Connect Id token (in seconds)."));
		defaults.put(ACCESS_TOKEN_VALIDITY, new PropertyMD("3600").setPositive().
				setDescription("Controls the maximum validity period of an Access token (in seconds)."));
		defaults.put(CREDENTIAL, new PropertyMD().setMandatory().
				setDescription("Name of a credential which is used to sign tokens. "
						+ "Used only for the OpenId Connect mode, but currently it is always required."));
		defaults.put(CLIENTS_GROUP, new PropertyMD("/oauth-clients").
				setDescription("Group in which authorized OAuth Clients must be present. "
						+ "OAuth related attributes defined in this group are used"
						+ "to configure the client."));
		defaults.put(USERS_GROUP, new PropertyMD("/").
				setDescription("Group for resolving attributes of OAuth users. "
						+ "Only members of this group can authorize with OAuth."));
		defaults.put(SCOPES, new PropertyMD().setStructuredList(false).
				setDescription("Under this prefix OAuth scopes can be defined. In general scope"
						+ " defines a set of attribtues returned when it is requested."));
		defaults.put(SCOPE_NAME, new PropertyMD().setStructuredListEntry(SCOPES).setMandatory().
				setDescription("Name of the scope as used in OAuth protocol."));
		defaults.put(SCOPE_DESCRIPTION, new PropertyMD().setStructuredListEntry(SCOPES).
				setDescription("Human readable description of the scope meaning."));
		defaults.put(SCOPE_ATTRIBUTES, new PropertyMD().setStructuredListEntry(SCOPES).setList(false).
				setDescription("List of Unity attributes that should be returned when the scope is "
						+ "requested. Note that those attribtues are merely an input to the "
						+ "configured output translation profile."));
		defaults.put(TRANSLATION_PROFILE, new PropertyMD().
				setDescription("Name of an output translation profile which can be used to dynamically modify the "
						+ "data being returned on this endpoint. "
						+ "When not defined the default profile is used which simply return all Unity attribtues."));

	}
	
	private X509Credential credential;
	
	public OAuthASProperties(Properties properties, PKIManagement pkiManamgenet) throws ConfigurationException
	{
		super(P, properties, defaults, log);
		
		String credential = getValue(CREDENTIAL);
		try
		{
			if (!pkiManamgenet.getCertificateNames().contains(credential))
				throw new ConfigurationException("There is no credential named '" + credential + 
						"' which is configured in the OAuth endpoint.");
			this.credential = pkiManamgenet.getCredential(credential);
			PrivateKey pk = this.credential.getKey();
			if (!(pk instanceof RSAPrivateKey) && !(pk instanceof ECPrivateKey))
				throw new ConfigurationException("The private key of credential "
						+ "is neither RSA or EC - it is unsupported.");
		} catch (EngineException e)
		{
			throw new ConfigurationException("Can't obtain credential names.", e);
		}
	}

	public X509Credential getCredential()
	{
		return credential;
	}
}

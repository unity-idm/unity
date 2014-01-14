/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.proto.AuthnRequest;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.NameFormat;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Binding irrelevant SAML logic: creation of a SAML authentication request and verification of the answer.
 * @author K. Benedyczak
 */
public class SAMLValidator extends AbstractRemoteVerificator implements SAMLExchange
{
	private SAMLSPValidatorProperties samlProperties;
	private PKIManagement pkiMan;
	
	public SAMLValidator(String name, String description, TranslationProfileManagement profileManagement, 
			AttributesManagement attrMan, PKIManagement pkiMan)
	{
		super(name, description, SAMLExchange.ID, profileManagement, attrMan);
		this.pkiMan = pkiMan;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		StringWriter sbw = new StringWriter();
		try
		{
			samlProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize SAML verificator configuration", e);
		}
		return sbw.toString();	
	}

	@Override
	public void setSerializedConfiguration(String source) throws InternalException
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			samlProperties = new SAMLSPValidatorProperties(properties, pkiMan);
			setTranslationProfile(samlProperties.getValue(SAMLSPValidatorProperties.TRANSLATION_PROFILE));
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator(?)", e);
		} catch (EngineException e)
		{
			throw new InternalException("Problem with the translation profile of the SAML verificator", e);
		}
	}

	@Override
	public AuthnRequestDocument createSAMLRequest(String identityProviderURL, String returnURL) 
			throws InternalException
	{
		String requestrId = samlProperties.getValue(SAMLSPValidatorProperties.REQUESTER_ID);
		NameID issuer = new NameID(requestrId, SAMLConstants.NFORMAT_ENTITY);
		AuthnRequest request = new AuthnRequest(issuer.getXBean());
		
		NameFormat requestedFormat = samlProperties.getEnumValue(SAMLSPValidatorProperties.REQUESTED_NAME_FORMAT, 
				NameFormat.class);
		if (requestedFormat != null)
			request.setFormat(requestedFormat.getSamlRepresentation());
		request.getXMLBean().setDestination(identityProviderURL);
		request.getXMLBean().setAssertionConsumerServiceURL(returnURL);

		if (samlProperties.getBooleanValue(SAMLSPValidatorProperties.SIGN_REQUEST))
		{
			try
			{
				X509Credential credential = pkiMan.getCredential(
						samlProperties.getValue(SAMLSPValidatorProperties.CREDENTIAL));
				request.sign(credential.getKey(), credential.getCertificateChain());
			} catch (Exception e)
			{
				throw new InternalException("Can't sign request", e);
			}
		}
		return request.getXMLBeanDoc();
	}

	@Override
	public void verifySAMLResponse(ResponseDocument response)
	{
		// TODO Auto-generated method stub
		
	}
}

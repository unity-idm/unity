/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ws;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.messages.XMLExpandedMessage;
import eu.unicore.samly2.webservice.SAMLAuthnInterface;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import org.apache.cxf.interceptor.Fault;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.slo.SamlRoutableSignableMessage;
import pl.edu.icm.unity.saml.validator.UnityAuthnRequestValidator;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the SAML authentication protocol over SOAP.
 * 
 * @author K. Benedyczak
 */
class SAMLAuthnImpl implements SAMLAuthnInterface
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLAuthnImpl.class);
	protected SAMLIdPConfiguration samlConfiguration;
	protected String endpointAddress;
	protected IdPEngine idpEngine;
	protected PreferencesManagement preferencesMan;
	protected AttributeTypeSupport aTypeSupport;
	protected final LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;

	private final SamlIdpStatisticReporter idpStatisticReporter;

	public SAMLAuthnImpl(AttributeTypeSupport aTypeSupport, SAMLIdPConfiguration samlConfiguration, String endpointAddress,
	                     IdPEngine idpEngine, PreferencesManagement preferencesMan, SamlIdpStatisticReporter idpStatisticReporter,
	                     LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement)
	{
		this.aTypeSupport = aTypeSupport;
		this.samlConfiguration = samlConfiguration;
		this.endpointAddress = endpointAddress;
		this.idpEngine = idpEngine;
		this.preferencesMan = preferencesMan;
		this.idpStatisticReporter = idpStatisticReporter;
		this.lastAccessAttributeManagement = lastAccessAttributeManagement;
	}

	@Override
	public ResponseDocument authnRequest(AuthnRequestDocument reqDoc)
	{
		if (log.isTraceEnabled())
			log.trace("Received SAML AuthnRequest: " + reqDoc.xmlText());
		XMLExpandedMessage verifiableMessage = new XMLExpandedMessage(reqDoc, reqDoc.getAuthnRequest());
		SAMLAuthnContext context = new SAMLAuthnContext(reqDoc, samlConfiguration, verifiableMessage);
		try
		{
			validate(context);
		} catch (SAMLServerException e1)
		{
			idpStatisticReporter.reportStatus(context, Status.FAILED);
			log.warn("Throwing SAML fault, caused by validation exception", e1);
			throw new Fault(e1);
		}
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(aTypeSupport, lastAccessAttributeManagement,
				context);
		NameIDType samlRequester = context.getRequest().getIssuer();

		ResponseDocument respDoc;
		try
		{
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
			SPSettings spPreferences = preferences.getSPSettings(samlRequester);

			TranslationResult userInfo = getUserInfo(samlProcessor);
			IdentityParam selectedIdentity = getIdentity(userInfo, samlProcessor, spPreferences);
			log.info("Authentication of " + selectedIdentity);
			Collection<Attribute> attributes = samlProcessor.getAttributes(userInfo, spPreferences);
			SamlRoutableSignableMessage<ResponseDocument> routableMessage = samlProcessor
					.processAuthnRequestReturningResponse(selectedIdentity, attributes, null,
							context.getResponseDestination());
			respDoc = routableMessage.getSignedMessage();
		} catch (Exception e)
		{

			log.warn("Throwing SAML fault, caused by processing exception", e);
			SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
			idpStatisticReporter.reportStatus(context, Status.FAILED);
			respDoc = samlProcessor.getErrorResponse(convertedException);
		}
		if (log.isTraceEnabled())
			log.trace("Returning SAML Response: " + respDoc.xmlText());
		idpStatisticReporter.reportStatus(context, Status.SUCCESSFUL);

		return respDoc;
	}

	protected TranslationResult getUserInfo(AuthnResponseProcessor processor) throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();

		return idpEngine.obtainUserInformationWithEnrichingImport(new EntityParam(ae.getEntityId()),
				processor.getChosenGroup(), samlConfiguration.getOutputTranslationProfile(), processor.getIdentityTarget(),
				Optional.empty(), "SAML2", SAMLConstants.BINDING_SOAP, processor.isIdentityCreationAllowed(),
				samlConfiguration.userImportConfigs);
	}

	protected IdentityParam getIdentity(TranslationResult userInfo, AuthnResponseProcessor samlProcessor,
			SPSettings preferences) throws EngineException, SAMLRequesterException
	{
		List<IdentityParam> validIdentities = samlProcessor.getCompatibleIdentities(userInfo.getIdentities());
		return idpEngine.getIdentity(validIdentities, preferences.getSelectedIdentity());
	}

	protected void validate(SAMLAuthnContext context) throws SAMLServerException
	{
		UnityAuthnRequestValidator validator = new UnityAuthnRequestValidator(endpointAddress,
				samlConfiguration.getSoapTrustChecker(), samlConfiguration.requestValidityPeriod,
				samlConfiguration.getReplayChecker(),
				samlConfiguration.ignoreAttributeConsumingServiceIndex);
		validator.validate(context.getRequestDocument(), context.getVerifiableElement());
	}
}

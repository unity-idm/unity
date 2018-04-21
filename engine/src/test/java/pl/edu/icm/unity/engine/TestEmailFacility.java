/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.notifications.EmailFacility;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Tests the core notifications mechanism and the email facility.
 * @author K. Benedyczak
 */
public class TestEmailFacility extends DBIntegrationTestBase
{
	@Autowired
	private EmailFacility emailFacility;
	
	@Autowired
	private InitializerCommon initCommon;
	
	@Autowired
	private TransactionalRunner tx;
	
	private VerifiableEmail plainA = new VerifiableEmail("email1@ex.com");
	private VerifiableEmail onlyConfirmedA = new VerifiableEmail("email2@ex.com");
	private VerifiableEmail plainI = new VerifiableEmail("email5@ex.com");
	private VerifiableEmail onlyConfirmedI = new VerifiableEmail("email6@ex.com");
	
	{
		onlyConfirmedA.setConfirmationInfo(new ConfirmationInfo(true));
		onlyConfirmedI.setConfirmationInfo(new ConfirmationInfo(true));
	}

	private void setEmailAttr(EntityParam entity, VerifiableEmail... emails) throws Exception
	{
		Attribute attribute = VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, 
				"/", emails);
		attrsMan.setAttribute(entity, attribute);
	}

	private void check(EntityParam entity, String expected) throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			assertEquals(expected, emailFacility.getAddressForEntity(entity, null, false));
		}); 
	}
	
	@Test
	public void preferredEmailIsUsedIfPresent() throws Exception
	{
		AttributeType verifiableEmail = new AttributeType(InitializerCommon.EMAIL_ATTR, 
				StringAttributeSyntax.ID);
		verifiableEmail.setMinElements(1);
		verifiableEmail.setMaxElements(5);
		verifiableEmail.getMetadata().put(ContactEmailMetadataProvider.NAME, "");
		aTypeMan.addAttributeType(verifiableEmail);

		setupPasswordAuthn();

		Identity entity = idsMan.addEntity(new IdentityParam(EmailIdentity.ID, "email2@ex.com"), 
				DBIntegrationTestBase.CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityP = new EntityParam(entity);

		Attribute attribute = StringAttribute.of(InitializerCommon.EMAIL_ATTR, 
				"/", "email1@ex.com");
		attrsMan.setAttribute(entityP, attribute);
		
		tx.runInTransactionThrowing(() -> {
			assertEquals("email2@ex.com", emailFacility.getAddressForEntity(
					entityP, "email2@ex.com", false));
			assertEquals("email1@ex.com", emailFacility.getAddressForEntity(
					entityP, "email1@ex.com", false));
			assertEquals("email2@ex.com", emailFacility.getAddressForEntity(
					entityP, "emailNNN@ex.com", false));
		});
	}
	
	@Test
	public void emailExtractedFromAttributeInOrderWithStringContactEmail() throws Exception
	{
		AttributeType verifiableEmail = new AttributeType(InitializerCommon.EMAIL_ATTR, 
				StringAttributeSyntax.ID);
		verifiableEmail.setMinElements(1);
		verifiableEmail.setMaxElements(5);
		verifiableEmail.getMetadata().put(ContactEmailMetadataProvider.NAME, "");
		aTypeMan.addAttributeType(verifiableEmail);

		setupPasswordAuthn();

		Identity entity = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "123"), 
				DBIntegrationTestBase.CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityP = new EntityParam(entity);

		Attribute attribute = StringAttribute.of(InitializerCommon.EMAIL_ATTR, 
				"/", "email1@ex.com");
		attrsMan.setAttribute(entityP, attribute);
		
		check(entityP, "email1@ex.com");
	}
	
	@Test
	public void emailExtractedFromAttributeInOrder() throws Exception
	{
		initCommon.initializeCommonAttributeTypes();
		setupPasswordAuthn();
		
		Identity entity = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "123"), 
				DBIntegrationTestBase.CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityP = new EntityParam(entity);
		
		setEmailAttr(entityP, plainA);
		check(entityP, plainA.getValue());

		setEmailAttr(entityP, plainA, onlyConfirmedA);
		check(entityP, onlyConfirmedA.getValue());
	}
	
	@Test
	public void emailExtractedFromIdentityInOrderOneAttr() throws Exception
	{
		initCommon.initializeCommonAttributeTypes();
		setupPasswordAuthn();
		
		Identity entity = idsMan.addEntity(new IdentityParam(EmailIdentity.ID, plainI.getValue()), 
				DBIntegrationTestBase.CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityP = new EntityParam(entity);

		setEmailAttr(entityP, plainA);
		check(entityP, plainI.getValue());
		
		idsMan.addIdentity(EmailIdentity.toIdentityParam(onlyConfirmedI, null, null), entityP, false);
		check(entityP, onlyConfirmedI.getValue());
	}
	
	@Test
	public void emailExtractedFromIdentityInOrderTwoAttr() throws Exception
	{
		initCommon.initializeCommonAttributeTypes();
		setupPasswordAuthn();
		
		Identity entity = idsMan.addEntity(new IdentityParam(EmailIdentity.ID, plainI.getValue()), 
				DBIntegrationTestBase.CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityP = new EntityParam(entity);

		setEmailAttr(entityP, plainA, onlyConfirmedA);
		check(entityP, onlyConfirmedA.getValue());
		
		idsMan.addIdentity(EmailIdentity.toIdentityParam(onlyConfirmedI, null, null), entityP, false);
		check(entityP, onlyConfirmedI.getValue());
	}


	
	private void setEmailAttr(RegistrationRequestState request, VerifiableEmail... emails) throws Exception
	{
		Attribute attribute = VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, 
				"/", emails);
		List<Attribute> attributes = request.getRequest().getAttributes();
		attributes.clear();
		attributes.add(attribute);
	}

	private void setEmailId(RegistrationRequestState request, VerifiableEmail... emails) throws Exception
	{
		List<IdentityParam> ids = request.getRequest().getIdentities();
		for (VerifiableEmail email: emails)
			ids.add(EmailIdentity.toIdentityParam(email, null, null));
	}

	private void check(RegistrationRequestState request, String expected) throws Exception
	{
		tx.runInTransactionThrowing(() ->
		{
			assertEquals(expected, emailFacility.getAddressForUserRequest(request));
		});
	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void emailExtractedFromRegistration() throws Exception
	{
		initCommon.initializeCommonAttributeTypes();
		
		RegistrationRequestState request = new RegistrationRequestState();
		request.setRequest(new RegistrationRequest());
		request.getRequest().setAttributes(new ArrayList());
		request.getRequest().setIdentities(new ArrayList());
		
		setEmailAttr(request, plainA);
		check(request, plainA.getValue());

		setEmailAttr(request, plainA);
		setEmailId(request, plainI);
		check(request, plainI.getValue());

		setEmailAttr(request);
		setEmailId(request, plainI);
		check(request, plainI.getValue());
	}

}

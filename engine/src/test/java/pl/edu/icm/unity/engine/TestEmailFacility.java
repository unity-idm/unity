/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.engine.notifications.EmailFacility;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
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
	private DBSessionManager db;
	
	@Autowired
	private InitializerCommon initCommon;	
	
	private VerifiableEmail plainA = new VerifiableEmail("email1@ex.com");
	private VerifiableEmail onlyConfirmedA = new VerifiableEmail("email2@ex.com");
	private VerifiableEmail onlyMainA = new VerifiableEmail("email3@ex.com");
	private VerifiableEmail mainAndConfirmedA = new VerifiableEmail("email4@ex.com");
	private VerifiableEmail plainI = new VerifiableEmail("email5@ex.com");
	private VerifiableEmail onlyConfirmedI = new VerifiableEmail("email6@ex.com");
	private VerifiableEmail onlyMainI = new VerifiableEmail("email7@ex.com");
	private VerifiableEmail mainAndConfirmedI = new VerifiableEmail("email8@ex.com");
	
	{
		onlyConfirmedA.setConfirmationInfo(new ConfirmationInfo(true));
		onlyMainA.addTags(EmailUtils.TAG_MAIN);
		mainAndConfirmedA.setConfirmationInfo(new ConfirmationInfo(true));
		mainAndConfirmedA.addTags(EmailUtils.TAG_MAIN);
		onlyConfirmedI.setConfirmationInfo(new ConfirmationInfo(true));
		onlyMainI.addTags(EmailUtils.TAG_MAIN);
		mainAndConfirmedI.setConfirmationInfo(new ConfirmationInfo(true));
		mainAndConfirmedI.addTags(EmailUtils.TAG_MAIN);
	}

	private void setEmailAttr(EntityParam entity, VerifiableEmail... emails) throws Exception
	{
		VerifiableEmailAttribute attribute = new VerifiableEmailAttribute(InitializerCommon.EMAIL_ATTR, 
				"/", AttributeVisibility.full,  emails);
		attrsMan.setAttribute(entity, attribute, true);
	}

	private void check(EntityParam entity, String expected) throws Exception
	{
		SqlSession sqlSession = db.getSqlSession(true);
		try
		{
			assertEquals(expected, emailFacility.getAddressForEntity(entity, sqlSession));
			sqlSession.commit();
		} finally
		{
			db.releaseSqlSession(sqlSession);
		}
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

		setEmailAttr(entityP, plainA, onlyMainA);
		check(entityP, onlyMainA.getValue());

		setEmailAttr(entityP, plainA, onlyMainA, onlyConfirmedA);
		check(entityP, onlyConfirmedA.getValue());

		setEmailAttr(entityP, plainA, onlyMainA, onlyConfirmedA, mainAndConfirmedA);
		check(entityP, mainAndConfirmedA.getValue());
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
		
		idsMan.addIdentity(EmailIdentity.toIdentityParam(onlyMainI, null, null), entityP, false);
		check(entityP, onlyMainI.getValue());
		
		idsMan.addIdentity(EmailIdentity.toIdentityParam(onlyConfirmedI, null, null), entityP, false);
		check(entityP, onlyConfirmedI.getValue());

		idsMan.addIdentity(EmailIdentity.toIdentityParam(mainAndConfirmedI, null, null), entityP, false);
		check(entityP, mainAndConfirmedI.getValue());
	}
	
	@Test
	public void emailExtractedFromIdentityInOrderTwoAttr() throws Exception
	{
		initCommon.initializeCommonAttributeTypes();
		setupPasswordAuthn();
		
		Identity entity = idsMan.addEntity(new IdentityParam(EmailIdentity.ID, plainI.getValue()), 
				DBIntegrationTestBase.CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityP = new EntityParam(entity);

		setEmailAttr(entityP, plainA, onlyMainA);
		check(entityP, onlyMainA.getValue());
		
		idsMan.addIdentity(EmailIdentity.toIdentityParam(onlyMainI, null, null), entityP, false);
		check(entityP, onlyMainI.getValue());
		
		idsMan.addIdentity(EmailIdentity.toIdentityParam(onlyConfirmedI, null, null), entityP, false);
		check(entityP, onlyConfirmedI.getValue());

		idsMan.addIdentity(EmailIdentity.toIdentityParam(mainAndConfirmedI, null, null), entityP, false);
		check(entityP, mainAndConfirmedI.getValue());
	}


	@Test
	public void emailExtractedFromIdentityInOrderThreeAttr() throws Exception
	{
		initCommon.initializeCommonAttributeTypes();
		setupPasswordAuthn();
		
		Identity entity = idsMan.addEntity(new IdentityParam(EmailIdentity.ID, plainI.getValue()), 
				DBIntegrationTestBase.CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityP = new EntityParam(entity);

		setEmailAttr(entityP, plainA, onlyMainA, onlyConfirmedA);
		check(entityP, onlyConfirmedA.getValue());
		
		idsMan.addIdentity(EmailIdentity.toIdentityParam(onlyMainI, null, null), entityP, false);
		check(entityP, onlyConfirmedA.getValue());
		
		idsMan.addIdentity(EmailIdentity.toIdentityParam(onlyConfirmedI, null, null), entityP, false);
		check(entityP, onlyConfirmedI.getValue());

		idsMan.addIdentity(EmailIdentity.toIdentityParam(mainAndConfirmedI, null, null), entityP, false);
		check(entityP, mainAndConfirmedI.getValue());
	}

	@Test
	public void emailExtractedFromIdentityInOrderFourAttr() throws Exception
	{
		initCommon.initializeCommonAttributeTypes();
		setupPasswordAuthn();
		
		Identity entity = idsMan.addEntity(new IdentityParam(EmailIdentity.ID, plainI.getValue()), 
				DBIntegrationTestBase.CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityP = new EntityParam(entity);

		setEmailAttr(entityP, plainA, onlyMainA, onlyConfirmedA, mainAndConfirmedA);
		check(entityP, mainAndConfirmedA.getValue());
		
		idsMan.addIdentity(EmailIdentity.toIdentityParam(onlyMainI, null, null), entityP, false);
		check(entityP, mainAndConfirmedA.getValue());
		
		idsMan.addIdentity(EmailIdentity.toIdentityParam(onlyConfirmedI, null, null), entityP, false);
		check(entityP, mainAndConfirmedA.getValue());

		idsMan.addIdentity(EmailIdentity.toIdentityParam(mainAndConfirmedI, null, null), entityP, false);
		check(entityP, mainAndConfirmedI.getValue());
	}
	
	
	
	private void setEmailAttr(RegistrationRequestState request, VerifiableEmail... emails) throws Exception
	{
		VerifiableEmailAttribute attribute = new VerifiableEmailAttribute(InitializerCommon.EMAIL_ATTR, 
				"/", AttributeVisibility.full,  emails);
		List<Attribute<?>> attributes = request.getRequest().getAttributes();
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
		SqlSession sqlSession = db.getSqlSession(true);
		try
		{
			assertEquals(expected, emailFacility.getAddressForRegistrationRequest(request, sqlSession));
			sqlSession.commit();
		} finally
		{
			db.releaseSqlSession(sqlSession);
		}
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

		setEmailAttr(request, plainA, onlyMainA);
		check(request, onlyMainA.getValue());

		setEmailAttr(request, plainA, onlyMainA);
		setEmailId(request, plainI);
		check(request, onlyMainA.getValue());
		
		setEmailAttr(request, plainA);
		setEmailId(request, plainI);
		check(request, plainI.getValue());

		setEmailAttr(request);
		setEmailId(request, plainI);
		check(request, plainI.getValue());
		
		setEmailAttr(request, plainA, onlyMainA);
		setEmailId(request, plainI, onlyMainI);
		check(request, onlyMainI.getValue());
	}

}

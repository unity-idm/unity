/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.reg.EnquiryFormDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.internal.BaseFormValidator;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.api.registration.EnquiryFilledTemplateDef;
import pl.edu.icm.unity.server.api.registration.NewEnquiryTemplateDef;
import pl.edu.icm.unity.server.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.EnquiryResponse;

/**
 * Implementation of the enquiry management API.
 * 
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class EnquiryManagementImpl implements EnquiryManagement
{
	private EnquiryFormDB enquiryDB;
	private NotificationProducer notificationProducer;
	private UnityMessageSource msg;
	private AuthorizationManager authz;
	private BaseFormValidator baseValidator;
	private SharedEndpointManagement sharedEndpointMan;

	@Autowired
	public EnquiryManagementImpl(EnquiryFormDB enquiryDB,
			NotificationProducer notificationProducer, UnityMessageSource msg,
			AuthorizationManager authz, BaseFormValidator baseValidator,
			SharedEndpointManagement sharedEndpointMan)
	{
		this.enquiryDB = enquiryDB;
		this.notificationProducer = notificationProducer;
		this.msg = msg;
		this.authz = authz;
		this.baseValidator = baseValidator;
		this.sharedEndpointMan = sharedEndpointMan;
	}

	@Transactional
	@Override
	public void addEnquiry(EnquiryForm form) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		validateFormContents(form, sql);
		enquiryDB.insert(form.getName(), form, sql);
	}
	
	@Transactional
	@Override
	public void sendEnquiry(String enquiryId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		EnquiryForm form = enquiryDB.get(enquiryId, SqlSessionTL.get());
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		
		if (notificationsCfg.getChannel() != null && notificationsCfg.getEnquiryToFillTemplate() != null)
		{

			Map<String, String> params = new HashMap<>();
			params.put(NewEnquiryTemplateDef.FORM_NAME, form.getDisplayedName().getDefaultValue());
			params.put(NewEnquiryTemplateDef.URL, 
					PublicRegistrationURLSupport.getPublicEnquiryLink(enquiryId, sharedEndpointMan));
			
			for (String group: form.getTargetGroups())
				notificationProducer.sendNotificationToGroup(
					group, 
					notificationsCfg.getChannel(), 
					notificationsCfg.getEnquiryToFillTemplate(),
					params,
					msg.getDefaultLocaleCode());
		}
	}
	
	@Transactional
	@Override
	public void removeEnquiry(String formId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		enquiryDB.remove(formId, SqlSessionTL.get());
	}
	
	@Transactional
	@Override
	public void updateEnquiry(EnquiryForm updatedForm) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		validateFormContents(updatedForm, sql);
		enquiryDB.update(updatedForm.getName(), updatedForm, sql);
	}
	
	@Transactional
	@Override
	public List<EnquiryForm> getEnquires() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return enquiryDB.getAll(SqlSessionTL.get());
	}

	@Override
	public void submitEnquiryResponse(EnquiryResponse response) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}
	
	private void validateFormContents(EnquiryForm form, SqlSession sql) throws EngineException
	{
		baseValidator.validateBaseFormContents(form, sql);
		
		EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg == null)
			throw new WrongArgumentException("NotificationsConfiguration must be set in the form.");
		baseValidator.checkTemplate(notCfg.getEnquiryFilledTemplate(), EnquiryFilledTemplateDef.NAME,
				sql, "enquiry filled");
		baseValidator.checkTemplate(notCfg.getEnquiryToFillTemplate(), NewEnquiryTemplateDef.NAME,
				sql, "new enquiry");
		
		if (form.getTargetGroups() == null || form.getTargetGroups().length == 0)
			throw new WrongArgumentException("Target groups must be set in the form.");
	}
}

/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IdentityExistsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Logic behind {@link EnquiryResponseEditor}. Provides a simple method to create editor instance and to handle 
 * response submission.
 * 
 * @author K. Benedyczak
 */
@Component
public class EnquiryResponseEditorController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryResponseEditorController.class);

	@Autowired
	private UnityMessageSource msg;
	
	@Autowired @Qualifier("insecure") 
	private EnquiryManagement enquiryManagement;
	
	@Autowired
	private IdentityEditorRegistry identityEditorRegistry;
	
	@Autowired
	private CredentialEditorRegistry credentialEditorRegistry;
	
	@Autowired
	private AttributeHandlerRegistry attributeHandlerRegistry;
	
	@Autowired @Qualifier("insecure") 
	private AttributeTypeManagement atMan;
	
	@Autowired @Qualifier("insecure") 
	private CredentialManagement credMan;
	
	@Autowired @Qualifier("insecure") 
	private GroupsManagement groupsMan;	
	
	@Autowired
	private IdPLoginController idpLoginController;

	
	public EnquiryResponseEditor getEditorInstance(EnquiryForm form, 
			RemotelyAuthenticatedContext remoteContext) throws Exception
	{
		return new EnquiryResponseEditor(msg, form, remoteContext, 
				identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, atMan, credMan, groupsMan);
	}

	public boolean isFormApplicable(String formName)
	{
		List<EnquiryForm> formsToFill = getFormsToFill();
		Optional<String> found = formsToFill.stream()
				.map(form -> form.getName())
				.filter(name -> name.equals(formName))
				.findAny();
		return found.isPresent();
	}
	
	public List<EnquiryForm> getFormsToFill()
	{
		EntityParam entity = new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId());
		try
		{
			return enquiryManagement.getPendingEnquires(entity);
		} catch (EngineException e)
		{
			log.error("Can't load pending enquiry forms", e);
			return new ArrayList<>();
		}
	}
	
	public void markFormAsIgnored(String formId)
	{
		EntityParam entity = new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId());
		try
		{
			enquiryManagement.ignoreEnquiry(formId, entity);
		} catch (EngineException e)
		{
			log.error("Can't mark form as ignored", e);
		}
	}	
	
	public EnquiryForm getForm(String name)
	{
		try
		{
			List<EnquiryForm> forms = enquiryManagement.getEnquires();
			for (EnquiryForm regForm: forms)
				if (regForm.getName().equals(name))
					return regForm;
		} catch (EngineException e)
		{
			log.error("Can't load enquiry forms", e);
		}
		return null;
	}
	
	public WorkflowFinalizationConfiguration submitted(EnquiryResponse response, EnquiryForm form, 
			TriggeringMode mode) throws WrongArgumentException
	{
		RegistrationContext context = new RegistrationContext(
				idpLoginController.isLoginInProgress(), mode);
		try
		{
			String requestId = enquiryManagement.submitEnquiryResponse(response, context);
			WebSession.getCurrent().getEventBus().fireEvent(new EnquiryResponseChangedEvent(requestId));
			return getFinalizationHandler(form).getFinalRegistrationConfigurationPostSubmit(requestId,
					getRequestStatus(requestId));
		} catch (IdentityExistsException e)
		{
			return getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.PRESET_USER_EXISTS);
		} catch (WrongArgumentException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.warn("Registration request submision failed", e);
			return getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.GENERAL_ERROR);
		}
	}
	
	private RegistrationRequestStatus getRequestStatus(String requestId) 
	{
		try
		{
			return enquiryManagement.getEnquiryResponse(requestId).getStatus();
		} catch (Exception e)
		{
			log.error("Shouldn't happen: can't get request status, assuming rejected", e);
			return RegistrationRequestStatus.rejected;
		}
	}
	
	public WorkflowFinalizationConfiguration cancelled(EnquiryForm form, TriggeringMode mode,
			boolean markFormAsIgnored)
	{
		if (form.getType() != EnquiryType.REQUESTED_MANDATORY)
		{
			if (markFormAsIgnored)
				markFormAsIgnored(form.getName());
			return getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.IGNORED_ENQUIRY);
		} else
		{
			return getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.CANCELLED);
		}
	}
	
	private PostFillingHandler getFinalizationHandler(EnquiryForm form)
	{
		String pageTitle = form.getPageTitle() == null ? null : form.getPageTitle().getValue(msg);
		return new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), false);
	}
}

/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
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
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.forms.PostFormFillingHandler;

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
				.filter(name -> formName.equals(formName))
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
	
	public boolean submitted(EnquiryResponse response, EnquiryForm form, 
			TriggeringMode mode) throws WrongArgumentException
	{
		RegistrationContext context = new RegistrationContext(true, 
				idpLoginController.isLoginInProgress(), mode);
		String id;
		try
		{
			id = enquiryManagement.submitEnquiryResponse(response, context);
			WebSession.getCurrent().getEventBus().fireEvent(new EnquiryResponseChangedEvent(id));
		} catch (WrongArgumentException e)
		{
			throw e;
		} catch (Exception e)
		{
			new PostFormFillingHandler(idpLoginController, form, msg, 
					enquiryManagement.getFormAutomationSupport(form)).submissionError(e, context);
			return false;
		}

		new PostFormFillingHandler(idpLoginController, form, msg, 
				enquiryManagement.getFormAutomationSupport(form), mode != TriggeringMode.manualAdmin).
			submittedEnquiryResponse(id, enquiryManagement, response, context);
		return true;
	}
	
	public void cancelled(EnquiryForm form, TriggeringMode mode)
	{
		RegistrationContext context = new RegistrationContext(false, idpLoginController.isLoginInProgress(), 
				mode);
		new PostFormFillingHandler(idpLoginController, form, msg, 
				enquiryManagement.getFormAutomationSupport(form)).
			cancelled(false, context);
	}
}

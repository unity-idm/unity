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
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.internal.IdPLoginController;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
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
	@Autowired
	private EnquiryManagement enquiryManagement;
	@Autowired
	private IdentityEditorRegistry identityEditorRegistry;
	@Autowired
	private CredentialEditorRegistry credentialEditorRegistry;
	@Autowired
	private AttributeHandlerRegistry attributeHandlerRegistry;
	@Autowired
	private AttributesManagement attrsMan;
	@Autowired
	private AuthenticationManagement authnMan;
	@Autowired
	private GroupsManagement groupsMan;	
	@Autowired
	private IdPLoginController idpLoginController;

	
	public EnquiryResponseEditor getEditorInstance(EnquiryForm form, 
			RemotelyAuthenticatedContext remoteContext) throws Exception
	{
		return new EnquiryResponseEditor(msg, form, remoteContext, 
				identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, attrsMan, authnMan, groupsMan);
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
					enquiryManagement.getProfileInstance(form)).submissionError(e, context);
			return false;
		}

		new PostFormFillingHandler(idpLoginController, form, msg, 
				enquiryManagement.getProfileInstance(form), false).
			submittedEnquiryResponse(id, enquiryManagement, response, context);
		return true;
	}
	
	public void cancelled(EnquiryForm form, TriggeringMode mode)
	{
		RegistrationContext context = new RegistrationContext(false, idpLoginController.isLoginInProgress(), 
				mode);
		new PostFormFillingHandler(idpLoginController, form, msg, enquiryManagement.getProfileInstance(form)).
			cancelled(false, context);
	}
}

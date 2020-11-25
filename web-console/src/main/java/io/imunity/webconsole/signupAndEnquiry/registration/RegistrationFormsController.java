/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.registration;

import java.util.Collection;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.signupAndEnquiry.forms.RegistrationFormEditor;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all registration form views.
 * 
 * @author P.Piernik
 *
 */
@Component
public class RegistrationFormsController
{
	private MessageSource msg;
	private RegistrationsManagement regMan;
	private SharedEndpointManagement sharedEndpointMan;
	private ObjectFactory<RegistrationFormEditor> editorFactory;

	@Autowired
	RegistrationFormsController(MessageSource msg, RegistrationsManagement regMan,
			SharedEndpointManagement sharedEndpointMan, ObjectFactory<RegistrationFormEditor> editorFactory)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.editorFactory = editorFactory;
		this.sharedEndpointMan = sharedEndpointMan;
	}

	void addRegistrationForm(RegistrationForm toAdd) throws ControllerException

	{
		try
		{
			regMan.addForm(toAdd);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("RegistrationFormsController.addError", toAdd.getName()), e);
		}
	}

	void updateRegistrationForm(RegistrationForm toUpdate, boolean ignoreRequestsAndInvitations)
			throws ControllerException

	{
		try
		{
			regMan.updateForm(toUpdate, ignoreRequestsAndInvitations);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("RegistrationFormsController.updateError", toUpdate.getName()),
					e);
		}
	}

	void removeRegistrationForm(RegistrationForm toRemove, boolean dropRequests) throws ControllerException

	{
		try
		{
			regMan.removeForm(toRemove.getName(), dropRequests);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("RegistrationFormsController.removeError", toRemove.getName()),
					e);
		}
	}

	RegistrationForm getRegistrationForm(String name) throws ControllerException
	{
		try
		{
			return regMan.getForm(name);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("RegistrationFormsController.getError"), e);
		}
	}

	Collection<RegistrationForm> getRegistrationForms() throws ControllerException
	{
		try
		{
			return regMan.getForms();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("RegistrationFormsController.getAllError"), e);
		}
	}

	String getPublicFormLink(RegistrationForm form)
	{
		return PublicRegistrationURLSupport.getPublicRegistrationLink(form, sharedEndpointMan);
	}

	RegistrationFormEditor getEditor(RegistrationForm form, boolean copyMode) throws ControllerException
	{
		try
		{
			RegistrationFormEditor editor = editorFactory.getObject().init(copyMode);
			if (form != null)
			{
				editor.setForm(form);
			}
			return editor;
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("RegistrationFormsController.createEditorError"), e);

		}
	}
}

/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.registration;

import java.util.Collection;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.signupAndEnquiry.forms.RegistrationFormEditorV8;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
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
	private final MessageSource msg;
	private final RegistrationsManagement regMan;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final ObjectFactory<RegistrationFormEditorV8> editorFactory;

	@Autowired
	RegistrationFormsController(MessageSource msg, RegistrationsManagement regMan,
			PublicRegistrationURLSupport publicRegistrationURLSupport, ObjectFactory<RegistrationFormEditorV8> editorFactory)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.editorFactory = editorFactory;
		this.publicRegistrationURLSupport = publicRegistrationURLSupport;
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
		return publicRegistrationURLSupport.getPublicRegistrationLink(form);
	}

	RegistrationFormEditorV8 getEditor(RegistrationForm form, boolean copyMode) throws ControllerException
	{
		try
		{
			RegistrationFormEditorV8 editor = editorFactory.getObject().init(copyMode);
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

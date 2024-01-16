/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.forms;

import io.imunity.console.views.signup_and_enquiry.RegistrationFormEditor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.Collection;

@Component
public class RegistrationFormsController
{
	private final MessageSource msg;
	private final RegistrationsManagement regMan;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final ObjectFactory<RegistrationFormEditor> editorFactory;

	@Autowired
	RegistrationFormsController(MessageSource msg, RegistrationsManagement regMan,
			PublicRegistrationURLSupport publicRegistrationURLSupport, ObjectFactory<RegistrationFormEditor> editorFactory)
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

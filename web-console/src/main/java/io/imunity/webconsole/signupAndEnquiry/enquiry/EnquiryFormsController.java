/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.enquiry;

import java.util.Collection;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.signupAndEnquiry.forms.EnquiryFormEditor;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all enquiry form views
 * 
 * @author P.Piernik
 *
 */
@Component
public class EnquiryFormsController
{
	private MessageSource msg;
	private EnquiryManagement enqMan;
	private SharedEndpointManagement sharedEndpointMan;
	private ObjectFactory<EnquiryFormEditor> editorFactory;

	@Autowired
	EnquiryFormsController(MessageSource msg, EnquiryManagement enqMan,
			SharedEndpointManagement sharedEndpointMan, ObjectFactory<EnquiryFormEditor> editorFactory)
	{
		this.msg = msg;
		this.enqMan = enqMan;
		this.editorFactory = editorFactory;
		this.sharedEndpointMan = sharedEndpointMan;
	}

	void addEnquiryForm(EnquiryForm toAdd) throws ControllerException

	{
		try
		{
			enqMan.addEnquiry(toAdd);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("EnquiryFormsController.addError", toAdd.getName()), e);
		}
	}

	void updateEnquiryForm(EnquiryForm toUpdate, boolean ignoreRequestsAndInvitations)
			throws ControllerException

	{
		try
		{
			enqMan.updateEnquiry(toUpdate, ignoreRequestsAndInvitations);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("EnquiryFormsController.updateError", toUpdate.getName()), e);
		}
	}

	void removeEnquiryForm(EnquiryForm toRemove, boolean dropRequests) throws ControllerException

	{
		try
		{
			enqMan.removeEnquiry(toRemove.getName(), dropRequests);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("EnquiryFormsController.removeError", toRemove.getName()), e);
		}
	}

	EnquiryForm getEnquiryForm(String name) throws ControllerException
	{
		try
		{
			return enqMan.getEnquiry(name);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("EnquiryFormsController.getError"), e);
		}
	}

	Collection<EnquiryForm> getEnquiryForms() throws ControllerException
	{
		try
		{
			return enqMan.getEnquires();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("EnquiryFormsController.getAllError"), e);
		}
	}

	String getPublicEnquiryLink(EnquiryForm form)
	{
		return PublicRegistrationURLSupport.getWellknownEnquiryLink(form.getName(), sharedEndpointMan);
	}

	EnquiryFormEditor getEditor(EnquiryForm form, boolean copyMode) throws ControllerException
	{
		try
		{
			EnquiryFormEditor editor = editorFactory.getObject().init(copyMode);
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

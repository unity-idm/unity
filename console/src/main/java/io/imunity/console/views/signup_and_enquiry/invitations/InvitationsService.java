/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.console.views.signup_and_enquiry.invitations.editor.InvitationEditor;
import io.imunity.console.views.signup_and_enquiry.invitations.editor.InvitationEditor.InvitationEditorFactory;
import io.imunity.console.views.signup_and_enquiry.invitations.viewer.MainInvitationViewer;
import io.imunity.console.views.signup_and_enquiry.invitations.viewer.MainInvitationViewer.InvitationViewerFactory;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;

/**
 * Controller for all invitation views
 * 
 * @author P.Piernik
 *
 */
@Component
class InvitationsService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InvitationsService.class);
	private final InvitationManagement invMan;
	private final MessageSource msg;
	private final InvitationEditorFactory editorFactory;
	private final InvitationViewerFactory viewerFactory;

	@Autowired
	InvitationsService(InvitationManagement invMan, MessageSource msg, InvitationViewerFactory viewerFactory,
			InvitationEditorFactory editorFactory)
	{
		this.invMan = invMan;
		this.msg = msg;
		this.editorFactory = editorFactory;
		this.viewerFactory = viewerFactory;
	}

	Collection<InvitationEntry> getInvitations() throws ControllerException
	{
		try
		{
			return invMan.getInvitations()
					.stream()
					.map(i -> new InvitationEntry(msg, i))
					.collect(Collectors.toList());
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("InvitationsController.getAllError"), e);
		}
	}

	void addInvitation(InvitationParam toAdd) throws ControllerException
	{
		try
		{
			invMan.addInvitation(toAdd);
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("InvitationsController.addError"), e);
		}
	}

	void sendInvitations(Set<InvitationEntry> items) throws ControllerException
	{
		List<String> sent = new ArrayList<>();
		try
		{
			for (InvitationEntry item : items)
			{
				invMan.sendInvitation(item.getCode());
				sent.add(item.getAddress());
			}
		} catch (Exception e)
		{
			if (sent.isEmpty())
			{
				throw new ControllerException(msg.getMessage("InvitationsController.sendError"), e);
			} else
			{
				throw new ControllerException(msg.getMessage("InvitationsController.sendError"),
						msg.getMessage("InvitationsController.partiallySent", sent), e);
			}
		}
	}

	void removeInvitations(Set<InvitationEntry> items) throws ControllerException
	{
		List<String> removed = new ArrayList<>();
		try
		{
			for (InvitationEntry item : items)
			{
				invMan.removeInvitation(item.getCode());
				removed.add(item.getAddress());
			}
		} catch (Exception e)
		{
			if (removed.isEmpty())
			{
				throw new ControllerException(msg.getMessage("InvitationsController.removeError"), e);
			} else
			{
				throw new ControllerException(msg.getMessage("InvitationsController.removeError"),
						msg.getMessage("InvitationsController.partiallyRemoved", removed), e);
			}
		}
	}

	MainInvitationViewer getViewer() throws ControllerException
	{
		try
		{
			return viewerFactory.getViewer();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("InvitationsController.getViewerError"), e);
		}
	}

	InvitationEditor getEditor() throws ControllerException
	{
		try
		{
			return editorFactory.getEditor();
		} catch (Exception e)
		{
			log.error(msg.getMessage("InvitationsController.getEditorError"), e);
			throw new ControllerException(msg.getMessage("InvitationsController.getEditorError"), e);
		}
	}

	InvitationEditor getEditor(String type, String formName) throws ControllerException
	{
		InvitationEditor editor = getEditor();
		try
		{
			editor.setInvitationToForm(InvitationType.valueOf(type), formName);
		} catch (Exception e)
		{
			log.error(msg.getMessage("InvitationsController.invalidForm"), e);
			throw new ControllerException(msg.getMessage("InvitationsController.invalidForm"), e);
		}

		return editor;
	}
}

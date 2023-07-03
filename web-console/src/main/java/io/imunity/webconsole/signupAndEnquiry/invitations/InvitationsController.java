/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.signupAndEnquiry.invitations.editor.InvitationEditor;
import io.imunity.webconsole.signupAndEnquiry.invitations.editor.InvitationEditor.InvitationEditorFactory;
import io.imunity.webconsole.signupAndEnquiry.invitations.viewer.MainInvitationViewer;
import io.imunity.webconsole.signupAndEnquiry.invitations.viewer.MainInvitationViewer.InvitationViewerFactory;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all invitation views
 * 
 * @author P.Piernik
 *
 */
@Component
class InvitationsController
{
	private final InvitationManagement invMan;
	private final MessageSource msg;
	private final InvitationEditorFactory editorFactory;
	private final InvitationViewerFactory viewerFactory;

	@Autowired
	InvitationsController(InvitationManagement invMan, MessageSource msg, InvitationEditorFactory editorFactory,
			InvitationViewerFactory viewerFactory)
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
			return invMan.getInvitations().stream().map(i -> new InvitationEntry(msg, i)).collect(Collectors.toList());
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
			throw new ControllerException(msg.getMessage("InvitationsController.invalidForm"), e);
		}

		return editor;
	}
}

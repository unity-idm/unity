/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;

/**
 * Presents an {@link InvitationWithCode}
 * 
 * @author Krzysztof Benedyczak
 */
@PrototypeComponent
public class MainInvitationViewer extends CustomComponent
{
	private final MessageSource msg;
	private Label type;
	private VerticalLayout viewerContent;

	private final Map<InvitationType, InvitationViewer> viewers;

	public MainInvitationViewer(MessageSource msg, List<InvitationViewer> viewers)
	{
		this.msg = msg;
		this.viewers = viewers.stream().collect(Collectors.toMap(InvitationViewer::getSupportedType, v -> v));
		initUI();
	}

	private void initUI()
	{
		type = new Label();
		type.setCaption(msg.getMessage("InvitationViewer.type"));

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);

		FormLayoutWithFixedCaptionWidth top = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		top.addComponent(type);
		top.setMargin(new MarginInfo(false, true));
		top.setSpacing(false);

		viewerContent = new VerticalLayout();
		viewerContent.setMargin(false);
		viewerContent.setSpacing(false);

		main.addComponents(top, viewerContent);
		setVisible(false);
	}

	public void setInput(InvitationWithCode invitationWithCode) throws IllegalFormTypeException
	{
		if (invitationWithCode == null)
		{
			setVisible(false);
			return;
		}

		setVisible(true);

		InvitationParam invitation = invitationWithCode.getInvitation();
		InvitationType itype = invitation.getType();
		type.setValue(msg.getMessage("InvitationType." + itype.toString().toLowerCase()));
		viewers.get(itype).setInput(invitationWithCode);
		viewerContent.removeAllComponents();
		viewerContent.addComponent(viewers.get(itype));
	}

	@org.springframework.stereotype.Component
	public static class InvitationViewerFactory
	{
		private ObjectFactory<MainInvitationViewer> viewerFactory;

		public InvitationViewerFactory(ObjectFactory<MainInvitationViewer> editor)
		{
			this.viewerFactory = editor;
		}

		public MainInvitationViewer getViewer() throws EngineException
		{
			return viewerFactory.getObject();
		}
	}
}

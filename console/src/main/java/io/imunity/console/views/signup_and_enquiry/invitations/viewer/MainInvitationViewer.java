/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.invitations.viewer;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
public class MainInvitationViewer extends VerticalLayout
{
	private final MessageSource msg;
	private NativeLabel type;
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
		type = new NativeLabel();
		setMargin(false);
		setSpacing(false);
		FormLayout top = new FormLayout();
		top.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		top.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		top.addFormItem(type, msg.getMessage("InvitationViewer.type"));
		viewerContent = new VerticalLayout();
		viewerContent.setMargin(false);
		viewerContent.setSpacing(false);
		viewerContent.setPadding(false);
		add(top, viewerContent);
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
		type.setText(msg.getMessage("InvitationType." + itype.toString().toLowerCase()));
		viewers.get(itype).setInput(invitationWithCode);
		viewerContent.removeAll();
		viewerContent.add(viewers.get(itype).getComponent());
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

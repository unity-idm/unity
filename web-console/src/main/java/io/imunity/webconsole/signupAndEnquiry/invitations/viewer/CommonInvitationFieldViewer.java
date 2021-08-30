/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import java.util.Map;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;

@PrototypeComponent
class CommonInvitationFieldViewer extends CustomComponent
{
	private final MessageSource msg;

	private Label code;
	private Label expiration;
	private Label contactAddress;
	private Label channelId;
	private Label lastSentTime;
	private Label notificationsSent;
	private Link link;
	private Label messageParams;

	CommonInvitationFieldViewer(MessageSource msg)
	{
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		code = new Label();
		code.setCaption(msg.getMessage("InvitationViewer.code"));

		link = new Link();
		link.setCaption(msg.getMessage("InvitationViewer.link"));

		expiration = new Label();
		expiration.setCaption(msg.getMessage("InvitationViewer.expiration"));

		channelId = new Label();
		channelId.setCaption(msg.getMessage("InvitationViewer.channelId"));

		contactAddress = new Label();
		contactAddress.setCaption(msg.getMessage("InvitationViewer.contactAddress"));

		lastSentTime = new Label();
		lastSentTime.setCaption(msg.getMessage("InvitationViewer.lastSentTime"));

		notificationsSent = new Label();
		notificationsSent.setCaption(msg.getMessage("InvitationViewer.notificationsSent"));

		messageParams = new Label();
		messageParams.setWidth(100, Unit.PERCENTAGE);
		messageParams.setCaption(msg.getMessage("InvitationViewer.messageParams"));
		
		FormLayoutWithFixedCaptionWidth main = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		setCompositionRoot(main);
		main.addComponents(code, link, expiration, channelId, contactAddress, lastSentTime, notificationsSent, messageParams);
		main.setMargin(false);
	}

	void setInput(InvitationWithCode invitationWithCode, String channel, String linkUrl, Map<String, String> messageParamsValue)
	{
		code.setValue(invitationWithCode.getRegistrationCode());
		expiration.setValue(TimeUtil.formatStandardInstant(invitationWithCode.getInvitation().getExpiration()));
		contactAddress.setValue(invitationWithCode.getInvitation().getContactAddress());
		notificationsSent.setValue(String.valueOf(invitationWithCode.getNumberOfSends()));
		lastSentTime.setValue(invitationWithCode.getLastSentTime() != null
				? TimeUtil.formatStandardInstant(invitationWithCode.getLastSentTime())
				: "-");
		link.setTargetName("_blank");
		link.setResource(new ExternalResource(linkUrl));
		channelId.setValue(channel);
		
		messageParams.setVisible(!messageParamsValue.isEmpty());
		messageParams.setValue(messageParamsValue.toString());
	}

}

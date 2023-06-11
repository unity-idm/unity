/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;

@PrototypeComponent
class CommonInvitationFieldViewer extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CommonInvitationFieldViewer.class);
	
	private final MessageSource msg;
	private final AttributeSupport attributeSupport;

	private Label creationTime;
	private Label code;
	private Label expiration;
	private Label contactAddress;
	private Label inviter;
	private Label channelId;
	private Label lastSentTime;
	private Label notificationsSent;
	private Link link;
	private Label messageParams;

	CommonInvitationFieldViewer(MessageSource msg, AttributeSupport attributeSupport)
	{
		this.msg = msg;
		this.attributeSupport = attributeSupport;
		initUI();
	}

	private void initUI()
	{
		creationTime = new Label();
		creationTime.setCaption(msg.getMessage("InvitationViewer.creationTime"));

		
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

		inviter = new Label();
		inviter.setCaption(msg.getMessage("InvitationViewer.inviter"));

		lastSentTime = new Label();
		lastSentTime.setCaption(msg.getMessage("InvitationViewer.lastSentTime"));

		notificationsSent = new Label();
		notificationsSent.setCaption(msg.getMessage("InvitationViewer.notificationsSent"));

		messageParams = new Label();
		messageParams.setWidth(100, Unit.PERCENTAGE);
		messageParams.setCaption(msg.getMessage("InvitationViewer.messageParams"));

		FormLayoutWithFixedCaptionWidth main = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		setCompositionRoot(main);
		main.addComponents(creationTime, code, link, expiration, channelId, contactAddress, inviter, lastSentTime, notificationsSent,
				messageParams);
		main.setMargin(false);
	}

	void setInput(InvitationWithCode invitationWithCode, String channel, String linkUrl,
			Map<String, String> messageParamsValue)
	{
		code.setValue(invitationWithCode.getRegistrationCode());
		creationTime.setValue(invitationWithCode.getCreationTime() != null
				? TimeUtil.formatStandardInstant(invitationWithCode.getCreationTime())
				: "");
		expiration.setValue(TimeUtil.formatStandardInstant(invitationWithCode.getInvitation().getExpiration()));
		contactAddress.setValue(invitationWithCode.getInvitation().getContactAddress());
		inviter.setValue(invitationWithCode.getInvitation().getInviterEntity().isEmpty() ? ""
				: getInviterLabel(invitationWithCode.getInvitation().getInviterEntity().get()));

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

	private String getInviterLabel(Long inviterEntity)
	{
		String entityLabel = "[" + inviterEntity + "]";
		try
		{
			AttributeExt attributeByMetadata = attributeSupport.getAttributeByMetadata(new EntityParam(inviterEntity),
					"/", EntityNameMetadataProvider.NAME);
			if (attributeByMetadata != null && !attributeByMetadata.getValues().isEmpty())
			{
				entityLabel = attributeByMetadata.getValues().get(0) + " " + entityLabel;
			}
		} catch (EngineException e)
		{
			log.error("Can not get attribute", e);
		}
		return entityLabel;
	}
}

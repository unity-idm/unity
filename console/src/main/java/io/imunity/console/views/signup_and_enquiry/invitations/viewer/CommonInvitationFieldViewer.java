/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.viewer;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.NativeLabel;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

@PrototypeComponent
class CommonInvitationFieldViewer extends FormLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CommonInvitationFieldViewer.class);
	
	private final MessageSource msg;
	private final AttributeSupport attributeSupport;

	private NativeLabel creationTime;
	private NativeLabel code;
	private NativeLabel expiration;
	private NativeLabel contactAddress;
	private NativeLabel inviter;
	private NativeLabel channelId;
	private NativeLabel lastSentTime;
	private NativeLabel notificationsSent;
	private Anchor link;
	private NativeLabel messageParams;

	private FormItem messageParamFormItem;

	CommonInvitationFieldViewer(MessageSource msg, AttributeSupport attributeSupport)
	{
		this.msg = msg;
		this.attributeSupport = attributeSupport;
		initUI();
	}

	private void initUI()
	{
		setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		
		creationTime = new NativeLabel();
		addFormItem(creationTime, msg.getMessage("InvitationViewer.creationTime"));		
		code = new NativeLabel();
		addFormItem(code, msg.getMessage("InvitationViewer.code"));
	
		link = new Anchor();
		link.setText(msg.getMessage("InvitationViewer.link"));
		addFormItem(link, "");

		expiration = new NativeLabel();
		addFormItem(expiration, msg.getMessage("InvitationViewer.expiration"));

		channelId = new NativeLabel();
		addFormItem(channelId, msg.getMessage("InvitationViewer.channelId"));

		contactAddress = new NativeLabel();
		addFormItem(contactAddress, msg.getMessage("InvitationViewer.contactAddress"));

		inviter = new NativeLabel();
		addFormItem(inviter, msg.getMessage("InvitationViewer.inviter"));

		lastSentTime = new NativeLabel();
		addFormItem(lastSentTime, msg.getMessage("InvitationViewer.lastSentTime"));

		notificationsSent = new NativeLabel();
		addFormItem(notificationsSent, msg.getMessage("InvitationViewer.notificationsSent"));

		messageParams = new NativeLabel();
		messageParamFormItem = addFormItem(messageParams, msg.getMessage("InvitationViewer.messageParams"));
	}

	void setInput(InvitationWithCode invitationWithCode, String channel, String linkUrl,
			Map<String, String> messageParamsValue)
	{
		code.setText(invitationWithCode.getRegistrationCode());
		creationTime.setText(invitationWithCode.getCreationTime() != null
				? TimeUtil.formatStandardInstant(invitationWithCode.getCreationTime())
				: "");
		expiration.setText(TimeUtil.formatStandardInstant(invitationWithCode.getInvitation().getExpiration()));
		contactAddress.setText(invitationWithCode.getInvitation().getContactAddress());
		inviter.setText(invitationWithCode.getInvitation().getInviterEntity().isEmpty() ? ""
				: getInviterLabel(invitationWithCode.getInvitation().getInviterEntity().get()));

		notificationsSent.setText(String.valueOf(invitationWithCode.getNumberOfSends()));
		lastSentTime.setText(invitationWithCode.getLastSentTime() != null
				? TimeUtil.formatStandardInstant(invitationWithCode.getLastSentTime())
				: "-");
		link.setHref(linkUrl);
		channelId.setText(channel);

		messageParamFormItem.setVisible(!messageParamsValue.isEmpty());
		messageParams.setText(messageParamsValue.toString());
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

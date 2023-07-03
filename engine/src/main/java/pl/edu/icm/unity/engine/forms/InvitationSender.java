/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.forms;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.reg.BaseRegistrationTemplateDef;
import pl.edu.icm.unity.base.msg_template.reg.InvitationTemplateDef;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.store.api.GroupDAO;

@Component
class InvitationSender
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, InvitationSender.class);

	private final GroupDAO groupDao;
	private final NotificationProducer notificationProducer;
	private final MessageSource msg;

	InvitationSender(GroupDAO groupDao, NotificationProducer notificationProducer,
			MessageSource msg)
	{
		this.groupDao = groupDao;
		this.notificationProducer = notificationProducer;
		this.msg = msg;
	}

	void sendInvitation(ResolvedInvitationSendData invitationToSend) throws EngineException
	{
		if (invitationToSend.sendData.contactAddress == null)
			throw new WrongArgumentException("The invitation has no contact address configured");
		
		if (invitationToSend.invitationTemplate == null)
			throw new WrongArgumentException(
					"The form of the invitation has no invitation message " + "template configured");

		String userLocale = msg.getDefaultLocaleCode();
		Map<String, String> notifyParams = new HashMap<>();
		notifyParams.put(BaseRegistrationTemplateDef.FORM_NAME,
				invitationToSend.formDisplayedName);
		notifyParams.put(InvitationTemplateDef.CODE, invitationToSend.code);
		notifyParams.put(InvitationTemplateDef.URL, invitationToSend.url);
		ZonedDateTime expiry = invitationToSend.sendData.expiration.atZone(ZoneId.systemDefault());
		notifyParams.put(InvitationTemplateDef.EXPIRES, expiry.format(DateTimeFormatter.RFC_1123_DATE_TIME));
		notifyParams.put(InvitationTemplateDef.PREFILLED_GROUPS, getPrefilledGroups(invitationToSend.sendData.groupSelections));

		notifyParams.putAll(invitationToSend.sendData.messageParams);

		notificationProducer.sendNotification(invitationToSend.sendData.contactAddress,
				invitationToSend.invitationTemplate, notifyParams, userLocale);
	}

	private String getPrefilledGroups(Map<Integer, PrefilledEntry<GroupSelection>> groups)
	{
		if (groups == null || groups.isEmpty())
			return "";

		Set<Group> onlyChildren = Group.getOnlyChildrenOfSet(
				groups.values().stream().map(prefilledEntry -> prefilledEntry.getEntry().getSelectedGroups())
						.flatMap(List::stream).map(group -> new Group(group)).collect(Collectors.toSet()));
		return onlyChildren.stream().map(group -> getGroupDisplayedName(group.getPathEncoded()))
				.collect(Collectors.joining(", "));
	}
	
	private String getGroupDisplayedName(String group)
	{
		try
		{
			return groupDao.get(group).getDisplayedName().getValue(msg);
		} catch (Exception e)
		{
			log.error("Can not get group", e);
			return group;
		}
	}

}

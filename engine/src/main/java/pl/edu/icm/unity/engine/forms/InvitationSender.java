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

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.reg.BaseRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationTemplateDef;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

@Component
class InvitationSender implements pl.edu.icm.unity.types.registration.invite.InvitationSender
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, InvitationSender.class);

	private final RegistrationFormDB registrationDB;
	private final EnquiryFormDB enquiryDB;
	private final GroupDAO groupDao;
	private final NotificationProducer notificationProducer;
	private final MessageSource msg;
	private final SharedEndpointManagement sharedEndpointMan;

	public InvitationSender(RegistrationFormDB registrationDB, EnquiryFormDB enquiryDB, GroupDAO groupDao,
			NotificationProducer notificationProducer, MessageSource msg, SharedEndpointManagement sharedEndpointMan)
	{
		this.registrationDB = registrationDB;
		this.enquiryDB = enquiryDB;
		this.groupDao = groupDao;
		this.notificationProducer = notificationProducer;
		this.msg = msg;
		this.sharedEndpointMan = sharedEndpointMan;
	}

	@Override
	public void send(RegistrationInvitationParam registrationInvitationParam, String code) throws EngineException
	{
		RegistrationForm form = registrationDB.get(registrationInvitationParam.getFormPrefill().getFormId());
		sendInvitation(form, registrationInvitationParam, registrationInvitationParam.getFormPrefill(),
				PublicRegistrationURLSupport.getPublicRegistrationLink(form, code, sharedEndpointMan), code);
	}

	@Override
	public void send(EnquiryInvitationParam enquiryInvitationParam, String code) throws EngineException
	{
		if (enquiryInvitationParam.getEntity() == null)
			throw new WrongArgumentException("The invitation has no entity configured");

		EnquiryForm form = enquiryDB.get(enquiryInvitationParam.getFormPrefill().getFormId());
		sendInvitation(form, enquiryInvitationParam, enquiryInvitationParam.getFormPrefill(),
				PublicRegistrationURLSupport.getPublicEnquiryLink(form, code, sharedEndpointMan), code);
	}

	private void sendInvitation(BaseForm form, InvitationParam invitationParam, FormPrefill prefilledInfo,
			String url, String code) throws EngineException
	{
		if (invitationParam.getContactAddress() == null)
			throw new WrongArgumentException("The invitation has no contact address configured");
		if (form.getNotificationsConfiguration().getInvitationTemplate() == null)
			throw new WrongArgumentException(
					"The form of the invitation has no invitation message " + "template configured");

		String userLocale = msg.getDefaultLocaleCode();
		Map<String, String> notifyParams = new HashMap<>();
		notifyParams.put(BaseRegistrationTemplateDef.FORM_NAME,
				form.getDisplayedName().getValue(userLocale, msg.getDefaultLocaleCode()));
		notifyParams.put(InvitationTemplateDef.CODE, code);
		notifyParams.put(InvitationTemplateDef.URL, url);
		ZonedDateTime expiry = invitationParam.getExpiration().atZone(ZoneId.systemDefault());
		notifyParams.put(InvitationTemplateDef.EXPIRES, expiry.format(DateTimeFormatter.RFC_1123_DATE_TIME));
		notifyParams.put(InvitationTemplateDef.PREFILLED_GROUPS,
				getPrefilledGroups(prefilledInfo.getGroupSelections()));

		notifyParams.putAll(prefilledInfo.getMessageParams());

		notificationProducer.sendNotification(invitationParam.getContactAddress(),
				form.getNotificationsConfiguration().getInvitationTemplate(), notifyParams, userLocale);
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

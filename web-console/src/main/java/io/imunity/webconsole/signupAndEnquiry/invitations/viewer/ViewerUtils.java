/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;

@Component
class ViewerUtils
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ViewerUtils.class);

	private final RegistrationsManagement regMan;
	private final EnquiryManagement enqMan;
	private final MessageTemplateManagement msgTemplateMan;

	ViewerUtils(RegistrationsManagement regMan, EnquiryManagement enqMan, MessageTemplateManagement msgTemplateMan)
	{

		this.regMan = regMan;
		this.enqMan = enqMan;
		this.msgTemplateMan = msgTemplateMan;
	}

	BaseForm getRegistrationForm(String id)
	{
		try
		{
			return regMan.getForm(id);
		} catch (EngineException e)
		{
			log.warn("Unable to list registration forms for invitations", e);
			return null;
		}
	}

	BaseForm getEnquiryForm(String id)
	{
		List<EnquiryForm> forms;
		try
		{
			forms = enqMan.getEnquires();
		} catch (EngineException e)
		{
			log.warn("Unable to list enquiry forms for invitations", e);
			return null;
		}
		Optional<EnquiryForm> found = forms.stream().filter(form -> form.getName().equals(id)).findAny();
		return found.orElse(null);
	}

	String getChannel(BaseForm form)
	{
		try
		{
			return msgTemplateMan.getTemplate(form.getNotificationsConfiguration().getInvitationTemplate())
					.getNotificationChannel();

		} catch (Exception e)
		{
			return "";
		}
	}
}

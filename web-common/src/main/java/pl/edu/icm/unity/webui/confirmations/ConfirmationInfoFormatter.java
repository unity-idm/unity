/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.confirmations;

import java.sql.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Helper to generate string with confirmation info
 * @author P.Piernik
 *
 */
@Component
public class ConfirmationInfoFormatter
{
	@Autowired
	private UnityMessageSource msg;
	
	public String getConfirmationStatusString(ConfirmationInfo cdata)
	{
		return getConfirmationStatusString(cdata, "ConfirmationInfo");
	}
	
	public String getSimpleConfirmationStatusString(ConfirmationInfo cdata)
	{
		return getConfirmationStatusString(cdata, "SimpleConfirmationInfo");
	}
	
	private String getConfirmationStatusString(ConfirmationInfo cdata, String msgPrefix)
	{
		StringBuilder rep = new StringBuilder();
		if (cdata != null)
		{
			rep.append(" ");
			if (cdata.isConfirmed())
			{
				rep.append(msg.getMessage(msgPrefix +".confirmed",
						new Date(cdata.getConfirmationDate())));
			} else
			{
				if (cdata.getSentRequestAmount() == 0)
					rep.append(msg.getMessage(msgPrefix + ".unconfirmed"));
				else
					rep.append(msg.getMessage(msgPrefix + ".unconfirmedWithRequests",
							cdata.getSentRequestAmount()));
			}
		}
		return rep.toString();
	}
}

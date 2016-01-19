/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration.invite;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.msgtemplates.MessageTemplateVariable;

/**
 * Template definition of a message send with an invitation to fill a registration request. 
 * @author Krzysztof Benedyczak
 */
@Component
public class InvitationTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "InvitationWithCode";
	public static final String FORM_NAME = "formName";
	public static final String CODE = "code";
	public static final String URL = "url";
	
	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.InvitationWithCode.desc";
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public Map<String, MessageTemplateVariable> getVariables()
	{
		Map<String, MessageTemplateVariable> vars = new HashMap<>();
		vars.put(FORM_NAME, new MessageTemplateVariable(FORM_NAME , 
				"MessageTemplateConsumer.BaseForm.var.formName", false));
		vars.put(CODE, new MessageTemplateVariable(CODE, 
				"MessageTemplateConsumer.InvitationWithCode.var.code", false));
		vars.put(URL, new MessageTemplateVariable(URL, 
				"MessageTemplateConsumer.InvitationWithCode.var.url", false));
		return vars;
	}

}

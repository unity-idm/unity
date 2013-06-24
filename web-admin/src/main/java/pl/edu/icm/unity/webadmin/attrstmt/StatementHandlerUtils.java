/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Utilities useful for various statement handlers.
 * @author K. Benedyczak
 */
public class StatementHandlerUtils
{
	/**
	 * How many chars should be used as a limit for text attribute representation
	 */
	public static final int ATTR_LEN = 50;
	
	/**
	 * @param msg
	 * @param handlersReg
	 * @param as
	 * @return a string of the form 'assign ATTRIBUTE to '
	 */
	public static StringBuilder getAssignedAttributeText(UnityMessageSource msg, AttributeHandlerRegistry handlersReg,
			AttributeStatement as)
	{
		StringBuilder sb = new StringBuilder(256);
		Attribute<?> assigned = as.getAssignedAttribute();
		sb.append(msg.getMessage("AttributeStatements.assign")).append(" ");
		sb.append(handlersReg.getSimplifiedAttributeRepresentation(assigned, ATTR_LEN));
		sb.append(" ").append(msg.getMessage("AttributeStatements.to")).append(" ");
		return sb;
	}

	/**
	 * @param msg
	 * @param handlersReg
	 * @param as
	 * @param stmtMsgKey
	 * @return a string of the form 'assign ATTRIBUTE to CONDITION CONDITION-ATTR in group GROUP'
	 */
	public static String getTextRepresentation(UnityMessageSource msg, AttributeHandlerRegistry handlersReg,
			AttributeStatement as, String stmtMsgKey)
	{
		StringBuilder sb = getAssignedAttributeText(msg, handlersReg, as);

		Attribute<?> a = as.getConditionAttribute();
		String condAttrStr = handlersReg.getSimplifiedAttributeRepresentation(a, ATTR_LEN);
		sb.append(msg.getMessage(stmtMsgKey)).append(" ").append(condAttrStr);
		sb.append(" ").append(msg.getMessage("AttributeStatements.inGroup")).append(" ");
		sb.append(a.getGroupPath());
		return sb.toString();
	}
}

/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.msgtemplates;

import java.util.Map;

/**
 * Message template definition. The implementation defines the contract between the template and the 
 * code using the template. In particular all the variables which are supported by the code using the 
 * template are defined in the implementation. 
 * 
 * @author P. Piernik
 */
public interface MessageTemplateDefinition
{
	/**
	 * @return message bundle key with the description of the template definition, with information on
	 * the purpose of the messages created with this template.
	 */
	public String getDescriptionKey();

	/**
	 * @return unique name of the message template definition
	 */
	public String getName();

	/**
	 * @return map of variables supported by this template consumer. The map keys are variable names. The values
	 * are the keys in the message bundle with descriptions.  
	 */
	public Map<String, String> getVariables();
}

/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.msgtemplates;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.notifications.FacilityName;

/**
 * Message template definition. The implementation defines the contract between the template and the 
 * code using the template. In particular all the variables which are supported by the code using the 
 * template are defined in the implementation. 
 * 
 * @author P. Piernik
 */
public interface MessageTemplateDefinition
{
	public static final Set<String> ALL_FACILITIES = Collections.unmodifiableSet(Sets
			.newHashSet(FacilityName.EMAIL.toString(), FacilityName.SMS.toString()));

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
	public Map<String, MessageTemplateVariable> getVariables();
	
	
	/**
	 * @return set of supported facilities. Message from the template can be 
	 * sent only by notification channels which are using this facilities.  
	 */
	public Set<String> getCompatibleFacilities();
	
}

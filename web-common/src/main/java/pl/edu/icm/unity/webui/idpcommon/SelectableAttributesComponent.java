/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import java.util.Collection;
import java.util.Map;

import pl.edu.icm.unity.base.attribute.Attribute;

public interface SelectableAttributesComponent
{

	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	Collection<Attribute> getUserFilteredAttributes();

	/**
	 * @return collection of attributes with values hidden by the user.
	 */
	Map<String, Attribute> getHiddenAttributes();

	void setInitialState(Map<String, Attribute> savedState);

}
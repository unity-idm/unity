/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.authn.endpoints.authnlayout;

import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * 
 * @author P.Piernik
 *
 * @param <T>
 */
public interface ColumnElementWithValue<T> extends ColumnElement
{	
	void refresh();
	void validate() throws FormValidationException;
	void setValue(T state);
	T getValue();
}

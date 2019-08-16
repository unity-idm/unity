/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.authn.services.authnlayout;

import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * {@link ColumnElement} which can have value
 * @author P.Piernik
 *
 * @param <T>
 */
public interface ColumnElementWithValue<T> extends ColumnElement
{	
	void refresh();
	void validate() throws FormValidationException;
	void setValue(T state);
	void addValueChangeListener(Runnable valueChange);
	T getValue();
}

/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.EditorProvider;
import pl.edu.icm.unity.webui.common.composite.CompositeLayoutAdapter;

/**
 * Wrapper of the {@link ListOfEmbeddedElementsStub} as a standalone component.
 * Internally this is a simple proxy of the underlying {@link ListOfEmbeddedElementsStub} implementation.
 *  
 * @author K. Benedyczak
 */
public class ListOfEmbeddedElements<T> extends CompactFormLayout
{
	private ListOfEmbeddedElementsStub<T> stub;
	
	public ListOfEmbeddedElements(UnityMessageSource msg, EditorProvider<T> editorProvider,
			int min, int max, boolean showLine)
	{
		this(null, msg, editorProvider, min, max, showLine);
	}

	public ListOfEmbeddedElements(String caption, UnityMessageSource msg, EditorProvider<T> editorProvider,
			int min, int max, boolean showLine)
	{
		stub = new ListOfEmbeddedElementsStub<T>(msg, editorProvider, min, max, showLine);
		new CompositeLayoutAdapter(this, stub.getComponentsGroup());
		if (caption != null)
			setCaption(caption);
	}

	public void setValueChangeListener(Runnable listener)
	{
		stub.setValueChangeListener(listener);
	}
	
	/**
	 * Sets label which is displayed before the button to add the <b>first</b> value.
	 * By default this label is empty.
	 * @param label
	 */
	public void setLonelyLabel(String label)
	{
		stub.setLonelyLabel(label);
	}
	
	public void setEntries(Collection<T> values)
	{
		stub.setEntries(values);
	}
	
	public void addEntry(T value, ListOfEmbeddedElementsStub<T>.Entry after)
	{
		stub.addEntry(value, after);
	}
	
	public void clearContents()
	{
		stub.clearContents();
	}
	
	public void resetContents()
	{
		stub.resetContents();
	}
	
	public List<T> getElements() throws FormValidationException
	{
		return stub.getElements();
	}
	
	public void refresh()
	{
		stub.refresh();
	}
}

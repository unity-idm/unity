/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.ListOfElementsStub.EditHandler;
import pl.edu.icm.unity.webui.common.ListOfElementsStub.LabelConverter;
import pl.edu.icm.unity.webui.common.ListOfElementsStub.RemoveHandler;

import com.vaadin.ui.VerticalLayout;

/**
 * Component maintaining a list of values. The values are displayed as labels. Additionally it is possible 
 * to have a remove and edit buttons with custom action handlers for each entry.
 * 
 * @author K. Benedyczak
 */
public class ListOfElements<T> extends VerticalLayout
{
	private ListOfElementsStub<T> stub;
	
	public ListOfElements(UnityMessageSource msg, LabelConverter<T> labelConverter)
	{
		stub = new ListOfElementsStub<T>(msg, this, labelConverter); 
		this.setId("ListOfElements");
	}
	
	public ListOfElements(UnityMessageSource msg)
	{
		stub = new ListOfElementsStub<T>(msg, this); 
		this.setId("ListOfElements");
	}
	
	public void setEditHandler(EditHandler<T> editHandler)
	{
		stub.setEditHandler(editHandler);
	}

	public boolean isAddSeparatorLine()
	{
		return stub.isAddSeparatorLine();
	}

	public void setAddSeparatorLine(boolean addSeparatorLine)
	{
		stub.setAddSeparatorLine(addSeparatorLine);
	}

	public void setRemoveHandler(RemoveHandler<T> removeHandler)
	{
		stub.setRemoveHandler(removeHandler);
	}

	public void addEntry(T entry)
	{
		stub.addEntry(entry);
	}
	
	public void clearContents()
	{
		stub.clearContents();
	}
	
	public List<T> getElements()
	{
		return stub.getElements();
	}
	
	public int size()
	{
		return stub.size();
	}
}

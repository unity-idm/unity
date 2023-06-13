/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities;

import java.util.Collection;

import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.EditorProvider;
import pl.edu.icm.unity.webui.common.composite.ComponentsGroup;

/**
 * Editor component allowing to edit identities of a fixed {@link IdentityType}.
 * For smooth integration this is not Vaadin's component but it can be added to an existing layout. 
 * @author K. Benedyczak
 */
public class SingleTypeIdentityEditor 
{
	private IdentityType idType;
	private IdentityEditorRegistryV8 idEdRegistry;
	private MessageSource msg;
	private String userFriendlyName;
	private ListOfEmbeddedElementsStub<IdentityParam> componentsList;

	public SingleTypeIdentityEditor(IdentityType idType, Collection<Identity> initial,
	                                IdentityEditorRegistryV8 idEdRegistry, MessageSource msg,
	                                IdentityTypeSupport idTypeSupport)
	{
		this.idType = idType;
		this.idEdRegistry = idEdRegistry;
		this.msg = msg;
		this.userFriendlyName = idTypeSupport.getTypeDefinition(idType.getIdentityTypeProvider()).
				getHumanFriendlyName(msg);

		initUI(initial);
	}
	
	private void initUI(Collection<Identity> initial)
	{
		int min = Math.min(initial.size(), idType.getMinInstances());
		componentsList = new ListOfEmbeddedElementsStub<>(
				msg, new IdentityEditorProvider(), 
				min, idType.getMaxInstances(), false);
		componentsList.setEntries(initial);
		componentsList.setLonelyLabel(userFriendlyName + ":");
	}
	
	public ComponentsGroup getComponentsGroup()
	{
		return componentsList.getComponentsGroup();
	}
	
	public void removeAll()
	{
		componentsList.clearContents();
	}
	
	public Collection<IdentityParam> getIdentities() throws FormValidationException
	{
		return componentsList.getElements();
	}
	
	public IdentityType getType()
	{
		return idType;
	}
	
	private class IdentityEditorProvider implements EditorProvider<IdentityParam>
	{
		@Override
		public Editor<IdentityParam> getEditor()
		{
			return new IdentityEditorWrapper();
		}
	}
	
	private class IdentityEditorWrapper implements Editor<IdentityParam>
	{
		private IdentityEditor editor;

		@Override
		public ComponentsContainer getEditorComponent(IdentityParam value, int position)
		{
			editor = idEdRegistry.getEditor(idType.getIdentityTypeProvider());
			ComponentsContainer ret = editor.getEditor(IdentityEditorContext.builder()
					.withRequired(true).build());
			if (value != null)
				editor.setDefaultValue(value);
			return ret;
		}

		@Override
		public void setEditedComponentPosition(int position)
		{
			editor.setLabel(esablishLabel(position));
		}

		private String esablishLabel(int position)
		{
			return (position > 0) ? userFriendlyName +" (" + (position+1) +"):" 
					: userFriendlyName + ":";
		}

		@Override
		public IdentityParam getValue() throws FormValidationException
		{
			try
			{
				return editor.getValue();
			} catch (IllegalIdentityValueException e)
			{
				throw new FormValidationException(e);
			}
		}
	}
}

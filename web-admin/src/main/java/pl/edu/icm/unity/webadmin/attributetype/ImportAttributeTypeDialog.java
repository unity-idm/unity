/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.Set;

import com.vaadin.ui.Component;

import io.imunity.webadmin.attributetype.ImportAttributeTypeEditor;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Allows import attribute types from json file
 * 
 * @author P.Piernik
 *
 */
public class ImportAttributeTypeDialog extends AbstractDialog
{

	private UnityServerConfiguration serverConfig;
	private AttributeTypeManagement attrTypeMan;
	private Runnable callback;
	private AttributeTypeSupport attrTypeSupport;

	private ImportAttributeTypeEditor editor;

	public ImportAttributeTypeDialog(UnityMessageSource msg, String caption, UnityServerConfiguration serverConfig,
			AttributeTypeManagement attrTypeMan, AttributeTypeSupport attrTypeSupport, Runnable callback)
	{
		super(msg, caption);
		this.serverConfig = serverConfig;
		this.callback = callback;
		this.attrTypeMan = attrTypeMan;
		this.attrTypeSupport = attrTypeSupport;
	}

	@Override
	protected Component getContents() throws Exception
	{

		editor = new ImportAttributeTypeEditor(msg, attrTypeMan.getAttributeTypes(), serverConfig,
				attrTypeSupport);

		return editor;
	}

	@Override
	protected void onCancel()
	{
		editor.clear();
		close();
	}

	@Override
	protected void onConfirm()
	{
		mergeAttributeTypes(editor.getAttributeTypes(), editor.isOverwriteMode());
		editor.clear();
		callback.run();
		close();
	}

	private void mergeAttributeTypes(Set<AttributeType> toMerge, boolean overwrite)
	{

		try
		{
			Set<String> existing = attrTypeMan.getAttributeTypesAsMap().keySet();
			for (AttributeType at : toMerge)
			{

				if (!existing.contains(at.getName()))
				{
					attrTypeMan.addAttributeType(at);
				} else if (overwrite)
				{
					attrTypeMan.updateAttributeType(at);
				}
			}
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("ImportAttributeTypes.errorImport"), e);
		}
	}
}

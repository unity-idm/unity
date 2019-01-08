/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webadmin.attribute.AttributeFieldWithEdit;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Shows a multi-add attribute dialog. The class is used to define attributes which are required.
 * 
 * @author K. Benedyczak
 */
public class RequiredAttributesDialog extends AbstractDialog
{
	private UnityMessageSource msg;
	private Set<String> missingAttributes;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private Collection<AttributeType> attributeTypes;
	private String group;
	private Callback callback;
	
	private List<AttributeFieldWithEdit> attrEdits;
	private String info;

	public RequiredAttributesDialog(UnityMessageSource msg, String info, 
			Set<String> missingAttributes, AttributeHandlerRegistry attrHandlerRegistry,
			Collection<AttributeType> attributeTypes, String group, Callback callback)
	{
		super(msg, msg.getMessage("RequiredAttributesDialog.caption"));
		this.msg = msg;
		this.missingAttributes = missingAttributes;
		this.info = info;
		this.group = group;
		this.attributeTypes = attributeTypes;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		Label infoL = new Label(info);
		infoL.setStyleName(Styles.bold.toString());
		Label spacer = new Label(" ");
		main.addComponents(infoL, spacer);
		
		Map<String, AttributeType> typesMap = new HashMap<String, AttributeType>();
		for (AttributeType at: attributeTypes)
			typesMap.put(at.getName(), at);
		attrEdits = new ArrayList<>(missingAttributes.size());
		for (String a: missingAttributes)
		{
			AttributeFieldWithEdit aEdit = new AttributeFieldWithEdit(msg, a, 
					attrHandlerRegistry, attributeTypes, group, null, true);
			aEdit.setFixedType(typesMap.get(a));
			attrEdits.add(aEdit);
			main.addComponent(aEdit);
		}
		
		return main;
	}
	
	@Override
	protected void onCancel()
	{
		callback.onCancel();
		super.onCancel();
	}
	
	@Override
	protected void onConfirm()
	{
		List<Attribute> ret = new ArrayList<>(attrEdits.size());
		for (AttributeFieldWithEdit aEdit: attrEdits)
		{
			Attribute a;
			try
			{
				a = aEdit.getAttribute();
			} catch (FormValidationException e)
			{
				NotificationPopup.showError(msg.getMessage("Generic.formError"),
						msg.getMessage("RequiredAttributesDialog.someAttributesUnset"));
				return;
			}
			ret.add(a);
		}
		callback.onConfirm(ret);
		close();
	}
	
	public interface Callback
	{
		public void onConfirm(List<Attribute> attributes);
		public void onCancel();
	}
}

/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tokens;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.CompactFormLayout;

/**
 * Simple component allowing to view single token
 * 
 * @author P.Piernik
 *
 */
public class TokenViewer extends VerticalLayout
{
	private UnityMessageSource msg;

	private AttributeSupport attrProcessor;
	private AttributesManagement attrMan;
	
	private Label type;
	private Label value;
	private Label exp;
	private Label createTime;
	private FormLayout main;
	private Label ids;

	private boolean showId;

	public TokenViewer(UnityMessageSource msg, AttributeSupport attrProcessor,
			AttributesManagement attrMan)
	{
		this.msg = msg;
		this.attrMan = attrMan;
		this.attrProcessor = attrProcessor;
		initUI();
	}

	private void initUI()
	{
		main = new CompactFormLayout();

		type = new Label();
		type.setCaption(msg.getMessage("TokenViewer.type"));

		value = new Label();
		value.setCaption(msg.getMessage("TokenViewer.value"));

		exp = new Label();
		exp.setCaption(msg.getMessage("TokenViewer.expires"));

		createTime = new Label();
		createTime.setCaption(msg.getMessage("TokenViewer.create"));
		
		ids = new Label();
		ids.setCaption(msg.getMessage("TokenViewer.indentities"));
		
		main.addComponents(ids, type, value, createTime, exp);
		main.setSizeFull();
		
		addComponent(main);
		setSizeFull();
		setShowId(true);

	}
	
	public void setShowId(boolean showId)
	{
		this.showId = showId;
		ids.setVisible(showId);
	}

	public void setInput(Token token)
	{
		if (token == null)
		{
			type.setValue("");
			value.setValue("");
			exp.setValue("");
			main.setVisible(false);
			ids.setValue("");
			return;
		}
		
		
		try
		{
			type.setValue(msg.getMessage("TokenType." + token.getType()));
		} catch (Exception e)
		{
			type.setValue(token.getType());
		}
		
		value.setValue(token.getValue());
		exp.setValue(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT)
				.format(token.getExpires()));
		createTime.setValue(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT)
				.format(token.getCreated()));

		if (showId)
		{
			long ownerId = token.getOwner();
			String idLabel = "[" + ownerId + "]";
			String attrNameValue = getAttrNameValue(ownerId);
			if (attrNameValue != null)
				idLabel = idLabel + " " + attrNameValue;

			ids.setValue(idLabel);
			ids.setVisible(true);
		}else
		{
			ids.setVisible(false);
			
		}

		main.setVisible(true);
	}

	private String getAttrNameValue(long owner)
	{
		String idLabel = null;
		try
		{
			AttributeType nameAt = attrProcessor.getAttributeTypeWithSingeltonMetadata(
					EntityNameMetadataProvider.NAME);
			String attrToLabel = nameAt == null ? null : nameAt.getName();
			Collection<AttributeExt> rootAttrs = new ArrayList<AttributeExt>();
			rootAttrs = attrMan.getAllAttributes(new EntityParam(owner), true, "/",
					null, true);
			if (attrToLabel != null)
				for (AttributeExt at : rootAttrs)
				{
					if (at.getName().equals(attrToLabel))
						idLabel = at.getValues().get(0);
				}
		} catch (EngineException e)
		{
			// ok only user id is present in label
		}
		return idLabel;
	}

}

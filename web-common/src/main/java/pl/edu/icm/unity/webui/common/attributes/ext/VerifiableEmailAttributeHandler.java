/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

import com.vaadin.server.Resource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

/**
 * VerifiableEmail attribute handler for the web
 * @author P. Piernik
 */
@org.springframework.stereotype.Component
public class VerifiableEmailAttributeHandler implements WebAttributeHandler<VerifiableEmail>, WebAttributeHandlerFactory
{

	private UnityMessageSource msg;
	
	@Autowired
	public VerifiableEmailAttributeHandler(UnityMessageSource msg)
	{
		this.msg = msg;
	}
	

	@Override
	public String getSupportedSyntaxId()
	{
		return VerifiableEmailAttributeSyntax.ID;
	}

	@Override
	public WebAttributeHandler<?> createInstance()
	{
		return new VerifiableEmailAttributeHandler(msg);
	}

	@Override
	public String getValueAsString(VerifiableEmail value,
			AttributeValueSyntax<VerifiableEmail> syntax, int limited)
	{
		return new String(syntax.serialize(value));
	}

	@Override
	public Resource getValueAsImage(VerifiableEmail value,
			AttributeValueSyntax<VerifiableEmail> syntax, int maxWidth, int maxHeight)
	{
		return null;
	}

	@Override
	public Component getRepresentation(VerifiableEmail value,
			AttributeValueSyntax<VerifiableEmail> syntax)
	{
		return new Label(value.getValue().toString(), ContentMode.PREFORMATTED);
	}

	@Override
	public AttributeValueEditor<VerifiableEmail> getEditorComponent(
			VerifiableEmail initialValue, String label,
			AttributeValueSyntax<VerifiableEmail> syntaxDesc)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Component getSyntaxViewer(AttributeValueSyntax<VerifiableEmail> syntax)
	{
		return new Label();
	}

	@Override
	public AttributeSyntaxEditor<VerifiableEmail> getSyntaxEditorComponent(
			AttributeValueSyntax<VerifiableEmail> initialValue)
	{
		// TODO Auto-generated method stub
		return null;
	}

	


}

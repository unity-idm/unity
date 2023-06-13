/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.attributes.ext;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementState;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.stdext.attr.PolicyAgreementAttributeSyntax;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ReadOnlyField;
import pl.edu.icm.unity.webui.common.attributes.*;

import java.util.Collections;
import java.util.List;

import static pl.edu.icm.unity.engine.api.utils.TimeUtil.formatStandardInstant;

public class PolicyAgreeementAttributeHandler extends TextOnlyAttributeHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PolicyAgreeementAttributeHandler.class);

	private PolicyDocumentManagement docMan;

	public PolicyAgreeementAttributeHandler(MessageSource msg, PolicyDocumentManagement docMan,
			AttributeValueSyntax<?> syntax)
	{
		super(msg, syntax);
		this.docMan = docMan;
	}

	@Override
	protected List<String> getHints()
	{
		return Collections.emptyList();
	}

	@Override
	public Component getRepresentation(String value, AttributeViewerContext context)
	{
		PolicyAgreementState state;
		PolicyDocumentWithRevision policyDocument;
		try
		{
			state = PolicyAgreementState.fromJson(value);
			policyDocument = docMan.getPolicyDocument(state.policyDocumentId);
		} catch (EngineException e)
		{
			log.error("Invalid value of policy agreement attribute:" + value);
			return super.getRepresentation(value, context);
		}

		String nvalue = msg.getMessage("PolicyAgreeementsAttributeHandler.representation", policyDocument.name,
				state.policyDocumentRevision,
				msg.getMessage("PolicyAgreementAcceptanceStatus." + state.acceptanceStatus),
				formatStandardInstant(state.decisionTs.toInstant()));

		Component component;
		if (!context.isShowAsLabel())
		{
			component = new ReadOnlyField(nvalue);
			if (context.isCustomWidth())
			{

				if (context.getCustomWidth() > 0)
				{
					component.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
				} else
				{
					component.setWidthUndefined();
				}
			}
			if (context.isCustomHeight())
			{
				if (context.getCustomHeight() > 0)
				{
					component.setHeight(context.getCustomHeight(), context.getCustomHeightUnit());
				}

				else
				{
					component.setHeightUndefined();
				}
			}

		} else
		{
			component = new Label(nvalue);
		}
		return component;

	}

	private static class PolicyAgreementSyntaxEditor implements AttributeSyntaxEditor<String>
	{
		@Override
		public Component getEditor()
		{
			return new CompactFormLayout();
		}

		@Override
		public AttributeValueSyntax<String> getCurrentValue() throws IllegalAttributeTypeException
		{
			return new PolicyAgreementAttributeSyntax();
		}
	}

	@org.springframework.stereotype.Component
	public static class PolicyAgreementAttributeHandlerFactoryV8 implements WebAttributeHandlerFactory
	{
		private MessageSource msg;
		private PolicyDocumentManagement docMan;

		@Autowired
		public PolicyAgreementAttributeHandlerFactoryV8(MessageSource msg, PolicyDocumentManagement docMan)
		{
			this.msg = msg;
			this.docMan = docMan;
		}

		@Override
		public String getSupportedSyntaxId()
		{
			return PolicyAgreementAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new PolicyAgreeementAttributeHandler(msg, docMan, syntax);
		}

		@Override
		public AttributeSyntaxEditor<String> getSyntaxEditorComponent(AttributeValueSyntax<?> initialValue)
		{
			return new PolicyAgreementSyntaxEditor();
		}
	}
}

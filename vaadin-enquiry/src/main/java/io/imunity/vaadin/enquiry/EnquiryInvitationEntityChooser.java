/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.enquiry;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import io.imunity.vaadin.endpoint.common.forms.ResolvedInvitationParam;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

import java.util.Optional;
import java.util.function.Consumer;

@PrototypeComponent
class EnquiryInvitationEntityChooser extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryInvitationEntityChooser.class);

	private final MessageSource msg;
	private final AttributeSupport attributeSupport;

	private RadioButtonGroup<Entity> entityChooser;
	private final EnquiryInvitationEntityRepresentationProvider entityRepresentationProvider;

	@Autowired
	public EnquiryInvitationEntityChooser(MessageSource msg, AttributeSupport attributeSupport)
	{
		this.msg = msg;
		this.attributeSupport = attributeSupport;
		this.entityRepresentationProvider = new EnquiryInvitationEntityRepresentationProvider(this::getDisplayName, msg);
	}

	public EnquiryInvitationEntityChooser init(ResolvedInvitationParam invitation, Consumer<Long> callback, Runnable cancel)
	{
		VerticalLayout main = new VerticalLayout();
		entityChooser = new RadioButtonGroup<>();
		entityChooser.setItems(invitation.entities);
		entityChooser.setItemLabelGenerator(entityRepresentationProvider::getEntityRepresentation);
		entityChooser.setValue(invitation.entities.iterator().next());
		Button confirm = new Button(msg.getMessage("EnquiryInvitationEntityChooser.proceed"));
		confirm.addClickListener(e -> callback.accept(entityChooser.getValue().getId()));
		confirm.addClassName("u-button-form");
		
		Button cancelB = new Button(msg.getMessage("cancel"));
		cancelB.addClickListener(e -> cancel.run());
		cancelB.addClassName("u-button-form");
		
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.add(cancelB);
		buttons.add(confirm);

		Span infoTitle = new Span(msg.getMessage("EnquiryInvitationEntityChooser.chooseEntityTitle", invitation.contactAddress));

		Span infoDesc = new Span(msg.getMessage("EnquiryInvitationEntityChooser.chooseEntityDescription"));

		main.add(infoTitle);
		main.add(infoDesc);
		main.add(entityChooser);
		main.add(buttons);
		main.setAlignItems(Alignment.CENTER);
		add(main);
		setWidth(40, Unit.EM);
		return this;
	}

	private Optional<String> getDisplayName(Long entity)
	{
		try
		{
			return attributeSupport.getAttributeValueByMetadata(new EntityParam(entity), "/",
					EntityNameMetadataProvider.NAME);
		} catch (EngineException e)
		{
			log.error("Failed to get entity {} display name", entity, e);
		}
		return Optional.empty();
	}

	@Component
	public static class InvitationEntityChooserComponentFactory
	{

		private final ObjectFactory<EnquiryInvitationEntityChooser> factory;

		public InvitationEntityChooserComponentFactory(ObjectFactory<EnquiryInvitationEntityChooser> factory)
		{
			this.factory = factory;
		}

		EnquiryInvitationEntityChooser get(ResolvedInvitationParam invitation, Consumer<Long> callback, Runnable cancel)
		{
			return factory.getObject().init(invitation, callback, cancel);
		}
	}

}
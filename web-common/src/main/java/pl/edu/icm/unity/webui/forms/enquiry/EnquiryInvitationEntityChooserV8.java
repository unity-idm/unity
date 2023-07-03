/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import com.vaadin.ui.*;
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
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

import java.util.Optional;
import java.util.function.Consumer;

@PrototypeComponent
class EnquiryInvitationEntityChooserV8 extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryInvitationEntityChooserV8.class);

	private final MessageSource msg;
	private final AttributeSupport attributeSupport;

	private RadioButtonGroup<Entity> entityChooser;
	private final EnquiryInvitationEntityRepresentationProvider entityRepresentationProvider;

	@Autowired
	public EnquiryInvitationEntityChooserV8(MessageSource msg, AttributeSupport attributeSupport)
	{
		this.msg = msg;
		this.attributeSupport = attributeSupport;
		this.entityRepresentationProvider = new EnquiryInvitationEntityRepresentationProvider(this::getDisplayName, msg);
	}

	public EnquiryInvitationEntityChooserV8 init(ResolvedInvitationParam invitation, Consumer<Long> callback, Runnable cancel)
	{
		VerticalLayout main = new VerticalLayout();
		entityChooser = new RadioButtonGroup<>();
		entityChooser.setItems(invitation.entities);
		entityChooser.setItemCaptionGenerator(e -> entityRepresentationProvider.getEntityRepresentation(e));
		entityChooser.setSelectedItem(invitation.entities.iterator().next());
		Button confirm = new Button(msg.getMessage("EnquiryInvitationEntityChooser.proceed"));
		confirm.setStyleName(Styles.buttonAction.toString());
		confirm.addClickListener(e -> callback.accept(entityChooser.getSelectedItem().get().getId()));
		confirm.addStyleName("u-button-form");
		
		Button cancelB = new Button(msg.getMessage("cancel"));
		cancelB.addClickListener(e -> cancel.run());
		cancelB.addStyleName("u-button-form");
		
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.addComponent(cancelB);
		buttons.addComponent(confirm);

		Label infoTitle = new Label(msg.getMessage("EnquiryInvitationEntityChooser.chooseEntityTitle", invitation.contactAddress));
		infoTitle.setCaptionAsHtml(true);
		infoTitle.addStyleName(Styles.vLabelH1.toString());
		infoTitle.addStyleName(Styles.wordWrap.toString());
		infoTitle.addStyleName(Styles.textCenter.toString());
		
		Label infoDesc = new Label(msg.getMessage("EnquiryInvitationEntityChooser.chooseEntityDescription"));
		infoDesc.setCaptionAsHtml(true);
		infoDesc.addStyleName(Styles.wordWrap.toString());
		infoDesc.addStyleName(Styles.textCenter.toString());

		main.addComponent(infoTitle);
		main.addComponent(infoDesc);
		main.addComponent(entityChooser);
		main.addComponent(buttons);
		main.setComponentAlignment(infoTitle, Alignment.MIDDLE_CENTER);
		main.setComponentAlignment(infoDesc, Alignment.MIDDLE_CENTER);
		main.setComponentAlignment(entityChooser, Alignment.MIDDLE_CENTER);
		main.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
		setCompositionRoot(main);
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

		private final ObjectFactory<EnquiryInvitationEntityChooserV8> factory;

		public InvitationEntityChooserComponentFactory(ObjectFactory<EnquiryInvitationEntityChooserV8> factory)
		{
			this.factory = factory;
		}

		EnquiryInvitationEntityChooserV8 get(ResolvedInvitationParam invitation, Consumer<Long> callback, Runnable cancel)
		{
			return factory.getObject().init(invitation, callback, cancel);
		}
	}

}
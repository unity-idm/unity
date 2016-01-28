/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.DateField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Edit UI for {@link InvitationParam}.
 * @author Krzysztof Benedyczak
 */
public class InvitationEditor extends CustomComponent
{
	public static final long DEFAULT_TTL = 1000 * 24 * 3600 * 3; //3 days
	private UnityMessageSource msg;
	private IdentityEditorRegistry identityEditorRegistry;
	private AttributeHandlerRegistry attrHandlersRegistry;
	private Map<String, RegistrationForm> formsByName;
	private Map<String, AttributeType> attrTypes;
	
	private ComboBox forms;
	private DateField expiration;
	private TextField contactAddress;
	private ComboBox channelId;
	
	private TabSheet tabs;
	private ListOfEmbeddedElements<PrefilledEntry<IdentityParam>> presetIdentities;
	private ListOfEmbeddedElements<PrefilledEntry<Selection>> presetGroups;
	private ListOfEmbeddedElements<PrefilledEntry<Attribute<?>>> presetAttributes;

	
	public InvitationEditor(UnityMessageSource msg, IdentityEditorRegistry identityEditorRegistry,
			AttributeHandlerRegistry attrHandlersRegistry,
			Collection<RegistrationForm> availableForms,
			Collection<String> channels,
			Map<String, AttributeType> attrTypes) throws WrongArgumentException
	{
		this.msg = msg;
		this.identityEditorRegistry = identityEditorRegistry;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.attrTypes = attrTypes;
		initUI(availableForms, channels);
	}

	private void initUI(Collection<RegistrationForm> availableForms,
			Collection<String> channels) throws WrongArgumentException
	{
		formsByName = new HashMap<>();
		forms = new ComboBox(msg.getMessage("InvitationViewer.formId"));
		forms.setNewItemsAllowed(false);
		forms.setNullSelectionAllowed(false);
		availableForms.stream()
			.filter(form -> form.getRegistrationCode() == null && form.isPubliclyAvailable())
			.forEach(form -> {
				forms.addItem(form.getName());
				formsByName.put(form.getName(), form);
			});
		forms.addValueChangeListener(event -> {
			setPerFormUI(formsByName.get(forms.getValue()));
		});
		
		if (forms.getItemIds().isEmpty())
			throw new WrongArgumentException("There are no public registration forms to create an invitation for.");
		
		expiration = new DateField(msg.getMessage("InvitationViewer.expiration"));
		expiration.setRequired(true);
		expiration.setResolution(Resolution.MINUTE);
		
		channelId = new ComboBox(msg.getMessage("InvitationViewer.channelId"));
		channelId.addItems(channels);
		
		contactAddress = new TextField(msg.getMessage("InvitationViewer.contactAddress"));
		
		Label prefillInfo = new Label(msg.getMessage("InvitationEditor.prefillInfo"));
		
		tabs = new TabSheet();
		
		FormLayout top = new FormLayout();
		top.addComponents(forms, expiration, channelId, contactAddress);
		
		VerticalLayout main = new VerticalLayout(top, prefillInfo, tabs);
		main.setSpacing(true);
		setCompositionRoot(main);
		setInitialData();
	}

	private void setPerFormUI(RegistrationForm form)
	{
		tabs.removeAllComponents();
		
		int idParamsNum = form.getIdentityParams() == null ? 0 : form.getIdentityParams().size();
		presetIdentities = new ListOfEmbeddedElements<>(msg, () ->
		{
			return new PresetIdentityEditor(identityEditorRegistry, form.getIdentityParams(), msg);
		}, idParamsNum, idParamsNum, true);
		presetIdentities.setCaption(msg.getMessage("InvitationEditor.identities"));
		if (idParamsNum > 0)
			addTabWithMargins(presetIdentities);
		
		int attrParamsNum = form.getAttributeParams() == null ? 0 : form.getAttributeParams().size();
		presetAttributes = new ListOfEmbeddedElements<>(msg, () ->
		{
			return new PresetAttributeEditor(msg, form.getAttributeParams(), attrHandlersRegistry,
					attrTypes);
		}, attrParamsNum, attrParamsNum, true);
		presetAttributes.setCaption(msg.getMessage("InvitationEditor.attributes"));
		if (attrParamsNum > 0)
			addTabWithMargins(presetAttributes);
		
		int groupParamsNum = form.getGroupParams() == null ? 0 : form.getGroupParams().size();
		presetGroups = new ListOfEmbeddedElements<>(msg, () ->
		{
			return new PresetMembershipEditor(msg, form.getGroupParams());
		}, groupParamsNum, groupParamsNum, true);
		presetGroups.setCaption(msg.getMessage("InvitationEditor.groups"));
		if (groupParamsNum > 0)
			addTabWithMargins(presetGroups);
	}
	
	private void addTabWithMargins(Component src)
	{
		VerticalLayout wrapper = new VerticalLayout(src);
		wrapper.setMargin(true);
		tabs.addTab(wrapper).setCaption(src.getCaption());
		src.setCaption("");
	}
	
	private void setInitialData()
	{
		forms.setValue(forms.getItemIds().iterator().next());
		if (!channelId.getItemIds().isEmpty())
			channelId.setValue(channelId.getItemIds().iterator().next());
		expiration.setValue(new Date(System.currentTimeMillis() + DEFAULT_TTL));
	}
	
	public InvitationParam getInvitation() throws FormValidationException
	{
		String addr = contactAddress.isEmpty() ? null : contactAddress.getValue();
		String channel = channelId.isEmpty() ? null : (String)channelId.getValue();
		InvitationParam ret = new InvitationParam((String)forms.getValue(), 
				Instant.ofEpochMilli(expiration.getValue().getTime()),
				addr,
				channel);
		
		prefill(presetIdentities.getElements(), ret.getIdentities());
		prefill(presetAttributes.getElements(), ret.getAttributes());
		prefill(presetGroups.getElements(), ret.getGroupSelections());
		return ret;
	}
	
	private <T> void prefill(List<PrefilledEntry<T>> input, Map<Integer, PrefilledEntry<T>> output)
	{
		for (int i=0; i<input.size(); i++)
		{
			PrefilledEntry<T> element = input.get(i);
			if (element != null)
				output.put(i, element);
		}
	}
}

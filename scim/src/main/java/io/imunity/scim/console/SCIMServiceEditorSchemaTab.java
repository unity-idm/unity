/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.server.StreamResource;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.console.EditSchemaSubView.EditSchemaSubViewFactory;
import io.imunity.scim.console.mapping.SchemaWithMappingBean;
import io.imunity.scim.schema.SchemaResourceDeserialaizer;
import io.imunity.vaadin.auth.services.ServiceEditorBase.EditorTab;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.exceptions.RuntimeEngineException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

class SCIMServiceEditorSchemaTab extends VerticalLayout implements EditorTab
{
	private final MessageSource msg;
	private final SubViewSwitcher subViewSwitcher;
	private final EditSchemaSubViewFactory editSchemaSubViewFactory;
	private final ConfigurationVaadinBeanMapper configurationVaadinBeanMapper;
	private final NotificationPresenter notificationPresenter;

	private SCIMServiceEditorSchemaTab(MessageSource msg, NotificationPresenter notificationPresenter, EditSchemaSubViewFactory editSchemaSubViewFactory,
			UnityServerConfiguration unityServerConfiguration, SubViewSwitcher subViewSwitcher,
			ConfigurationVaadinBeanMapper configurationVaadinBeanMapper)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		this.subViewSwitcher = subViewSwitcher;
		this.editSchemaSubViewFactory = editSchemaSubViewFactory;
		this.configurationVaadinBeanMapper = configurationVaadinBeanMapper;
	}

	void initUI(Binder<SCIMServiceConfigurationBean> configBinder)
	{
		VerticalLayout mainL = new VerticalLayout();
		mainL.setSizeFull();
		
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		MultiSelectComboBox<String> membershipAttributes = new CustomValuesMultiSelectComboBox();
		membershipAttributes.setWidth(TEXT_FIELD_BIG.value());
		membershipAttributes.setPlaceholder(msg.getMessage("typeOrSelect"));
		main.addFormItem(membershipAttributes, msg.getMessage("SCIMServiceEditorSchemaTab.membershipAttributes"));
		configBinder.forField(membershipAttributes)
				.withConverter(List::copyOf, HashSet::new)
				.bind("membershipAttributes");
		mainL.add(main);
		
		SchemasComponent schemas = new SchemasComponent();
		configBinder.forField(schemas).bind("schemas");
		mainL.add(schemas);

		schemas.addValueChangeListener(e ->
		{
			Set<String> attr = new HashSet<>();
			for (SchemaWithMappingBean schema : e.getValue())
			{
				if (schema == null)
					continue;
				schema.getAttributes().forEach(a -> attr.add(a.getAttributeDefinition().getName()));
			}
			membershipAttributes.setItems(attr);
		});
		add(mainL);
		setSizeFull();
		
	}

	@Override
	public String getType()
	{
		return ServiceEditorComponent.ServiceEditorTab.OTHER.toString();
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	private class SchemasComponent extends CustomField<List<SchemaWithMappingBean>>
	{
		@Override
		protected List<SchemaWithMappingBean> generateModelValue()
		{
			return schemasGrid.getElements();
		}

		@Override
		protected void setPresentationValue(List<SchemaWithMappingBean> newPresentationValue)
		{
			schemasGrid.setItems(newPresentationValue);
			
		}
		private GridWithActionColumn<SchemaWithMappingBean> schemasGrid;
		private VerticalLayout main;
		private FileBuffer fileBuffer;
		private Upload upload;
		public SchemasComponent()
		{
			initUI();
		}

		private void initUI()
		{
			main = new VerticalLayout();
			main.setSizeFull();
			main.setMargin(false);
			main.setPadding(false);		
			main.add(createHeaderActionLayout());
			schemasGrid = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
			schemasGrid.addComponentColumn(
					p ->
					{
						Button button = new Button(p.getId(), e -> gotoEdit(p));
						button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
						return button;
					})
							.setHeader(msg.getMessage("SCIMServiceEditorSchemaTab.schemaId"))
							.setAutoWidth(true);
			
			
			
			schemasGrid.addBooleanColumn(s -> s.isEnable()).setHeader(msg.getMessage("SCIMServiceEditorSchemaTab.enabled"));
			schemasGrid.addComponentColumn(
					s -> getMappingStatusLabel(!s.getType().equals(SchemaType.GROUP_CORE) && s.hasInvalidMappings())).setHeader(
					msg.getMessage("SCIMServiceEditorSchemaTab.mappingStatus")).setAutoWidth(true);
			schemasGrid.setWidthFull();
			main.add(schemasGrid);
			
			add(main);
			setSizeFull();
			setMargin(false);
			setPadding(false);
			
		}
		
		public HorizontalLayout createHeaderActionLayout()
		{
			HorizontalLayout headerLayout = new HorizontalLayout();
			headerLayout.setPadding(false);
			headerLayout.setMargin(false);
			headerLayout.setSpacing(true);
			headerLayout.setWidthFull();
			Button addButton = new Button(msg.getMessage("create"), e -> gotoNew());
			addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			addButton.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
			Button importButton = new Button(msg.getMessage("SCIMServiceEditorSchemaTab.import"));
			importButton.setIcon(VaadinIcon.DOWNLOAD.create());
			fileBuffer = new FileBuffer();
			upload = new Upload(fileBuffer);
			upload.setAcceptedFileTypes("application/json");
			upload.addFinishedListener(e -> importUserSchema());
			upload.getElement()
					.addEventListener("file-remove", e -> clear());
			upload.addFileRejectedListener(
					e -> notificationPresenter.showError(msg.getMessage("error"), e.getErrorMessage()));
			upload.setDropAllowed(false);
			upload.setVisible(true);
			upload.setUploadButton(importButton);
			headerLayout.add(upload);
			headerLayout.add(addButton);
			headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
			return headerLayout;
		}
		
		private Icon getMappingStatusLabel(boolean warn)
		{
			return new Icon(!warn ? VaadinIcon.CHECK_CIRCLE_O : VaadinIcon.EXCLAMATION_CIRCLE_O);
			
		}
		
		private void importUserSchema()
		{
			try
			{
				SchemaWithMappingBean schema;
				try
				{
					schema = configurationVaadinBeanMapper.mapFromConfigurationSchema(
							SchemaResourceDeserialaizer.deserializeUserSchemaFromFile(fileBuffer.getFileData().getFile()));
				} catch (EngineException | RuntimeEngineException e)
				{
					notificationPresenter.showError("", e.getMessage());
					upload.clearFileList();
					return;
				}
				upload.clearFileList();
				if (schemasGrid.getElements().stream().filter(s -> s.getId().equals(schema.getId())).findAny()
						.isPresent())
				{
					notificationPresenter.showError("", msg.getMessage("SCIMServiceEditorSchemaTab.schemaExistError", schema.getId()));
					upload.clearFileList();
					return;
				}

				schemasGrid.addElement(schema);
			} catch (IOException e)
			{
				upload.clearFileList();
				notificationPresenter.showError("Can not import schema", e.getMessage());
			}
		}

		private List<SingleActionHandler<SchemaWithMappingBean>> getActionsHandlers()
		{

			SingleActionHandler<SchemaWithMappingBean> export = SingleActionHandler.builder(SchemaWithMappingBean.class)
					.withCaption(msg.getMessage("SCIMServiceEditorSchemaTab.exportAction"))
					.withIcon(VaadinIcon.UPLOAD).withHandler(r -> export(r.iterator().next())).build();

			SingleActionHandler<SchemaWithMappingBean> edit = SingleActionHandler
					.builder4Edit(msg::getMessage, SchemaWithMappingBean.class).withHandler(r ->
					{
						SchemaWithMappingBean edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<SchemaWithMappingBean> remove = SingleActionHandler
					.builder4Delete(msg::getMessage, SchemaWithMappingBean.class).withHandler(r ->
					{

						SchemaWithMappingBean schema = r.iterator().next();
						schemasGrid.removeElement(schema);
					}).withDisabledPredicate(s -> !getEditMode(s).equals(AttributesEditMode.FULL_EDIT)).build();

			return Arrays.asList(export, edit, remove);
		}

		
		private void export(SchemaWithMappingBean mapping)
		{
			Anchor download = new Anchor(getStreamResource(mapping), "");
			download.getElement()
					.setAttribute("download", true);
			add(download);
			download.getElement()
					.executeJs("return new Promise(resolve =>{this.click(); setTimeout(() => resolve(true), 150)})",
							download.getElement())
					.then(j -> remove(download));
		}

		private StreamResource getStreamResource(SchemaWithMappingBean mapping)
		{
			return new StreamResource(getNewFilename(mapping), () ->
			{

				try
				{
					byte[] content = Constants.MAPPER.writerWithDefaultPrettyPrinter()
							.writeValueAsBytes(configurationVaadinBeanMapper.mapToConfigurationSchema(mapping));
					return new ByteArrayInputStream(content);
				} catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			})
			{
				@Override
				public Map<String, String> getHeaders()
				{
					Map<String, String> headers = new HashMap<>(super.getHeaders());
					headers.put("Content-Disposition", "attachment; filename=\"" + getNewFilename(mapping) + "\"");
					return headers;
				}
			};
		}

		private String getNewFilename(SchemaWithMappingBean mapping)
		{
			return mapping.getName()  + ".json";
		}

		private void gotoNew()
		{
			gotoEditSubView(null, s ->
			{
				subViewSwitcher.exitSubViewAndShowUpdateInfo();
				schemasGrid.addElement(s);
			});

		}

		private void gotoEdit(SchemaWithMappingBean edited)
		{
			gotoEditSubView(edited, s ->
			{
				schemasGrid.replaceElement(edited, s);
				subViewSwitcher.exitSubViewAndShowUpdateInfo();
			});
		}

		private void gotoEditSubView(SchemaWithMappingBean edited, Consumer<SchemaWithMappingBean> onConfirm)
		{
			EditSchemaSubView subView;
			try
			{
				subView = editSchemaSubViewFactory.getSubView(
						schemasGrid.getElements().stream().map(s -> s.getId()).collect(Collectors.toList()), edited,
						getEditMode(edited), s ->
						{
							onConfirm.accept(s);
						//	fireChange();
							schemasGrid.focus();
						}, () ->
						{
							subViewSwitcher.exitSubView();
							schemasGrid.focus();
						});
				subViewSwitcher.goToSubView(subView);
			} catch (EngineException e)
			{
				notificationPresenter.showError("Can not edit schema", e.getMessage());
			}
		}

		private AttributesEditMode getEditMode(SchemaWithMappingBean schema)
		{
			if (schema != null)
			{
				if (schema.getType().equals(SchemaType.USER_CORE))
					return AttributesEditMode.EDIT_MAPPING_ONLY;
				if (schema.getType().equals(SchemaType.GROUP_CORE))
					return AttributesEditMode.HIDE_MAPPING;
			}

			return AttributesEditMode.FULL_EDIT;

		}
	}

	@org.springframework.stereotype.Component
	static class SCIMServiceEditorSchemaTabFactory
	{
		private final EditSchemaSubViewFactory editSchemaSubViewFactory;
		private final MessageSource msg;
		private final UnityServerConfiguration configuration;
		private final ConfigurationVaadinBeanMapper configurationVaadinBeanMapper;
		private final NotificationPresenter notificationPresenter;

		SCIMServiceEditorSchemaTabFactory(NotificationPresenter notificationPresenter, EditSchemaSubViewFactory editSchemaSubViewFactory, MessageSource msg,
				ConfigurationVaadinBeanMapper configurationVaadinBeanMapper, UnityServerConfiguration configuration)
		{
			this.notificationPresenter = notificationPresenter;
			this.editSchemaSubViewFactory = editSchemaSubViewFactory;
			this.msg = msg;
			this.configuration = configuration;
			this.configurationVaadinBeanMapper = configurationVaadinBeanMapper;
		}

		SCIMServiceEditorSchemaTab getSCIMServiceEditorSchemaTab(SubViewSwitcher subViewSwitcher)
		{
			return new SCIMServiceEditorSchemaTab(msg, notificationPresenter, editSchemaSubViewFactory, configuration, subViewSwitcher,
					configurationVaadinBeanMapper);
		}

	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.TAGS;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("SCIMServiceEditorSchemaTab.schemas");
	}

}
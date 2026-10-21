package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;

@Tag("admin-directory")
class AttributeClassTest extends UserAccountTestBase
{
	private final String className = "ui-class-" + UUID.randomUUID();
	private final String attributeName = "ui-required-" + UUID.randomUUID();
	private EntityParam member;

	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;
	@Autowired
	@Qualifier("insecure")
	private AttributeClassManagement classes;

	@AfterEach
	void removeSchema() throws Exception
	{
		if (member != null)
			classes.setEntityAttributeClasses(member, "/", List.of());
		if (classes.getAttributeClasses().containsKey(className))
			classes.removeAttributeClass(className);
		if (types.getAttributeTypesAsMap().containsKey(attributeName))
			types.removeAttributeType(attributeName, true);
	}

	@Test
	void shouldPersistClassAndEnforceItsMandatoryAttribute() throws Exception
	{
		member = createUser("Class member").entity();
		types.addAttributeType(new AttributeType(attributeName, "string"));
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/attribute-classes/edit");
		page.fill(testId("AttributeClassesEditView.name"), className);
		page.fill(testId("AttributeClassesEditView.description"), "Required project reference");
		page.check(testId("AttributeClassesEditView.allowArbitrary"), true);
		page.selectMultiple(testId("AttributeClassesEditView.mandatory"), attributeName);
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/attribute-classes");

		driver.get(baseUrl + "/admin/attribute-classes/edit/" + className);
		assertThat(page.value(testId("AttributeClassesEditView.name"))).isEqualTo(className);
		page.fill(testId("AttributeClassesEditView.description"), "Updated required project reference");
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/attribute-classes");
		driver.get(baseUrl + "/admin/attribute-classes/edit/" + className);
		assertThat(page.value(testId("AttributeClassesEditView.description")))
				.isEqualTo("Updated required project reference");

		AttributesClass stored = classes.getAttributeClass(className);
		assertThat(stored.getMandatory()).containsExactly(attributeName);
		assertThat(stored.isAllowArbitrary()).isTrue();
		assertThat(stored.getDescription()).isEqualTo("Updated required project reference");
		assertThatThrownBy(() -> classes.setEntityAttributeClasses(member, "/", List.of(className)))
				.isInstanceOf(SchemaConsistencyException.class).hasMessageContaining(attributeName);
		assertThat(classes.getEntityAttributeClasses(member, "/")).isEmpty();
		attributes.setAttribute(member, StringAttribute.of(attributeName, "/", "project-42"));
		classes.setEntityAttributeClasses(member, "/", List.of(className));
		assertThat(classes.getEntityAttributeClasses(member, "/"))
				.extracting(AttributesClass::getName).containsExactly(className);
	}
}

package io.imunity.test.ui.admin;

import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("admin-directory")
class AttributeWidgetTest extends AttributeWidgetTestBase
{
	@ParameterizedTest(name = "{0}")
	@MethodSource("widgetCases")
	void shouldCreateEditReloadAndRenderAttributeValue(AttributeWidgetCase scenario) throws Exception
	{
		assertRoundTrip(scenario);
	}

	private static Stream<AttributeWidgetCase> widgetCases()
	{
		return AttributeWidgetCase.cases().filter(scenario -> !scenario.usesImage());
	}
}

# Nightly Selenium runner

This module is selected only by the root `nightly-ui` profile. It uses the ordinary
`unity-ui-test-support` artifact with test scope and has no dependency on `integration-tests`.
Run commands from the repository root with Chrome and a compatible ChromeDriver available.

```sh
mvn -Pnightly-ui -pl ui-tests -am verify \
  '-Dunity.selenium.opts=--headless=new,--disable-dev-shm-usage,--disable-gpu' \
  -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost= > nightly-ui-build.log 2>&1
```

After installing upstream artifacts from the same revision, run an individual class with:

```sh
mvn -Pnightly-ui -pl ui-tests test -Dtest=AttributeTypeEditorTest \
  '-Dunity.selenium.opts=--headless=new,--disable-dev-shm-usage,--disable-gpu' \
  -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost= > nightly-ui-build.log 2>&1
```

Use `-Dgroups=<tag>` to select `admin-directory`, `admin-settings`, `home`, `upman`, or
`public-policies`. The runner fails when no tests are found. Current coverage includes:

- Admin directory: string syntax settings, mandatory attribute classes, and nested group lifecycle.
- Admin settings: identity self-service limits and running build information.
- Home: profile save/reload/reset with read-only authorization roles.
- UpMan: subgroup creation, displayed-name changes, visibility, and deletion with an unrelated project control.
- Public policies: English and Polish content without authentication, and unknown-document handling.

Each execution gets independent fixture data. Stored outcomes are checked through engine APIs;
workflow actions use the browser. The public-policy locale is seeded with the supported language
cookie. This checks localized content; interactive language switching has separate coverage needs.

Both executable runners build a production frontend before tests. Their application dependencies
use compile scope so Vaadin can discover endpoint views and plugin resources. The support jar
stays test-scoped. Vaadin's Maven plugin manages the frontend packages; no separate npm command
is needed.

New Selenium hooks use component-qualified `data-testid` values, such as
`EntityCreationDialog.credentialRequirement` and `DialogWithActionFooter.confirm`.
These attributes can repeat across component instances. Scope reusable controls through the
owning dialog or editor, and use `data-group-path`, `data-attribute-name`, or `data-credential`
to identify repeated business objects. Request and member controls use `data-request-id` and
`data-member-id`. The shared page helper selects visible controls when hidden menu instances
are also present, and re-resolves inputs if Vaadin replaces them while typing. Reserve HTML IDs
for elements requiring document-wide uniqueness. `VaadinPage.testId(...)` accepts an owner followed by descendant hooks for scoped
selectors.

The shared fixture uses an in-memory database, a fresh Spring context per execution, and only
explicit default content plus scenario-specific API setup. It enables AdminUI, Home, and UpMan.
The old integration scenarios retain their existing loopback fixture. Each browser has its own
profile, and different actors use separate browsers. Execution is sequential, with a five-minute
fork timeout and bounded browser waits. Port 2443 remains fixed: run simultaneous workers in
separate containers or network namespaces.

On failure, `target/selenium-failures/<test-class>-<unique-id>/` contains the test and fixture
identifier, screenshots, DOM, current URLs, browser console, capabilities (including browser and
driver versions), and server log. Capture and cleanup errors are attached to the original failure.
JUnit XML and per-execution timings are in `target/surefire-reports/`. Retain the Maven log on
failure; delete it after a successful run once it is no longer needed.

Scheduling, sharding, notification capture, broader feature coverage, and unattended reliability
validation remain future work.

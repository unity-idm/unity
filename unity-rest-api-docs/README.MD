# REST API documentation

From the repository root, generate and validate both the Admin and UpMan references without starting Unity:

```sh
mvn -pl unity-rest-api-docs -am verify -DskipTests -Dgpg.skip=true
```

The exporter runs during `prepare-package`, including when tests are skipped. It requires no database,
configured endpoint, listening port, Node installation or separately installed renderer. The normal reactor
`verify`/`install` lifecycle runs it too; no separate CI job is required. No CI configuration is tracked here.

Output in `unity-rest-api-docs/target/openapi`:

- `admin-openapi-v1.yaml`: deterministic Administration API v1 specification.
- `admin-openapi-v1.html`: offline Administration API reference with the specification embedded.
- `upman-openapi-v1.yaml`: deterministic OpenAPI v1 specification.
- `upman-openapi-v1.html`: offline reference with the specification embedded.
- `openapi-assets/`: Redoc standalone JavaScript and its license notice from the version pinned in the root POM.

Keep the HTML and assets together; the Redoc HTML can be opened directly from disk without a network connection.
The specification is embedded and fonts use local system fonts. This published reference is for reading;
configure the actual deployed endpoint URL in your API client.

## Live documentation

Set `unity.endpoint.rest.openApiPublication` in each REST endpoint's configuration.

| Value | Public routes below the endpoint path |
| --- | --- |
| `disabled` (default) | None |
| `specification` | `openapi.json` and `openapi.yaml` |
| `specificationAndUI` | Both specifications and the Scalar UI at `api-docs`, including its bundled JavaScript |

For example, enable `specificationAndUI` and open `/restupm/api-docs` in a browser. The page and spec
load without credentials. To call the API, select the configured Basic or Bearer authentication scheme
in Scalar and enter the corresponding credentials. UpMan bearer tokens need the documented API scopes.
Scalar sends requests directly to the endpoint and keeps credentials in memory for the current page.
The renderer is bundled locally and uses no external request proxy. Each endpoint instance has its own
publication setting. Build-time generation runs in every mode. If your configuration used `enableOpenApi`,
set `openApiPublication` explicitly to opt in to public docs.

The `documentation` module invokes the same exporter during `pre-site` and places the generated
references beside the handwritten guides and workflow documents. After installing reactor dependencies, build
the site from the repository root with `mvn -pl documentation compile site -Dgpg.skip=true` and the usual
Asciidoctor executable on `PATH`. API version `1` and the Unity product release version are separate.

## Validation and maintenance

Run the documentation and production schema checks with:

```sh
mvn -pl unity-rest-api-docs -am test \
  '-Dtest=OpenApiExporterTest,OpenApiIsolationTest,*OpenApiTest,*OpenApiHttpContractTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost=
```

Generation rejects invalid OpenAPI, unresolved references, duplicate operation IDs, opaque object bodies,
and examples that fail their schemas. The checked-in HTTP route inventories cover all 51 UpMan and 80 Admin
operations;
production tests compare the live document to the offline document and preserve literal wire
fixtures. The complete `web-upman` and `rest-admin` suites and repository headless verification remain regression gates.
The isolation tests compare both live specifications with their exports in both endpoint initialization orders,
check that global model converters stay unchanged, and exercise per-endpoint publication settings.

Review semantic changes to paths, media types, statuses, required fields, enums, discriminators and security
when API code changes. Compare generated YAML from the base revision with the branch output; property ordering
is deterministic. Do not edit generated YAML or HTML.

Maintain operation explanations in
`web-upman/src/main/resources/io/imunity/upman/rest/openapi/operations.properties`.
Literal examples in the adjacent `contract` directory are shared with the HTTP tests and remain independent
of DTO serialization.

The Admin operation explanations are maintained in
`rest-admin/src/main/resources/io/imunity/rest/admin/openapi/operations.properties`, with independent HTTP
fixtures in the adjacent `contract` directory. Admin timestamps, flattened invitations and request states,
plugin payloads, error ordering and media types retain their existing contracts. See
`rest-admin/src/test/java/pl/edu/icm/unity/restadm/HTTP_CONTRACT_COVERAGE.md` for the route coverage.

The former `-PopenapiSpec` profile is retained as a compatibility alias. Its fixed-port server bootstrap and
specification download have been replaced by the normal build-time exporter for both APIs.

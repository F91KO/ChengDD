# Spring Cloud Alibaba Nacos Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the direct Nacos client implementation with Spring Cloud Alibaba Config and Discovery, then verify configuration loading, service registration, Gateway load balancing, and the complete local Docker workflow.

**Architecture:** A new `cdd-common-nacos` adapter module owns Spring Cloud Alibaba dependencies and startup-contract validation while `cdd-common-core` remains Spring Cloud-free. Each executable service uses an explicit `nacos` Spring Profile with two non-optional Config Data imports; Gateway resolves instances through Spring Cloud LoadBalancer and retains its static URL fallback.

**Tech Stack:** JDK 21, Spring Boot 3.5.14, Spring Cloud 2025.0.3, Spring Cloud Alibaba 2025.0.0.0, Nacos Client/Server 3.0.3, Maven, JUnit 5, Docker Compose, Bash, PowerShell.

## Global Constraints

- Keep JDK exactly 21.
- Use Spring Boot 3.5.14, Spring Cloud 2025.0.3, Spring Cloud Alibaba 2025.0.0.0, and Nacos Client/Server 3.0.3.
- Import Config through `spring.config.import`; do not add `bootstrap.yaml`, `shared-configs`, or `extension-configs`.
- Set `refreshEnabled=false`; configuration changes take effect through controlled service restarts.
- Use group `CHENGDD`; use the Nacos Namespace ID for non-local environments and the public namespace for `local`.
- `cdd-db-migration`, `cdd-api-*`, `cdd-pay-core`, and `cdd-agent-core` must not depend on `cdd-common-nacos`.
- Keep the current Spring MVC Gateway proxy; do not migrate to Spring Cloud Gateway WebFlux.
- Keep static Gateway `base-url` values as the fallback when discovery has no usable instance.
- Preserve unrelated uncommitted frontend, documentation, and runtime-script work. Stage only the exact paths named by each task.
- Do not commit `runtime-logs/`, `.local/`, generated Maven output, credentials, access tokens, or local Nacos data.

## File Map

- `cdd-parent/pom.xml`: version train, BOM imports, module aggregation, and Maven Enforcer execution.
- `cdd-parent/cdd-common-nacos/`: dependency adapter, official Starter boundary, startup validation, and Nacos contract tests.
- `cdd-parent/cdd-common-core/`: retains only generic runtime models; all direct Nacos code and dependency are removed.
- Ten executable modules (`cdd-gateway` plus nine `*-service` modules): depend on the adapter and own their reliable `application-nacos.yaml` Config Data imports.
- `cdd-parent/cdd-gateway/`: resolves `ServiceInstance` through Spring Cloud LoadBalancer and retains static routing fallback.
- `scripts/validation/check_module_boundaries.py`: enforces module consumers, forbidden dependencies, and Profile/config asset completeness.
- `infrastructure/local/docker-compose.yml`: local MySQL, Redis, and Nacos 3.0.3 only; this is not a production deployment file.
- `scripts/nacos/`: publishes the shared/service DataIds and verifies real configuration and registered instances.
- `scripts/local/`: maps `CDD_CONFIG_MODE` to Spring Profiles and manages the complete local backend lifecycle.
- `scripts/testing/run_nacos_integration.sh`: starts the external Nacos contract test and records exact pass/fail evidence.
- Nacos architecture/delivery documents and `README.md`: describe the official integration and distinguish local verification from production work.

## Implementation References

- Spring Cloud Alibaba 2025.0 version matrix: <https://sca.aliyun.com/docs/2025.0.0.0/overview/version-explain/>
- Nacos 3 Docker quick start: <https://www.nacos.io/en/docs/v3.0/quickstart/quick-start-docker/>
- Nacos 3 deployment ports: <https://www.nacos.io/en/docs/v3.0/manual/admin/deployment/deployment-overview/>

---

### Task 1: Upgrade and Enforce the Spring Version Train

**Files:**
- Modify: `cdd-parent/pom.xml`

**Interfaces:**
- Consumes: the approved exact version baseline.
- Produces: Maven-managed Spring Cloud and Spring Cloud Alibaba dependencies for later modules.

- [ ] **Step 1: Capture the current resolved baseline**

Run:

```bash
mvn -f cdd-parent/pom.xml -DskipTests help:evaluate -Dexpression=spring.boot.version -q -DforceStdout
mvn -f cdd-parent/pom.xml -DskipTests dependency:tree -Dincludes=com.alibaba.nacos:nacos-client
```

Expected: Boot resolves to `3.3.2`, and the existing direct dependency resolves Nacos Client `2.3.2`.

- [ ] **Step 2: Update version properties and import both Cloud BOMs**

Set these properties in `cdd-parent/pom.xml`:

```xml
<spring.boot.version>3.5.14</spring.boot.version>
<spring.cloud.version>2025.0.3</spring.cloud.version>
<spring.cloud.alibaba.version>2025.0.0.0</spring.cloud.alibaba.version>
<maven.enforcer.plugin.version>3.5.0</maven.enforcer.plugin.version>
```

Add these imports after `spring-boot-dependencies`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>${spring.cloud.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-dependencies</artifactId>
    <version>${spring.cloud.alibaba.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

- [ ] **Step 3: Execute Maven Enforcer for every reactor build**

Add `maven-enforcer-plugin` under `build/plugins` rather than only `pluginManagement`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>${maven.enforcer.plugin.version}</version>
    <executions>
        <execution>
            <id>enforce-build-baseline</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <requireJavaVersion>
                        <version>[21,22)</version>
                    </requireJavaVersion>
                    <dependencyConvergence/>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 4: Verify the upgraded reactor still validates and compiles**

Run:

```bash
mvn -f cdd-parent/pom.xml -DskipTests validate
mvn -f cdd-parent/pom.xml -DskipTests compile
```

Expected: both commands exit `0`. Resolve genuine convergence failures with explicit BOM-managed versions; do not suppress `dependencyConvergence` or add wildcard exclusions.

- [ ] **Step 5: Commit the version train**

```bash
git add cdd-parent/pom.xml
git commit -m "build: align spring cloud alibaba version train"
```

### Task 2: Introduce the Official Nacos Adapter and Remove Direct Clients

**Files:**
- Create: `cdd-parent/cdd-common-nacos/pom.xml`
- Create: `cdd-parent/cdd-common-nacos/src/main/java/com/cdd/common/nacos/CddNacosAutoConfiguration.java`
- Create: `cdd-parent/cdd-common-nacos/src/main/java/com/cdd/common/nacos/NacosStartupValidator.java`
- Create: `cdd-parent/cdd-common-nacos/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `cdd-parent/cdd-common-nacos/src/test/java/com/cdd/common/nacos/NacosStartupValidatorTest.java`
- Modify: `cdd-parent/pom.xml`
- Modify: `cdd-parent/cdd-common-core/pom.xml`
- Delete: `cdd-parent/cdd-common-core/src/main/java/com/cdd/common/core/runtime/NacosConfigEnvironmentPostProcessor.java`
- Delete: `cdd-parent/cdd-common-core/src/main/java/com/cdd/common/core/runtime/NacosRuntimeSupport.java`
- Delete: `cdd-parent/cdd-common-core/src/main/java/com/cdd/common/core/runtime/NacosServiceRegistrationListener.java`
- Delete: `cdd-parent/cdd-common-core/src/test/java/com/cdd/common/core/runtime/NacosConfigEnvironmentPostProcessorTest.java`
- Delete: `cdd-parent/cdd-common-core/src/test/java/com/cdd/common/core/runtime/NacosRuntimeSupportTest.java`
- Delete: `cdd-parent/cdd-common-core/src/test/java/com/cdd/common/core/runtime/NacosServiceRegistrationListenerTest.java`
- Delete: `cdd-parent/cdd-common-core/src/main/resources/META-INF/spring.factories`

**Interfaces:**
- Consumes: BOM-managed Config and Discovery Starters from Task 1.
- Produces: `com.cdd:cdd-common-nacos` and `NacosStartupValidator#validate()`; later executable modules depend on this adapter.

- [ ] **Step 1: Write validator unit tests before the adapter implementation**

Create tests that construct `MockEnvironment` directly and assert these exact contracts:

```java
@Test
void shouldAcceptLocalPublicNamespace() {
    MockEnvironment environment = validEnvironment("local");
    assertDoesNotThrow(() -> new NacosStartupValidator(environment).validate());
}

@Test
void shouldRequireNamespaceIdOutsideLocal() {
    MockEnvironment environment = validEnvironment("dev")
            .withProperty("spring.cloud.nacos.config.namespace", "")
            .withProperty("spring.cloud.nacos.discovery.namespace", "");
    assertThatThrownBy(() -> new NacosStartupValidator(environment).validate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("namespace ID");
}

@Test
void shouldRequireMatchingNonBlankGroups() {
    MockEnvironment environment = validEnvironment("local")
            .withProperty("spring.cloud.nacos.discovery.group", "");
    assertThatThrownBy(() -> new NacosStartupValidator(environment).validate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CHENGDD");
}
```

`validEnvironment` must set `spring.application.name=cdd-contract-service`, `cdd.runtime.env`, `spring.cloud.nacos.server-addr=127.0.0.1:8848`, and both groups to `CHENGDD`; for non-local environments it sets both namespace values to `namespace-id-<env>`.

Use this helper so every property name is fixed before implementation:

```java
private static MockEnvironment validEnvironment(String runtimeEnv) {
    MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.application.name", "cdd-contract-service")
            .withProperty("cdd.runtime.env", runtimeEnv)
            .withProperty("spring.cloud.nacos.server-addr", "127.0.0.1:8848")
            .withProperty("spring.cloud.nacos.config.group", "CHENGDD")
            .withProperty("spring.cloud.nacos.discovery.group", "CHENGDD");
    if (!"local".equals(runtimeEnv)) {
        environment.withProperty("spring.cloud.nacos.config.namespace", "namespace-id-" + runtimeEnv)
                .withProperty("spring.cloud.nacos.discovery.namespace", "namespace-id-" + runtimeEnv);
    }
    return environment;
}
```

- [ ] **Step 2: Run the tests and verify the adapter does not exist yet**

Run:

```bash
mvn -f cdd-parent/pom.xml -pl cdd-common-nacos -am -Dtest=NacosStartupValidatorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the module/class has not been implemented.

- [ ] **Step 3: Add the adapter POM and aggregate it**

The new POM must contain only the official Starters plus test support:

```xml
<dependencies>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Add `<module>cdd-common-nacos</module>` immediately after `cdd-common-core` in the reactor.

- [ ] **Step 4: Implement startup-contract validation without creating a Nacos client**

`CddNacosAutoConfiguration` is active only for the `nacos` Profile and exposes one validator bean:

```java
@AutoConfiguration
@Profile("nacos")
public class CddNacosAutoConfiguration {

    @Bean
    NacosStartupValidator nacosStartupValidator(Environment environment) {
        NacosStartupValidator validator = new NacosStartupValidator(environment);
        validator.validate();
        return validator;
    }
}
```

`NacosStartupValidator#validate()` must enforce non-blank application name and server address, require both groups to equal `CHENGDD`, allow blank namespaces only for `local`, and require matching non-blank Config/Discovery Namespace IDs for `dev`, `test`, and `prod`. It must not import `com.alibaba.nacos.*`, register instances, listen for configuration changes, or create threads.

Implement the validator with this interface and decision logic:

```java
public final class NacosStartupValidator {

    private static final Set<String> ENVIRONMENTS = Set.of("local", "dev", "test", "prod");
    private final Environment environment;

    public NacosStartupValidator(Environment environment) {
        this.environment = environment;
    }

    public void validate() {
        requireText("spring.application.name");
        requireText("spring.cloud.nacos.server-addr");

        String runtimeEnv = environment.getProperty(
                "cdd.runtime.env",
                environment.getProperty("CDD_ENV", "local"));
        if (!ENVIRONMENTS.contains(runtimeEnv)) {
            throw new IllegalStateException("Unsupported cdd.runtime.env: " + runtimeEnv);
        }

        String configGroup = requireText("spring.cloud.nacos.config.group");
        String discoveryGroup = requireText("spring.cloud.nacos.discovery.group");
        if (!"CHENGDD".equals(configGroup) || !configGroup.equals(discoveryGroup)) {
            throw new IllegalStateException("Nacos Config and Discovery group must both be CHENGDD");
        }

        String configNamespace = environment.getProperty("spring.cloud.nacos.config.namespace", "").trim();
        String discoveryNamespace = environment.getProperty("spring.cloud.nacos.discovery.namespace", "").trim();
        if (!"local".equals(runtimeEnv)
                && (configNamespace.isEmpty() || !configNamespace.equals(discoveryNamespace))) {
            throw new IllegalStateException("Non-local Nacos Config and Discovery require the same namespace ID");
        }
    }

    private String requireText(String key) {
        String value = environment.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Required Nacos property is blank: " + key);
        }
        return value;
    }
}
```

Register the auto-configuration with this one-line imports file:

```text
com.cdd.common.nacos.CddNacosAutoConfiguration
```

- [ ] **Step 5: Remove the direct client implementation from common-core**

Delete the three direct-client classes and their tests, remove `com.alibaba.nacos:nacos-client:2.3.2` from `cdd-common-core/pom.xml`, and delete `spring.factories` because it becomes empty. Keep `ConfigMode` and `RuntimeEnv` as generic runtime models.

- [ ] **Step 6: Run adapter and core tests**

Run:

```bash
mvn -f cdd-parent/pom.xml -pl cdd-common-core,cdd-common-nacos -am test
rg -n "com\.alibaba\.nacos|nacos-client" cdd-parent/cdd-common-core && exit 1 || true
```

Expected: Maven exits `0`; the `rg` command prints no direct Nacos reference under `cdd-common-core`.

- [ ] **Step 7: Commit the adapter boundary**

```bash
git add cdd-parent/pom.xml cdd-parent/cdd-common-core cdd-parent/cdd-common-nacos
git commit -m "refactor: replace direct nacos clients with alibaba adapter"
```

### Task 3: Apply Nacos Profiles to Every Executable Service

**Files:**
- Modify: `scripts/validation/check_module_boundaries.py`
- Modify: `cdd-parent/cdd-gateway/pom.xml`
- Modify: `cdd-parent/cdd-auth-service/pom.xml`
- Modify: `cdd-parent/cdd-merchant-service/pom.xml`
- Modify: `cdd-parent/cdd-decoration-service/pom.xml`
- Modify: `cdd-parent/cdd-product-service/pom.xml`
- Modify: `cdd-parent/cdd-order-service/pom.xml`
- Modify: `cdd-parent/cdd-marketing-service/pom.xml`
- Modify: `cdd-parent/cdd-release-service/pom.xml`
- Modify: `cdd-parent/cdd-report-service/pom.xml`
- Modify: `cdd-parent/cdd-config-service/pom.xml`
- Modify: the ten matching `src/main/resources/application.yaml` files
- Create: the ten matching `src/main/resources/application-nacos.yaml` files

**Interfaces:**
- Consumes: `cdd-common-nacos` from Task 2.
- Produces: `{env},nacos` and `{env},file` runtime modes with exact shared/service Config Data imports.

- [ ] **Step 1: Make the boundary checker describe the desired dependency graph**

Add `cdd-common-nacos` to `REQUIRED_MODULES`, and define:

```python
NACOS_CONSUMERS = {
    "cdd-gateway",
    "cdd-auth-service",
    "cdd-merchant-service",
    "cdd-decoration-service",
    "cdd-product-service",
    "cdd-order-service",
    "cdd-marketing-service",
    "cdd-release-service",
    "cdd-report-service",
    "cdd-config-service",
}
NACOS_FORBIDDEN = {
    "cdd-common-core",
    "cdd-common-db",
    "cdd-common-redis",
    "cdd-common-security",
    "cdd-common-web",
    "cdd-db-migration",
    "cdd-agent-core",
    "cdd-pay-core",
}
```

Extend validation so every consumer must depend directly on `cdd-common-nacos`, every `cdd-api-*` and forbidden module must not depend on it, every consumer must own `application-nacos.yaml`, and `cdd-db-migration` must not own that file.

Implement and call this function from `main()`:

```python
def validate_nacos_boundaries() -> list[str]:
    errors: list[str] = []
    for module in sorted(NACOS_CONSUMERS):
        pom = PARENT_ROOT / module / "pom.xml"
        if "cdd-common-nacos" not in project_dependencies(pom):
            errors.append(f"{module} 必须直接依赖 cdd-common-nacos")
        profile = PARENT_ROOT / module / "src/main/resources/application-nacos.yaml"
        if not profile.exists():
            errors.append(f"{module} 缺少 application-nacos.yaml")

    for pom in sorted(PARENT_ROOT.glob("cdd-*/pom.xml")):
        module = project_artifact_id(pom)
        forbidden = module in NACOS_FORBIDDEN or module.startswith("cdd-api-")
        if forbidden and "cdd-common-nacos" in project_dependencies(pom):
            errors.append(f"{module} 不允许依赖 cdd-common-nacos")

    migration_profile = PARENT_ROOT / "cdd-db-migration/src/main/resources/application-nacos.yaml"
    if migration_profile.exists():
        errors.append("cdd-db-migration 不允许包含 application-nacos.yaml")
    return errors
```

- [ ] **Step 2: Run the boundary checker and verify it fails for missing integration**

Run:

```bash
python3 scripts/validation/check_module_boundaries.py
```

Expected: FAIL listing the ten missing adapter dependencies and ten missing `application-nacos.yaml` files.

- [ ] **Step 3: Add the adapter dependency to all ten consumers**

Add this exact dependency to each listed consumer POM:

```xml
<dependency>
    <groupId>com.cdd</groupId>
    <artifactId>cdd-common-nacos</artifactId>
    <version>${project.version}</version>
</dependency>
```

Do not add it transitively to `cdd-common-web`, `cdd-common-db`, `cdd-common-security`, or any API module.

- [ ] **Step 4: Make file mode the safe base configuration**

In each consumer's `application.yaml`, keep its existing application name and business settings, set active profiles to both runtime dimensions, and replace the custom `cdd.nacos` block with disabled official switches:

```yaml
spring:
  profiles:
    active: ${CDD_ENV:local},${CDD_CONFIG_MODE:nacos}
  cloud:
    nacos:
      config:
        enabled: false
        import-check:
          enabled: false
      discovery:
        enabled: false

cdd:
  runtime:
    env: ${CDD_ENV:local}
    config-mode: ${CDD_CONFIG_MODE:nacos}
```

The existing `spring.application.name`, security, management, and Gateway route values remain in place. With `CDD_CONFIG_MODE=file`, the active Profiles become `{env},file`, and no Config Import or registration occurs.

- [ ] **Step 5: Add the exact official Nacos Profile to every consumer**

Create identical `application-nacos.yaml` content in each consumer; `${spring.application.name}` makes the service DataId independent:

```yaml
spring:
  config:
    import:
      - "nacos:cdd-common-${CDD_ENV:local}.yaml?group=${CDD_NACOS_GROUP:CHENGDD}&refreshEnabled=false"
      - "nacos:${spring.application.name}-${CDD_ENV:local}.yaml?group=${CDD_NACOS_GROUP:CHENGDD}&refreshEnabled=false"
  cloud:
    nacos:
      server-addr: ${CDD_NACOS_SERVER_ADDR:127.0.0.1:8848}
      username: ${CDD_NACOS_USERNAME:}
      password: ${CDD_NACOS_PASSWORD:}
      config:
        enabled: true
        import-check:
          enabled: true
        namespace: ${CDD_NACOS_NAMESPACE:}
        group: ${CDD_NACOS_GROUP:CHENGDD}
      discovery:
        enabled: true
        namespace: ${CDD_NACOS_NAMESPACE:}
        group: ${CDD_NACOS_GROUP:CHENGDD}
```

Both imports are non-optional, shared configuration comes first, and dynamic refresh is disabled on both.

Do not enable `management.health.nacos`; service liveness remains independent of a short Nacos outage, while `scripts/nacos/check_nacos_state.sh` supplies the separate Nacos readiness and registration evidence.

- [ ] **Step 6: Verify file mode and the dependency boundary**

Run:

```bash
python3 scripts/validation/check_module_boundaries.py
CDD_CONFIG_MODE=file mvn -f cdd-parent/pom.xml -pl cdd-auth-service,cdd-gateway -am test
```

Expected: the boundary checker and tests exit `0` without contacting Nacos.

- [ ] **Step 7: Commit consumer wiring and Profiles**

```bash
git add scripts/validation/check_module_boundaries.py cdd-parent/cdd-gateway cdd-parent/cdd-auth-service cdd-parent/cdd-merchant-service cdd-parent/cdd-decoration-service cdd-parent/cdd-product-service cdd-parent/cdd-order-service cdd-parent/cdd-marketing-service cdd-parent/cdd-release-service cdd-parent/cdd-report-service cdd-parent/cdd-config-service
git commit -m "feat: configure services for alibaba nacos profiles"
```

### Task 4: Replace Gateway NamingService with Spring Cloud LoadBalancer

**Files:**
- Modify: `cdd-parent/cdd-gateway/pom.xml`
- Modify: `cdd-parent/cdd-gateway/src/main/java/com/cdd/gateway/config/GatewayRouteProperties.java`
- Replace: `cdd-parent/cdd-gateway/src/main/java/com/cdd/gateway/service/GatewayRouteResolver.java`
- Modify: `cdd-parent/cdd-gateway/src/main/java/com/cdd/gateway/web/GatewayProxyController.java`
- Modify: `cdd-parent/cdd-gateway/src/main/java/com/cdd/gateway/web/GatewayDashboardController.java`
- Create: `cdd-parent/cdd-gateway/src/test/java/com/cdd/gateway/service/GatewayRouteResolverTest.java`
- Test: `cdd-parent/cdd-gateway/src/test/java/com/cdd/gateway/web/GatewayProxyIntegrationTest.java`

**Interfaces:**
- Consumes: `LoadBalancerClient#choose(String)` and `GatewayRouteProperties.ServiceRoute#getServiceName()`.
- Produces: `GatewayRouteResolver#resolveBaseUrl(ServiceRoute): String`, used by both proxy controllers.

- [ ] **Step 1: Write resolver tests using an interface mock**

Cover these exact cases:

```java
@Test
void shouldUseDiscoveredInstanceInNacosProfile() {
    when(environment.acceptsProfiles(Profiles.of("nacos"))).thenReturn(true);
    when(loadBalancerClient.choose("cdd-product-service"))
            .thenReturn(new DefaultServiceInstance("product-1", "cdd-product-service", "10.0.0.8", 8084, false));
    assertThat(resolver.resolveBaseUrl(route("cdd-product-service", "http://127.0.0.1:8084")))
            .isEqualTo("http://10.0.0.8:8084");
}

@Test
void shouldUseStaticUrlOutsideNacosProfile() {
    when(environment.acceptsProfiles(Profiles.of("nacos"))).thenReturn(false);
    GatewayRouteProperties.ServiceRoute route = route("cdd-product-service", "http://127.0.0.1:8084");
    assertThat(resolver.resolveBaseUrl(route)).isEqualTo("http://127.0.0.1:8084");
    verifyNoInteractions(loadBalancerClient);
}

@Test
void shouldFallBackWhenNoInstanceExists() {
    when(environment.acceptsProfiles(Profiles.of("nacos"))).thenReturn(true);
    when(loadBalancerClient.choose("cdd-product-service")).thenReturn(null);
    assertThat(resolver.resolveBaseUrl(route("cdd-product-service", "http://127.0.0.1:8084")))
            .isEqualTo("http://127.0.0.1:8084");
}

@Test
void shouldFallBackWhenLoadBalancerThrows() {
    when(environment.acceptsProfiles(Profiles.of("nacos"))).thenReturn(true);
    when(loadBalancerClient.choose("cdd-product-service"))
            .thenThrow(new IllegalStateException("discovery unavailable"));
    assertThat(resolver.resolveBaseUrl(route("cdd-product-service", "http://127.0.0.1:8084")))
            .isEqualTo("http://127.0.0.1:8084");
}
```

Initialize `resolver = new GatewayRouteResolver(loadBalancerClient, environment)` in `@BeforeEach`, and define `route(serviceName, baseUrl)` by constructing `ServiceRoute` and calling both setters.

Use Mockito's interface mocking with the repository's non-inline mock maker; do not enable JVM self-attach.

- [ ] **Step 2: Run the resolver tests and verify direct-client code fails the contract**

Run:

```bash
mvn -f cdd-parent/pom.xml -pl cdd-gateway -am -Dtest=GatewayRouteResolverTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the current resolver constructs `NamingService` and has no injectable `LoadBalancerClient`.

- [ ] **Step 3: Add LoadBalancer and replace the resolver**

Add the explicit Gateway dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

Remove `com.alibaba.nacos:nacos-client`. Implement the resolver with constructor-injected `LoadBalancerClient` and `Environment`; call `choose(serviceName)` only when `environment.acceptsProfiles(Profiles.of("nacos"))` is true. Convert `ServiceInstance#getUri()` to a base URL, and log a warning before returning `serviceRoute.getBaseUrl()` for null instances or runtime exceptions. The class must not own shutdown logic because Spring Cloud Alibaba owns the client lifecycle.

- [ ] **Step 4: Preserve service names and route all controllers through the resolver**

Keep `baseUrl` required and `serviceName` trimmed/nullable in `GatewayRouteProperties.ServiceRoute`. Replace every controller use of `route.getBaseUrl()` for downstream calls with `gatewayRouteResolver.resolveBaseUrl(route)`; do not change request paths, authorization, propagated headers, query strings, or response semantics.

- [ ] **Step 5: Run Gateway unit and integration tests**

Run:

```bash
mvn -f cdd-parent/pom.xml -pl cdd-gateway -am -Dtest=GatewayRouteResolverTest,GatewayProxyIntegrationTest,GatewayContextControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
rg -n "NacosFactory|NamingService|com\.alibaba\.nacos" cdd-parent/cdd-gateway && exit 1 || true
```

Expected: tests exit `0`; no direct Nacos API reference remains in Gateway.

- [ ] **Step 6: Commit Gateway discovery**

```bash
git add cdd-parent/cdd-gateway
git commit -m "feat: resolve gateway routes with cloud loadbalancer"
```

### Task 5: Upgrade the Local Nacos Container and Configuration Publisher

**Files:**
- Modify: `infrastructure/local/docker-compose.yml`
- Modify: `scripts/local/up_local_infra.sh`
- Modify: `scripts/nacos/publish_nacos_configs.sh`
- Modify: `scripts/nacos/publish_nacos_configs.ps1`
- Create: `scripts/nacos/check_nacos_state.sh`

**Interfaces:**
- Consumes: Nacos 3.0.3 server ports and the DataId/group contract.
- Produces: local Console at port `8080`, client API at `8848`, client gRPC at `9848`, and repeatable publish/status commands.

- [ ] **Step 1: Update Compose to the Nacos 3 port model**

Set `image: nacos/nacos-server:v3.0.3`. Keep standalone mode and disabled local auth, add non-secret local-only identity defaults required by the 3.x image, and expose only these ports:

```yaml
environment:
  MODE: standalone
  PREFER_HOST_MODE: hostname
  NACOS_AUTH_ENABLE: "false"
  NACOS_AUTH_TOKEN: ${CDD_LOCAL_NACOS_AUTH_TOKEN:-Y2hlbmdkZGxvY2FsbmFjb3NhdXRoLXRva2VuLTIwMjY=}
  NACOS_AUTH_IDENTITY_KEY: ${CDD_LOCAL_NACOS_IDENTITY_KEY:-chengdd-local-key}
  NACOS_AUTH_IDENTITY_VALUE: ${CDD_LOCAL_NACOS_IDENTITY_VALUE:-chengdd-local-value}
  JVM_XMS: 256m
  JVM_XMX: 256m
  JVM_XMN: 128m
ports:
  - "${CDD_LOCAL_NACOS_CONSOLE_PORT:-8080}:8080"
  - "${CDD_LOCAL_NACOS_PORT:-8848}:8848"
  - "${CDD_LOCAL_NACOS_GRPC_PORT:-9848}:9848"
```

Remove host mapping `9849`; it is a server-to-server port and is not needed by the standalone local workflow. Add a health check against `http://127.0.0.1:8848/nacos/v1/console/health/liveness`.

- [ ] **Step 2: Update local infrastructure output and readiness checks**

`up_local_infra.sh` must wait for the Nacos health endpoint before returning and print:

```text
Nacos Console: http://127.0.0.1:8080/index.html
Nacos Client API: 127.0.0.1:8848
```

Do not print the old `http://127.0.0.1:8848/nacos` Console URL.

- [ ] **Step 3: Keep publishing idempotent and verify every response**

The Bash and PowerShell publishers must publish exactly one shared DataId plus these ten service DataIds to group `CHENGDD`: Gateway, Auth, Merchant, Decoration, Product, Order, Marketing, Release, Report, and Config. Retain the Nacos 3-compatible `/nacos/v1/cs/configs` endpoint during this migration, require the response body to equal `true`, and fail on missing source files instead of silently skipping them.

For non-local environments, send `tenant=${CDD_NACOS_NAMESPACE}` and fail before the first request when the namespace is blank.

- [ ] **Step 4: Add a read-only Nacos status script**

`check_nacos_state.sh <env> [running|stopped]` must:

1. GET `cdd-common-<env>.yaml` from `/nacos/v1/cs/configs` with `group=CHENGDD` and optional `tenant`.
2. Fail if the shared configuration body is empty.
3. Query `/nacos/v1/ns/instance/list` for each of the ten service names with `groupName=CHENGDD`.
4. In default `running` mode, print `registered <service> hosts=<count>` and fail when the returned JSON has zero hosts.
5. In `stopped` mode, print `deregistered <service>` and fail when the returned JSON still has one or more hosts.

Use `python3 -c` to parse JSON so `jq` is not a workstation prerequisite.

- [ ] **Step 5: Validate syntax and Compose resolution**

Run:

```bash
bash -n scripts/local/up_local_infra.sh scripts/nacos/publish_nacos_configs.sh scripts/nacos/check_nacos_state.sh
docker compose -f infrastructure/local/docker-compose.yml config
```

Expected: Bash syntax and Compose rendering exit `0`; rendered Nacos image is `v3.0.3` and includes ports `8080`, `8848`, and `9848`.

- [ ] **Step 6: Commit local Nacos 3 infrastructure**

```bash
git add infrastructure/local/docker-compose.yml scripts/local/up_local_infra.sh scripts/nacos
git commit -m "chore: upgrade local nacos infrastructure to 3.0.3"
```

### Task 6: Map Runtime Scripts to Official Spring Profiles

**Files:**
- Modify: `scripts/local/run_packaged_module.sh`
- Modify: `scripts/local/run_all_services_mysql.sh`
- Modify: `scripts/local/status_all_services.sh`
- Modify: `scripts/local/stop_all_services.sh`
- Modify: `scripts/local/backend_runtime_guard.sh`
- Test: all `scripts/local/run_*service*.sh` and `scripts/local/run_gateway.sh`

**Interfaces:**
- Consumes: `CDD_ENV`, `CDD_CONFIG_MODE=file|nacos`, the publisher, and the runtime service catalog.
- Produces: Java processes with `spring.profiles.active={env},{mode}` through the application configuration and lifecycle evidence for all ten registered services.

- [ ] **Step 1: Remove custom-client environment switches**

Delete exports and documentation for `CDD_NACOS_FAIL_FAST`, `CDD_NACOS_REQUIRE_SHARED_CONFIG`, `CDD_NACOS_REQUIRE_SERVICE_CONFIG`, and `CDD_NACOS_DISCOVERY_ENABLED`. They are not Spring Cloud Alibaba properties.

Keep these supported inputs:

```text
CDD_ENV=local|dev|test|prod
CDD_CONFIG_MODE=file|nacos
CDD_NACOS_SERVER_ADDR=127.0.0.1:8848
CDD_NACOS_NAMESPACE=<namespace-id>
CDD_NACOS_GROUP=CHENGDD
CDD_NACOS_USERNAME=<optional-local-empty>
CDD_NACOS_PASSWORD=<optional-local-empty>
```

- [ ] **Step 2: Validate runtime mode before building**

In `run_packaged_module`, reject any `CDD_CONFIG_MODE` other than `file` or `nacos`, and reject any `CDD_ENV` outside `local`, `dev`, `test`, and `prod`. Publish Nacos configuration only in `nacos` mode. In `file` mode, start without calling any Nacos script.

- [ ] **Step 3: Pass explicit Profiles to packaged applications**

Start each JAR with both existing port argument and this exact argument:

```bash
--spring.profiles.active="${runtime_env},${runtime_config_mode}"
```

This command-line value is authoritative over the default in `application.yaml` and makes logs reproducible.

- [ ] **Step 4: Integrate Nacos state into status and shutdown**

When `CDD_CONFIG_MODE=nacos`, `status_all_services.sh` must count HTTP-healthy services: call `scripts/nacos/check_nacos_state.sh "$CDD_ENV" running` when one or more are running and call `scripts/nacos/check_nacos_state.sh "$CDD_ENV" stopped` when all ten are stopped. `stop_all_services.sh` must stop services in reverse order, wait up to 30 seconds, then poll `scripts/nacos/check_nacos_state.sh "$CDD_ENV" stopped` for another 30 seconds and return non-zero if stale instances remain.

- [ ] **Step 5: Run script contract checks**

Run:

```bash
bash -n scripts/local/*.sh scripts/nacos/*.sh scripts/db/*.sh
rg -n "CDD_NACOS_FAIL_FAST|CDD_NACOS_REQUIRE_|CDD_NACOS_DISCOVERY_ENABLED" scripts/local scripts/nacos && exit 1 || true
python3 scripts/validation/check_module_boundaries.py
```

Expected: syntax and boundary checks exit `0`; the removed custom switches are absent.

- [ ] **Step 6: Commit official Profile runtime wiring**

```bash
git add scripts/local scripts/nacos scripts/validation/check_module_boundaries.py
git commit -m "chore: run services with official nacos profiles"
```

### Task 7: Add Real Nacos Config, Discovery, and LoadBalancer Contract Tests

**Files:**
- Modify: `cdd-parent/cdd-common-nacos/pom.xml`
- Create: `cdd-parent/cdd-common-nacos/src/test/java/com/cdd/common/nacos/NacosContractApplication.java`
- Create: `cdd-parent/cdd-common-nacos/src/test/java/com/cdd/common/nacos/NacosConfigImportIT.java`
- Create: `cdd-parent/cdd-common-nacos/src/test/java/com/cdd/common/nacos/NacosMissingConfigIT.java`
- Create: `cdd-parent/cdd-common-nacos/src/test/java/com/cdd/common/nacos/NacosUnavailableIT.java`
- Create: `cdd-parent/cdd-common-nacos/src/test/java/com/cdd/common/nacos/NacosFileModeTest.java`
- Create: `cdd-parent/cdd-common-nacos/src/test/resources/application-nacos.yaml`
- Create: `scripts/testing/run_nacos_integration.sh`

**Interfaces:**
- Consumes: a running local Nacos 3.0.3 server at `CDD_NACOS_SERVER_ADDR`.
- Produces: automated proof of file isolation, import precedence, missing-config failure, discovery visibility, and LoadBalancer selection.

- [ ] **Step 1: Add a Maven integration-test Profile**

Add test-scoped `spring-boot-starter-web`, `spring-cloud-starter-loadbalancer`, and `org.awaitility:awaitility`. Add a `nacos-integration` Maven Profile to `cdd-common-nacos/pom.xml` that runs Failsafe `integration-test` and `verify` for `**/*IT.java`. Normal `mvn test` must not start or require Docker.

- [ ] **Step 2: Write the contract application and Nacos Profile resource**

The test application is a minimal servlet application with `server.port=0`. Its test `application-nacos.yaml` uses the same two non-optional imports as production modules and disables refresh. It uses application name `cdd-nacos-contract-test` and group `CHENGDD`.

- [ ] **Step 3: Write the integration assertions before the harness**

`NacosConfigImportIT` must start `SpringApplication` with Profiles `local,nacos` and assert:

```java
assertThat(environment.getProperty("cdd.contract.shared-only")).isEqualTo("from-common");
assertThat(environment.getProperty("cdd.contract.precedence")).isEqualTo("from-service");
await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
        assertThat(discoveryClient.getInstances("cdd-nacos-contract-test")).isNotEmpty());
assertThat(loadBalancerClient.choose("cdd-nacos-contract-test")).isNotNull();
```

Close the application context in a `finally` block. The external harness performs the post-close deregistration assertion because the context-owned `DiscoveryClient` is no longer usable after shutdown.

`NacosMissingConfigIT` must use application name `cdd-nacos-missing-contract-test`, start with `local,nacos`, and assert startup throws because the service DataId is absent. `NacosUnavailableIT` starts `local,nacos` against `127.0.0.1:65534` and asserts startup fails during Config Data import. A separate file-mode test starts with `local,file`, uses the same unreachable address, and asserts the context starts without a Nacos import or registered service.

- [ ] **Step 4: Write the external harness**

`run_nacos_integration.sh` must:

1. Start `nacos` through `infrastructure/local/docker-compose.yml` and wait for liveness.
2. POST `cdd-common-local.yaml` containing `shared-only: from-common` and `precedence: from-common`.
3. POST `cdd-nacos-contract-test-local.yaml` containing `precedence: from-service`.
4. Ensure `cdd-nacos-missing-contract-test-local.yaml` is deleted before tests.
5. Run `mvn -f cdd-parent/pom.xml -pl cdd-common-nacos -am -Pnacos-integration verify`.
6. Poll `/nacos/v1/ns/instance/list?serviceName=cdd-nacos-contract-test&groupName=CHENGDD` for up to 30 seconds and fail unless `hosts` becomes empty.
7. Delete both contract DataIds in a shell `trap` without deleting project service configurations.

- [ ] **Step 5: Run the real contract test**

Run:

```bash
bash scripts/testing/run_nacos_integration.sh
```

Expected: imports load in the asserted order, missing configuration fails, file mode remains isolated, discovery returns the contract service, LoadBalancer selects it, and closing the context deregisters it.

- [ ] **Step 6: Commit integration coverage**

```bash
git add cdd-parent/cdd-common-nacos scripts/testing/run_nacos_integration.sh
git commit -m "test: cover alibaba nacos integration contracts"
```

### Task 8: Run Full Backend Regression and Local End-to-End Acceptance

**Files:**
- Create: `cdd-parent/cdd-merchant-service/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- Update: `docs/05-delivery/reports/phase1-acceptance-latest.md`
- Update: `docs/05-delivery/reports/frontend-acceptance.md`

**Interfaces:**
- Consumes: all code, configuration, infrastructure, and scripts from Tasks 1-7.
- Produces: fresh Maven, dependency, service-registration, Gateway, and business-smoke evidence.

- [ ] **Step 1: Prove dependency convergence and exact versions**

Run:

```bash
mvn -f cdd-parent/pom.xml -DskipTests validate
mvn -f cdd-parent/pom.xml -DskipTests help:evaluate -Dexpression=spring.boot.version -q -DforceStdout
mvn -f cdd-parent/pom.xml -DskipTests help:evaluate -Dexpression=spring.cloud.version -q -DforceStdout
mvn -f cdd-parent/pom.xml -DskipTests help:evaluate -Dexpression=spring.cloud.alibaba.version -q -DforceStdout
mvn -f cdd-parent/pom.xml -DskipTests dependency:tree -Dincludes=org.springframework.boot:*,org.springframework.cloud:*,com.alibaba.cloud:*,com.alibaba.nacos:nacos-client
```

Expected: the three evaluated properties print `3.5.14`, `2025.0.3`, and `2025.0.0.0`; the tree contains Boot `3.5.14`, Alibaba Starter `2025.0.0.0`, and exactly one Nacos Client `3.0.3`. No Nacos Client 2.x appears.

- [ ] **Step 2: Force non-inline Mockito for the remaining interface mocks**

Create the Merchant test extension with this exact one-line content:

```text
mock-maker-subclass
```

Run:

```bash
CDD_CONFIG_MODE=file mvn -f cdd-parent/pom.xml -pl cdd-merchant-service -am -Dtest=MerchantAccountApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: the test exits `0` on JDK 21 without Byte Buddy self-attach flags.

- [ ] **Step 3: Run the full reactor regression in file mode**

Run:

```bash
CDD_CONFIG_MODE=file mvn -f cdd-parent/pom.xml test
python3 scripts/validation/check_module_boundaries.py
bash -n scripts/local/*.sh scripts/nacos/*.sh scripts/testing/*.sh
```

Expected: all tests and static checks exit `0`. Do not use `-DskipTests`, `MAVEN_OPTS` self-attach flags, or historical Surefire reports.

- [ ] **Step 4: Start infrastructure and all services in Nacos mode**

Run:

```bash
export CDD_ENV=local
export CDD_CONFIG_MODE=nacos
bash scripts/local/run_all_services_mysql.sh
bash scripts/local/status_all_services.sh
```

Expected: MySQL, Redis, Nacos 3.0.3, Gateway, and all nine services are healthy; Nacos group `CHENGDD` contains all ten application names.

- [ ] **Step 5: Prove Gateway forwarding with authenticated business requests**

Through `http://127.0.0.1:8080`, perform merchant login and keep the token only in a shell variable:

```bash
login_json="$(curl -fsS -X POST http://127.0.0.1:8080/api/auth/merchant/login -H 'Content-Type: application/json' -d '{"account_name":"merchant_admin","password":"merchant123456"}')"
access_token="$(printf '%s' "$login_json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["access_token"])')"
```

Send `Authorization: Bearer ${access_token}` for these exact requests:

```text
GET /api/auth/me
GET /api/product/spu?merchant_id=1001&store_id=1001
GET /api/order/orders?merchant_id=1001&store_id=1001&user_id=1001
GET /api/report/merchant-dashboard/latest?merchant_id=1001&store_id=1001
GET /api/config/merchant/feature-switches?merchant_id=merchant_1001
```

Expected: every response has HTTP 2xx and JSON `code=0`. Preserve only redacted response summaries in the acceptance report; never store the token.

- [ ] **Step 6: Prove static fallback separately**

Run `GatewayRouteResolverTest#shouldFallBackWhenNoInstanceExists`, then stop one downstream service, restart Gateway with `CDD_CONFIG_MODE=file`, and call that service through its configured static URL after restarting the downstream process. Expected: file-mode forwarding succeeds and Gateway never queries discovery.

- [ ] **Step 7: Stop services and prove deregistration**

Run:

```bash
bash scripts/local/stop_all_services.sh
bash scripts/local/status_all_services.sh
```

Expected: ports `8080` through `8089` have no listeners and each Nacos service host list is empty. Keep infrastructure running until documentation checks finish.

- [ ] **Step 8: Record fresh acceptance evidence and commit compatibility fixes**

Update both reports with timestamp, exact commands, test counts, resolved versions, registered service list, smoke endpoints, and remaining production-only risks. Do not pre-emptively edit production sources in this acceptance task. If a fresh compiler or regression failure occurs, pause acceptance, invoke `superpowers:systematic-debugging`, add a focused red/green repair task to this plan, and commit that repair before resuming this step.

```bash
git add docs/05-delivery/reports/phase1-acceptance-latest.md docs/05-delivery/reports/frontend-acceptance.md
git commit -m "test: record spring cloud alibaba acceptance"
```

### Task 9: Synchronize Documentation and Run the Final Verification Gate

**Files:**
- Modify: `README.md`
- Modify: `docs/02-architecture/Nacos配置命名与加载约定.md`
- Modify: `docs/05-delivery/Nacos配置导入与服务加载说明.md`
- Modify: `docs/05-delivery/本地数据库与Nacos启动说明.md`
- Modify: `docs/05-delivery/骨架验证与验收说明.md`
- Modify: `docs/05-delivery/当前任务收口清单.md`

**Interfaces:**
- Consumes: verified commands and behavior from Task 8.
- Produces: one consistent operator/developer guide matching the implementation.

- [ ] **Step 1: Replace direct-client documentation with official terminology**

Document the exact version table, `cdd-common-nacos` boundary, `spring.config.import` order, `refreshEnabled=false`, official Config/Discovery keys, `{env},nacos` and `{env},file` Profiles, Gateway LoadBalancer behavior, and the database migration exception. Remove links and prose naming `NacosConfigEnvironmentPostProcessor`, `NacosServiceRegistrationListener`, direct `NamingService`, and custom fail-fast switches.

- [ ] **Step 2: Correct local Nacos 3 operator commands**

Use Console URL `http://127.0.0.1:8080/index.html`, client address `127.0.0.1:8848`, gRPC port `9848`, image `nacos/nacos-server:v3.0.3`, group `CHENGDD`, and `bash scripts/nacos/check_nacos_state.sh local`. State explicitly that this local single-node, auth-disabled Compose is not production-ready.

- [ ] **Step 3: Mark deployment work accurately**

Mark code integration and local Docker verification complete only if Task 8 evidence passed. Keep production Compose, Nacos authentication, Nacos clustering, Nginx/HTTPS, and CI/CD marked pending.

- [ ] **Step 4: Run the final verification gate from a clean service state**

Run fresh:

```bash
git diff --check
python3 scripts/validation/check_module_boundaries.py
CDD_CONFIG_MODE=file mvn -f cdd-parent/pom.xml test
bash scripts/testing/run_nacos_integration.sh
bash -n scripts/local/*.sh scripts/nacos/*.sh scripts/testing/*.sh scripts/db/*.sh
rg -n "nacos-client:2|NacosFactory|NamingService|NacosConfigEnvironmentPostProcessor|NacosServiceRegistrationListener" cdd-parent README.md docs/02-architecture docs/05-delivery && exit 1 || true
```

Expected: all commands exit `0`, and the final `rg` finds no retired implementation reference. If Docker is deliberately stopped after acceptance, start only the Nacos Compose service for the integration command and stop it again afterward.

- [ ] **Step 5: Commit documentation separately**

```bash
git add README.md docs/02-architecture/Nacos配置命名与加载约定.md docs/05-delivery/Nacos配置导入与服务加载说明.md docs/05-delivery/本地数据库与Nacos启动说明.md docs/05-delivery/骨架验证与验收说明.md docs/05-delivery/当前任务收口清单.md
git commit -m "docs: document spring cloud alibaba nacos workflow"
```

- [ ] **Step 6: Inspect the final commit range and worktree**

Run:

```bash
git log --oneline --decorate -12
git status --short
git diff --check 2993e8a..HEAD
```

Expected: feature commits are focused, no generated files or secrets are tracked, and any remaining dirty paths are the preserved unrelated user changes identified before execution.

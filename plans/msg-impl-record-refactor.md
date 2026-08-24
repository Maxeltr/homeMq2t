# Plan: Replace MsgImpl with a Java Record (Approach B — eliminate Builder)

## Context

`MsgImpl` is currently a manual immutable value class implementing the `Msg` interface with an
internal mutable `Msg.Builder` (`MsgBuilder`) used for construction, JSON deserialization
(`@JsonDeserialize(as = MsgImpl.MsgBuilder.class)`), and copy-and-modify (`toBuilder()`).

Java toolchain is 25; Spring Boot 3.5.6 / Jackson deserialize records natively.

Goal: convert `MsgImpl` to a `record` and remove the `Builder` pattern everywhere, switching to
immutable `withXxx` copy methods.

## Design

### 1. `Msg.java` — remove the Builder
- Delete nested `Builder` interface.
- Remove `@JsonDeserialize` and `@JsonIgnoreProperties` annotations + imports.
- Remove `toBuilder()`.
- Keep getters `getId()`, `getData()`, `getType()`, `getTimestamp()` (so callers are unchanged).
- Add immutable copy methods returning `Msg`: `withId(...)`, `withData(...)`, `withType(...)`,
  `withTimestamp(...)`.

### 2. `MsgImpl` → record
```java
public record MsgImpl(String id, String data, String type, String timestamp) implements Msg {
    public MsgImpl {
        id = Objects.requireNonNullElse(id, "");
        data = Objects.requireNonNullElse(data, "");
        type = Objects.requireNonNullElse(type, "");
        timestamp = Objects.requireNonNullElse(timestamp, "");
    }
    // getId/getData/getType/getTimestamp delegating to accessors
    // withId/withData/withType/withTimestamp returning new MsgImpl
    // override toString() truncating data at MAX_CHAR_TO_PRINT (preserve logs)
    // equals/hashCode auto-generated over all 4 components (identical to current)
}
```
Delete `MsgBuilder`. Jackson maps websocket JSON into the record via the canonical constructor.

### 3. Call-site migrations
| File | Change |
|------|--------|
| Controller/InputUIController.java | `Msg.Builder` params -> `Msg` |
| Controller/InputUIControllerImpl.java | params `Msg.Builder` -> `Msg`; drop `.build()` |
| Service/ServiceMediatorImpl.java | `readValue(..., Msg.Builder.class)` -> `Msg.class`; catch branch -> record constructor |
| Service/UI/ConnectManagerImpl.java | `new MsgImpl.MsgBuilder("onConnect")` imperative mutation -> `withXxx` reassignment |
| Service/UI/Base64HtmlJsonFormatterImpl.java | `MsgImpl.newBuilder().data(...).build()` -> record |
| Service/UI/DisplayManagerImpl.java | `toBuilder()` -> `withXxx` |
| Service/UI/LocalTaskManagerImpl.java | `toBuilder()` -> `withXxx` |
| Service/UI/MqttManagerImpl.java | `toBuilder()` -> `withXxx` |
| Service/UI/UIServiceImpl.java | `toBuilder().id("").build()` -> `withId("")` |
| Service/UI/DashboardItemCardManagerImpl.java | `toBuilder()` -> `withXxx` (2 sites) |
| Service/UI/DashboardItemMqttSettingManagerImpl.java | `toBuilder()` -> `withXxx` |

## Notes / Risks
- `ConnectManagerImpl.connect()` mutates one builder across branches; must become reassignment
  (`msg = msg.withData(...)`).
- Preserve behavior: null -> `""` normalization, truncated `toString`, identical equals/hashCode.
- Keep interface getter names (`getId()` etc.) to minimize caller churn; record delegates to its
  accessors. Optional alternative: rename everything to record-style `id()/data()/type()/timestamp()`.

## Verification
- `gradlew build`
- `gradlew test`
- `gradlew bootRun` + sanity-check websocket JSON deserialization into the record

## Todo checklist
- [ ] Rewrite Msg.java: remove nested Builder, @JsonDeserialize/@JsonIgnoreProperties, and toBuilder(); add withId/withData/withType/withTimestamp returning Msg
- [ ] Convert MsgImpl.java to a record MsgImpl(String id, String data, String type, String timestamp) implements Msg with compact constructor (null normalization), delegating getters, withXxx copy methods, and truncated toString; delete MsgBuilder
- [ ] Update InputUIController.java and InputUIControllerImpl.java: replace Msg.Builder parameters with Msg and drop .build() calls
- [ ] Update ServiceMediatorImpl.java: readValue into Msg.class and replace MsgImpl.newBuilder() fallback with record construction
- [ ] Update ConnectManagerImpl.connect(): convert imperative builder mutation to withXxx reassignment
- [ ] Update Base64HtmlJsonFormatterImpl.java: replace newBuilder().data(...).build() with record construction
- [ ] Update toBuilder() call sites to withXxx: DisplayManagerImpl, LocalTaskManagerImpl, MqttManagerImpl, UIServiceImpl, DashboardItemCardManagerImpl, DashboardItemMqttSettingManagerImpl
- [ ] Run gradlew build and gradlew test; verify compilation and tests pass
- [ ] Run gradlew bootRun and sanity-check websocket JSON deserialization into the record
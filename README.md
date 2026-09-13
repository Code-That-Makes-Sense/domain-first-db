# Domain First, Database Last

This is the companion code for a four-part series in
[Code That Makes Sense](https://codethatmakessense.substack.com). It is one webshop.
We build it database-first, then take it to domain-first one module at a time,
without a rewrite. The git history is the content. Every commit is one step, it
builds, and its tests pass. Read it in order.

## The plan

1. **Part 1, the legacy webshop** (in progress). We build the webshop the way most teams do, database first. The entities mirror the tables, the rules live in services, every feature adds a migration, and every test boots Spring and H2. At the end we expose it over HTTP, so you can try it.
2. **Part 2, a module built domain-first** (not started). We add the next feature, returns, the other way round and inside the same application. Behavior comes first, the domain owns its ports, the use cases run on in-memory adapters, and the schema comes last.
3. **Part 3, the legacy module migrated** (not started). We strangle the order module onto an aggregate, one method at a time, on the tables it already has. There is no rewrite and no dual write. The schema contracts afterwards.
4. **Part 4, the tests and the numbers** (not started). We retire the tests that boot the world, turn the architecture into a build rule, split the fast suite from the full one, and measure.

Each part ends with a tag, from `part-1` to `part-4`. `git checkout part-2` shows
the code as the second post leaves it. `git log --oneline part-1..part-2` lists
the steps that post walks through.

## Status

Part 1 is in progress. The latest step is "feat: reserve stock while an order is open". A second module, stock, arrives. Placing an order reserves stock, cancelling releases it, shipping consumes it.

The earlier steps of this part:

- chore: bootstrap the Spring Boot project
- feat: place orders
- feat: pay for orders
- feat: ship orders
- feat: cancel orders

## Run it

You need Java 25. Gradle comes with the wrapper. There is no database to install,
because H2 runs in memory and Flyway creates the schema on start.

```sh
./gradlew test       # everything
```

## Layout now

```text
com.codethatmakessense.shop
├── order   the legacy service and the JPA entities that mirror the tables
└── stock   reserve, release and consume stock
```

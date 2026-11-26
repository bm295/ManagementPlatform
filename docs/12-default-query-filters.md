# Default query filters in Java

## Architectural decision

The application currently uses Java 21, plain Maven, and an in-memory repository, not an ORM.
EF Core's `HasQueryFilter` is therefore implemented here as a repository-level read
policy rather than adding an unused Hibernate annotation or changing the persistence stack.

`OrderQueryFilter.activeTenantsOnly()` hides orders associated with inactive tenants
on ordinary reads. `InMemoryOrderRepository` applies the same predicate to:
- `findById` (including endpoints and use cases depending on this lookup)
- `search` (before sorting and pagination)
- `count` (so pagination metadata does not leak hidden rows)

`save` is unchanged: query filtering is a read concern, not authorization for writes.
`findTenantById` is deliberately unfiltered because it is used for tenant identity
resolution during order creation; hiding an inactive tenant at that point could cause
a duplicate identity to be constructed. Existing business rules must independently
reject operations for inactive tenants.

A privileged recovery method `findByIdIgnoringFilters` exists only on the concrete
infrastructure adapter. It is not on `OrderRepository` and must not be exposed to
public HTTP handlers.

## Limitations and future migration

Unlike a database-level row-level security policy, this is a repository default, not
a guarantee against every access to stored objects. Other repositories, raw database
queries, and administrative reads require their own security design. No request-supplied
tenant identifier is trusted as an authorization boundary.

If Hibernate/JPA is introduced later, a static rule can use `@SQLRestriction`, while
dynamic Hibernate filters can use `@FilterDef` and `@Filter` with carefully scoped
session activation. Hibernate filters are not automatically active for every session
unless configured and enabled accordingly, and direct entity access/native SQL need
separate verification. Tenant separation should be driven by authenticated context and
preferably enforced as defense in depth at the storage layer.

## Verification

Run `mvn test`. `InMemoryOrderRepositoryGlobalQueryFilterTest` verifies filtered
identifier lookup, name queries, count, pagination, explicit bypass and composition.

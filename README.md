# Developer Preview LogicNG 3

This is a developer preview of the new major version of <a href="https://www.logicng.org">LogicNG</a>.  LogicNG is a
Java Library for creating, manipulating and solving Boolean and Pseudo-Boolean formulas.  Its main focus lies on
memory-efficient data-structures for Boolean formulas and efficient algorithms for manipulating and solving them.  The
library is designed to be used in industrial systems which have to manipulate and solve millions of formulas per day.

This is a developer preview, pre-alpha, and **not ready for production use**.  For a stable version of LogicNG use the
version at  [https://github.com/logic-ng/LogicNG](https://github.com/logic-ng/LogicNG).  For a use case of LogicNG 3 you
can look at our [BooleRules](https://github.com/booleworks/boolerules) project.

## Changes

LogicNG 3 is a major rewrite of many parts of LogicNG and definitely breaks backwards-compatibility.  Here are some
highlighted features of the new release already present in the first developer preview:

- *New formula factory that does not cache formulas.*  Therefore all formula classes are now interfaces which are
  implemented by two implementations: the existing caching one and a new native non-caching one.  Whereas formula
  caching can be beneficial in a lot of use cases, there are some cases, where heap pollution is an issue and a
  non-caching factory can yield better results in terms of heap usage and performance.
- *New model enumeration algorithm.*  The existing model enumeration algorithm was rewritten from the ground up and uses
  now a recursive approach.  In a lot of real-world use cases, this can speed up model enumeration by a two-digit
  factor.  The new enumeration is also highly configurable and can be extended with own strategies.
- *Clean up of state handling in transformations.*  A clearer distinction between caching (optional) and mandatory state
  handling in algorithms like Tseitin or Plaisted-Greenbaum has been introduced.
- *More flexible cache handling in transformations, functions, and predicates.*  All caching formula manipulations can
  now be used more flexible with temporary caches or no cache at all for both formula factory types.

Planned features (no guarantee they will make the 3.0 release)

- *New unified SAT solver.*  The three solvers of LogicNG 2 (MiniSAT, Glucose, MiniCard) are merged into one unified
  solver which uses the best features of each solver.
- *Thread-safe formula factory.*  A formula factory which can be used in a multi-threaded environment.
- *Unified safe SAT solver interface.*  A new interface to the SAT solver which allows combination of features like
  assumptions, proof tracing, backbone generation, model enumeration, etc. in a safe manner.

Furthermore there are some extensions planned for LogicNG:

- A serialization library for formulas, BDDs, and most important SAT solvers based on Google Protocol Buffers.  A
  developer preview can be found [here](https://github.com/booleworks/logicng-serialization).
- An extension to embed Microsoft's Z3 SMT solver into LogicNG allowing it's usage as SAT solver but more importantly
  also as SMT solver and therefore extend LogicNG with the ability to handle different theories.

## Funding

LogicNG development is partly funded by the [SofDCar project](https://sofdcar.de/):

<a href="https://www.sofdcar.de"><img src="https://github.com/booleworks/logicng-core/blob/main/doc/logo/bmwk.png" alt="logo" width="200"></a>

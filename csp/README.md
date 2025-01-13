# Developer Preview LogicNG 3 CSP

This library is an extension of LogicNG for finite integer arithmetic.  It can be used to create and manage arithmetic
expressions and predicates.  It is directly integrated into LogicNG and can be used with many existing features.

This is a developer preview, pre-alpha, and **not ready for production use**.

## Features

- **Arithmetic Expression and Predicates:** Analogous to LogicNG, you can create and manage terms and predicates using a
  `CspFactory`.  Terms can be integer variables with a finite domain or arithmetic functions, such as addition,
  subtraction, multiplication, maximum, and more.  Predicates can then be used to formulate constraints on terms.  For
  example equality of two terms.
- **Propositional Encodings:** Predicates and CSP can be encoded to propositional formulas in CNF
  using [Order Encoding]("https://doi.org/10.1007/s10601-008-9061-0")
  or [Compact Order Encoding]("https://doi.org/10.1007/978-3-642-21581-0_36").  The propositional formula can then be
  used with any other LogicNG feature.  In particular, it can be solved with a SAT-solver.  Models of encoded formulas
  can then be decoded into integer assignments that satisfy the problem.
- Additionally, the library provides various **utilities**, such as Backbones, Model Enumeration, Model Counting,
  Value-restricted Solving, Substitution, and more.  Many of those features are a direct translation of the
  propositional
  algorithm.  However, there are many annoying details that makes it tedious to implement them yourself.  So feel free
  to
  use our implementation before you torture yourself with a bunch of edge cases :)

## Getting Started

This section shows some basic examples on how to get started with this extension.  Note that, this library and the
underlying LogicNG version are still in an unstable development state. So keep in mind, that things might change and
that we will not always update the documentation right away.

### Create Terms and Predicates

Like in LogicNG, the central data-structure is a factory.  The `CspFactory`.  Internally it is also based on a
`FormulaFactory`.  To create a CspFactory we only need to pass a FormulaFactory to the constructor.  The FF can be
either a caching or non-caching factory, both work.  However, the CspFactory will always cache its terms and
predicates, no matter which FF is used. An FF can also be used in multiple CspFactories at once (not concurrently
though) and it can also be an already used one.

Let's create a `CspFactory`:

```java
import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.formulas.FormulaFactory;

FormulaFactory f = FormulaFactory.caching();
CspFactory cf = new CspFactory(f);
```

Now we can use the factory to define new integer variables.  A variable is defined by a name and a finite domain.  There
are range-domains, which are closed intervals, and set-domains, which are a set of all values in the domain.

```java
IntegerVariable a = cf.variable("A", 0, 10);
IntegerVariable b = cf.variable("B", List.of(2, 3, 5, 7, 11));
```

"A" is a ranged variable that can be assigned to any value in `[0,10]`.  "B" is non-continuous variable that can be
assigned to `{2,3,5,7,11}`.

We can now use these variables to build up more complex terms:

```java
IntegerConstant c2 = cf.constant(2);    // 2
Term t1 = cf.add(a, c2);    // "A" + 2  
Term t2 = cf.sub(t1, b);    // ("A" + 2) - "B"
Term t3 = cf.abs(t2);       // | ("A" + 2) - "B" |
Term t4 = cf.mul(a, b);     // "A" * "B"
```

As you can see the library provides all function you need to build up your arithmetic expressions.  There are:
Addition (`add()`), Subtraction(`sub()`), Negation(`minus()`), Multiplication (`mul()`), Integer Division (`div()`),
Modulo (`mod()`), Absolute Function (`abs()`), Maximum (`max()`), and Minimum (`min()`).

Now, we can build predicates with the constructed terms:

```java
CspPredicate p1 = cf.eq(t3, c2); // | ("A" + 2) - "B" | = 2
CspPredicate p2 = cf.ne(t4, t1); // "A" * "B" != "A" + 2 
CspPredicate p3 = cf.le(t1, t2); // t1 <= t2
CspPredicate p4 = cf.ge(t1, t2); // t1 >= t2
CspPredicate p5 = cf.lt(t1, t2); // t1 < t2
CspPredicate p6 = cf.gt(t1, t2); // t1 > t2
CspPredicate p6 = cf.allDifferent(List.of(a, b, t1)) // "A" != "B" != t1
```

`CspPredicate` extends from LogicNG's `Predicate`. It can, thus, be used inside any ordinary LogicNG formula:

```java
Formula formula = f.and(f.variable("V"), p1) // V & ( | ("A" + 2) - "B" | = 2 )
```

*Remark:* This is handy for many cases and also intended to be used.  However, the predicate support in LogicNG is in an
early stage and many algorithms do not work as expected or at all with predicates.  So, if you use a formula with
predicates in other LogicNG features, make sure that it works properly.

### Decomposition & CSPs

In general, a *Constraint Satisfaction Problem* is a set of integer variables and constraints.  In this library, we
usually express a CSP as a Formula with Predicates and Terms.  This is most convenient and flexible method for a user.
The encoding algorithms, however, can only deal with a very limited subset of formulas and predicates.  Therefore, it
is necessary to decompose formulas, that are not directly encodable, into simpler formulas before encoding them.  The
result of a decomposition is stored in a `Csp` data structure.

The factory offers a method `buildCsp()` that can be used to decompose the formula and build the `Csp`.  One can either
pass any formula or a list of predicates, which is interpreted as the conjunction of the predicates:

```java
CspPredicate predicate1;
CspPredicate predicate2;
Formula formula;

Csp csp1 = cf.buildCsp(predicate1);
Csp csp2 = cf.buildCsp(predicate1, predicate2);
Csp csp3 = cf.buildCsp(formula);
```

You also have the option to manually decompose (`cf.decompose`, `CspDecomposition`) and to manually build the `Csp`
(`Csp.Builder`) or to avoid the `Csp` data structure at all by directly encoding variables and predicates.

### Encode CSPs and Predicates

Now to the most important part: the encoding. As of now, we are providing two algorithms _Order Encoding_ and _Compact
Order Encoding_.  Order Encoding is a simpler encoding that usually can be solved faster but also yields larger
formulas.  The Compact Order Encoding yields a smaller formula, but usually takes longer to solve.  So a rule of
thumb is: Use Order Encoding as long as you can, but when the formula size can no longer be handled switch to the
Compact Order Encoding.

`CspEncodingContext` stores all the encoding information, such as the boolean variables used to represent an integer
variable.  This is necessary for decoding the model afterward but also allows to incrementally encode predicates and
variables.  So you want to reuse one EncodingContext for everything that belongs together and will in the same encoding.

A `CspEncodingContext` can be created using one of two factory methods: `order()` and `compactOrder(int base)`:

```java
import com.booleworks.logicng.csp.encodings.CspEncodingContext;

OrderEncodingContext context1 =
        CspEncodingContext.order();
CompactOrderEncodingContext context2 =
        CspEncodingContext.compactOrder(10);
CompactOrderEncodingContext context3 =
        CspEncodingContext.compactOrder(5); 
```

The Compact Order Encoding takes a `base` as argument.  Usually, 10 is a good default for that, but you might want to
experiment with different values from 5 upwards.

The factory, again, provides functions for performing the encoding:

```
List<Formula> encoded1 = cf.encodeCsp(csp, context);
cf.encodeCsp(csp, context, result);
```

There are two variants: The one without third argument returns a list of all clauses of the encoding.  But, usually, you
want to have the result on the solver anyway.  So there is also a method where you can pass an `EncodingResult` which
allows you to write the result directly to a solver.

As mentioned before, it is also possible to directly encode variables and predicates without `Csp`.  For that, you can
use the methods `encodeVariable()` and `encodeConstraint()`.

```
cf.encodeVariable(v1, context, result);
cf.encodeVariable(v2, context, result);
cf.encodeConstraint(predicate1, context, result);
cf.encodeConstraint(predicate2, context, result);
```

If you use this interface, you are responsible that all variables that are used in the predicates are encoded.  The
order is not important, you can first encode predicates and the variables afterward.  `encodeConstraint()` decomposes
the predicate, so you can pass any predicate, and you don't need to care about potential auxiliary variables.  They are
encoded automatically.

Assuming you obtained a model from solving the encoded formula, you can decode it:

```
IntegerAssignment intModel1 = 
  cf.decode(model, csp, context);
IntegerAssignment intModel2 = 
  cf.decode(model, List.of(v1), List.of(), context);
```

You can either pass the `Csp` data structure, this will decode all variables that where originally in formula that was
used to build the `Csp`, or you can pass explicit lists of integer and boolean variables that you want to have decoded.

## Funding

LogicNG development is partly funded by the [SofDCar project](https://sofdcar.de/):

<a href="https://www.sofdcar.de"><img src="https://github.com/booleworks/logicng-core/blob/main/doc/logo/bmwk.png" alt="logo" width="200"></a>

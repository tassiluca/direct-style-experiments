+++
title = "Direct style experiments presentation"
outputs = ["Reveal"]
+++

# Direct Style for **Functional** **Reactive** Programming in *Scala*

<hr/>

Tassinari Luca

---

## Effects, Monads and Capabilities

- Most of the time when we say *effect* we mean *monads*
- Monads do have cons:
  - syntactic and pedagogical overhead
  - awkward to integrate with regular control structures
  - composing monads is tricky even with monad transformers, MLT, ...

- *Research-field*: instead of pushing effects and resources into frameworks, upgrade the _type system_ to track them directly in the program {{< math "\Rightarrow" />}} **CAPabilities for RESources and Effects (CAPRESE)** @ Programming Methods Laboratory EPFL

  - to have an effect you need the capability to have that effect
  - **Capability**: a value that is passed (usually implicitly) to the function that need to perform the effect the capability enables

---

## Suspension Effect

---

## Aside: Boundary & Break

---

## Scala Gears

---



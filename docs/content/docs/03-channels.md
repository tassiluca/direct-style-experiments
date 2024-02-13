# Channels as a communication primitive

Both in Kotlin and Scala **channels** are the primitive communication mean to exchnage `Future`s results.

- Channels in gears
  - design: ephimeral Async sources
    - work stealing behavior
    - fairness (?)
    - closable
      - at closing the reader cannot anymore read values (differ from kotlin)
    - three types: Buffered, Unbounded, Sync

- Channels in Kotlin (w.r.t. gears)
  - pipeline (not supported in Gears)
  - closable

## Organization analyzer example
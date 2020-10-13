# Random Seminar

### Introduction

This is a little application by which to run a "random seminar", over a teleconference or in-person.
It lets you populate a list of participants, from which a "next" speaker will be randomly drawn.
You then run a timer, which limits speaking time up to a present length. When that time's up, click
"next" again and continue.

Participants are drawn "without replacement" until all participants have spoken. Then
all participants are added back into the pool for another round.

There are some "cheats":

1. Select a participant and hit `<return>` to (nonrandomly) choose that participant as a speaker
2. Select one or more participants and hist `<space>` to toggle their `spoken-yet` status
3. Select one or more participants and hit <delete> or <backspace> to remove that participant

To change the time limit, enter a new value in the `limit:` text field and _don't forget to hit return_.

### Build and run

You'll need [sbt](https://www.scala-sbt.org/).

Clone or download this repository. Within its top directory...

```
$ sbt
sbt:randomseminar> stage
[info] Wrote /Users/swaldman/Dropbox/BaseFolders/development-why/gitproj/randomseminar/target/scala-2.12/randomseminar_2.12-0.0.1-SNAPSHOT.pom
[warn] There may be incompatibilities among your library dependencies; run 'evicted' to see detailed eviction warnings.
[info] Main Scala API documentation to /Users/swaldman/Dropbox/BaseFolders/development-why/gitproj/randomseminar/target/scala-2.12/api...
[info] Compiling 1 Scala source to /Users/swaldman/Dropbox/BaseFolders/development-why/gitproj/randomseminar/target/scala-2.12/classes ...
model contains 8 documentable templates
[info] Main Scala API documentation successful.
[success] Total time: 5 s, completed Oct 12, 2020, 9:06:52 PM
```

You can run the application directly with `sbt`:

```
sbt:randomseminar> run
```

Or you can break out with <ctrl-d> and run the script `random-seminar` which you will find in `target/universal/stage/bin`:

```
$ ./target/universal/stage/bin/randomseminar &
```

Either way, the application should pop up!


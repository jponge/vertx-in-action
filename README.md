[![Build Status](https://travis-ci.com/jponge/vertx-in-action.svg?branch=master)](https://travis-ci.com/jponge/vertx-in-action)

# Vert.x in Action book examples

👋 Welcome!

These are the working examples for [Vert.x in Action](https://www.manning.com/books/vertx-in-action) (ISBN 9781617295621) from [Manning Publications Co](https://www.manning.com/) and written by [Julien Ponge](https://julien.ponge.org/).

## How to open and run the examples?

Readers of the book should directly open projects from subfolders: they are all independent.

You will find both Gradle and Maven build descriptors for each project, so you can load the projects with text editors or integrated development environments such as IntelliJ IDEA, Eclipse IDE or Microsoft Visual Studio Code.

As an example if you want to build the chapter 1 with Gradle, open a terminal and run:

    $ cd chapter1
    $ ./gradlew build

or with Maven run:

    $ cd chapter1
    $ mvn package

The book examples work best using some Unix environment: Linux, macOS or the Windows Subsystem for Linux from Microsoft.

## What is the structure of the repository?

The following folders are available:

* `chapter1`
* `chapter2`
* `chapter3`
* `chapter4`
* `chapter5`
* `chapter6`
* `part2-steps-challenge` (covers chapters 7 to 12)
* `chapter13`

The `master` branch is where you must look for working examples.

Chapter 12 provides variants of the same code which you can get from the following branches:

* `chapter12/public-api-with-timeouts`
* `chapter12/public-api-with-circuit-breaker`
* `chapter12/public-api-with-circuit-breaker-and-timeouts`

## Will there be updates?

The book went to production with Manning in August 2020.

This repository contains samples against Eclipse Vert.x 4.0.0.Beta3 (see tag `vertx-4.0.0.Beta3`) that was released in September 2020.

At my own discretion I _may_ update to newer versions of Vert.x when they are published.

Note that the Vert.x core team has made a goal of ensuring that Vert.x 4.0.0 will work against all examples in this repository.

## Can I contribute?

Due to the nature of this project I will not accept any contribution to this repository.

## What if I have a question / issue?

If you are a Manning customer then you have access to forums.
Please refer to [Vert.x in Action on the Manning website](https://www.manning.com/books/vertx-in-action) where a link to the forum is provided.

If you have a question on Vert.x then please get in touch with the [Eclipse Vert.x community](https://vertx.io).
There are several channels that you can use including public mailing-lists and chat.

If you have a problem with your book order or any special request, then please contact Manning.

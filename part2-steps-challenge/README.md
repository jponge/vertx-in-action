# 10k steps challenge application

The application is based on a set of event-driven micro-services.

## Building the project

You need _Docker_ to be up and running in order to build and run the project.
Make sure that you have sufficient user permissions to create and start container images with _Docker_.

To build all services run:

    ./gradlew build

Tests are being run as part of the build process, relying on container images to be started to run integration tests against various middleware such as _Apache Kafka_ or _PostgreSQL_.
Things are not always deterministic when containers are being started, so you may encounter occasional _flaky_ tests.
You may also not have enough resources to run all containers while running tests, which may explain potential errors.

To build all services without running tests run:

    ./gradlew assemble

## Running the project

First open a terminal to start all middleware containers with _Docker Compose_:

    docker-compose up

The micro-services are specified in a `Procfile` and there exist many tools to run them:

* in the book I recommend [foreman](https://github.com/ddollar/foreman) which is written in Ruby,
* you can alternatively use [hivemind](https://github.com/DarthSim/hivemind) which is written in Go and thus is available as a zero-dependency executable,
* you can find another tool, but `foreman` and `hivemind` are those that I have personally tested.

Open another terminal to run the services.
With `hivemind`:

    hivemind

or with `foreman`:

    foreman start

The services should now start.
There are 2 web applications you can use with a web browser:

* the user web application is at http://127.0.0.1:8080
* the dashboard is at http://127.0.0.1:8081

You can interact with the other HTTP services on ports 3000, 3001 and 4000.

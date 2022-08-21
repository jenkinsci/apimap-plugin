Apimap.io Jenkins Plugin
=====

🥳 **Happy Coding** 🥳

This section is targeted to developers that want to use the Jenkins Plugin provided by the project.

## Table of Contents

* [Introduction](#introduction)
* [Getting Started](#getting-started)
* [Other Resources](#other-resources)

## Introduction

### Global Configuration

#### Debug Mode

More extensive logging. Please note that the output is generated using System.out.println and will be printed to STDOUT.

#### Dryrun Mode

Does not communicate to any APIs on any actions and returns a default object from create resources.

#### Configuration Options

##### Update Global Build Status

| Status          | Description                                          |
|-----------------|------------------------------------------------------|
| Enabled         | Updated the global build status to match step result |
| Disabled        | Only returns the validation result as a object       |

#### ApiMap.io instance URL

Complete URL to the API instance the build system should connect to. 

> E.g. http://127.0.0.1:8080

## Getting Started

This plugin is created to be used with Pipeline-as-Code and returns a result object depending on the step activated.

### Results from the Validate Step

The following Java-object is returned from the validate step:

| Variable    | Description                                                                                  |
|-------------|----------------------------------------------------------------------------------------------|
| status      | The status of the action. If the API was created, updated, something failed or if it crashed |
| description | An explanation to the status                                                                 |

```java
public class ValidateResult {
    public enum Status {
        VALID,
        MISSING,
        INVALID,
        ABORTED,
        FAILED,
        UNKNOWN
    }

    private final Status status;
    private final String description;

    public ValidateResult(Status status, String description) {
        this.status = status;
        this.description = description;
    }

    @Whitelisted
    public Status getStatus() {
        return status;
    }

    @Whitelisted
    public String getDescription() {
        return description;
    }
}
```

### Results from the Publish Step

The following Java-object is returned from the publish step:

| Variable    | Description                                                                                                             |
|-------------|-------------------------------------------------------------------------------------------------------------------------|
| status      | The status of the action. If the API was created, updated, something failed or if it crashed                            |
| description | An explanation to the status                                                                                            |
| token       | If the status is created theres is also a token returned with the result. This token must be used in any future updates |

> Note: Do not use the token or description to test if the API is created or updated. Use the status ENUM to determine the status.

```java
public class PublishResult {
    public enum Status {
        CREATED,
        UPDATED,
        FAILED,
        ABORTED,
        UNKNOWN
    }

    private final Status status;
    private final String description;
    private final String token;

    public PublishResult(Status status, String description) {
        this.status = status;
        this.description = description;
        this.token = null;
    }

    public PublishResult(Status status, String description, String token) {
        this.status = status;
        this.description = description;
        this.token = token;
    }

    @Whitelisted
    public Status getStatus() {
        return status;
    }

    @Whitelisted
    public String getDescription() {
        return description;
    }

    @Whitelisted
    public String getToken() {
        return token;
    }
}
```

## Other Resources
___

- [Hypermedia as the Engine of Application State (HATEOAS) ](https://en.wikipedia.org/wiki/HATEOAS)
- [JSON:API — A specification for building APIs in JSON](https://jsonapi.org/)
- [Jenkins Plugin development](https://www.jenkins.io/doc/developer/plugin-development/)

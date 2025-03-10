# Nordnet API v2 Java Sample

## Disclaimer
The code examples are to be used in testing *only* (!). They are provided as is
without any warranty of any kind, see `LICENSE` for more information.

_Note that all the JSON objects end with newline. As such you need to listen
and read from the buffer when a full object has been transferred._

## Requirements
* There is no longer any test environment available. Contact Nordnet Trading Support to get started.
* [Java 21](https://www.oracle.com/java/technologies/downloads/) and [Maven](https://maven.apache.org/install.html) installed.

## Install and run
1. Download the `nordnet/next-api-v2-examples` repo
2. Update the API_KEY and PRIVATE_KEY_PATH constants in `com.nordnet.api.NordnetApiClientSample.java`
3. Update the main class in `pom.xml` to the correct main class:

```xml
<mainClass>com.nordnet.api.NordnetApiClientSample</mainClass>
```

4. Build and run the application:

```bash
cd java
mvn clean package
java -jar target/nordnet-api-client-sample-1.0.0.jar
```

Running the application should output something similar to the following:

```
Checking Nordnet API status...
<< HTTP request GET https://www.nordnet-test.se/api/2/
Nordnet API status: {"valid_version":true,"message":"","timestamp":1528730480984,"system_running":true}
Starting authentication challenge...
<< HTTP request POST https://www.nordnet-test.se/api/2/login/start?service=NEXTAPI&api_key=YOUR_API_KEY
Received challenge: CHALLENGE_STRING
Completing authentication...
<< HTTP request POST https://www.nordnet-test.se/api/2/login/verify?service=NEXTAPI&api_key=YOUR_API_KEY&signature=ENCODED_SIGNATURE
Successfully authenticated. Session key: 01ba12bfd3244e63ad52c1731a54e2f3f98f949e

Connecting to feed pub.next.nordnet.se:443...

>> JSON updates from public feed
{
    "cmd": "login",
    "args": {
        "session_key": "01ba12bfd3244e63ad52c1731a54e2f3f98f949e",
        "service": "NEXTAPI"
    }
}
>> JSON updates from public feed
{
    "type": "price",
    "data": {
        "m": 11,
        "i": "101",
        "bid": 65.0,
        "bid_volume": 26200,
        "ask": 0.0,
        "ask_volume": 0,
        "last": 0.0,
        "last_volume": 0,
        "high": 0.0,
        "low": 0.0,
        "open": 0.0,
        "close": 64.36,
        "turnover": 0.0,
        "turnover_volume": 0,
        "vwap": 64.19,
        "tick_timestamp": 1528726500001,
        "trade_timestamp": 1528726500001
    }
}
```

## Questions
If you have technical questions then,
1. Check out the code, it is documented
2. Read the [API documentation](https://www.nordnet.se/externalapi/docs)

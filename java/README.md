## Disclaimer
The code examples are to be used in testing *only* (!). They are provided as is
without any warranty of any kind, see `LICENSE` for more information.

## Requirements
* Register an account to get access to the
  [forums](https://api.test.nordnet.se) and the test system
  [here](https://api.test.nordnet.se/account/register). Your username
  and password are needed to authenticated to the API
* Read about the [test system](
  https://api.test.nordnet.se/projects/api/wiki/Test_system) to
  learn about the delimitations and how the test market works
* [Java](http://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html)
  installed

## Install and run
1. Download the `nordnet/next-api-v2-examples` repo
2. Configure your IDE to JDK 10 or higher and download the Maven dependencies 
3. Add username and password as program arguments 
4. Run the program and you should see the following
```json
{
    "message": "",
    "system_running": true,
    "timestamp": 1528730480984,
    "valid_version": true
}
>> Response from logging into the session
{
    "country": "SE",
    "environment": "exttest",
    "expires_in": 300,
    "private_feed": {
        "encrypted": true,
        "hostname": "priv.api.test.nordnet.se",
        "port": 443
    },
    "public_feed": {
        "encrypted": true,
        "hostname": "pub.api.test.nordnet.se",
        "port": 443
    },
    "session_key": "01ba12bfd3244e63ad52c1731a54e2f3f98f949e"
}
>> Response from Price Feed
{
    "data": {
        "ask": 0.0,
        "ask_volume": 0,
        "bid": 65.0,
        "bid_volume": 26200,
        "close": 64.36,
        "high": 0.0,
        "i": "101",
        "last": 0.0,
        "last_volume": 0,
        "low": 0.0,
        "m": 11,
        "open": 0.0,
        "tick_timestamp": 1528726500001,
        "trade_timestamp": 1528726500001,
        "turnover": 0.0,
        "turnover_volume": 0,
        "vwap": 64.19
    },
    "type": "price"
}
```

## Common issues
* ClassNotFoundException: Check your Maven
* NEXT\_LOGIN\_INVALID\_LOGIN\_PARAMETER: double-check your [password](https://api.test.nordnet.se/login)

## Questions
If you have technical questions then,
1. Check out the code, it is documented
2. Read the [documentation](https://api.test.nordnet.se/api-docs/index.html)
3. Ask questions in the
   [forum](https://api.test.nordnet.se/projects/api/boards)

Otherwise, contact Nordnet trading support with the contact details provided
[here](https://api.test.nordnet.se)

## Disclaimer
The code examples are to be used in testing *only* (!). They are provided as is
without any warranty of any kind, see `LICENSE` for more information.

_Note that all the JSON objects end with newline.  As such you need to listen
and read from the buffer when a full object has been transferred_

## Requirements
* There is no longer any test environment available. Contact Nordnet Trading Support to get started.
* [Python 3](https://www.python.org/downloads/) and
  [pip](https://pip.pypa.io/en/stable/installing/) installed

## Install and run
1. Download the `nordnet/next-api-v2-examples` repo
2. Run and provide your username and password as arguments
```
cd python3
pip3 install -r requirements.txt
./test_program.py [insert username] [insert password]
```
Running the test program should output something that looks similar to the following example output
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
    "environment": "prod",
    "expires_in": 300,
    "private_feed": {
        "encrypted": true,
        "hostname": "priv.next.nordnet.se",
        "port": 443
    },
    "public_feed": {
        "encrypted": true,
        "hostname": "pub.next.nordnet.se",
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
* SyntaxError: check that your Python version is 3 or higher

## Questions
If you have technical questions then,
1. Check out the code, it is documented
2. Read the [API documentation](https://www.nordnet.se/externalapi/docs)

## Disclaimer
The code in this repo is intended as examples only. It is provided as is without
any warranty of any kind, see `LICENSE` for more information.

_Note that all the JSON objects end with newline.  As such you need to listen
and read from the buffer when a full object has been transferred._

## Requirements
* There is no longer any test environment available. Contact Nordnet Trading Support to get started.
* [Python 3](https://www.python.org/downloads/) and
  [pip](https://pip.pypa.io/en/stable/installation/) installed.

## Install and run
1. Download the `nordnet/next-api-v2-examples` repo
2. Run test_program.py and provide your API-key, country code and the path to your private key

This repo also includes a small program called sign.py which takes a private key file and a
challenge and prints the corresponding signature. This can be used to debug your
own signature code.
```
cd python3
pip3 install -r requirements.txt
./test_program.py [insert API-key] [insert country code] [insert private key file path]
```
Running the test program should output something that looks similar to the following example output
```json
Checking Nordnet API status...
<< HTTP request GET /api/2/
{
    "message": "",
    "system_running": true,
    "timestamp": 1741781442953,
    "valid_version": true
}
Starting authentication challenge...
<< HTTP request POST /api/2/login/start
{
    "challenge": "f0dcd2fa-92b1-4151-93af-61697eae217a"
}
Received challenge: f0dcd2fa-92b1-4151-93af-61697eae217a
Completing authentication...
<< HTTP request POST /api/2/login/verify
{
    "expires_in": 1800,
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
    "session_key": "15a6c4db-05b9-481c-b94a-ccffed83e693"
}
Successfully authenticated. Session key: 15a6c4db-05b9-481c-b94a-ccffed83e693

Connecting to feed pub.next.prod.nordnet.se:443...

<< Sending cmd to feed: {'cmd': 'login', 'args': {'session_key': '15a6c4db-05b9-481c-b94a-ccffed83e693', 'service': 'NEXTAPI'}}
<< Sending cmd to feed: {'cmd': 'subscribe', 'args': {'t': 'price', 'm': 11, 'i': '101'}}

Starting receiving from socket...

>> JSON updates from public feed
{
    "data": {
        "ask": 87.0,
        "ask_volume": 1200,
        "bid": 83.44,
        "bid_volume": 1,
        "close": 77.22,
        "high": 87.0,
        "i": "101",
        "id": 16750901,
        "last": 87.0,
        "last_volume": 154,
        "low": 82.96,
        "m": 11,
        "open": 83.12,
        "tick_timestamp": 1741781407194,
        "trade_timestamp": 1741780275120,
        "turnover": 8492556.03,
        "turnover_volume": 101883,
        "vwap": 84.56
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

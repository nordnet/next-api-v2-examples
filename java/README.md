## Disclaimer
The code examples are provided as is
without any warranty of any kind, see `LICENSE` for more information.

_Note that all the JSON objects end with newline.  As such you need to listen
and read from the buffer when a full object has been transferred_


## Requirements
* Java 15
* There is no longer any test environment available.
Contact Nordnet Trading Support to get started.

## Install and run
1. Download the `nordnet/next-api-v2-examples` repo
2. Configure your IDE to JDK 15 or higher and download the Maven dependencies 
3. Add username and password as program arguments 
4. Run the program and you should see the following
```json
>> Response from checking API status
{
  "timestamp" : 1606142695922,
  "valid_version" : true,
  "system_running" : true,
  "message" : ""
}
>> logging in as: '<username>' with pwd: '<password>'
>> Response from logging into the session
{
"remaining" : 300,
"session_key" : "<session_key>",
"expires_in" : 1800,
"environment" : "prod",
"country" : "SE",
"private_feed" : {
"hostname" : "priv.next.nordnet.se",
"port" : 443,
"encrypted" : true
},
"public_feed" : {
"hostname" : "pub.next.nordnet.se",
"port" : 443,
"encrypted" : true
}
}
>> Response from /accounts
[ {
"accno" : <my account number>,
"accid" : 1,
"type" : "Stock- and mutual funds account",
"atyid" : 108,
"default" : true,
"alias" : "<MY NORDNET API ACCOUNT>"
} ]
>> Account number is <my account number>
CN=pub.next.nordnet.se, O=Nordnet Bank AB, L=Stockholm, C=SE, SERIALNUMBER=516406-0021, OID.1.3.6.1.4.1.311.60.2.1.3=SE, OID.2.5.4.15=Private Organization
CN=DigiCert SHA2 Extended Validation Server CA, OU=www.digicert.com, O=DigiCert Inc, C=US
>> NAPI's certificate
Peer host: pub.next.nordnet.se
Cipher: TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
Protocol: TLSv1.2
ID: -48447125637079494804142499394251868883865652524411785373650593178669629361431
Session created: 1606142696176
Session accessed: 1606142696176
>> Response from Price Feed
{
"type" : "price",
"data" : {
"i" : "101",
"m" : 11,
"id" : 16101929,
"trade_timestamp" : 1606142694590,
"tick_timestamp" : 1606142694813,
"bid" : 103.95,
"bid_volume" : 2405,
"ask" : 104.0,
"ask_volume" : 7239,
"close" : 104.95,
"high" : 105.4,
"last" : 104.0,
"last_volume" : 10,
"low" : 103.65,
"open" : 104.95,
"vwap" : 104.27,
"turnover" : 2.8558632899E8,
"turnover_volume" : 2752693
}
}
>> Response from Price Feed
{
"type" : "indicator",
"data" : {
"i" : "170.10.OMXS30GI",
"m" : "201",
"tick_timestamp" : 1606142695000,
"high" : 337.37588,
"low" : 334.34677,
"close" : 335.00768,
"last" : 335.13598
}
}
```

## Common issues
* ClassNotFoundException: Check your Maven

## Questions
If you have technical questions then,
1. Check out the code, it is documented
2. Read the [API documentation](https://www.nordnet.se/externalapi/docs)

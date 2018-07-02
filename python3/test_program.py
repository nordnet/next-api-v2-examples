#!/usr/bin/env python3
"""
Copyright 2018 Nordnet Bank AB
Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
"""

import time
import socket
import ssl
import sys
import http.client
import json
import base64
from urllib.parse import urlencode
from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_v1_5

# global variables with static information about nExt API
SERVICE = 'NEXTAPI'
URL = 'api.test.nordnet.se'
API_VERSION = '2'
PUBLIC_KEY_FILENAME = 'NEXTAPI_TEST_public.pem'

"""
Helper function to encrypt with the public key provided
"""


def get_hash(username, password):
    timestamp = int(round(time.time() * 1000))
    timestamp = str(timestamp).encode('ascii')

    username_b64 = base64.b64encode(username.encode('ascii'))
    password_b64 = base64.b64encode(password.encode('ascii'))
    timestamp_b64 = base64.b64encode(timestamp)

    auth_val = username_b64 + b':' + password_b64 + b':' + timestamp_b64
    # Need local copy of public key for NNAPI in PEM format

    try:
        public_key_file_handler = open(PUBLIC_KEY_FILENAME).read()
    except IOError:
        print("Could not find the following file: ",
              "\"", PUBLIC_KEY_FILENAME, "\"", sep="")
        sys.exit()
    rsa_key = RSA.importKey(public_key_file_handler)
    cipher_rsa = PKCS1_v1_5.new(rsa_key)
    encrypted_hash = cipher_rsa.encrypt(auth_val)
    encoded_hash = base64.b64encode(encrypted_hash)

    return encoded_hash


def main():
    # Input username and password for your account in the test system
    if len(sys.argv) != 3:
        raise Exception(
            'To run test_program you need to provide as arguments [USERNAME] [PASSWORD]')
    USERNAME = sys.argv[1]
    PASSWORD = sys.argv[2]
    auth_hash = get_hash(USERNAME, PASSWORD)

    headers = {"Accept": "application/json"}
    conn = http.client.HTTPSConnection(URL)

    # Check NNAPI status
    conn.request('GET', '/next/' + API_VERSION + '/', '', headers)
    r = conn.getresponse()
    response = r.read().decode('utf-8')
    j = json.loads(response)
    print(">> Response from checking NNAPI status")
    print(json.dumps(j, indent=4, sort_keys=True))

    # POST login to NNAPI
    params = urlencode({'service': 'NEXTAPI', 'auth': auth_hash})
    conn.request('POST', '/next/' + API_VERSION + '/login', params, headers)
    r = conn.getresponse()
    response = r.read().decode('utf-8')
    j = json.loads(response)
    print(">> Response from logging into the session")
    print(json.dumps(j, indent=4, sort_keys=True))

    # Store NNAPI login response data
    public_hostname = j["public_feed"]["hostname"]
    public_port = j["public_feed"]["port"]
    our_session_key = j["session_key"]

    # Establish connection to public feed
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    ssl_socket = ssl.wrap_socket(s)
    ssl_socket.connect((public_hostname, public_port))

    # Login to public feed with our session_key from NNAPI response
    cmd = {'cmd': 'login', 'args': {
        'session_key': our_session_key, 'service': 'NEXTAPI'}}
    ssl_socket.send(bytes(json.dumps(cmd) + '\n', 'utf-8'))

    # Subscribe to ERIC B price in public feed
    cmd = {'cmd': 'subscribe', 'args': {'t': 'price', 'm': 11, 'i': '101'}}
    ssl_socket.send(bytes(json.dumps(cmd) + '\n', 'utf-8'))

    # Consume message (price data or heartbeat) from public feed
    #> Note that all the JSON objects end with newline. As such you
    #> need to listen and read from the buffer when a full object 
    #> has been transferred
    time.sleep(0.01)
    output = ssl_socket.recv(1024)
    output = output.decode('utf-8')
    j = json.loads(output)
    print(">> Response from Price Feed")
    print(json.dumps(j, indent=4, sort_keys=True))

    del ssl_socket
    s.close()


main()

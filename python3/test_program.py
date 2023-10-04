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

import base64
import http.client
import json
import socket
import ssl
import sys
import time
from threading import Thread
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding
from urllib.parse import urlencode


# global variables with static information about Nordnet API
API_URL = 'public.nordnet.se'
API_PREFIX = '/api'
API_VERSION = '2'
SERVICE_NAME = 'NEXTAPI'


def get_hash(username, password, public_key_filename):
    """
    Helper function to encrypt with the public key provided
    """
    timestamp = int(round(time.time() * 1000))
    timestamp = str(timestamp).encode('ascii')

    username_b64 = base64.b64encode(username.encode('ascii'))
    password_b64 = base64.b64encode(password.encode('ascii'))
    timestamp_b64 = base64.b64encode(timestamp)

    auth_val = username_b64 + b':' + password_b64 + b':' + timestamp_b64
    # Need local copy of public key for Nordnet API in PEM format

    try:
        public_key_file_handler = open(public_key_filename, "rb").read()
    except IOError:
        print("Could not find the following file: ",
              "\"", public_key_filename, "\"", sep="")
        sys.exit()
    rsa_key = serialization.load_pem_public_key(public_key_file_handler, backend=default_backend())
    encrypted_hash = rsa_key.encrypt(auth_val, padding.PKCS1v15())
    encoded_hash = base64.b64encode(encrypted_hash)

    return encoded_hash


def send_http_request(conn, method, uri, params, headers):
    """
    Send a HTTP request
    """
    conn.request(method, uri, params, headers)
    r = conn.getresponse()
    print("<< HTTP request " + method + " " + uri)
    response = r.read().decode('utf-8')
    j = json.loads(response)
    print(json.dumps(j, indent=4, sort_keys=True))
    return j


def connect_to_feed(public_feed_hostname, public_feed_port):
    """
    Connect to the feed and get back a TCP socket
    """
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    if public_feed_port == 443:
        c = ssl.create_default_context()
        s = c.wrap_socket(s, server_hostname=public_feed_hostname)
    s.connect((public_feed_hostname, public_feed_port))
    return s


def send_cmd_to_socket(socket, cmd):
    """
    Send commands to the feed through the socket
    """
    socket.send(bytes(json.dumps(cmd) + '\n', 'utf-8'))
    print("<< Sending cmd to feed: " + str(cmd))


def try_parse_into_json(string):
    """
    Try parsing the string into JSON objects. Return the unparsable
    parts as buffer
    """
    json_strings = string.split('\n')

    for i in range(0, len(json_strings)):
        try:
            json_data = json.loads(json_strings[i])
            print(">> JSON updates from public feed")
            print(json.dumps(json_data, indent=4, sort_keys=True))
        except:
            ## If this part cannot be parsed into JSON, It's probably not
            ## complete. Stop it right here. Merge the rest of list and
            ## return it, parse it next time
            return ''.join(json_strings[i:])

    ## If all JSONs are successfully parsed, we return an empty buffer
    return ''


def do_receive_from_socket(socket, last_buffer):
    """
    Receive data from the socket, and try to parse it into JSON. Return
    the unparsable parts as buffer
    """
    # Consume message (price data or heartbeat) from public feed
    #> Note that a full message with all the JSON objects ends with a
    #> newline symbol "\n". As such you need to listen and read from
    #> the buffer until a full message has been transferred
    time.sleep(0.01)
    new_data = socket.recv(1024).decode('utf-8')

    string = last_buffer + new_data
    if string != '':
        new_buffer = try_parse_into_json(string)
        return new_buffer

    return ''


def receive_message_from_socket(socket):
    """
    Receive data from the socket and parse it
    """
    print('\nStarting receiving from socket...\n')
    buffer = ''
    while socket.session:
        buffer = do_receive_from_socket(socket, buffer)
    print('\nFinishing receiving from socket...\n')


def main():
    """
    The main function
    """
    # Input username and password for your account in the test system
    if len(sys.argv) != 4:
        raise Exception('To run test_program you need to provide as arguments [USERNAME] [PASSWORD] [PEM_KEY_FILE]')
    username = sys.argv[1]
    password = sys.argv[2]
    public_key_filename = sys.argv[3]
    auth_hash = get_hash(username, password, public_key_filename)

    headers = {"Accept": "application/json"}
    conn = http.client.HTTPSConnection(API_URL)

    # Check Nordnet API status. Check Nordnet API documentation page to verify the path
    print("Checking Nordnet API status...")
    uri = API_PREFIX + '/' + API_VERSION + '/'
    j = send_http_request(conn, 'GET', uri, '', headers)

    # POST login to Nordnet API. Check Nordnet API documentation page to verify the path
    print("Logging in Nordnet API...")
    uri = API_PREFIX + '/' + API_VERSION + '/login'
    params = urlencode({'service': SERVICE_NAME, 'auth': auth_hash})
    j = send_http_request(conn, 'POST', uri, params, headers)

    # Store Nordnet API login response data
    public_feed_hostname = j["public_feed"]["hostname"]
    public_feed_port = j["public_feed"]["port"]
    our_session_key = j["session_key"]

    # Establish connection to public feed
    print("\nConnecting to feed " + str(public_feed_hostname) + ":" + str(public_feed_port) + "...\n")
    feed_socket = connect_to_feed(public_feed_hostname, public_feed_port)
    # feed_socket.shutdown(socket.SHUT_RDWR)
    # feed_socket.close()

    # Start a thread that keeps receiving updates from the TCP socket
    thread = Thread(target=receive_message_from_socket, args=(feed_socket,))
    thread.start()

    # Login to public feed with our session_key from Nordnet API response
    cmd = {"cmd": "login", "args": {"session_key": our_session_key, "service": SERVICE_NAME}}
    send_cmd_to_socket(feed_socket, cmd)

    # Subscribe to ERIC B price in public feed
    cmd = {"cmd": "subscribe", "args": {"t": "price", "m": 11, "i": "101"}}
    send_cmd_to_socket(feed_socket, cmd)

    console_input = input()
    while console_input != "exit":
        try:
            cmd = json.loads(console_input)
            send_cmd_to_socket(feed_socket, cmd)
        except Exception as e:
            print(e)
        console_input = input()

    # End feed_socket.session and then the receiver thread will terminate gracefully
    feed_socket.shutdown(socket.SHUT_RDWR)
    feed_socket.close()

    sys.exit(0)


if __name__ == '__main__':
    main()

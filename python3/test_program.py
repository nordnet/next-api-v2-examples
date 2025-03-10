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
import multiprocessing
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding
from urllib.parse import urlencode


# global variables with static information about Nordnet API
API_URL = 'public.nordnet.se'
API_PREFIX = '/api'
API_VERSION = '2'
SERVICE_NAME = 'NEXTAPI'


def ssh_key_authentication(api_key, private_key_path, service_name="NEXTAPI"):
    """
    Authenticate using the new SSH key-based authentication flow

    Args:
        api_key: The API key provided by Nordnet
        private_key_path: Path to your private key file (e.g., id_ed25519)
        service_name: Service name provided by Nordnet

    Returns:
        The session response data
    """
    # 1. Start authentication challenge
    conn = http.client.HTTPSConnection(API_URL)
    uri = f"{API_PREFIX}/{API_VERSION}/login/start"
    params = urlencode({'api_key': api_key})

    print("Starting authentication challenge...")
    challenge_response = send_http_request(conn, 'POST', uri, params, {"Accept": "application/json"})
    challenge = challenge_response["challenge"]
    print(f"Received challenge: {challenge}")

    # 2. Sign the challenge with the private key
    # Load the private key
    try:
        with open(private_key_path, "rb") as key_file:
            private_key = serialization.load_ssh_private_key(
                key_file.read(),
                password=None,  # If your key has a passphrase, provide it here
                backend=default_backend()
            )
    except IOError:
        print(f"Could not find the following file: \"{private_key_path}\"")
        sys.exit()

    # Sign the challenge
    from cryptography.hazmat.primitives.asymmetric import utils
    from cryptography.hazmat.primitives import hashes

    # Convert challenge string to bytes
    challenge_bytes = challenge.encode('utf-8')

    # Sign the challenge with the private key
    signature = private_key.sign(
        challenge_bytes,
    )

    # Base64 encode the signature
    signature_b64 = base64.b64encode(signature).decode('utf-8')

    # 3. Complete the authentication
    uri = f"{API_PREFIX}/{API_VERSION}/login/verify"
    params = urlencode({
        'service': service_name,
        'api_key': api_key,
        'signature': signature_b64
    })

    print("Completing authentication...")
    login_response = send_http_request(conn, 'POST', uri, params, {"Accept": "application/json"})

    return login_response

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
    while True:
        buffer = do_receive_from_socket(socket, buffer)
    print('\nFinishing receiving from socket...\n')


def main():

    # Input API key string (from uploading your public key on www.nordnet.se|dk|no|fi) and path to your private key
    if len(sys.argv) != 3:
        raise Exception('To run test_program you need to provide as arguments [API_KEY] [PRIVATE_KEY_PATH]')
    api_key = sys.argv[1]
    private_key_path = sys.argv[2]

    # Create an HTTPS connection
    conn = http.client.HTTPSConnection(API_URL)
    headers = {"Accept": "application/json"}

    # Check Nordnet API status. Check Nordnet API documentation page to verify the path
    print("Checking Nordnet API status...")
    uri = API_PREFIX + '/' + API_VERSION + '/'
    j = send_http_request(conn, 'GET', uri, '', headers)

    # Login using SSH key authentication
    j = ssh_key_authentication(api_key, private_key_path)

    # Store Nordnet API login response data
    public_feed_hostname = j["public_feed"]["hostname"]
    public_feed_port = j["public_feed"]["port"]
    our_session_key = j["session_key"]

    print(f"Successfully authenticated. Session key: {our_session_key}")

    # Establish connection to public feed
    print("\nConnecting to feed " + str(public_feed_hostname) + ":" + str(public_feed_port) + "...\n")
    feed_socket = connect_to_feed(public_feed_hostname, public_feed_port)

    # Start a parallel process that keeps receiving updates from the TCP socket
    multiprocessing.set_start_method('fork')
    proc = multiprocessing.Process(target=receive_message_from_socket, args=(feed_socket,))
    proc.start()

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

    feed_socket.shutdown(socket.SHUT_RDWR)
    feed_socket.close()
    proc.terminate()
    sys.exit(0)

main()

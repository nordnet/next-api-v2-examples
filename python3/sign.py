#!/usr/bin/env python3
"""
Copyright 2025 Nordnet Bank AB
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
import sys
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization

def ssh_key_sign(private_key_path, challenge):
    """
    Calculate the signature of a challenge using an ed25519 private key

    Args:
        private_key_path: Path to your private key file (e.g., id_ed25519)
        challenge: The challenge to sign

    Returns:
        The base64 encoded signature
    """
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

    # Convert challenge string to bytes
    challenge_bytes = challenge.encode('utf-8')

    # Sign the challenge with the private key
    signature = private_key.sign(
        challenge_bytes,
    )

    # Base64 encode the signature
    signature_b64 = base64.b64encode(signature).decode('utf-8')

    return signature_b64

def main():
    # Input path to your private key and the challenge to sign
    if len(sys.argv) != 3:
        raise Exception('To run test_program you need to provide as arguments [PRIVATE_KEY_PATH] [CHALLENGE_TO_SIGN]')
    private_key_path = sys.argv[1]
    challenge = sys.argv[2]

    signature = ssh_key_sign(private_key_path, challenge)

    print(signature);
    sys.exit(0)


if __name__ == "__main__":
    main()

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
* [Python 3](https://www.python.org/downloads/) and
  [pip](https://pip.pypa.io/en/stable/installing/) installed

## Install and run
1. Log into the [system](https://api.test.nordnet.se/api-docs/index.html?#) to
   start your session
2. Download the `nordnet/next-api-v2-examples` repo
3. Run and provide your username and password as arguments
```
cd python3
pip3 install -r requirements.txt
./test_program.py [insert username] [insert password]
```

## Common issues
* SyntaxError: check that your Python version is 3 or higher
* NEXT\_LOGIN\_INVALID\_LOGIN\_PARAMETER: double-check your [password](https://api.test.nordnet.se/login)


## Questions
If you have technical questions then,
1. Check out the code, it is documented
2. Read the [documentation](https://api.test.nordnet.se/api-docs/index.html)
3. Ask questions in the [forum](https://api.test.nordnet.se/projects/api/boards)

Otherwise, contact Nordnet trading support with the contact details provided
[here](https://api.test.nordnet.se)

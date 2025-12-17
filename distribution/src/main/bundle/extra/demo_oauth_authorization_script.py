#!/usr/bin/python3

import sys
import json

import sys
import json

def write_response(response):
    json.dump(response, sys.stdout)
    sys.stdout.flush()

def main():
    try:
        input_data = json.load(sys.stdin)

        # Deny if any attribute named 'role' has value 'spy'
        for attribute in input_data.get("attributes", []):
            if attribute.get("name") == "role":
                if "spy" in attribute.get("values", []):
                    write_response({"status": "DENY", "claims": []})
                    return

        # Deny if any identity with value 'evil-user' is present
        for identity in input_data.get("identities", []):
            if identity.get("value") == "evil-user":
                write_response({"status": "DENY", "claims": []})
                return

        # Deny if request clientID is 'evil-client'
        request = input_data.get("request", {})
        if request.get("clientID") == "evil-client":
            write_response({"status": "DENY", "claims": []})
            return

        # Otherwise, proceed and add claims
        write_response({
            "status": "PROCEED",
            "claims": [
                {"name": "example_claim1", "values": ["authorized"]},
                {"name": "organization", "values": ["org1", "org2"]}
            ]
        })

    except Exception:
        # Catch all: deny
        write_response({"status": "DENY", "claims": []})


if __name__ == "__main__":
    main()

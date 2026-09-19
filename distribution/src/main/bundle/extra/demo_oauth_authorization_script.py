#!/usr/bin/python3

import sys
import json

def deny():
    write_response({"status": "DENY", "claims": []})

def proceed(claims: list[dict]):
    write_response({"status": "PROCEED", "claims": claims})

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
                    deny(); return

        # Deny if any identity with value 'evil-user' is present
        for identity in input_data.get("identities", []):
            if identity.get("value") == "evil-user":
                deny(); return

        # Deny if OAuth client id is 'evil-client'
        request = input_data.get("request", {})
        if request.get("clientID") == "evil-client":
            deny(); return

        # Otherwise, proceed and add claims
        proceed([
                {"name": "example_claim1", "values": ["authorized"]},
                {"name": "organization", "values": ["org1", "org2"]}
            ])
        return
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        deny()
        return


if __name__ == "__main__":
    main()

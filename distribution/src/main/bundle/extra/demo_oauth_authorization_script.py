#!/usr/bin/python3

import sys
import json

def main():
    try:
        input_data = json.load(sys.stdin)
        
        # Example checks:
        # Deny if any attribute named 'role' has value 'spy'
        for attribute in input_data.get("attributes", []):
            if attribute.get("name") == "role":
                values = attribute.get("values", [])
                if "spy" in values:
                    response = {
                        "status": "DENY",
                        "claims": []
                    }
                    json.dump(response, sys.stdout)
                    sys.stdout.flush()
                    return
        
        # Deny if any identity with typeId 'banned' is present
        for identity in input_data.get("identities", []):
            if identity.get("value") == "evil-user":
                response = {
                    "status": "DENY",
                    "claims": []
                }
                json.dump(response, sys.stdout)
                sys.stdout.flush()
                return
        
        # Deny if request clientID is 'evil-client'
        request = input_data.get("request", {})
        if request.get("clientID") == "evil-client":
            response = {
                "status": "DENY",
                "claims": []
            }
            json.dump(response, sys.stdout)
            sys.stdout.flush()
            return
        
        # Otherwise, proceed and add a claim
        response = {
            "status": "PROCEED",
            "claims": [
                    {"name": "example_claim1", "values": ["authorized"]},
                    {"name": "organization", "values": ["org1", "org2"]}
            ]
        }
        json.dump(response, sys.stdout)
        sys.stdout.flush()
    except Exception:
        response = {
            "status": "DENY",
            "claims": []
        }
        json.dump(response, sys.stdout)
        sys.stdout.flush()
        
        
if __name__ == "__main__":
    main()

#!/usr/bin/python3

import sys
import json

def main():
    try:
        input_data = json.load(sys.stdin)
        
        # Example checks:
        # Deny if any attribute named 'blocked' is present
        for attribute in input_data.get("attributes", []):
            if attribute.get("name") == "blocked":
                response = {
                    "status": "DENY",
                    "claims": []
                }
                json.dump(response, sys.stdout)
                sys.stdout.flush()
                return
        
        # Deny if any identity with typeId 'banned' is present
        for identity in input_data.get("identities", []):
            if identity.get("typeId") == "banned":
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
                    {"type": "example_claim", "value": "authorized"}
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
